package com.hebergames.letmecook.utiles;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Rectangle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.util.ArrayList;

/**
 * HERRAMIENTA DE UNA SOLA VEZ
 * Ejecuta esto para cada mapa TMX y genera su JSON correspondiente
 */
public class ExtractorMapaAJson {

    public static class DatosMapa {
        public String nombre;
        public ArrayList<RectanguloData> colisionables;
        public ArrayList<RectanguloData> interactuables;
        public ArrayList<EstacionData> estaciones;
        public ArrayList<RectanguloData> spawnsJugadores;

        public DatosMapa() {
            colisionables = new ArrayList<>();
            interactuables = new ArrayList<>();
            estaciones = new ArrayList<>();
            spawnsJugadores = new ArrayList<>();
        }
    }

    public static class RectanguloData {
        public float x, y, width, height;
        public String nombre;

        public RectanguloData(Rectangle rect, String nombre) {
            this.x = rect.x;
            this.y = rect.y;
            this.width = rect.width;
            this.height = rect.height;
            this.nombre = nombre;
        }
    }

    public static class EstacionData {
        public String tipo;
        public float x, y, width, height;

        public EstacionData(String tipo, Rectangle rect) {
            this.tipo = tipo;
            this.x = rect.x;
            this.y = rect.y;
            this.width = rect.width;
            this.height = rect.height;
        }
    }

    public static void extraerMapa(String rutaTmx, String rutaJsonSalida, String nombreMapa) {
        TmxMapLoader loader = new TmxMapLoader();
        TiledMap mapa = loader.load(rutaTmx);

        DatosMapa datos = new DatosMapa();
        datos.nombre = nombreMapa;

        // Extraer colisionables
        if (mapa.getLayers().get("Colisionables") != null) {
            MapObjects objetos = mapa.getLayers().get("Colisionables").getObjects();
            for (MapObject objeto : objetos) {
                if (objeto instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) objeto).getRectangle();
                    datos.colisionables.add(new RectanguloData(rect, "colision"));
                }
            }
        }

        // Extraer interactuables/estaciones
        if (mapa.getLayers().get("Interactuables") != null) {
            MapObjects objetos = mapa.getLayers().get("Interactuables").getObjects();
            for (MapObject objeto : objetos) {
                String tipo = objeto.getName();
                Rectangle rect = null;

                if (objeto instanceof RectangleMapObject) {
                    rect = ((RectangleMapObject) objeto).getRectangle();
                } else {
                    float x = (Float) objeto.getProperties().get("x");
                    float y = (Float) objeto.getProperties().get("y");
                    float width = (Float) objeto.getProperties().get("width");
                    float height = (Float) objeto.getProperties().get("height");
                    rect = new Rectangle(x, y, width, height);
                }

                datos.interactuables.add(new RectanguloData(rect, tipo));
                datos.estaciones.add(new EstacionData(tipo, rect));
            }
        }

        // Extraer spawns de jugadores
        if (mapa.getLayers().get("Jugadores") != null) {
            MapObjects objetos = mapa.getLayers().get("Jugadores").getObjects();
            for (MapObject objeto : objetos) {
                if (objeto instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) objeto).getRectangle();
                    datos.spawnsJugadores.add(new RectanguloData(rect, objeto.getName()));
                }
            }
        }

        // Guardar como JSON
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(rutaJsonSalida)) {
            gson.toJson(datos, writer);
            System.out.println("Mapa extraído exitosamente a: " + rutaJsonSalida);
        } catch (Exception e) {
            System.err.println("Error guardando JSON: " + e.getMessage());
        }

        mapa.dispose();
    }

    public static void main(String[] args) {
        // Ejecuta esto para cada mapa
        extraerMapa(
            "core/src/main/java/com/hebergames/letmecook/recursos/mapas/Sucursal_1.tmx",
            "core/src/main/java/com/hebergames/letmecook/recursos/mapas/sucursal_1.json",
            "Sucursal 1"
        );

        extraerMapa(
            "core/src/main/java/com/hebergames/letmecook/recursos/mapas/Sucursal_2.tmx",
            "core/src/main/java/com/hebergames/letmecook/recursos/mapas/sucursal_2.json",
            "Sucursal 2"
        );

        // Añade más según tus mapas...
    }
}
