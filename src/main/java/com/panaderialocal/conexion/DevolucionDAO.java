package com.panaderialocal.conexion;

import com.panaderialocal.conexion.RegistroPedidosDAO.DetallePedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DevolucionDAO {

    // La tabla devoluciones solo admite ventas (id_venta NOT NULL), por eso
    // las devoluciones de pedidos se guardan en devoluciones_pedido.
    private static boolean esquemaListo = false;

    // CONSULTAR PEDIDOS Y VENTAS QUE SE PUEDEN DEVOLVER
    public List<OrigenDevolucion> obtenerOrigenes()
            throws SQLException {

        List<OrigenDevolucion> origenes = new ArrayList<>();

        origenes.addAll(consultarPedidos());
        origenes.addAll(consultarVentas());

        return origenes;
    }

    // PEDIDOS DE CLIENTES YA ENTREGADOS (el cliente ya recibio el pan)
    private List<OrigenDevolucion> consultarPedidos()
            throws SQLException {

        List<OrigenDevolucion> pedidos = new ArrayList<>();

        String sql = """
            SELECT p.id_pedido,
                   c.nombre AS cliente,
                   p.fecha_entrega,
                   p.estado,
                   p.total
            FROM pedidos p
            INNER JOIN clientes c
                ON c.id_cliente = p.id_cliente
            WHERE p.estado IN ('Completado', 'Entregado')
              AND p.total > 0.004
            ORDER BY p.id_pedido DESC
            LIMIT 50
            """;

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                BigDecimal total = BigDecimal.valueOf(
                        rs.getDouble("total")
                ).setScale(2, RoundingMode.HALF_UP);

                pedidos.add(new OrigenDevolucion(
                    true,
                    rs.getInt("id_pedido"),
                    "Pedido #" + rs.getInt("id_pedido")
                        + " - " + rs.getString("cliente")
                        + " - Entrega " + rs.getString("fecha_entrega")
                        + " - " + rs.getString("estado")
                        + " - Total $" + total,
                    total
                ));
            }
        }

        return pedidos;
    }

    // VENTAS QUE TODAVIA TIENEN SALDO POR DEVOLVER
    private List<OrigenDevolucion> consultarVentas()
            throws SQLException {

        List<OrigenDevolucion> ventas = new ArrayList<>();

        String sql = """
            SELECT id_venta, fecha, metodo_pago, total, devuelto
            FROM (
                SELECT v.id_venta,
                       datetime(v.fecha_hora, 'localtime') AS fecha,
                       v.metodo_pago,
                       v.total,
                       COALESCE((SELECT SUM(d.monto_devuelto)
                                 FROM devoluciones d
                                 WHERE d.id_venta = v.id_venta), 0)
                           AS devuelto
                FROM ventas v
            )
            WHERE total - devuelto > 0.004
            ORDER BY id_venta DESC
            LIMIT 50
            """;

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                BigDecimal total = BigDecimal.valueOf(
                        rs.getDouble("total")
                ).setScale(2, RoundingMode.HALF_UP);

                BigDecimal saldo = total.subtract(
                        BigDecimal.valueOf(rs.getDouble("devuelto"))
                ).setScale(2, RoundingMode.HALF_UP);

                ventas.add(new OrigenDevolucion(
                    false,
                    rs.getInt("id_venta"),
                    "Venta #" + rs.getInt("id_venta")
                        + " - " + rs.getString("fecha")
                        + " - " + rs.getString("metodo_pago")
                        + " - Total $" + total
                        + " - Reembolsable $" + saldo,
                    saldo
                ));
            }
        }

        return ventas;
    }

    // CONSULTAR LOS PRODUCTOS DEL PEDIDO O DE LA VENTA
    public List<DetallePedido> obtenerLineas(OrigenDevolucion origen)
            throws SQLException {

        if (origen.esPedido()) {
            return new RegistroPedidosDAO()
                    .obtenerDetallesPedido(origen.getId());
        }

        return obtenerLineasVenta(origen.getId());
    }

    // El precio unitario de una venta se obtiene de subtotal / cantidad.
    private List<DetallePedido> obtenerLineasVenta(int idVenta)
            throws SQLException {

        List<DetallePedido> lineas = new ArrayList<>();

        String sql = """
            SELECT dv.id_pan,
                   p.nombre,
                   dv.cantidad,
                   dv.subtotal
            FROM detalle_venta dv
            INNER JOIN panes p
                ON p.id_pan = dv.id_pan
            WHERE dv.id_venta = ?
            ORDER BY dv.id_detalle_venta
            """;

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idVenta);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    int cantidad = rs.getInt("cantidad");

                    if (cantidad <= 0) {
                        continue;
                    }

                    lineas.add(new DetallePedido(
                        rs.getInt("id_pan"),
                        rs.getString("nombre"),
                        cantidad,
                        rs.getDouble("subtotal") / cantidad
                    ));
                }
            }
        }

        return lineas;
    }

    // REGISTRAR DEVOLUCION
    // Pedido: descuenta el pan devuelto del pedido y de su total.
    // Venta: registra el reembolso contra la venta.
    // Retorna el importe realmente devuelto.
    public BigDecimal registrarDevolucion(
            OrigenDevolucion origen,
            List<LineaDevuelta> lineas,
            String motivo) throws SQLException {

        if (origen == null) {
            throw new SQLException(
                "Debe seleccionar un pedido o una venta."
            );
        }

        if (lineas == null || lineas.isEmpty()) {
            throw new SQLException(
                "Debe indicar al menos un producto a devolver."
            );
        }

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new SQLException(
                "El motivo de la devolucion es obligatorio."
            );
        }

        try (Connection con = conectar()) {

            asegurarEsquema(con);

            boolean autocommit = con.getAutoCommit();
            con.setAutoCommit(false);

            try {

                BigDecimal monto = origen.esPedido()
                        ? devolverDePedido(
                            con, origen.getId(), lineas, motivo.trim())
                        : devolverDeVenta(
                            con, origen.getId(), lineas, motivo.trim());

                con.commit();
                return monto;

            } catch (SQLException | RuntimeException ex) {

                con.rollback();
                throw ex;

            } finally {

                con.setAutoCommit(autocommit);
            }
        }
    }

    // DESCUENTA EL PAN DEVUELTO DEL PEDIDO DEL CLIENTE
    private BigDecimal devolverDePedido(
            Connection con,
            int idPedido,
            List<LineaDevuelta> lineas,
            String motivo) throws SQLException {

        String consultaPedido = """
            SELECT estado, total
            FROM pedidos
            WHERE id_pedido = ?
            """;

        BigDecimal total;

        try (PreparedStatement ps =
                con.prepareStatement(consultaPedido)) {

            ps.setInt(1, idPedido);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    throw new SQLException("No se encontro el pedido.");
                }

                String estado = rs.getString("estado");

                if (!"Completado".equalsIgnoreCase(estado)
                        && !"Entregado".equalsIgnoreCase(estado)) {

                    throw new SQLException(
                        "Solo se puede devolver pan de pedidos "
                        + "completados o entregados."
                    );
                }

                total = BigDecimal.valueOf(rs.getDouble("total"))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal monto = BigDecimal.ZERO;

        for (LineaDevuelta linea : lineas) {
            monto = monto.add(
                descontarProducto(con, idPedido, linea)
            );
        }

        if (monto.compareTo(total) > 0) {
            throw new SQLException(
                "El importe a devolver excede el total del pedido."
            );
        }

        // Nuevo total del pedido. Si el adelanto quedara mayor que el
        // nuevo total, se ajusta para que el saldo no sea negativo.
        BigDecimal nuevoTotal = total.subtract(monto);

        String actualizar = """
            UPDATE pedidos
            SET total = ?,
                adelanto = MIN(COALESCE(adelanto, 0), ?),
                detalle_extra = CASE
                    WHEN detalle_extra IS NULL
                        OR TRIM(detalle_extra) = ''
                        THEN ?
                    ELSE detalle_extra || char(10) || ?
                END
            WHERE id_pedido = ?
            """;

        try (PreparedStatement ps =
                con.prepareStatement(actualizar)) {

            ps.setDouble(1, nuevoTotal.doubleValue());
            ps.setDouble(2, nuevoTotal.doubleValue());
            ps.setString(3, motivo);
            ps.setString(4, motivo);
            ps.setInt(5, idPedido);
            ps.executeUpdate();
        }

        String insertar = """
            INSERT INTO devoluciones_pedido
            (id_pedido, motivo, monto_devuelto)
            VALUES (?, ?, ?)
            """;

        try (PreparedStatement ps =
                con.prepareStatement(insertar)) {

            ps.setInt(1, idPedido);
            ps.setString(2, motivo);
            ps.setDouble(3, monto.doubleValue());
            ps.executeUpdate();
        }

        return monto;
    }

    // QUITA LAS PIEZAS DEVUELTAS DE detalle_pedido Y RETORNA SU IMPORTE
    // (precio_unitario guardado en cada renglon del pedido x piezas: el mismo
    // precio que muestra la pantalla, aunque el precio del pan haya cambiado)
    private BigDecimal descontarProducto(
            Connection con,
            int idPedido,
            LineaDevuelta linea) throws SQLException {

        if (linea.getCantidad() <= 0) {
            throw new SQLException(
                "La cantidad a devolver debe ser mayor que cero."
            );
        }

        String consulta = """
            SELECT dp.id_detalle_pedido,
                   dp.cantidad,
                   dp.precio_unitario
            FROM detalle_pedido dp
            WHERE dp.id_pedido = ?
              AND dp.id_pan = ?
            ORDER BY dp.id_detalle_pedido
            """;

        List<int[]> renglones = new ArrayList<>();
        List<BigDecimal> precios = new ArrayList<>();
        int disponible = 0;

        try (PreparedStatement ps = con.prepareStatement(consulta)) {

            ps.setInt(1, idPedido);
            ps.setInt(2, linea.getIdPan());

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    renglones.add(new int[] {
                        rs.getInt("id_detalle_pedido"),
                        rs.getInt("cantidad")
                    });

                    precios.add(BigDecimal.valueOf(
                        rs.getDouble("precio_unitario")
                    ));
                    disponible += rs.getInt("cantidad");
                }
            }
        }

        if (renglones.isEmpty()) {
            throw new SQLException(
                "El producto " + linea.getProducto()
                + " no esta en el pedido."
            );
        }

        if (linea.getCantidad() > disponible) {
            throw new SQLException(
                "No se pueden devolver " + linea.getCantidad()
                + " pieza(s) de " + linea.getProducto()
                + ": el pedido solo tiene " + disponible + "."
            );
        }

        int pendiente = linea.getCantidad();
        BigDecimal importe = BigDecimal.ZERO;

        for (int i = 0; i < renglones.size(); i++) {

            int[] renglon = renglones.get(i);

            if (pendiente == 0) {
                break;
            }

            int quitar = Math.min(pendiente, renglon[1]);

            // Cada renglon se descuenta a su propio precio.
            importe = importe.add(
                precios.get(i).multiply(BigDecimal.valueOf(quitar))
            );

            // Si se devuelve todo el renglon se elimina, si no se resta.
            if (quitar == renglon[1]) {

                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM detalle_pedido "
                        + "WHERE id_detalle_pedido = ?")) {

                    ps.setInt(1, renglon[0]);
                    ps.executeUpdate();
                }

            } else {

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE detalle_pedido "
                        + "SET cantidad = cantidad - ? "
                        + "WHERE id_detalle_pedido = ?")) {

                    ps.setInt(1, quitar);
                    ps.setInt(2, renglon[0]);
                    ps.executeUpdate();
                }
            }

            pendiente -= quitar;
        }

        return importe.setScale(2, RoundingMode.HALF_UP);
    }

    // REEMBOLSO CONTRA UNA VENTA (nunca mas de lo cobrado)
    private BigDecimal devolverDeVenta(
            Connection con,
            int idVenta,
            List<LineaDevuelta> lineas,
            String motivo) throws SQLException {

        BigDecimal monto = BigDecimal.ZERO;

        for (LineaDevuelta linea : lineas) {
            monto = monto.add(linea.getImporte());
        }

        monto = monto.setScale(2, RoundingMode.HALF_UP);

        BigDecimal saldo = obtenerSaldoVenta(con, idVenta);

        if (monto.signum() <= 0) {
            throw new SQLException(
                "El monto a devolver debe ser mayor que cero."
            );
        }

        if (monto.compareTo(saldo) > 0) {
            throw new SQLException(
                "El monto excede el saldo reembolsable "
                + "de la venta (" + saldo + ")."
            );
        }

        String insertar = """
            INSERT INTO devoluciones
            (id_venta, motivo, monto_devuelto)
            VALUES (?, ?, ?)
            """;

        try (PreparedStatement ps = con.prepareStatement(insertar)) {

            ps.setInt(1, idVenta);
            ps.setString(2, motivo);
            ps.setDouble(3, monto.doubleValue());
            ps.executeUpdate();
        }

        return monto;
    }

    // SALDO REEMBOLSABLE = TOTAL DE LA VENTA - LO YA DEVUELTO
    private BigDecimal obtenerSaldoVenta(Connection con, int idVenta)
            throws SQLException {

        String sql = """
            SELECT v.total,
                   COALESCE((SELECT SUM(d.monto_devuelto)
                             FROM devoluciones d
                             WHERE d.id_venta = v.id_venta), 0)
                       AS devuelto
            FROM ventas v
            WHERE v.id_venta = ?
            """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idVenta);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    return BigDecimal.valueOf(rs.getDouble("total"))
                            .subtract(BigDecimal.valueOf(
                                rs.getDouble("devuelto")))
                            .setScale(2, RoundingMode.HALF_UP);
                }
            }
        }

        throw new SQLException("No se encontro la venta.");
    }

    // CREA LA TABLA devoluciones_pedido (una sola vez)
    private void asegurarEsquema(Connection con) throws SQLException {

        if (esquemaListo) {
            return;
        }

        try (Statement st = con.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS devoluciones_pedido (
                    id_devolucion_pedido INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_pedido INTEGER NOT NULL,
                    motivo TEXT,
                    monto_devuelto REAL NOT NULL,
                    fecha_hora TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido)
                )
                """);
        }

        esquemaListo = true;
    }

    // CONEXION A SQLITE
    private Connection conectar() throws SQLException {

        Connection con = ConexionBD.conectar();

        if (con == null) {
            throw new SQLException(
                "No fue posible conectar con SQLite."
            );
        }

        return con;
    }

    // MODELO: PEDIDO O VENTA SOBRE EL QUE SE HACE LA DEVOLUCION
    public static class OrigenDevolucion {

        private final boolean pedido;
        private final int id;
        private final String descripcion;
        private final BigDecimal saldo;

        public OrigenDevolucion(
                boolean pedido,
                int id,
                String descripcion,
                BigDecimal saldo) {

            this.pedido = pedido;
            this.id = id;
            this.descripcion = descripcion;
            this.saldo = saldo;
        }

        public boolean esPedido() {
            return pedido;
        }

        public int getId() {
            return id;
        }

        // Maximo que se puede devolver: total actual del pedido
        // o saldo reembolsable de la venta.
        public BigDecimal getSaldo() {
            return saldo;
        }

        @Override
        public String toString() {
            return descripcion;
        }
    }

    // MODELO: PRODUCTO QUE SE DEVUELVE
    public static class LineaDevuelta {

        private final int idPan;
        private final String producto;
        private final int cantidad;
        private final BigDecimal importe;

        public LineaDevuelta(
                int idPan,
                String producto,
                int cantidad,
                BigDecimal importe) {

            this.idPan = idPan;
            this.producto = producto;
            this.cantidad = cantidad;
            this.importe = importe;
        }

        public int getIdPan() {
            return idPan;
        }

        public String getProducto() {
            return producto;
        }

        public int getCantidad() {
            return cantidad;
        }

        public BigDecimal getImporte() {
            return importe;
        }
    }
}