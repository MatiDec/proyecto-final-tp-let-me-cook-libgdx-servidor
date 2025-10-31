package com.hebergames.letmecook.red.paquetes;

import com.hebergames.letmecook.red.PaqueteRed;

public class PaqueteInteraccion extends PaqueteRed {
    private int idJugador;
    private int indexEstacion;
    private TipoInteraccion tipoInteraccion;
    private int parametroExtra;

    public enum TipoInteraccion {
        INTERACTUAR_BASICO,
        SELECCION_MENU,
        TOMAR_INGREDIENTE,
        DEPOSITAR_OBJETO,
        INICIAR_PROCESO,
        RECOGER_RESULTADO
    }

    public PaqueteInteraccion(int idJugador, int indexEstacion, TipoInteraccion tipo) {
        this.idJugador = idJugador;
        this.indexEstacion = indexEstacion;
        this.tipoInteraccion = tipo;
        this.parametroExtra = -1;
    }

    public PaqueteInteraccion(int idJugador, int indexEstacion, TipoInteraccion tipo, int parametro) {
        this.idJugador = idJugador;
        this.indexEstacion = indexEstacion;
        this.tipoInteraccion = tipo;
        this.parametroExtra = parametro;
    }

    @Override
    public TipoPaquete getTipo() {
        return TipoPaquete.INTERACCION;
    }

    public int getIdJugador() { return idJugador; }
    public int getIndexEstacion() { return indexEstacion; }
    public TipoInteraccion getTipoInteraccion() { return tipoInteraccion; }
    public int getParametroExtra() { return parametroExtra; }
}
