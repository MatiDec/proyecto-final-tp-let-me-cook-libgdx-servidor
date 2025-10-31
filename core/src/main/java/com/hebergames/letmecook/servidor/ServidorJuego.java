package com.hebergames.letmecook.servidor;

import com.badlogic.gdx.Gdx;
import com.hebergames.letmecook.red.PaqueteRed;
import com.hebergames.letmecook.red.paquetes.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServidorJuego {
    private static final int PUERTO = 25565;
    private static final int TICK_RATE = 30; // 30 actualizaciones por segundo
    private static final int MAX_JUGADORES = 2;

    private DatagramSocket socket;
    private boolean ejecutando;
    private LogicaServidor logicaJuego;
    private Map<String, InfoJugador> jugadoresConectados;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> tareaVerificacionConexiones;
    private ScheduledFuture<?> tareaEnvioEstados;
    private HiloReceptorPaquetes hiloReceptor;
    private static final long TIMEOUT_CONEXION = 5000;

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

            // Hilo para recibir paquetes
            hiloReceptor = new HiloReceptorPaquetes(this, socket);
            new Thread(hiloReceptor, "HiloReceptorPaquetes").start();

            // Tarea para enviar estado del juego
            tareaEnvioEstados = scheduler.scheduleAtFixedRate(this::enviarEstadoJuego,
                0, 1000 / TICK_RATE, TimeUnit.MILLISECONDS);

            // Tarea de verificación de conexiones
            tareaVerificacionConexiones = scheduler.scheduleAtFixedRate(
                this::verificarConexiones,
                0, 1000, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            System.err.println("Error al iniciar servidor: " + e.getMessage());
        }
    }

    void procesarPaquete(PaqueteRed paquete, InetAddress direccion, int puerto) {
        switch (paquete.getTipo()) {
            case CONEXION:
                manejarConexion(direccion, puerto);
                break;

            case INPUT_JUGADOR:
                PaqueteInput input = (PaqueteInput) paquete;
                if (logicaJuego != null) {
                    logicaJuego.procesarInput(input);
                }
                actualizarPing(direccion, puerto);
                break;

            case PING:
                actualizarPing(direccion, puerto);
                break;

            case DESCONEXION:
                manejarDesconexion(direccion, puerto);
                break;
        }
    }

    private void manejarConexion(InetAddress direccion, int puerto) {
        String key = direccion.getHostAddress() + ":" + puerto;

        if (jugadoresConectados.size() >= MAX_JUGADORES) {
            enviarPaquete(new PaqueteConexion(-1, false), direccion, puerto);
            return;
        }

        int idJugador = jugadoresConectados.size() + 1;
        InfoJugador jugador = new InfoJugador(direccion, puerto, idJugador);
        jugadoresConectados.put(key, jugador);

        System.out.println("Jugador " + idJugador + " conectado desde " + key);

        enviarPaquete(new PaqueteConexion(idJugador, true), direccion, puerto);

        // Si ya hay 2 jugadores, iniciar el juego
        if (jugadoresConectados.size() == MAX_JUGADORES) {
            System.out.println("2 jugadores conectados. Iniciando juego...");
            iniciarJuego();
        }
    }

    private void iniciarJuego() {
        System.out.println("Inicializando lógica del juego...");

        // La inicialización debe hacerse en el hilo de renderizado de LibGDX
        // porque crea texturas y otros recursos de OpenGL
        Gdx.app.postRunnable(() -> {
            logicaJuego = new LogicaServidor();

            try {
                logicaJuego.inicializar();
                System.out.println("Juego inicializado correctamente. Comenzando envío de estados.");
            } catch (Exception e) {
                System.err.println("Error al inicializar juego: " + e.getMessage());
                e.printStackTrace();
                logicaJuego = null;
            }
        });
    }

    private void enviarEstadoJuego() {
        if (logicaJuego == null) return;

        // Solo actualizar si hay 2 jugadores conectados y los jugadores están listos
        if (jugadoresConectados.size() < MAX_JUGADORES) return;
        if (!logicaJuego.estanJugadoresListos()) return;

        try {
            logicaJuego.actualizar(1f / TICK_RATE);
            PaqueteEstado estado = logicaJuego.generarEstado();

            for (InfoJugador jugador : jugadoresConectados.values()) {
                enviarPaquete(estado, jugador.direccion, jugador.puerto);
            }

        } catch (Exception e) {
            System.err.println("Error generando/enviando estado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void verificarConexiones() {
        long tiempoActual = System.currentTimeMillis();
        List<String> jugadoresDesconectados = new ArrayList<>();

        for (Map.Entry<String, InfoJugador> entry : jugadoresConectados.entrySet()) {
            if (tiempoActual - entry.getValue().ultimoPing > TIMEOUT_CONEXION) {
                jugadoresDesconectados.add(entry.getKey());
            }
        }

        for (String key : jugadoresDesconectados) {
            InfoJugador jugador = jugadoresConectados.remove(key);
            if (jugador != null) {
                System.out.println("Jugador " + jugador.id + " desconectado por timeout");
                notificarDesconexionJugador(jugador.id, "TIMEOUT");
            }
        }
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

    private void manejarDesconexion(InetAddress direccion, int puerto) {
        String key = direccion.getHostAddress() + ":" + puerto;
        InfoJugador jugador = jugadoresConectados.remove(key);
        if (jugador != null) {
            System.out.println("Jugador " + jugador.id + " desconectado voluntariamente");
            notificarDesconexionJugador(jugador.id, "JUGADOR_ABANDONO");
        }
    }

    public void detener() {
        System.out.println("Cerrando servidor...");
        ejecutando = false;

        // Notificar a todos los clientes que el servidor se cierra
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

        // Esperar un momento para que los paquetes se envíen
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Detener hilo receptor
        if (hiloReceptor != null) {
            hiloReceptor.detener();
        }

        // Cancelar tareas programadas
        if (tareaVerificacionConexiones != null) {
            tareaVerificacionConexiones.cancel(true);
        }
        if (tareaEnvioEstados != null) {
            tareaEnvioEstados.cancel(true);
        }

        scheduler.shutdown();

        if (socket != null) {
            socket.close();
        }

        System.out.println("Servidor cerrado completamente");
    }
}
