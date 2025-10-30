package com.hebergames.letmecook.elementos;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.hebergames.letmecook.pantallas.juego.ObjetoVisualizable;
import com.hebergames.letmecook.utiles.Render;

/**
 * Clase Texto adaptable a cliente y servidor.
 * En servidor (Gdx no inicializado) no carga fuentes, solo mantiene lógica de texto y posición.
 */
public class Texto implements ObjetoVisualizable {

    private final BitmapFont FUENTE;
    private float x = 0, y = 0;
    private String texto = "";
    private final GlyphLayout LAYOUT;

    /**
     * Constructor de Texto.
     *
     * @param RUTA_FUENTE Ruta del archivo de fuente (solo se usa si Gdx está inicializado)
     * @param DIMENSION Tamaño de la fuente
     * @param COLOR Color de la fuente
     * @param SOMBRA Si debe tener sombra
     */
    public Texto(final String RUTA_FUENTE, final int DIMENSION, final Color COLOR, final boolean SOMBRA) {
        if (Gdx.app != null) {
            // Cliente: se inicializa la fuente
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(RUTA_FUENTE));
            FreeTypeFontGenerator.FreeTypeFontParameter parametro = new FreeTypeFontGenerator.FreeTypeFontParameter();

            parametro.size = DIMENSION;
            parametro.color = COLOR;
            if (SOMBRA) {
                parametro.shadowColor = Color.BLACK;
                parametro.shadowOffsetX = 1;
                parametro.shadowOffsetY = 1;
            }

            FUENTE = generator.generateFont(parametro);
            generator.dispose();
        } else {
            // Servidor: no se carga la fuente, solo dummy para evitar NPE
            FUENTE = null;
        }

        LAYOUT = new GlyphLayout();
    }

    /**
     * Verifica si se hizo clic en el texto
     */
    public boolean fueClickeado(float x, float y) {
        float ancho = getAncho();
        float alto = getAlto();
        float yInferior = this.y - alto;

        return x >= this.x && x <= this.x + ancho && y >= yInferior && y <= this.y;
    }

    /**
     * Dibuja el texto usando Render.batch (solo si FUENTE existe)
     */
    public void dibujar() {
        if (FUENTE != null) {
            FUENTE.draw(Render.batch, this.texto, this.x, this.y);
        }
    }

    /**
     * Dibuja el texto en un batch específico (UI)
     */
    @Override
    public void dibujarEnUi(SpriteBatch batch) {
        if (FUENTE != null) {
            FUENTE.draw(batch, this.texto, this.x, this.y);
        }
    }

    /**
     * Cambia el texto mostrado
     */
    public void setTexto(String nuevoTexto) {
        if (!this.texto.equals(nuevoTexto)) {
            this.texto = nuevoTexto;
            if (FUENTE != null) {
                LAYOUT.setText(FUENTE, nuevoTexto);
            }
        }
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getAncho() {
        return LAYOUT.width;
    }

    public float getAlto() {
        return LAYOUT.height;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public String getTexto() {
        return this.texto;
    }

    public BitmapFont getFuente() {
        return this.FUENTE;
    }
}
