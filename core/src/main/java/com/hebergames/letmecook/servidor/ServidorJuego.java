package com.hebergames.letmecook.servidor;

import com.badlogic.gdx.Gdx;
import com.hebergames.letmecook.red.PaqueteRed;
import com.hebergames.letmecook.red.paquetes.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServidorJuego {
    private static final int PUERTO = 25565;
    private static final int TICK_RATE = 60;
    private static final int MAX_JUGADORES = 2;

    private DatagramSocket socket;
    private LogicaServidor logicaJuego;
    private Map<String, InfoJugador> jugadoresConectados;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> tareaVerificacionConexiones;
    private ScheduledFuture<?> tareaEnvioEstados;
    private HiloReceptorPaquetes hiloReceptor;
    private static final long TIMEOUT_CONEXION = 20000;

    private Thread hiloActualizacion;
    private volatile boolean ejecutando = false;
    private long ultimaActualizacion = 0;
    private static final long INTERVALO_TICK = 1000 / TICK_RATE; // ~33ms para 30 TPS

    public ServidorJuego() {
        jugadoresConectados = new ConcurrentHashMap<>();
        scheduler = Executors.newScheduledThreadPool(2);
    }

    public void iniciar() {
        try {
            socket = new DatagramSocket(PUERTO);
            ejecutando = true;

            System.out.println("Servidor iniciado en puerto " + PUERTO);
            System.out.println("Esperando jugadores...");

            hiloReceptor = new HiloReceptorPaquetes(this, socket);
            new Thread(hiloReceptor, "HiloReceptorPaquetes").start();

            // Hilo de actualización del juego
            hiloActualizacion = new Thread(this::loopActualizacion, "HiloActualizacionServidor");
            hiloActualizacion.start();

        } catch (Exception e) {
            System.err.println("Error al iniciar servidor: " + e.getMessage());
        }
    }

    private void loopActualizacion() {
        while (ejecutando) {
            long ahora = System.currentTimeMillis();

            if (ahora - ultimaActualizacion >= INTERVALO_TICK) {
                final float delta = (ahora - ultimaActualizacion) / 1000f;
                ultimaActualizacion = ahora;

                // 🔍 Verificar desconexión durante el juego (SOLO UNA VEZ)
                if (logicaJuego != null && jugadoresConectados.size() < MAX_JUGADORES) {
                    System.out.println("⚠️ Jugador desconectado durante el juego");

                    // 🔒 Prevenir detecciones múltiples
                    final LogicaServidor logicaTemporal = logicaJuego;
                    logicaJuego = null; // Detener actualizaciones inmediatamente

                    Gdx.app.postRunnable(() -> {
                        if (logicaTemporal != null) {
                            logicaTemporal.finalizarPorDesconexion("Un jugador abandonó la partida");
                        }

                        // Enviar notificación al jugador restante
                        PaqueteDesconexion paqueteDesc = new PaqueteDesconexion(0, "JUGADOR_ABANDONO");
                        for (InfoJugador jugador : jugadoresConectados.values()) {
                            try {
                                enviarPaquete(paqueteDesc, jugador.direccion, jugador.puerto);
                                Thread.sleep(50);
                                enviarPaquete(paqueteDesc, jugador.direccion, jugador.puerto);
                            } catch (Exception e) {
                                System.err.println("Error notificando desconexión: " + e.getMessage());
                            }
                        }

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        reiniciarServidor();
                    });
                    continue; // Saltar esta iteración
                }

                // Actualizar lógica del juego SOLO si todo está OK
                if (logicaJuego != null && jugadoresConectados.size() == MAX_JUGADORES) {
                    Gdx.app.postRunnable(() -> {
                        try {
                            // 🔒 Verificar nuevamente por seguridad
                            if (logicaJuego != null) {
                                logicaJuego.actualizar(delta);
                                enviarEstadoATodos();
                            }
                        } catch (Exception e) {
                            System.err.println("Error en actualización: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                }
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void procesarPaquete(PaqueteRed paquete, InetAddress direccion, int puerto) {
        switch (paquete.getTipo()) {
            case CONEXION:
                Gdx.app.postRunnable(() -> manejarConexion(direccion, puerto));
                break;

            case INPUT_JUGADOR:
                PaqueteInput input = (PaqueteInput) paquete;
                Gdx.app.postRunnable(() -> {
                    if (logicaJuego != null) {
                        logicaJuego.procesarInput(input);
                    }
                });
                actualizarPing(direccion, puerto);
                break;

            case PING:
                actualizarPing(direccion, puerto);
                break;

            case DESCONEXION:
                // 🔥 CRÍTICO: Manejar desconexión inmediatamente
                PaqueteDesconexion paqueteDesc = (PaqueteDesconexion) paquete;
                Gdx.app.postRunnable(() -> manejarDesconexionVoluntaria(direccion, puerto, paqueteDesc));
                break;

            case INTERACCION:
                PaqueteInteraccion interaccion = (PaqueteInteraccion) paquete;
                Gdx.app.postRunnable(() -> {
                    if (logicaJuego != null) {
                        logicaJuego.procesarInteraccion(interaccion);
                    }
                });
                actualizarPing(direccion, puerto);
                break;

            case INICIO_PARTIDA:
                break;
            case CAMBIO_NIVEL:
                break;
        }
    }

    private void manejarDesconexionVoluntaria(InetAddress direccion, int puerto, PaqueteDesconexion paquete) {
        String key = direccion.getHostAddress() + ":" + puerto;
        InfoJugador jugador = jugadoresConectados.remove(key);

        if (jugador != null) {
            System.out.println("👋 Jugador " + jugador.id + " se desconectó voluntariamente");

            // 🔥 Si hay un juego en curso, finalizarlo
            if (logicaJuego != null) {
                // Anular lógica para detener actualizaciones
                final LogicaServidor logicaTemporal = logicaJuego;
                logicaJuego = null;

                if (logicaTemporal != null) {
                    logicaTemporal.finalizarPorDesconexion("Jugador " + jugador.id + " abandonó la partida");
                }

                // Notificar al otro jugador
                PaqueteDesconexion notificacion = new PaqueteDesconexion(0, "JUGADOR_ABANDONO");
                for (InfoJugador otroJugador : jugadoresConectados.values()) {
                    try {
                        enviarPaquete(notificacion, otroJugador.direccion, otroJugador.puerto);
                        Thread.sleep(50);
                        enviarPaquete(notificacion, otroJugador.direccion, otroJugador.puerto);
                    } catch (Exception e) {
                        System.err.println("Error notificando desconexión: " + e.getMessage());
                    }
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Reiniciar servidor
                reiniciarServidor();
            }
        }
    }

    private void manejarConexion(InetAddress direccion, int puerto) {
        String key = direccion.getHostAddress() + ":" + puerto;

        if (jugadoresConectados.containsKey(key)) {
            System.out.println("El jugador " + key + " ya esta conectado. Ignorando paquete de conexion duplicado.");
            return;
        }

        if (jugadoresConectados.size() >= MAX_JUGADORES) {
            enviarPaquete(new PaqueteConexion(-1, false), direccion, puerto);
            return;
        }

        int idJugador = jugadoresConectados.size() + 1;
        InfoJugador jugador = new InfoJugador(direccion, puerto, idJugador);
        jugadoresConectados.put(key, jugador);

        System.out.println("Jugador " + idJugador + " conectado desde " + key);

        enviarPaquete(new PaqueteConexion(idJugador, true), direccion, puerto);

        if (jugadoresConectados.size() == MAX_JUGADORES) {
            System.out.println("2 jugadores conectados. Iniciando juego...");
            iniciarJuego();
        }
    }

    private void iniciarJuego() {
        System.out.println("Inicializando lógica del juego...");

        Gdx.app.postRunnable(() -> {
            logicaJuego = new LogicaServidor();

            try {
                logicaJuego.inicializar();
                ultimaActualizacion = System.currentTimeMillis();
                System.out.println("Juego inicializado correctamente.");

                // 👇 NUEVO - Enviar configuración de la partida a ambos clientes
                PaqueteInicioPartida paqueteInicio = logicaJuego.generarPaqueteInicio();
                for (InfoJugador jugador : jugadoresConectados.values()) {
                    enviarPaquete(paqueteInicio, jugador.direccion, jugador.puerto);
                }
                System.out.println("Configuración de partida enviada a todos los clientes");

            } catch (Exception e) {
                System.err.println("Error al inicializar juego: " + e.getMessage());
                e.printStackTrace();
                logicaJuego = null;
            }
        });
    }

    public void reiniciarServidor() {
        System.out.println("🔄 Reiniciando servidor para nueva partida...");

        // 🔒 CRÍTICO: Detener actualizaciones primero
        LogicaServidor logicaTemporal = logicaJuego;
        logicaJuego = null; // ⚠️ Esto detiene el loop de actualización

        // Limpiar jugadores
        jugadoresConectados.clear();

        // Resetear lógica del juego
        if (logicaTemporal != null) {

        }

        // Resetear estado
        ultimaActualizacion = 0;

        System.out.println("✅ Servidor reiniciado. Esperando nuevos jugadores...");
    }

    private void enviarEstadoATodos() {
        if (logicaJuego == null || !logicaJuego.estanJugadoresListos()) return;

        try {
            PaqueteEstado estado = logicaJuego.generarEstado();

            if (estado.isJuegoTerminado() && !estado.getRazonFin().isEmpty()) {
                // Despido - enviar estado y desconectar clientes
                for (InfoJugador jugador : jugadoresConectados.values()) {
                    enviarPaquete(estado, jugador.direccion, jugador.puerto);
                }

                Thread.sleep(500);
                desconectarTodosLosJugadores("FIN_PARTIDA");
                reiniciarServidor();

            } else if (estado.isJuegoTerminado()) {
                PaqueteCambioNivel paqueteCambio = logicaJuego.generarPaqueteCambioNivel();

                if (paqueteCambio != null) {
                    // Hay siguiente nivel
                    for (InfoJugador jugador : jugadoresConectados.values()) {
                        enviarPaquete(paqueteCambio, jugador.direccion, jugador.puerto);
                    }
                    Thread.sleep(100);
                    logicaJuego.reiniciarParaNuevoNivel();
                } else {
                    // Partida completada - enviar estado final y desconectar
                    for (InfoJugador jugador : jugadoresConectados.values()) {
                        enviarPaquete(estado, jugador.direccion, jugador.puerto);
                    }

                    Thread.sleep(500);
                    desconectarTodosLosJugadores("FIN_PARTIDA");
                    reiniciarServidor();
                }
            } else {
                // Juego en curso
                for (InfoJugador jugador : jugadoresConectados.values()) {
                    enviarPaquete(estado, jugador.direccion, jugador.puerto);
                }
            }
        } catch (Exception e) {
            System.err.println("Error enviando estado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void desconectarTodosLosJugadores(String razon) {
        System.out.println("🔌 Desconectando todos los jugadores. Razón: " + razon);

        PaqueteDesconexion paqueteDesc = new PaqueteDesconexion(0, razon);

        for (InfoJugador jugador : jugadoresConectados.values()) {
            try {
                // Enviar múltiples veces para asegurar recepción
                enviarPaquete(paqueteDesc, jugador.direccion, jugador.puerto);
                Thread.sleep(50);
                enviarPaquete(paqueteDesc, jugador.direccion, jugador.puerto);
            } catch (Exception e) {
                System.err.println("Error desconectando jugador " + jugador.id + ": " + e.getMessage());
            }
        }

        jugadoresConectados.clear();
        System.out.println("✅ Todos los jugadores desconectados");
    }

    private void notificarDesconexionJugador(int idJugador, String razon) {
        System.out.println("Notificando desconexión del jugador " + idJugador + " por: " + razon);

        if (logicaJuego != null) {
            logicaJuego.finalizarPorDesconexion("Jugador " + idJugador + " desconectado");
        }

        PaqueteDesconexion paqueteDesc = new PaqueteDesconexion(idJugador, razon);
        for (InfoJugador jugador : jugadoresConectados.values()) {
            try {
                enviarPaquete(paqueteDesc, jugador.direccion, jugador.puerto);
                Thread.sleep(50);
                enviarPaquete(paqueteDesc, jugador.direccion, jugador.puerto);
            } catch (Exception e) {
                System.err.println("Error notificando desconexión: " + e.getMessage());
            }
        }
    }

    private void enviarPaquete(PaqueteRed paquete, InetAddress direccion, int puerto) {
        try {
            byte[] datos = paquete.serializar();
            DatagramPacket datagramPacket = new DatagramPacket(datos, datos.length, direccion, puerto);
            socket.send(datagramPacket);
        } catch (Exception e) {
            System.err.println("Error enviando paquete: " + e.getMessage());
        }
    }

    private void actualizarPing(InetAddress direccion, int puerto) {
        String key = direccion.getHostAddress() + ":" + puerto;
        InfoJugador jugador = jugadoresConectados.get(key);
        if (jugador != null) {
            jugador.ultimoPing = System.currentTimeMillis();
        }
    }

    public void detener() {
        System.out.println("Cerrando servidor...");
        ejecutando = false;

        PaqueteDesconexion paqueteCierre = new PaqueteDesconexion(0, "CIERRE_SERVIDOR");
        for (InfoJugador jugador : jugadoresConectados.values()) {
            try {
                enviarPaquete(paqueteCierre, jugador.direccion, jugador.puerto);
                Thread.sleep(50);
                enviarPaquete(paqueteCierre, jugador.direccion, jugador.puerto);
                Thread.sleep(50);
                enviarPaquete(paqueteCierre, jugador.direccion, jugador.puerto);
            } catch (Exception e) {
                System.err.println("Error notificando cierre a jugador: " + e.getMessage());
            }
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (hiloReceptor != null) {
            hiloReceptor.detener();
        }

        if (hiloActualizacion != null) {
            hiloActualizacion.interrupt();
            try {
                hiloActualizacion.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (socket != null) {
            socket.close();
        }

        System.out.println("Servidor cerrado completamente");
    }
}
