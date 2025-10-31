package com.hebergames.letmecook.servidor.estacionesheadless;

import com.badlogic.gdx.math.Rectangle;
import com.hebergames.letmecook.entidades.Jugador;
import com.hebergames.letmecook.entregables.ObjetoAlmacenable;
import com.hebergames.letmecook.entregables.ingredientes.Ingrediente;
import com.hebergames.letmecook.entregables.ingredientes.IngredienteGenerico;
import com.hebergames.letmecook.entregables.ingredientes.TipoEnvase;
import com.hebergames.letmecook.estaciones.EstacionTrabajo;

public class MaquinaEnvasadoraHeadless extends EstacionTrabajo {

    public MaquinaEnvasadoraHeadless(Rectangle area) {
        super(area);
    }

    @Override
    public void alInteractuar() {
        // Manejado por procesarInteraccion
    }

    public void intentarEnvasar(Jugador jugador) {
        ObjetoAlmacenable objetoJugador = jugador.getInventario();

        if (!(objetoJugador instanceof Ingrediente)) {
            System.out.println("El jugador no tiene un ingrediente para envasar");
            return;
        }

        Ingrediente ingrediente = (Ingrediente) objetoJugador;

        // Buscar el tipo de envase correcto usando el nombre completo del ingrediente
        TipoEnvase envaseAdecuado = TipoEnvase.obtenerPorIngrediente(ingrediente.getNombre());

        if (envaseAdecuado == null) {
            System.out.println("No hay envase adecuado para: " + ingrediente.getNombre());
            return;
        }

        // Crear el envase (que es un IngredienteGenerico)
        IngredienteGenerico envase = envaseAdecuado.crearEnvase();

        if (envase != null) {
            jugador.sacarDeInventario();
            jugador.guardarEnInventario(envase);
            System.out.println("Ingrediente envasado: " + envase.getNombre());
        }
    }

    public void procesarSeleccion(Jugador jugador, int opcion) {
        if (opcion == 0) { // Opción "Envasar" (primera opción, índice 0)
            intentarEnvasar(jugador);
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
}
