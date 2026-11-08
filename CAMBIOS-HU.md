# Cambios: HU1, HU3, HU7 y HU15

## Uso desde NetBeans
Abre el proyecto habitual y ejecuta Run Project (F6) con JDK 23. La base es `panaderia.db` en la carpeta del proyecto.

- **HU1:** agrega piezas con + o escribe la cantidad en la tarjeta del pan. La cuenta suma en un mismo renglón los panes de igual precio, conservando el producto exacto en el detalle de la venta.
- **HU3:** selecciona Efectivo, Tarjeta o Transferencia y pulsa Finalizar venta. Efectivo exige importe suficiente y muestra cambio. Tarjeta/transferencia exigen referencia y confirmación del cajero. No hay integración con bancos o terminales: se registra un cobro que ya fue confirmado externamente.
- **HU7:** Editar pedido → seleccionar y cargar → escoger pan/cantidad → Agregar / cambiar cantidad o Quitar seleccionado → Guardar cambios del pedido. También puedes modificar entrega y notas, actualizar adelanto o cancelar con confirmación. Un pedido cancelado/completado no se modifica. Si el nuevo total es menor que el adelanto, primero debes resolver y ajustar ese adelanto.
- **HU15:** Administrador → Catálogo de panes → Modificar. Guarda nombre, descripción, precio positivo con hasta dos decimales y foto. Los pedidos y ventas existentes conservan sus precios.

## Base de datos
La actualización agrega precio_unitario a detalle_pedido, referencia_pago/efectivo_recibido a ventas y fecha_completado a pedidos. Conserva tablas y registros existentes. Los tres pedidos encontrados tienen totales compatibles con el catálogo actual; la migración conserva sus importes y precios. Para una base antigua distinta, sin precios históricos guardados, se distribuye el total según los precios relativos del catálogo disponible.

Las ventas y las ediciones de pedidos se guardan en transacciones: si falla un detalle, no queda una operación incompleta. Se habilitan claves foráneas. Si falta el archivo de base de datos se informa el error en vez de crear una base vacía accidentalmente. Puede indicarse otra ruta mediante la propiedad Java `-Dpanaderia.db=RUTA`.

También se corrigió la apertura de Devoluciones (símbolo $ en FXML), la hoja de estilos del resumen de ventas, el acceso JavaFX a los modelos de tablas y una ruta de acceso a la pantalla de caja.

## Verificación reproducible
Ejecuta `verificacion\Verificar.ps1` en PowerShell. Compila todo con JDK 23 y las dependencias ya descargadas por Maven, crea `target\verificacion\pruebas.db`, comprueba 26 casos de integración y carga las 18 pantallas FXML. Comprueba también la agrupación por precio en ambas pantallas de caja. No ejecuta las ventas de prueba en la base original.

La compilación Maven también fue verificada con `mvn compile`. Persisten avisos previos sobre APIs obsoletas, operaciones genéricas y FXML declarado como versión 25 ejecutado con JavaFX 23; no impiden la compilación ni la carga de las pantallas.
