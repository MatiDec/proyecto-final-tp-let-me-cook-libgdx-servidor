package com.hebergames.letmecook.servidor.estacionesheadless;

import com.badlogic.gdx.math.Rectangle;
import com.hebergames.letmecook.entidades.Jugador;
import com.hebergames.letmecook.entregables.productos.bebidas.Cafe;
import com.hebergames.letmecook.entregables.productos.bebidas.EstadoMenuBebida;
import com.hebergames.letmecook.entregables.productos.bebidas.TamanoBebida;
import com.hebergames.letmecook.estaciones.EstacionTrabajo;

public class CafeteraHeadless extends EstacionTrabajo {
    private EstadoMenuBebida estadoActual;
    private Cafe cafeEnPreparacion;
    private TamanoBebida tamanoSeleccionado;
    private String tipoSeleccionado;
    private float tiempoPreparacion;
    private float tiempoTranscurrido;

    public CafeteraHeadless(Rectangle area) {
        super(area);
        estadoActual = EstadoMenuBebida.SELECCION_TAMANO;
        tiempoPreparacion = 0f;
        tiempoTranscurrido = 0f;
    }

    @Override
    public void alInteractuar() {
        // Manejado por procesarInteraccion
    }

    @Override
    public void actualizar(float delta) {
        super.actualizar(delta);

        if (estadoActual == EstadoMenuBebida.PREPARANDO && cafeEnPreparacion != null) {
            tiempoTranscurrido += delta;

            if (tiempoTranscurrido >= tiempoPreparacion) {
                estadoActual = EstadoMenuBebida.LISTO;
            }
        }
    }

    public void seleccionarTamano(TamanoBebida tamano) {
        if (estadoActual != EstadoMenuBebida.SELECCION_TAMANO) {
            return;
        }

        tamanoSeleccionado = tamano;
        estadoActual = EstadoMenuBebida.SELECCION_TIPO;
    }

    public void seleccionarTipoCafe(String tipo) {
        if (estadoActual != EstadoMenuBebida.SELECCION_TIPO || tamanoSeleccionado == null) {
            return;
        }

        // Verificar que el tipo existe
        if (!Cafe.getTiposCafe().containsKey(tipo)) {
            return;
        }

        tipoSeleccionado = tipo;
        cafeEnPreparacion = new Cafe(tipo, tamanoSeleccionado);
        tiempoPreparacion = cafeEnPreparacion.getTiempoPreparacion();
        tiempoTranscurrido = 0f;
        estadoActual = EstadoMenuBebida.PREPARANDO;

        System.out.println("Iniciando preparación de café: " + tipo + " " + tamanoSeleccionado.getNombre());
    }

    public boolean tomarCafe(Jugador jugador) {
        if (estadoActual != EstadoMenuBebida.LISTO || cafeEnPreparacion == null) {
            return false;
        }

        if (!jugador.tieneInventarioLleno()) {
            jugador.guardarEnInventario(cafeEnPreparacion);
            resetear();
            System.out.println("Café tomado por jugador");
            return true;
        }

        return false;
    }

    public void procesarSeleccion(Jugador jugador, int opcion) {
        switch (estadoActual) {
            case SELECCION_TAMANO:
                seleccionarTamanoPorIndice(opcion);
                break;

            case SELECCION_TIPO:
                seleccionarTipoPorIndice(opcion);
                break;

            case LISTO:
                tomarCafe(jugador);
                break;

            case PREPARANDO:
                // No se puede hacer nada mientras prepara
                break;
        }
    }

    private void seleccionarTamanoPorIndice(int indice) {
        TamanoBebida[] tamanos = TamanoBebida.values();
        if (indice >= 0 && indice < tamanos.length) {
            seleccionarTamano(tamanos[indice]);
        }
    }

    private void seleccionarTipoPorIndice(int indice) {
        String[] tipos = Cafe.getTiposCafe().keySet().toArray(new String[0]);
        if (indice >= 0 && indice < tipos.length) {
            seleccionarTipoCafe(tipos[indice]);
        }
    }

    private void resetear() {
        estadoActual = EstadoMenuBebida.SELECCION_TAMANO;
        cafeEnPreparacion = null;
        tamanoSeleccionado = null;
        tipoSeleccionado = null;
        tiempoPreparacion = 0f;
        tiempoTranscurrido = 0f;
    }

    public EstadoMenuBebida getEstadoActual() {
        return estadoActual;
    }

    public float getProgreso() {
        if (tiempoPreparacion == 0) return 0;
        return Math.min(1f, tiempoTranscurrido / tiempoPreparacion);
    }

    public void abrirMenu(Jugador jugador) {
        if (jugador != null && !jugador.estaEnMenu()) {
            jugador.entrarEnMenu(this);
            ocupar(jugador);
        }
    }

    public void cerrarMenu(Jugador jugador) {
        if (jugador != null && jugador.estaEnMenu()) {
            jugador.salirDeMenu();
        }
    }
}
