package com.hebergames.letmecook.elementos;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.hebergames.letmecook.pantallas.juego.ObjetoVisualizable;
import com.hebergames.letmecook.utiles.Render;

public class Texto implements ObjetoVisualizable {

    private final BitmapFont FUENTE;
    private float x = 0, y = 0;
    private String texto = "";
    private final GlyphLayout LAYOUT;

    public Texto(final String RUTA_FUENTE, final int DIMENSION, final Color COLOR, final boolean SOMBRA) {
        if (Gdx.app != null) {
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
            FUENTE = null;
        }

        LAYOUT = new GlyphLayout();
    }

    public boolean fueClickeado(float x, float y) {
        float ancho = getAncho();
        float alto = getAlto();
        float yInferior = this.y - alto;

        return x >= this.x && x <= this.x + ancho && y >= yInferior && y <= this.y;
    }

    public void dibujar() {
        if (FUENTE != null) {
            FUENTE.draw(Render.batch, this.texto, this.x, this.y);
        }
    }

    @Override
    public void dibujarEnUi(SpriteBatch batch) {
        if (FUENTE != null) {
            FUENTE.draw(batch, this.texto, this.x, this.y);
        }
    }

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
