package com.hebergames.letmecook.servidor.estacionesheadless;

import com.badlogic.gdx.math.Rectangle;
import com.hebergames.letmecook.entidades.Jugador;
import com.hebergames.letmecook.entregables.ObjetoAlmacenable;
import com.hebergames.letmecook.entregables.ingredientes.GestorIngredientes;
import com.hebergames.letmecook.entregables.ingredientes.TipoIngrediente;
import com.hebergames.letmecook.estaciones.EstacionTrabajo;

import java.util.ArrayList;
import java.util.Arrays;

public class HeladeraHeadless extends EstacionTrabajo {
    private final GestorIngredientes GESTOR_INGREDIENTES;
    private ArrayList<TipoIngrediente> ingredientesDisponibles;

    public HeladeraHeadless(Rectangle area) {
        super(area);
        GESTOR_INGREDIENTES = GestorIngredientes.getInstance();
        inicializarIngredientes();
    }

    private void inicializarIngredientes() {
        ingredientesDisponibles = new ArrayList<>();
        // Agregar todos los ingredientes disponibles
        ingredientesDisponibles.addAll(Arrays.asList(TipoIngrediente.values()));
    }

    @Override
    public void alInteractuar() {
        // La interacción se maneja desde procesarInteraccion del servidor
    }

    public void darIngrediente(Jugador jugador, TipoIngrediente tipo) {
        if (tipo == null) {
            System.err.println("Tipo de ingrediente inválido");
            return;
        }

        if (!jugador.tieneInventarioLleno()) {
            ObjetoAlmacenable objeto = GESTOR_INGREDIENTES.crearIngrediente(tipo);
            if (objeto != null) {
                jugador.guardarEnInventario(objeto);
                System.out.println("Jugador " + (jugador == null ? "?" : "") +
                    " tomó ingrediente: " + tipo.getNombre());
            }
        } else {
            System.out.println("Jugador tiene inventario lleno");
        }
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

    public void darIngredientePorIndice(Jugador jugador, int indice) {
        if (indice < 0 || indice >= ingredientesDisponibles.size()) {
            System.err.println("Índice de ingrediente inválido: " + indice);
            return;
        }

        TipoIngrediente tipo = ingredientesDisponibles.get(indice);
        darIngrediente(jugador, tipo);
    }

    public ArrayList<TipoIngrediente> getIngredientesDisponibles() {
        return ingredientesDisponibles;
    }

    public int getCantidadOpciones() {
        return ingredientesDisponibles.size();
    }
}
