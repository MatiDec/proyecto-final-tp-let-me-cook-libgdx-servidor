# CHANGELOG (Versión de servidor)

## [Nov 3, 2025]

### Correcciones
- Se corrigieron los **getters** de los paquetes.
- Se corrigió un error en los **nombres de las bebidas** que impedía que se leyeran sus texturas en el cliente.
- Se arregló la **animación de los jugadores**.
- Se corrigió un error en la **carga de niveles**.

### Mejoras
- Arreglado el sistema de **envío de paquetes de desconexión**.
- Ajustes en la **frecuencia** y **máquinas disponibles** para aparecer fuera de servicio.
- Cambios en la **lógica de indicadores**.
- Actualización de **reglas del servidor**.
- **Mapas actualizados** nuevamente.

### Estado
- **Debug en proceso.**

---

## [Nov 2, 2025]

### Actualizaciones
- Actualizados **mapas**.

---

## [Nov 1, 2025]

### Correcciones
- Se eliminó la **duplicación de paquetes** al procesarlos.

---

## [Oct 31, 2025]

### Mejoras y Seguridad
- Se agregó verificación para evitar que **dos jugadores se conecten desde la misma dirección IP y puerto**.

### Lógica y Multijugador
- Modificada la lógica de las clases del **modo local** para que sean funcionales en el **modo multijugador online**.
- Añadidos manejos lógicos (parcialmente) del **modo multijugador online**.

---

## [Oct 30, 2025]

### Hilos y Conectividad
- Arreglo en el **manejo de hilos del servidor**.
- Se logró realizar la **conexión** y ejecutar el juego correctamente en los clientes (aunque con múltiples defectos).

---

## [Oct 29, 2025]

### Servidor
- [Servidor] Realizada la **conexión inicial básica** entre servidor y cliente.
- **Commit inicial** para la versión del servidor.
