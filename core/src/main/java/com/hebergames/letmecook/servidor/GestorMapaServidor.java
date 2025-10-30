package com.hebergames.letmecook.servidor;

import com.badlogic.gdx.math.Rectangle;
import com.hebergames.letmecook.entidades.Jugador;
import com.hebergames.letmecook.estaciones.*;
import com.hebergames.letmecook.estaciones.conmenu.*;
import com.hebergames.letmecook.estaciones.interaccionclientes.*;
import com.hebergames.letmecook.estaciones.procesadoras.*;

import java.util.ArrayList;

/**
 * Gestor de mapa para el servidor - Carga desde JSON sin LibGDX
 */
public class GestorMapaServidor {

    private MapaDatosServidor mapaActual;
    private ArrayList<EstacionTrabajo> estaciones;

    public GestorMapaServidor() {
        this.estaciones = new ArrayList<>();
    }

    public void cargarMapa(String rutaJson) {
        mapaActual = new MapaDatosServidor(rutaJson);
        estaciones = crearEstaciones();
    }

    private ArrayList<EstacionTrabajo> crearEstaciones() {
        ArrayList<EstacionTrabajo> lista = new ArrayList<>();

        for (MapaDatosServidor.EstacionData estData : mapaActual.getEstacionesData()) {
            Rectangle rect = estData.toRectangle();
            String tipo = estData.tipo;

            EstacionTrabajo estacion = null;

            switch (tipo) {
                case "Horno":
                    estacion = new Horno(rect);
                    break;
                case "Cafetera":
                    estacion = new Cafetera(rect);
                    break;
                case "Freidora":
                    estacion = new Freidora(rect);
                    break;
                case "Tostadora":
                    estacion = new Tostadora(rect);
                    break;
                case "Fuente":
                    estacion = new Fuente(rect);
                    break;
                case "AreaEnsamblaje":
                    estacion = new Mesa(rect);
                    break;
                case "Heladera":
                    estacion = new Heladera(rect);
                    break;
                case "CajaRegistradora":
                    estacion = new CajaRegistradora(rect);
                    break;
                case "MesaRetiro":
                    estacion = new MesaRetiro(rect);
                    break;
                case "Basurero":
                    estacion = new Basurero(rect);
                    break;
                case "CajaVirtual":
                    estacion = new CajaVirtual(rect);
                    break;
                case "MaquinaEnvasadora":
                    estacion = new MaquinaEnvasadora(rect);
                    break;
            }

            if (estacion != null) {
                lista.add(estacion);
            }
        }

        return lista;
    }

    public void asignarColisionesYInteracciones(Jugador jugador) {
        jugador.setColisionables(mapaActual.getColisionables());
        jugador.setInteractuables(mapaActual.getInteractuables());
    }

    public Rectangle getPuntoSpawn(String nombreJugador) {
        return mapaActual.getSpawnJugador(nombreJugador);
    }

    public ArrayList<EstacionTrabajo> getEstaciones() {
        return this.estaciones;
    }

    public ArrayList<Rectangle> getTilesCaminables() {
        // Simplificado para servidor - calcula basándose en colisionables
        // O añade este dato al JSON si lo necesitas
        return new ArrayList<>();
    }
}
