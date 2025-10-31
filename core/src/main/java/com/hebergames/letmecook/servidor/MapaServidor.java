package com.hebergames.letmecook.servidor;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.hebergames.letmecook.estaciones.Basurero;
import com.hebergames.letmecook.estaciones.EstacionTrabajo;
import com.hebergames.letmecook.estaciones.interaccionclientes.CajaRegistradora;
import com.hebergames.letmecook.estaciones.interaccionclientes.CajaVirtual;
import com.hebergames.letmecook.estaciones.interaccionclientes.MesaRetiro;
import com.hebergames.letmecook.mapa.GestorMapa;
import com.hebergames.letmecook.red.ServerFileHandleResolver;
import com.hebergames.letmecook.servidor.estacionesheadless.*;

import java.io.File;
import java.util.ArrayList;

public class MapaServidor {
    private final TiledMap MAPA;
    private final String NOMBRE_SUCURSAL;

    public MapaServidor(String ruta, String nombreSucursal) {
        this.NOMBRE_SUCURSAL = nombreSucursal;

        File archivoMapa = new File(ruta);
        if (!archivoMapa.exists()) {
            throw new RuntimeException("No se encontró el mapa: " + archivoMapa.getAbsolutePath());
        }

        try {
            HeadlessTmxMapLoader loader = new HeadlessTmxMapLoader(new ServerFileHandleResolver());
            this.MAPA = loader.load(archivoMapa.getAbsolutePath());
            System.out.println("✅ Mapa cargado en servidor (headless): " + nombreSucursal);
        } catch (Exception e) {
            throw new RuntimeException("Error cargando mapa en servidor: " + ruta, e);
        }
    }

    private ArrayList<Rectangle> obtenerRectangulosDeCapa(String nombreCapa) {
        ArrayList<Rectangle> rectangulos = new ArrayList<>();
        MapObjects objetos = MAPA.getLayers().get(nombreCapa).getObjects();

        for (MapObject objeto : objetos) {
            if (objeto instanceof RectangleMapObject) {
                rectangulos.add(((RectangleMapObject) objeto).getRectangle());
            }
        }

        return rectangulos;
    }

    public ArrayList<Rectangle> getRectangulosColision() {
        return obtenerRectangulosDeCapa("Colisionables");
    }

    public ArrayList<Rectangle> getRectangulosInteractuables() {
        return obtenerRectangulosDeCapa("Interactuables");
    }

    public ArrayList<EstacionTrabajo> getEstacionesTrabajo() {
        ArrayList<EstacionTrabajo> estaciones = new ArrayList<>();
        MapObjects objetos = MAPA.getLayers().get("Interactuables").getObjects();

        for (MapObject objeto : objetos) {
            String tipo = objeto.getName();
            Rectangle rect;

            if (objeto instanceof RectangleMapObject) {
                rect = ((RectangleMapObject) objeto).getRectangle();
            } else {
                float x = (Float) objeto.getProperties().get("x");
                float y = (Float) objeto.getProperties().get("y");
                float width = (Float) objeto.getProperties().get("width");
                float height = (Float) objeto.getProperties().get("height");
                rect = new Rectangle(x, y, width, height);
            }

            EstacionTrabajo estacion = crearEstacionHeadless(tipo, rect);
            if (estacion != null) {
                estaciones.add(estacion);
            }
        }

        return estaciones;
    }

    private EstacionTrabajo crearEstacionHeadless(String tipo, Rectangle rect) {
        // Aquí crearemos versiones headless de las estaciones
        switch (tipo) {
            case "Horno":
                return new HornoHeadless(rect);
            case "Freidora":
                return new FreidoraHeadless(rect);
            case "Tostadora":
                return new TostadoraHeadless(rect);
            case "Cafetera":
                return new CafeteraHeadless(rect);
            case "Fuente":
                return new FuenteHeadless(rect);
            case "AreaEnsamblaje":
                return new MesaHeadless(rect);
            case "Heladera":
                return new HeladeraHeadless(rect);
            case "CajaRegistradora":
                return new CajaRegistradora(rect);
            case "MesaRetiro":
                return new MesaRetiro(rect);
            case "Basurero":
                return new Basurero(rect);
            case "CajaVirtual":
                return new CajaVirtual(rect);
            case "MaquinaEnvasadora":
                return new MaquinaEnvasadoraHeadless(rect);
            default:
                return null;
        }
    }

    public TiledMap getMapa() {
        return MAPA;
    }

    public String getNombre() {
        return NOMBRE_SUCURSAL;
    }

    public void dispose() {
        if (MAPA != null) {
            MAPA.dispose();
        }
    }
}
