package com.hebergames.letmecook.servidor;

import com.google.gson.Gson;
import com.badlogic.gdx.math.Rectangle;

import java.io.FileReader;
import java.util.ArrayList;

/**
 * Versión del mapa para el servidor - Solo datos, sin texturas
 */
public class MapaDatosServidor {

    public static class DatosMapa {
        public String nombre;
        public ArrayList<RectanguloData> colisionables;
        public ArrayList<RectanguloData> interactuables;
        public ArrayList<EstacionData> estaciones;
        public ArrayList<RectanguloData> spawnsJugadores;
    }

    public static class RectanguloData {
        public float x, y, width, height;
        public String nombre;

        public Rectangle toRectangle() {
            return new Rectangle(x, y, width, height);
        }
    }

    public static class EstacionData {
        public String tipo;
        public float x, y, width, height;

        public Rectangle toRectangle() {
            return new Rectangle(x, y, width, height);
        }
    }

    private DatosMapa datos;

    public MapaDatosServidor(String rutaJson) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(rutaJson)) {
            datos = gson.fromJson(reader, DatosMapa.class);
        } catch (Exception e) {
            System.err.println("Error cargando mapa JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ArrayList<Rectangle> getColisionables() {
        ArrayList<Rectangle> rects = new ArrayList<>();
        for (RectanguloData rd : datos.colisionables) {
            rects.add(rd.toRectangle());
        }
        return rects;
    }

    public ArrayList<Rectangle> getInteractuables() {
        ArrayList<Rectangle> rects = new ArrayList<>();
        for (RectanguloData rd : datos.interactuables) {
            rects.add(rd.toRectangle());
        }
        return rects;
    }

    public Rectangle getSpawnJugador(String nombre) {
        for (RectanguloData spawn : datos.spawnsJugadores) {
            if (spawn.nombre.equals(nombre)) {
                return spawn.toRectangle();
            }
        }
        return null;
    }

    public ArrayList<EstacionData> getEstacionesData() {
        return datos.estaciones;
    }

    public String getNombre() {
        return datos.nombre;
    }
}
