package com.hebergames.letmecook.servidor;

import java.net.InetAddress;

public class InfoJugador {
    public InetAddress direccion;
    public int puerto;
    public int id;
    public long ultimoPing;

    public InfoJugador(InetAddress direccion, int puerto, int id) {
        this.direccion = direccion;
        this.puerto = puerto;
        this.id = id;
        this.ultimoPing = System.currentTimeMillis();
    }
}
