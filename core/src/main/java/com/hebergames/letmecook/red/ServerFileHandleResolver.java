package com.hebergames.letmecook.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;

/**
 * Resuelve rutas de archivos en el servidor usando rutas absolutas del sistema.
 */
public class ServerFileHandleResolver implements FileHandleResolver {

    @Override
    public FileHandle resolve(String fileName) {
        File file = new File(fileName);

        // Si es ruta absoluta, usarla directamente
        if (file.isAbsolute()) {
            return new FileHandle(file);
        }

        // Si es relativa, intentar resolverla desde el directorio actual
        return new FileHandle(new File(System.getProperty("user.dir"), fileName));
    }
}
