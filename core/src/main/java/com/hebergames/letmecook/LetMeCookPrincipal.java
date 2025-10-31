package com.hebergames.letmecook;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hebergames.letmecook.pantallas.Pantalla;
import com.hebergames.letmecook.pantallas.PantallaMenu;
import com.hebergames.letmecook.servidor.ServidorJuego;
import com.hebergames.letmecook.utiles.Render;

public class LetMeCookPrincipal extends Game {

    private ServidorJuego servidorJuego;

    @Override
    public void create() {
        this.servidorJuego = new ServidorJuego();
        this.servidorJuego.iniciar();

        // Agregar hook para cerrar correctamente cuando se cierra la aplicación
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook activado");
            if (servidorJuego != null) {
                servidorJuego.detener();
            }
        }));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        this.servidorJuego.detener();
    }
}
