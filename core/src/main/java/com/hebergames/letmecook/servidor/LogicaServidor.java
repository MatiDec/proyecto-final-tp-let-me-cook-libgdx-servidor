package com.hebergames.letmecook.servidor;

import com.badlogic.gdx.math.Rectangle;
import com.hebergames.letmecook.entidades.Jugador;
import com.hebergames.letmecook.entidades.clientes.*;
import com.hebergames.letmecook.entregables.ingredientes.TipoIngrediente;
import com.hebergames.letmecook.entregables.productos.*;
import com.hebergames.letmecook.estaciones.*;
import com.hebergames.letmecook.estaciones.conmenu.Cafetera;
import com.hebergames.letmecook.estaciones.conmenu.EstacionConMenu;
import com.hebergames.letmecook.estaciones.conmenu.Fuente;
import com.hebergames.letmecook.estaciones.conmenu.Mesa;
import com.hebergames.letmecook.estaciones.interaccionclientes.CajaRegistradora;
import com.hebergames.letmecook.estaciones.interaccionclientes.CajaVirtual;
import com.hebergames.letmecook.estaciones.interaccionclientes.MesaRetiro;
import com.hebergames.letmecook.estaciones.procesadoras.Procesadora;
import com.hebergames.letmecook.eventos.entrada.DatosEntrada;
import com.hebergames.letmecook.eventos.puntaje.GestorPuntaje;
import com.hebergames.letmecook.mapa.*;
import com.hebergames.letmecook.mapa.niveles.*;
import com.hebergames.letmecook.pedidos.*;
import com.hebergames.letmecook.red.paquetes.*;
import java.util.*;

public class LogicaServidor {
    private Jugador jugador1;
    private Jugador jugador2;
    private GestorMapa gestorMapa;
    private GestorClientes gestorClientes;
    private GestorPedidos gestorPedidos;
    private GestorPuntaje gestorPuntaje;
    private ArrayList<EstacionTrabajo> estaciones;
    private Map<Integer, DatosEntrada> inputsJugadores;
    private float tiempoRestante;
    private boolean juegoTerminado;
    private String razonFin;

    public LogicaServidor() {
        inputsJugadores = new HashMap<>();
        inputsJugadores.put(1, new DatosEntrada());
        inputsJugadores.put(2, new DatosEntrada());
        tiempoRestante = 200f;
        juegoTerminado = false;
    }

