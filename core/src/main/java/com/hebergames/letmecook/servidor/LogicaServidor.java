package com.hebergames.letmecook.servidor;

import com.badlogic.gdx.math.Rectangle;
import com.hebergames.letmecook.entidades.Jugador;
import com.hebergames.letmecook.entidades.clientes.*;
import com.hebergames.letmecook.entregables.productos.*;
import com.hebergames.letmecook.estaciones.*;
import com.hebergames.letmecook.estaciones.conmenu.*;
import com.hebergames.letmecook.estaciones.interaccionclientes.*;
import com.hebergames.letmecook.estaciones.procesadoras.Procesadora;
import com.hebergames.letmecook.eventos.entrada.DatosEntrada;
import com.hebergames.letmecook.eventos.eventosaleatorios.*;
import com.hebergames.letmecook.eventos.puntaje.GestorPuntaje;
import com.hebergames.letmecook.mapa.*;
import com.hebergames.letmecook.mapa.indicadores.EstadoIndicador;
import com.hebergames.letmecook.mapa.niveles.*;
import com.hebergames.letmecook.pantallas.juego.DetectorInactividad;
import com.hebergames.letmecook.pedidos.*;
import com.hebergames.letmecook.red.paquetes.*;

import java.util.*;

public class LogicaServidor {
    private final int MIN_CLIENTES_SUCURSAL_CHICA = 10;
    private final int MIN_CLIENTES_SUCURSAL_GRANDE = 20;
    private final int TIEMPO_OBJETIVO = 20;
    private final float TIEMPO_LIMITE_INACTIVIDAD = 10f;
    private final int CANTIDAD_MAPAS = 7;
    private Jugador jugador1;
    private Jugador jugador2;
    private ArrayList<Jugador> jugadores;

    private GestorMapa gestorMapa;
    private GestorClientes gestorClientes;
    private GestorPedidos gestorPedidos;
    private GestorPuntaje gestorPuntaje;
    private GestorPartida gestorPartida;
    private DetectorInactividad detectorInactividad;

    private ArrayList<EstacionTrabajo> estaciones;
    private Map<Integer, DatosEntrada> inputsJugadores;
    private float tiempoRestante;
    private boolean juegoTerminado;
    private String razonDespido;
    private boolean despedido;

    private NivelPartida nivelActual;

    public LogicaServidor() {
        inputsJugadores = new HashMap<>();
        inputsJugadores.put(1, new DatosEntrada());
        inputsJugadores.put(2, new DatosEntrada());
        tiempoRestante = TIEMPO_OBJETIVO;
        juegoTerminado = false;
        despedido = false;
        razonDespido = "";
        jugadores = new ArrayList<>();
    }

    public void inicializar() {
        gestorPartida = GestorPartida.getInstancia();
        gestorPuntaje = new GestorPuntaje();

        if (gestorPartida.getNivelActual() == null) {
            ArrayList<String> rutasMapas = new ArrayList<>();
            for (int i = 1; i <= CANTIDAD_MAPAS; i++) {
                rutasMapas.add("mapas/Sucursal_" + i + ".tmx");
            }
            gestorPartida.generarNuevaPartida(rutasMapas, rutasMapas.size(), true);
        }

        nivelActual = gestorPartida.getNivelActual();

        inicializarMapa();
        inicializarJugadores();
        inicializarSistemaPedidos();
        inicializarEventosAleatorios();

        detectorInactividad = new DetectorInactividad(jugadores, TIEMPO_LIMITE_INACTIVIDAD);
    }

    private void inicializarMapa() {
        gestorMapa = new GestorMapa();
        gestorMapa.setModoServidor(true);
        gestorMapa.setMapaActual(nivelActual.getMapa());
        estaciones = gestorMapa.getEstaciones();
    }

