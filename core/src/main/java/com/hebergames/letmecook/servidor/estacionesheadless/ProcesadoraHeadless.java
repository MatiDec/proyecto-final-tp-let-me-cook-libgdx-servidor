package com.hebergames.letmecook.servidor.estacionesheadless;

import com.hebergames.letmecook.entregables.ingredientes.CoccionListener;
import com.hebergames.letmecook.entregables.ingredientes.EstadoCoccion;
import com.hebergames.letmecook.entregables.ingredientes.Ingrediente;
import com.hebergames.letmecook.estaciones.procesadoras.MaquinaProcesadora;
import com.hebergames.letmecook.estaciones.procesadoras.MetodoCoccion;
import com.hebergames.letmecook.mapa.indicadores.EstadoIndicador;

public class ProcesadoraHeadless implements MaquinaProcesadora {
    private Ingrediente ingredienteProcesando;
    private boolean procesando;
    private EstadoIndicador estadoActual;
    private final String tipoMaquina;
    private float tiempoProcesoTotal;
    private float tiempoTranscurrido;

    public ProcesadoraHeadless(String tipoMaquina) {
        this.tipoMaquina = tipoMaquina;
        this.procesando = false;
        this.estadoActual = EstadoIndicador.INACTIVO;
    }

    @Override
    public boolean puedeIniciarProceso() {
        return !procesando;
    }

    @Override
    public boolean iniciarProceso(Ingrediente ingrediente) {
        if (procesando || ingrediente == null) {
            return false;
        }

        MetodoCoccion metodoRequerido = getMetodoPorTipo(tipoMaquina);

        if (!ingrediente.esCocinableInterna()) {
            return false;
        }

        // Verificar que el ingrediente puede cocinarse en esta máquina
        // (Aquí necesitarías la lógica específica de tu juego)

        ingredienteProcesando = ingrediente;
        procesando = true;
        estadoActual = EstadoIndicador.PROCESANDO;

        // Calcular tiempo total de proceso
        tiempoProcesoTotal = ingrediente.getTiempoMaximo(); // Necesitarás agregar este getter
        tiempoTranscurrido = 0f;

        ingredienteProcesando.setCoccionListener(new CoccionListener() {
            @Override
            public void onCambioEstado(EstadoCoccion nuevoEstado) {
                if (nuevoEstado == EstadoCoccion.BIEN_HECHO) {
                    estadoActual = EstadoIndicador.LISTO;
                }
            }

            @Override
            public void onIngredienteQuemado() {
                estadoActual = EstadoIndicador.QUEMANDOSE;
            }
        });

        return true;
    }

    @Override
    public void actualizarProceso(float delta) {
        if (procesando && ingredienteProcesando != null) {
            ingredienteProcesando.actualizarCoccion(delta);
            tiempoTranscurrido += delta;
        }
    }

    public float getProgreso() {
        if (tiempoProcesoTotal == 0) return 0f;
        return Math.min(1f, tiempoTranscurrido / tiempoProcesoTotal);
    }

    @Override
    public Ingrediente obtenerResultado() {
        if (!procesando) {
            return null;
        }

        Ingrediente resultado = ingredienteProcesando;
        ingredienteProcesando = null;
        procesando = false;
        estadoActual = EstadoIndicador.INACTIVO;

        return resultado;
    }

    public boolean tieneProcesandose() {
        return procesando;
    }

    public EstadoIndicador getEstadoIndicador() {
        return estadoActual;
    }

    private MetodoCoccion getMetodoPorTipo(String tipo) {
        switch (tipo.toLowerCase()) {
            case "horno": return MetodoCoccion.HORNO;
            case "freidora": return MetodoCoccion.FREIDORA;
            case "tostadora": return MetodoCoccion.TOSTADORA;
            default: return null;
        }
    }
}
