package com.hebergames.letmecook.servidor.estacionesheadless;

import com.badlogic.gdx.math.Rectangle;
import com.hebergames.letmecook.estaciones.EstacionTrabajo;

public class FreidoraHeadless extends EstacionTrabajo {

    public FreidoraHeadless(Rectangle area) {
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