    private void inicializarJugadores() {
        Rectangle spawnJ1 = gestorMapa.getPuntoSpawn("Jugador_1");
        float posXJ1 = (spawnJ1 != null) ? spawnJ1.x + (spawnJ1.width / 2f) - 64 : 1000;
        float posYJ1 = (spawnJ1 != null) ? spawnJ1.y + (spawnJ1.height / 2f) - 64 : 672;

        Rectangle spawnJ2 = gestorMapa.getPuntoSpawn("Jugador_2");
        float posXJ2 = (spawnJ2 != null) ? spawnJ2.x + (spawnJ2.width / 2f) - 64 : 1000;
        float posYJ2 = (spawnJ2 != null) ? spawnJ2.y + (spawnJ2.height / 2f) - 64 : 872;

        jugador1 = new Jugador(posXJ1, posYJ1, null);
        jugador2 = new Jugador(posXJ2, posYJ2, null);

        gestorMapa.asignarColisionesYInteracciones(jugador1);
        gestorMapa.asignarColisionesYInteracciones(jugador2);

        jugadores.add(jugador1);
        jugadores.add(jugador2);

        jugador1.setOtrosJugadores(jugadores);
        jugador2.setOtrosJugadores(jugadores);
    }

    private void inicializarSistemaPedidos() {
        ArrayList<CajaRegistradora> cajas = new ArrayList<>();
        ArrayList<MesaRetiro> mesas = new ArrayList<>();
        ArrayList<CajaVirtual> cajasVirtuales = new ArrayList<>();

        for (EstacionTrabajo estacion : estaciones) {
            if (estacion instanceof CajaRegistradora) {
                cajas.add((CajaRegistradora) estacion);
            } else if (estacion instanceof MesaRetiro) {
                mesas.add((MesaRetiro) estacion);
            } else if (estacion instanceof CajaVirtual) {
                cajasVirtuales.add((CajaVirtual) estacion);
            }
        }

        TurnoTrabajo turnoActual = nivelActual.getTurno();
        int minClientesRequeridos = calcularMinClientesRequeridos();

        gestorClientes = new GestorClientes(cajas, cajasVirtuales, 15f, turnoActual, minClientesRequeridos);
        gestorPedidos = new GestorPedidos(gestorClientes, mesas);

        gestorClientes.setCallbackPenalizacion((puntos, razon) -> {
            gestorPuntaje.agregarPuntos(puntos);
        });

        for (CajaRegistradora caja : cajas) {
            caja.setGestorPedidos(gestorPedidos);
        }

        for (CajaVirtual cajaVirtual : cajasVirtuales) {
            cajaVirtual.setGestorPedidos(gestorPedidos);
            cajaVirtual.setCallbackPuntaje(gestorPuntaje);
        }

        for (MesaRetiro mesa : mesas) {
            mesa.setGestorPedidos(gestorPedidos);
            mesa.setCallbackPuntaje(gestorPuntaje);
        }
    }

    private void inicializarEventosAleatorios() {
        GestorEventosAleatorios gestorEventos = GestorEventosAleatorios.getInstancia();
        gestorEventos.reset();

        for (EstacionTrabajo estacion : estaciones) {
            if (!(estacion instanceof CajaRegistradora) &&
                !(estacion instanceof MesaRetiro) &&
                !(estacion instanceof CajaVirtual) &&
                !(estacion instanceof Basurero) &&
                !(estacion instanceof Heladera)){
                gestorEventos.registrarEventoPosible(new EventoMaquinaRota(estacion));
            }
        }

        ArrayList<Rectangle> tilesCaminables = gestorMapa.getTilesCaminables();
        if (!tilesCaminables.isEmpty()) {
            gestorEventos.registrarEventoPosible(new EventoPisoMojado(tilesCaminables));
        }

        gestorEventos.iniciarRonda();
    }

    private int calcularMinClientesRequeridos() {
        int nivelActualIndex = gestorPartida.getNivelActualIndex();
        if (nivelActualIndex == 0 || nivelActualIndex == 2) {
            return MIN_CLIENTES_SUCURSAL_CHICA;
        } else {
            return MIN_CLIENTES_SUCURSAL_GRANDE;
        }
    }

    public void actualizar(float delta) {
        if (juegoTerminado) return;

        if (jugador1 == null || jugador2 == null) {
            System.err.println("⚠️ Jugadores no inicializados, saltando actualización");
            return;
        }

        jugador1.manejarEntrada(inputsJugadores.get(1));
        jugador2.manejarEntrada(inputsJugadores.get(2));

        jugador1.actualizar(delta);
        jugador2.actualizar(delta);

        if (gestorClientes != null) {
            gestorClientes.actualizar(delta);
        }

        for (EstacionTrabajo estacion : estaciones) {
            estacion.actualizar(delta);
            estacion.verificarDistanciaYLiberar();
        }

        detectorInactividad.actualizar(delta);
        tiempoRestante -= delta;
        verificarFinDeJuego();
    }