    public void inicializar() {
        GestorPartida gestorPartida = GestorPartida.getInstancia();
        ArrayList<String> rutasMapas = new ArrayList<>();
        rutasMapas.add("core/src/main/java/com/hebergames/letmecook/recursos/mapas/Sucursal_1.tmx");

        // Generar partida en modo servidor
        gestorPartida.generarNuevaPartida(rutasMapas, 1, true);

        NivelPartida nivel = gestorPartida.getNivelActual();
        gestorMapa = new GestorMapa();
        gestorMapa.setModoServidor(true);
        gestorMapa.setMapaActual(nivel.getMapa());

        estaciones = gestorMapa.getEstaciones();

        // Crear jugadores con animación null
        Rectangle spawnJ1 = gestorMapa.getPuntoSpawn("Jugador_1");
        float posXJ1 = (spawnJ1 != null) ? spawnJ1.x + (spawnJ1.width / 2f) - 64 : 1000;
        float posYJ1 = (spawnJ1 != null) ? spawnJ1.y + (spawnJ1.height / 2f) - 64 : 672;

        Rectangle spawnJ2 = gestorMapa.getPuntoSpawn("Jugador_2");
        float posXJ2 = (spawnJ2 != null) ? spawnJ2.x + (spawnJ2.width / 2f) - 64 : 1000;
        float posYJ2 = (spawnJ2 != null) ? spawnJ2.y + (spawnJ2.height / 2f) - 64 : 872;

        jugador1 = new Jugador(posXJ1, posYJ1, null); // null para servidor
        jugador2 = new Jugador(posXJ2, posYJ2, null);

        gestorMapa.asignarColisionesYInteracciones(jugador1);
        gestorMapa.asignarColisionesYInteracciones(jugador2);

        ArrayList<Jugador> jugadores = new ArrayList<>();
        jugadores.add(jugador1);
        jugadores.add(jugador2);

        jugador1.setOtrosJugadores(jugadores);
        jugador2.setOtrosJugadores(jugadores);

        // Inicializar sistema de clientes y pedidos
        ArrayList<CajaRegistradora> cajas = new ArrayList<>();
        ArrayList<MesaRetiro> mesas = new ArrayList<>();
        ArrayList<CajaVirtual> cajasVirtuales = new ArrayList<>();

        for (EstacionTrabajo estacion : estaciones) {
            if (estacion instanceof CajaRegistradora) cajas.add((CajaRegistradora) estacion);
            else if (estacion instanceof MesaRetiro) mesas.add((MesaRetiro) estacion);
            else if (estacion instanceof CajaVirtual) cajasVirtuales.add((CajaVirtual) estacion);
        }

        gestorClientes = new GestorClientes(cajas, cajasVirtuales, 15f, nivel.getTurno(), 10);
        gestorPedidos = new GestorPedidos(gestorClientes, mesas);
        gestorPuntaje = new GestorPuntaje();

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

    public void actualizar(float delta) {
        if (juegoTerminado) return;

        jugador1.manejarEntrada(inputsJugadores.get(1));
        jugador2.manejarEntrada(inputsJugadores.get(2));

        jugador1.actualizar(delta);
        jugador2.actualizar(delta);

        gestorClientes.actualizar(delta);

        for (EstacionTrabajo estacion : estaciones) {
            estacion.actualizar(delta);
            estacion.verificarDistanciaYLiberar();
        }

        // Decrementar tiempo correctamente
        tiempoRestante -= delta;

        if (tiempoRestante <= 0) {
            juegoTerminado = true;
            razonFin = gestorPuntaje.getPuntajeActual() < 600 ?
                "Puntaje insuficiente" : "Completado";
        }

        if (gestorClientes.haAlcanzadoLimiteClientes()) {
            juegoTerminado = true;
            razonFin = "Límite de clientes alcanzado";
        }
    }

    public void finalizarPorDesconexion(String razon) {
        juegoTerminado = true;
        razonFin = razon;
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

    private TipoIngrediente obtenerTipoPorIndice(int indice) {
        TipoIngrediente[] tipos = TipoIngrediente.values();
        if (indice >= 0 && indice < tipos.length) {
            return tipos[indice];
        }
        return null;
    }

    public PaqueteEstado generarEstado() {
        DatosJugador datosJ1 = new DatosJugador(
            jugador1.getPosicion().x,
            jugador1.getPosicion().y,
            jugador1.getAnguloRotacion(),
            jugador1.getInventario() != null ? jugador1.getInventario().getNombre() : "vacio",
            jugador1.estaEnMenu()
        );

        DatosJugador datosJ2 = new DatosJugador(
            jugador2.getPosicion().x,
            jugador2.getPosicion().y,
            jugador2.getAnguloRotacion(),
            jugador2.getInventario() != null ? jugador2.getInventario().getNombre() : "vacio",
            jugador2.estaEnMenu()
        );

        ArrayList<DatosCliente> datosClientes = new ArrayList<>();
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

        ArrayList<DatosEstacion> datosEstaciones = new ArrayList<>();
        for (int i = 0; i < estaciones.size(); i++) {
            EstacionTrabajo est = estaciones.get(i);
            DatosEstacion datos = crearDatosEstacion(est, i);
            datosEstaciones.add(datos);
        }

        return new PaqueteEstado(
            datosJ1, datosJ2, datosClientes, datosEstaciones,
            gestorPuntaje.getPuntajeActual(),
            (int) tiempoRestante, // Convertir a int aquí
            juegoTerminado,
            razonFin != null ? razonFin : ""
        );
    }

    private DatosEstacion crearDatosEstacion(EstacionTrabajo est, int index) {
        String tipoEstacion = est.getClass().getSimpleName();
        DatosEstacion datos = new DatosEstacion(index, tipoEstacion);

        datos.tieneJugador = (est.getJugadorOcupante() != null);

        if (est.getProcesadora() instanceof Procesadora) {
            Procesadora proc = (Procesadora) est.getProcesadora();
            datos.procesando = proc.tieneProcesandose();
            datos.estadoIndicador = proc.getIndicador() != null ?
                proc.getIndicador().getEstado().toString() : "INACTIVO";
            // Calcular progreso basado en tiempos
        }

        if (est instanceof Mesa) {
            Mesa mesa = (Mesa) est;
            // Agregar objetos en slots
        }

        if (est instanceof Cafetera) {
            Cafetera cafetera = (Cafetera) est;
            datos.estadoMenuBebida = cafetera.getEstadoMenu().toString();
            // Agregar progreso
        }

        if (est instanceof Fuente) {
            Fuente fuente = (Fuente) est;
            datos.estadoMenuBebida = fuente.getEstadoMenu().toString();
        }

        return datos;
    }

    public boolean estanJugadoresListos() { return jugador1 != null && jugador2 != null; }
}
