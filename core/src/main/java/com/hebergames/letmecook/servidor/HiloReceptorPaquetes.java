package com.hebergames.letmecook.servidor;

import com.badlogic.gdx.Gdx;
import com.hebergames.letmecook.red.PaqueteRed;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class HiloReceptorPaquetes implements Runnable {
    private final ServidorJuego servidor;
    private final DatagramSocket socket;
    private volatile boolean ejecutando;

    public HiloReceptorPaquetes(ServidorJuego servidor, DatagramSocket socket) {
        this.servidor = servidor;
        this.socket = socket;
        this.ejecutando = true;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[4096];

        while (ejecutando) {
            try {
                DatagramPacket paqueteRecibido = new DatagramPacket(buffer, buffer.length);
                socket.receive(paqueteRecibido);

                final PaqueteRed paquete = PaqueteRed.deserializar(paqueteRecibido.getData());
                final InetAddress direccion = paqueteRecibido.getAddress();
                final int puerto = paqueteRecibido.getPort();

                // Usar postRunnable para procesar en el hilo de LibGDX
                Gdx.app.postRunnable(() -> {
                    servidor.procesarPaquete(paquete, direccion, puerto);
                });
                servidor.procesarPaquete(paquete, paqueteRecibido.getAddress(), paqueteRecibido.getPort());

            } catch (Exception e) {
                if (ejecutando) {
                    System.err.println("Error recibiendo paquete: " + e.getMessage());
                }
            }
        }
    }

    public void detener() {
        ejecutando = false;
    }
}