    private void verificarFinDeJuego() {
        if (detectorInactividad.haySuperadoLimite()) {
            despedido = true;
            razonDespido = "Despedido por inactividad";
            juegoTerminado = true;
            System.out.println("❌ Despedido por inactividad");
            return;
        }

        if (gestorClientes != null && gestorClientes.haAlcanzadoLimiteClientes()) {
            int puntajeFinal = gestorPuntaje.getPuntajeActual();

            System.out.println("🎯 Verificando fin de nivel:");
            System.out.println("   - Puntaje: " + puntajeFinal);
            System.out.println("   - Clientes atendidos: " + gestorClientes.getClientesAtendidos());
            System.out.println("   - Mínimo requerido: " + gestorClientes.getMinClientesRequeridos());

            if (puntajeFinal < 600) {
                despedido = true;
                razonDespido = "Puntaje insuficiente (menos de 600 puntos)";
                juegoTerminado = true;
                System.out.println("❌ Despedido por puntaje bajo");
                return;
            }

            if (!gestorClientes.cumpleRequisitoMinimo()) {
                despedido = true;
                razonDespido = "No atendiste a suficientes clientes (" +
                    gestorClientes.getClientesAtendidos() + "/" +
                    gestorClientes.getMinClientesRequeridos() + ")";
                juegoTerminado = true;
                System.out.println("❌ Despedido por pocos clientes");
                return;
            }

            System.out.println("✅ Nivel completado exitosamente");
            juegoTerminado = true;
            return;
        }

        if (tiempoRestante <= 0) {
            int puntajeFinal = gestorPuntaje.getPuntajeActual();

            System.out.println("⏰ Se acabó el tiempo:");
            System.out.println("   - Puntaje: " + puntajeFinal);

            if (puntajeFinal < 600) {
                despedido = true;
                razonDespido = "Puntaje insuficiente (menos de 600 puntos)";
                System.out.println("❌ Despedido por puntaje bajo");
            } else if (gestorClientes != null && !gestorClientes.cumpleRequisitoMinimo()) {
                despedido = true;
                razonDespido = "No atendiste a suficientes clientes (" +
                    gestorClientes.getClientesAtendidos() + "/" +
                    gestorClientes.getMinClientesRequeridos() + ")";
                System.out.println("❌ Despedido por pocos clientes");
            } else {
                System.out.println("✅ Nivel completado por tiempo");
            }

            juegoTerminado = true;
        }
    }

    public void finalizarPorDesconexion(String razon) {
        juegoTerminado = true;
        despedido = true;
        razonDespido = razon;
    }

    public void procesarInput(PaqueteInput input) {
        DatosEntrada datos = inputsJugadores.get(input.getIdJugador());
        if (datos != null) {
            datos.arriba = input.isArriba();
            datos.abajo = input.isAbajo();
            datos.izquierda = input.isIzquierda();
            datos.derecha = input.isDerecha();
            datos.correr = input.isCorrer();
        }
    }

    public void procesarInteraccion(PaqueteInteraccion interaccion) {
        Jugador jugador = (interaccion.getIdJugador() == 1) ? jugador1 : jugador2;
        int indexEstacion = interaccion.getIndexEstacion();

        if (indexEstacion < 0 || indexEstacion >= estaciones.size()) {
            return;
        }

        EstacionTrabajo estacion = estaciones.get(indexEstacion);

        switch (interaccion.getTipoInteraccion()) {
            case INTERACTUAR_BASICO:
                estacion.interactuarConJugador(jugador);
                break;

            case SELECCION_MENU:
                if (estacion instanceof EstacionConMenu) {
                    ((EstacionConMenu) estacion).manejarSeleccionMenu(jugador, interaccion.getParametroExtra() + 1);
                }
                break;
        }
    }

