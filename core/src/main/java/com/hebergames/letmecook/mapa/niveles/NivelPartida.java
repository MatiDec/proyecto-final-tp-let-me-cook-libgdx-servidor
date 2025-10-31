package com.hebergames.letmecook.mapa.niveles;

import com.hebergames.letmecook.mapa.Mapa;
import com.hebergames.letmecook.servidor.MapaServidor;
import com.hebergames.letmecook.sonido.CancionNivel;

public class NivelPartida {
    private final Object MAPA; // Puede ser Mapa o MapaServidor
    private final TurnoTrabajo TURNO;
    private int puntajeObtenido;
    private boolean completado;
    private final CancionNivel CANCION_NIVEL;
    private final boolean ES_SERVIDOR;

    public NivelPartida(Object mapa, TurnoTrabajo turno, CancionNivel cancion, boolean esServidor) {
        this.MAPA = mapa;
        this.TURNO = turno;
        this.CANCION_NIVEL = cancion;
        this.ES_SERVIDOR = esServidor;
        this.puntajeObtenido = 0;
        this.completado = false;
    }

    public void marcarCompletado(int puntaje) {
        this.completado = true;
        this.puntajeObtenido = puntaje;
    }

    public Mapa getMapa() {
        if (!ES_SERVIDOR && MAPA instanceof Mapa) {
            return (Mapa) MAPA;
        }
        throw new RuntimeException("getMapa() solo disponible en cliente");
    }

    public MapaServidor getMapaServidor() {
        if (ES_SERVIDOR && MAPA instanceof MapaServidor) {
            return (MapaServidor) MAPA;
        }
        throw new RuntimeException("getMapaServidor() solo disponible en servidor");
    }

    public TurnoTrabajo getTurno() { return this.TURNO; }
    public boolean isCompletado() { return this.completado; }
    public int getPuntajeObtenido() { return this.puntajeObtenido; }
    public CancionNivel getCancionNivel() { return this.CANCION_NIVEL; }
    public boolean esServidor() { return this.ES_SERVIDOR; }
}
