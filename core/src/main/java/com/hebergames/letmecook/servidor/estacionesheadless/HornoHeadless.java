package com.hebergames.letmecook.servidor.estacionesheadless;

import com.badlogic.gdx.math.Rectangle;
import com.hebergames.letmecook.estaciones.EstacionTrabajo;

public class HornoHeadless extends EstacionTrabajo {

    public HornoHeadless(Rectangle area) {
        super(area);
        procesadora = new ProcesadoraHeadless("horno");
    }

    @Override
    public void alInteractuar() {
        if (getJugadorOcupante() != null) {
            manejarProcesamiento(getJugadorOcupante());
        }
    }
}