    public PaqueteEstado generarEstado() {
        float velXJ1 = jugador1.getVelocidad().x;
        float velYJ1 = jugador1.getVelocidad().y;
        float velXJ2 = jugador2.getVelocidad().x;
        float velYJ2 = jugador2.getVelocidad().y;

        DatosJugador datosJ1 = new DatosJugador(
            jugador1.getPosicion().x,
            jugador1.getPosicion().y,
            jugador1.getAnguloRotacion(),
            jugador1.getInventario() != null ? jugador1.getInventario().getNombre() : "vacio",
            jugador1.estaEnMenu(),
            inputsJugadores.get(1).correr,
            velXJ1,
            velYJ1
        );

        DatosJugador datosJ2 = new DatosJugador(
            jugador2.getPosicion().x,
            jugador2.getPosicion().y,
            jugador2.getAnguloRotacion(),
            jugador2.getInventario() != null ? jugador2.getInventario().getNombre() : "vacio",
            jugador2.estaEnMenu(),
            inputsJugadores.get(2).correr,
            velXJ2,
            velYJ2
        );

        ArrayList<DatosCliente> datosClientes = new ArrayList<>();
        if (gestorClientes != null) {
            for (Cliente cliente : gestorClientes.getClientesActivos()) {
                ArrayList<String> productos = new ArrayList<>();
                for (Producto p : cliente.getPedido().getProductosSolicitados()) {
                    productos.add(p.getNombre());
                }

                int indexEstacion = estaciones.indexOf(cliente.getEstacionAsignada());

                datosClientes.add(new DatosCliente(
                    System.identityHashCode(cliente),
                    cliente.getTiempoRestante(),
                    cliente.getPorcentajeToleranciaActual(),
                    cliente.getPedido().getEstadoPedido().toString(),
                    productos,
                    cliente.esVirtual(),
                    indexEstacion
                ));
            }
        }

        ArrayList<DatosEstacion> datosEstaciones = new ArrayList<>();
        for (int i = 0; i < estaciones.size(); i++) {
            EstacionTrabajo est = estaciones.get(i);
            DatosEstacion datos = crearDatosEstacion(est, i);
            datosEstaciones.add(datos);
        }

        return new PaqueteEstado(
            datosJ1, datosJ2, datosClientes, datosEstaciones,
            gestorPuntaje.getPuntajeActual(),
            (int) tiempoRestante,
            juegoTerminado,
            despedido ? razonDespido : ""
        );
    }

    private DatosEstacion crearDatosEstacion(EstacionTrabajo est, int index) {
        String tipoEstacion = est.getClass().getSimpleName();
        DatosEstacion datos = new DatosEstacion(index, tipoEstacion);

        datos.tieneJugador = (est.getJugadorOcupante() != null);
        datos.fueraDeServicio = est.isFueraDeServicio();

        if (est.getProcesadora() instanceof Procesadora) {
            Procesadora proc = (Procesadora) est.getProcesadora();
            datos.procesando = proc.tieneProcesandose();

            if (proc.tieneProcesandose()) {
                datos.nombreIngrediente = "procesando";
            }

            if (proc.getIndicador() != null) {
                EstadoIndicador estadoIndicador = proc.getEstadoActual();
                datos.estadoIndicador = estadoIndicador.toString();

                if (estadoIndicador == EstadoIndicador.LISTO) {
                    datos.estadoMaquina = "LISTA";
                    datos.estadoIndicador = "LISTO";
                } else if (estadoIndicador == EstadoIndicador.PROCESANDO) {
                    datos.estadoMaquina = "ACTIVA";
                    datos.estadoIndicador = "PROCESANDO";
                } else if (estadoIndicador == EstadoIndicador.QUEMANDOSE) {
                     datos.estadoIndicador = "QUEMANDOSE";
                } else {
                    datos.estadoMaquina = "INACTIVA";
                    datos.estadoIndicador = "INACTIVO";
                }
            } else {
                datos.estadoIndicador = "INACTIVO";
                datos.estadoMaquina = "INACTIVA";
            }
        }

        if (est instanceof Mesa) {
            Mesa mesa = (Mesa) est;
            datos.objetosEnEstacion = new ArrayList<>();
            for (com.hebergames.letmecook.entregables.ObjetoAlmacenable obj : mesa.getObjetosEnMesa()) {
                if (obj != null) {
                    datos.objetosEnEstacion.add(obj.getNombre());
                } else {
                    datos.objetosEnEstacion.add("vacio");
                }
            }
        }

        if (est instanceof Cafetera) {
            Cafetera cafetera = (Cafetera) est;
            datos.estadoMenuBebida = cafetera.getEstadoMenu() != null ?
                cafetera.getEstadoMenu().toString() : "SELECCION_TAMANO";
            datos.progresoPreparacion = cafetera.getProgreso();
        }

        if (est instanceof Fuente) {
            Fuente fuente = (Fuente) est;
            datos.estadoMenuBebida = fuente.getEstadoMenu() != null ?
                fuente.getEstadoMenu().toString() : "SELECCION_TAMANO";
            datos.progresoPreparacion = fuente.getProgreso();
        }

        return datos;
    }

