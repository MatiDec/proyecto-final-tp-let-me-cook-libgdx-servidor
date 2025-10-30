package com.hebergames.letmecook.servidor;

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

    private static class InfoJugador {
        InetAddress direccion;
        int puerto;
        int id;
        long ultimoPing;

        InfoJugador(InetAddress direccion, int puerto, int id) {
            this.direccion = direccion;
            this.puerto = puerto;
            this.id = id;
            this.ultimoPing = System.currentTimeMillis();
        }
    }

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
            new Thread(this::recibirPaquetes).start();

            // Hilo para enviar estado del juego
            scheduler.scheduleAtFixedRate(this::enviarEstadoJuego,
                0, 1000 / TICK_RATE, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            System.err.println("Error al iniciar servidor: " + e.getMessage());
        }
    }

    private void recibirPaquetes() {
        byte[] buffer = new byte[4096];

        while (ejecutando) {
            try {
                DatagramPacket paqueteRecibido = new DatagramPacket(buffer, buffer.length);
                socket.receive(paqueteRecibido);

                PaqueteRed paquete = PaqueteRed.deserializar(paqueteRecibido.getData());
                procesarPaquete(paquete, paqueteRecibido.getAddress(), paqueteRecibido.getPort());

            } catch (Exception e) {
                if (ejecutando) {
                    System.err.println("Error recibiendo paquete: " + e.getMessage());
                }
            }
        }
    }

    private void procesarPaquete(PaqueteRed paquete, InetAddress direccion, int puerto) {
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
        logicaJuego = new LogicaServidor();

        try {
            logicaJuego.inicializar();
            System.out.println("Juego inicializado correctamente. Comenzando envío de estados.");
        } catch (Exception e) {
            System.err.println("Error al inicializar juego: " + e.getMessage());
            e.printStackTrace();
            logicaJuego = null;
        }
    }

    private void enviarEstadoJuego() {
        if (logicaJuego == null) {
            return;
        }

        if (jugadoresConectados.size() < MAX_JUGADORES) {
            return;
        }

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
            System.out.println("Jugador " + jugador.id + " desconectado");
        }
    }

    public void detener() {
        ejecutando = false;
        scheduler.shutdown();
        if (socket != null) {
            socket.close();
        }
    }

    public static void main(String[] args) {
        ServidorJuego servidor = new ServidorJuego();
        servidor.iniciar();
    }
}
