package com.hebergames.letmecook.servidor;

import com.hebergames.letmecook.entidades.Jugador;
import com.hebergames.letmecook.entidades.clientes.*;
import com.hebergames.letmecook.entregables.productos.*;
import com.hebergames.letmecook.estaciones.*;
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
import com.hebergames.letmecook.utiles.GestorAnimacion;
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
    private int tiempoRestante;
    private boolean juegoTerminado;
    private String razonFin;

    public LogicaServidor() {
        inputsJugadores = new HashMap<>();
        inputsJugadores.put(1, new DatosEntrada());
        inputsJugadores.put(2, new DatosEntrada());
        tiempoRestante = 200;
        juegoTerminado = false;
    }

    public void inicializar() {
        // Inicializar mapa y partida
        GestorPartida gestorPartida = GestorPartida.getInstancia();
        ArrayList<String> rutasMapas = new ArrayList<>();
        rutasMapas.add("core/src/main/java/com/hebergames/letmecook/recursos/mapas/Sucursal_1.tmx");
        gestorPartida.generarNuevaPartida(rutasMapas, 1, true);

        NivelPartida nivel = gestorPartida.getNivelActual();
        gestorMapa = new GestorMapa();
        gestorMapa.setMapaActual(nivel.getMapa());
        estaciones = gestorMapa.getEstaciones();

        // Crear jugadores (sin animaciones gráficas)
        GestorAnimacion animVacio = null; // El servidor no necesita animaciones
        jugador1 = new Jugador(1000, 672, animVacio);
        jugador2 = new Jugador(1000, 872, animVacio);

        gestorMapa.asignarColisionesYInteracciones(jugador1);
        gestorMapa.asignarColisionesYInteracciones(jugador2);

        ArrayList<Jugador> jugadores = new ArrayList<>();
        jugadores.add(jugador1);
        jugadores.add(jugador2);

        jugador1.setOtrosJugadores(jugadores);
        jugador2.setOtrosJugadores(jugadores);

        // Inicializar sistema de pedidos
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

        // Aplicar inputs de jugadores
        jugador1.manejarEntrada(inputsJugadores.get(1));
        jugador2.manejarEntrada(inputsJugadores.get(2));

        // Actualizar jugadores
        jugador1.actualizar(delta);
        jugador2.actualizar(delta);

        // Actualizar clientes y pedidos
        gestorClientes.actualizar(delta);

        // Actualizar estaciones
        for (EstacionTrabajo estacion : estaciones) {
            estacion.actualizar(delta);
            estacion.verificarDistanciaYLiberar();
        }

        // Actualizar tiempo
        tiempoRestante -= (int) delta;

        // Verificar condiciones de fin
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

    public PaqueteEstado generarEstado() {
        // Datos jugadores
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

        // Datos clientes
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

        // Datos estaciones procesadoras
        ArrayList<DatosEstacionProcesadora> datosEstaciones = new ArrayList<>();
        for (int i = 0; i < estaciones.size(); i++) {
            EstacionTrabajo est = estaciones.get(i);
            if (est.getProcesadora() instanceof Procesadora) {
                Procesadora proc = (Procesadora) est.getProcesadora();
                datosEstaciones.add(new DatosEstacionProcesadora(
                    i,
                    proc.tieneProcesandose(),
                    "", // El cliente no necesita el nombre específico
                    proc.getIndicador() != null ?
                        proc.getIndicador().getEstado().toString() : "INACTIVO"
                ));
            }
        }

        return new PaqueteEstado(
            datosJ1, datosJ2, datosClientes, datosEstaciones,
            gestorPuntaje.getPuntajeActual(),
            tiempoRestante,
            juegoTerminado,
            razonFin != null ? razonFin : ""
        );
    }

    public boolean estanJugadoresListos() {
        return jugador1 != null && jugador2 != null;
    }

}
