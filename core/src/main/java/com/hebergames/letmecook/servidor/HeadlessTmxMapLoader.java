package com.hebergames.letmecook.servidor;

import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.XmlReader;

/**
 * Cargador de mapas TMX para servidor headless.
 * Solo carga la estructura del mapa (propiedades, capas, objetos).
 * NO carga tilesets ni texturas.
 */
public class HeadlessTmxMapLoader {

    private final FileHandleResolver fileResolver;
    private final XmlReader xml = new XmlReader();

    public HeadlessTmxMapLoader(FileHandleResolver resolver) {
        this.fileResolver = resolver;
    }

    public TiledMap load(String fileName) {
        try {
            FileHandle tmxFile = fileResolver.resolve(fileName);
            XmlReader.Element root = xml.parse(tmxFile);

            TiledMap map = new TiledMap();

            // Cargar propiedades del mapa
            loadMapProperties(map, root);

            // Cargar todas las capas (tiles y objetos)
            loadLayers(map, root);

            System.out.println("✅ Mapa TMX cargado en modo headless: " + fileName);
            return map;

        } catch (Exception e) {
            throw new RuntimeException("No se pudo cargar el mapa TMX: " + fileName, e);
        }
    }

    private void loadMapProperties(TiledMap map, XmlReader.Element root) {
        MapProperties props = map.getProperties();

        // Propiedades básicas del mapa
        props.put("width", root.getIntAttribute("width", 0));
        props.put("height", root.getIntAttribute("height", 0));
        props.put("tilewidth", root.getIntAttribute("tilewidth", 32));
        props.put("tileheight", root.getIntAttribute("tileheight", 32));

        String orientation = root.getAttribute("orientation", "orthogonal");
        props.put("orientation", orientation);

        // Cargar propiedades personalizadas
        XmlReader.Element properties = root.getChildByName("properties");
        if (properties != null) {
            for (XmlReader.Element property : properties.getChildrenByName("property")) {
                String name = property.getAttribute("name");
                String value = property.getAttribute("value");
                props.put(name, value);
            }
        }
    }

    private void loadLayers(TiledMap map, XmlReader.Element root) {
        // Cargar capas de tiles (aunque no usamos los tiles visuales)
        for (XmlReader.Element layerElement : root.getChildrenByName("layer")) {
            String name = layerElement.getAttribute("name");
            int width = layerElement.getIntAttribute("width", 0);
            int height = layerElement.getIntAttribute("height", 0);

            TiledMapTileLayer layer = new TiledMapTileLayer(width, height,
                map.getProperties().get("tilewidth", Integer.class),
                map.getProperties().get("tileheight", Integer.class));
            layer.setName(name);

            map.getLayers().add(layer);
        }

        // Cargar capas de objetos (lo más importante para el servidor)
        for (XmlReader.Element objectGroupElement : root.getChildrenByName("objectgroup")) {
            loadObjectGroup(map, objectGroupElement);
        }
    }

    private void loadObjectGroup(TiledMap map, XmlReader.Element element) {
        String name = element.getAttribute("name", "");
        MapLayer layer = new MapLayer();
        layer.setName(name);

        // Cargar propiedades de la capa
        XmlReader.Element properties = element.getChildByName("properties");
        if (properties != null) {
            for (XmlReader.Element property : properties.getChildrenByName("property")) {
                String propName = property.getAttribute("name");
                String value = property.getAttribute("value");
                layer.getProperties().put(propName, value);
            }
        }

        // Cargar objetos
        for (XmlReader.Element objectElement : element.getChildrenByName("object")) {
            loadObject(layer, objectElement);
        }

        map.getLayers().add(layer);
    }

    private void loadObject(MapLayer layer, XmlReader.Element element) {
        String name = element.getAttribute("name", "");
        String type = element.getAttribute("type", "");

        float x = element.getFloatAttribute("x", 0);
        float y = element.getFloatAttribute("y", 0);
        float width = element.getFloatAttribute("width", 0);
        float height = element.getFloatAttribute("height", 0);

        Rectangle rect = new Rectangle(x, y, width, height);
        RectangleMapObject mapObject = new RectangleMapObject();

        mapObject.setName(name);
        mapObject.getProperties().put("type", type);

        // Cargar propiedades personalizadas del objeto
        XmlReader.Element properties = element.getChildByName("properties");
        if (properties != null) {
            for (XmlReader.Element property : properties.getChildrenByName("property")) {
                String propName = property.getAttribute("name");
                String value = property.getAttribute("value");
                mapObject.getProperties().put(propName, value);
            }
        }

        layer.getObjects().add(mapObject);
    }
}