    public PaqueteInicioPartida generarPaqueteInicio() {
        ArrayList<String> ordenSucursales = new ArrayList<>();
        for (NivelPartida nivel : gestorPartida.getTodosLosNiveles()) {
            ordenSucursales.add(nivel.getMapa().getRutaCompleta() + ";" + nivel.getTurno().toString());
        }

        return new PaqueteInicioPartida(
            ordenSucursales
        );
    }


    public void reiniciarParaNuevoNivel() {
        int indiceLevelCompletado = gestorPartida.getNivelActualIndex();
        int puntajeNivel = gestorPuntaje.getPuntajeActual();

        System.out.println("🔄 Completando nivel " + indiceLevelCompletado + " con " + puntajeNivel + " puntos");

        if (indiceLevelCompletado < gestorPartida.getTodosLosNiveles().size()) {
            gestorPartida.getTodosLosNiveles().get(indiceLevelCompletado)
                .marcarCompletado(puntajeNivel);
            gestorPartida.sumarPuntaje(puntajeNivel);
        }

        gestorPartida.avanzarIndiceNivel();

        int nuevoIndice = gestorPartida.getNivelActualIndex();
        System.out.println("📍 Avanzando a nivel " + nuevoIndice);

        if (nuevoIndice >= gestorPartida.getTodosLosNiveles().size()) {
            System.out.println("⚠️ No hay más niveles disponibles");
            juegoTerminado = true;
            return;
        }

        if (gestorMapa != null) {
            gestorMapa.dispose();
        }

        nivelActual = gestorPartida.getNivelActual();

        if (nivelActual == null) {
            System.out.println("❌ Error: nivel actual es null");
            juegoTerminado = true;
            return;
        }

        System.out.println("✅ Inicializando nivel: " + nivelActual.getMapa().getNombre());

        tiempoRestante = TIEMPO_OBJETIVO;
        juegoTerminado = false;
        despedido = false;
        razonDespido = "";

        gestorPuntaje = new GestorPuntaje();

        inputsJugadores.get(1).reset();
        inputsJugadores.get(2).reset();

        inicializarMapa();
        inicializarJugadores();
        inicializarSistemaPedidos();
        inicializarEventosAleatorios();

        detectorInactividad = new DetectorInactividad(jugadores, TIEMPO_LIMITE_INACTIVIDAD);

        System.out.println("✅ Servidor listo para nivel " + gestorPartida.getNivelActualIndex());
    }

    public PaqueteCambioNivel generarPaqueteCambioNivel() {
        int puntajeActual = gestorPuntaje.getPuntajeActual();
        int siguienteIndice = gestorPartida.getNivelActualIndex() + 1;

        System.out.println("📦 Generando paquete cambio nivel. Actual: " +
            gestorPartida.getNivelActualIndex() + ", Siguiente: " + siguienteIndice);

        if (siguienteIndice >= gestorPartida.getTodosLosNiveles().size()) {
            System.out.println("🏁 No hay más niveles - fin de partida");
            return null;
        }

        NivelPartida siguienteNivel = gestorPartida.getTodosLosNiveles().get(siguienteIndice);

        return new PaqueteCambioNivel(
            puntajeActual,
            siguienteNivel.getMapa().getRutaCompleta(),
            siguienteNivel.getTurno().toString(),
            siguienteIndice
        );
    }

    public boolean estanJugadoresListos() {
        return jugador1 != null && jugador2 != null;
    }
}
