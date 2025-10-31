package com.hebergames.letmecook.servidor.estacionesheadless;

import com.badlogic.gdx.math.Rectangle;
import com.hebergames.letmecook.entidades.Jugador;
import com.hebergames.letmecook.entregables.ObjetoAlmacenable;
import com.hebergames.letmecook.entregables.ingredientes.Ingrediente;
import com.hebergames.letmecook.entregables.productos.Producto;
import com.hebergames.letmecook.entregables.recetas.GestorRecetas;
import com.hebergames.letmecook.entregables.recetas.Receta;
import com.hebergames.letmecook.estaciones.EstacionTrabajo;

import java.util.ArrayList;

public class MesaHeadless extends EstacionTrabajo {
    private ArrayList<ObjetoAlmacenable> objetosEnMesa;
    private final int CAPACIDAD_MAXIMA = 4;

    public MesaHeadless(Rectangle area) {
        super(area);
        objetosEnMesa = new ArrayList<>();
    }

    @Override
    public void alInteractuar() {
        // Manejado por procesarInteraccion en el servidor
    }

    public void depositarObjeto(Jugador jugador) {
        if (objetosEnMesa.size() >= CAPACIDAD_MAXIMA) {
            return; // Mesa llena
        }

        ObjetoAlmacenable objeto = jugador.getInventario();
        if (objeto != null) {
            objetosEnMesa.add(objeto);
            jugador.sacarDeInventario();
        }
    }

    public void tomarObjeto(Jugador jugador, int indice) {
        if (indice < 0 || indice >= objetosEnMesa.size()) {
            return;
        }

        if (!jugador.tieneInventarioLleno()) {
            ObjetoAlmacenable objeto = objetosEnMesa.remove(indice);
            jugador.guardarEnInventario(objeto);
        }
    }

    public void manejarSeleccion(Jugador jugador, int seleccion) {
        if (seleccion == 0) {
            // Opción 0: Depositar
            depositarObjeto(jugador);
        } else if (seleccion > 0 && seleccion <= objetosEnMesa.size()) {
            // Opciones 1+: Tomar objeto
            tomarObjeto(jugador, seleccion - 1);
        } else if (seleccion == 9) {
            // Opción 9: Intentar preparar receta
            intentarPreparar(jugador);
        }
    }

    private void intentarPreparar(Jugador jugador) {
        if (jugador.tieneInventarioLleno()) {
            return; // No puede recibir el producto
        }

        // Convertir objetos a ingredientes
        ArrayList<Ingrediente> ingredientes = new ArrayList<>();
        for (ObjetoAlmacenable obj : objetosEnMesa) {
            if (obj instanceof Ingrediente) {
                ingredientes.add((Ingrediente) obj);
            }
        }

        if (ingredientes.isEmpty()) {
            return;
        }

        // Buscar receta
        GestorRecetas gestorRecetas = GestorRecetas.getInstance();
        Receta receta = gestorRecetas.buscarReceta(ingredientes);

        if (receta != null) {
            Producto producto = receta.preparar();
            objetosEnMesa.clear();
            jugador.guardarEnInventario(producto);
        }
    }

    public ArrayList<ObjetoAlmacenable> getObjetosEnMesa() {
        return objetosEnMesa;
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
            if (getJugadorOcupante() == jugador) {
                // No limpiar jugadorOcupante aquí, se limpia en verificarDistanciaYLiberar
            }
        }
    }
}
