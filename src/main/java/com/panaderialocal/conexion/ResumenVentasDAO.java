package com.panaderialocal.conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Consultas para la pantalla "Resumen de ventas".
 *
 * Los ingresos salen de dos lugares:
 *   1) ventas de caja       -> tablas ventas / detalle_venta
 *   2) pedidos COMPLETADOS  -> tablas pedidos / detalle_pedido
 *
 * Para saber cuando se completo un pedido se usa la columna
 * pedidos.fecha_completado, que este DAO crea solo (una vez) junto con un
 * trigger que la llena cuando el estado pasa a Completado. Los pedidos que ya
 * estaban completados antes usan su fecha de entrega.
 */
public class ResumenVentasDAO {

    // SQLite guarda CURRENT_TIMESTAMP en UTC. "localtime" lo convierte a la
    // hora de tu computadora. Si las horas salen desfasadas porque tu
    // CajeroController ya guarda la hora local, cambia "localtime" por "+0 minutes".
    private static final String AJUSTE_HORA = "localtime";

    private static final String HORA_VENTA =
            "datetime(v.fecha_hora, '" + AJUSTE_HORA + "')";

    private static final String HORA_PEDIDO =
            "datetime(p.fecha_completado, '" + AJUSTE_HORA + "')";

    // Etiqueta con la que los pedidos aparecen como "metodo de pago"
    public static final String METODO_PEDIDOS = "Pedidos";

    // Todos los ingresos en una sola lista: una fila por venta o pedido
    private static final String INGRESOS =
            "SELECT 'venta' AS origen, v.id_venta AS id, v.total AS total, "
            + "v.metodo_pago AS metodo, " + HORA_VENTA + " AS momento, "
            + "date(" + HORA_VENTA + ") AS dia "
            + "FROM ventas v "
            + "UNION ALL "
            + "SELECT 'pedido', p.id_pedido, p.total, '" + METODO_PEDIDOS + "', "
            + "CASE WHEN p.fecha_completado IS NULL THEN NULL ELSE " + HORA_PEDIDO + " END, "
            + "COALESCE(date(" + HORA_PEDIDO + "), date(p.fecha_entrega)) "
            + "FROM pedidos p "
            + "WHERE p.estado IN ('Completado', 'Entregado')";

    // Todos los renglones de producto vendidos (ventas + pedidos completados)
    private static final String DETALLES =
            "SELECT dv.id_pan AS id_pan, dv.cantidad AS cantidad, "
            + "dv.subtotal AS subtotal, date(" + HORA_VENTA + ") AS dia "
            + "FROM detalle_venta dv "
            + "INNER JOIN ventas v ON v.id_venta = dv.id_venta "
            + "UNION ALL "
            + "SELECT dp.id_pan, dp.cantidad, dp.cantidad * pa.precio, "
            + "COALESCE(date(" + HORA_PEDIDO + "), date(p.fecha_entrega)) "
            + "FROM detalle_pedido dp "
            + "INNER JOIN pedidos p ON p.id_pedido = dp.id_pedido "
            + "INNER JOIN panes pa ON pa.id_pan = dp.id_pan "
            + "WHERE p.estado IN ('Completado', 'Entregado')";

    private static final String FILTRO_FECHAS = "i.dia BETWEEN ? AND ?";

    private static boolean esquemaListo = false;

    // ---------------------------------------------------------
    // TOTAL DE INGRESOS Y NUMERO DE VENTAS/PEDIDOS
    // ---------------------------------------------------------
    public Totales obtenerTotales(LocalDate desde, LocalDate hasta)
            throws SQLException {

        String sql = "SELECT COALESCE(SUM(i.total), 0), COUNT(*) "
                + "FROM (" + INGRESOS + ") i WHERE " + FILTRO_FECHAS;

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Totales(rs.getDouble(1), rs.getInt(2));
            }
        }
    }

    // ---------------------------------------------------------
    // TOTAL POR METODO DE PAGO (los pedidos van aparte)
    // ---------------------------------------------------------
    public Map<String, Double> totalesPorMetodo(
            LocalDate desde, LocalDate hasta) throws SQLException {

        Map<String, Double> resultado = new LinkedHashMap<>();
        resultado.put("Efectivo", 0.0);
        resultado.put("Tarjeta", 0.0);
        resultado.put("Transferencia", 0.0);
        resultado.put(METODO_PEDIDOS, 0.0);

        double otros = 0;

        String sql = "SELECT i.metodo, SUM(i.total) "
                + "FROM (" + INGRESOS + ") i WHERE " + FILTRO_FECHAS
                + " GROUP BY i.metodo";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String metodo = rs.getString(1);
                    double total = rs.getDouble(2);

                    if (metodo != null && resultado.containsKey(metodo)) {
                        resultado.put(metodo, total);
                    } else {
                        otros += total;
                    }
                }
            }
        }

        if (otros > 0) {
            resultado.put("Otros", otros);
        }

        return resultado;
    }

    // ---------------------------------------------------------
    // INGRESOS POR HORA DEL DIA (indice 0..23)
    // Solo cuentan las filas que tienen hora (los pedidos completados
    // antes de crear fecha_completado no la tienen).
    // ---------------------------------------------------------
    public double[] ventasPorHora(LocalDate desde, LocalDate hasta)
            throws SQLException {

        double[] horas = new double[24];

        String sql = "SELECT CAST(strftime('%H', i.momento) AS INTEGER), "
                + "SUM(i.total) FROM (" + INGRESOS + ") i "
                + "WHERE i.momento IS NOT NULL AND " + FILTRO_FECHAS
                + " GROUP BY 1";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int hora = rs.getInt(1);
                    if (hora >= 0 && hora < 24) {
                        horas[hora] = rs.getDouble(2);
                    }
                }
            }
        }

        return horas;
    }

    // ---------------------------------------------------------
    // INGRESOS POR DIA
    // ---------------------------------------------------------
    public Map<LocalDate, Double> ventasPorDia(
            LocalDate desde, LocalDate hasta) throws SQLException {

        Map<LocalDate, Double> dias = new TreeMap<>();

        String sql = "SELECT i.dia, SUM(i.total) "
                + "FROM (" + INGRESOS + ") i WHERE " + FILTRO_FECHAS
                + " GROUP BY i.dia";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dias.put(LocalDate.parse(rs.getString(1)),
                            rs.getDouble(2));
                }
            }
        }

        return dias;
    }

    // ---------------------------------------------------------
    // ULTIMOS MOVIMIENTOS (metodo = null significa "todos")
    // ---------------------------------------------------------
    public List<VentaReciente> ultimasVentas(
            LocalDate desde, LocalDate hasta,
            String metodo, int limite) throws SQLException {

        List<VentaReciente> ventas = new ArrayList<>();

        String filtroMetodo = metodo != null ? " AND i.metodo = ?" : "";

        String sql = "SELECT i.origen, i.id, strftime('%d/%m', i.dia), "
                + "COALESCE(strftime('%H:%M', i.momento), '—'), "
                + "i.metodo, i.total, "
                + "CASE WHEN i.origen = 'venta' "
                + "THEN (SELECT COUNT(*) FROM detalle_venta d WHERE d.id_venta = i.id) "
                + "ELSE (SELECT COUNT(*) FROM detalle_pedido d WHERE d.id_pedido = i.id) END "
                + "FROM (" + INGRESOS + ") i "
                + "WHERE " + FILTRO_FECHAS + filtroMetodo
                + " ORDER BY COALESCE(i.momento, i.dia || ' 00:00:00') DESC, i.id DESC "
                + "LIMIT ?";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, desde.toString());
            ps.setString(i++, hasta.toString());
            if (metodo != null) {
                ps.setString(i++, metodo);
            }
            ps.setInt(i, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ventas.add(new VentaReciente(
                            rs.getString(1),
                            rs.getInt(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getString(5),
                            rs.getDouble(6),
                            rs.getInt(7)
                    ));
                }
            }
        }

        return ventas;
    }

    // ---------------------------------------------------------
    // PRODUCTOS MAS VENDIDOS (ventas + pedidos completados)
    // ---------------------------------------------------------
    public List<ProductoTop> productosTop(
            LocalDate desde, LocalDate hasta, int limite)
            throws SQLException {

        List<ProductoTop> productos = new ArrayList<>();

        String sql = "SELECT p.nombre, SUM(d.cantidad), SUM(d.subtotal) "
                + "FROM (" + DETALLES + ") d "
                + "INNER JOIN panes p ON p.id_pan = d.id_pan "
                + "WHERE d.dia BETWEEN ? AND ? "
                + "GROUP BY p.id_pan "
                + "ORDER BY SUM(d.cantidad) DESC, SUM(d.subtotal) DESC "
                + "LIMIT ?";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            ps.setInt(3, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(new ProductoTop(
                            rs.getString(1),
                            rs.getInt(2),
                            rs.getDouble(3)
                    ));
                }
            }
        }

        return productos;
    }

    // ---------------------------------------------------------
    // CONEXION + PREPARACION DE LA BASE (una sola vez)
    // ---------------------------------------------------------
    private Connection conectar() throws SQLException {

        Connection con = ConexionBD.conectar();

        if (con == null) {
            throw new SQLException("No fue posible conectar con SQLite.");
        }

        try {
            asegurarEsquema(con);
        } catch (SQLException ex) {
            con.close();
            throw ex;
        }

        return con;
    }

    /**
     * Agrega pedidos.fecha_completado y un trigger que la llena sola cuando
     * un pedido pasa a Completado. No modifica ni borra ningun dato.
     */
    private static synchronized void asegurarEsquema(Connection con)
            throws SQLException {

        if (esquemaListo) {
            return;
        }

        boolean existeColumna = false;

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(pedidos)")) {

            while (rs.next()) {
                if ("fecha_completado".equalsIgnoreCase(rs.getString("name"))) {
                    existeColumna = true;
                }
            }
        }

        if (!existeColumna) {
            try (Statement st = con.createStatement()) {
                st.execute("ALTER TABLE pedidos ADD COLUMN fecha_completado TEXT");
            }
        }

        try (Statement st = con.createStatement()) {
            st.execute("CREATE TRIGGER IF NOT EXISTS trg_pedido_completado "
                    + "AFTER UPDATE OF estado ON pedidos "
                    + "WHEN NEW.estado IN ('Completado', 'Entregado') "
                    + "AND OLD.estado NOT IN ('Completado', 'Entregado') "
                    + "AND NEW.fecha_completado IS NULL "
                    + "BEGIN "
                    + "UPDATE pedidos SET fecha_completado = CURRENT_TIMESTAMP "
                    + "WHERE id_pedido = NEW.id_pedido; "
                    + "END");
        }

        esquemaListo = true;
    }

    // ---------------------------------------------------------
    // MODELOS
    // ---------------------------------------------------------
    public static class Totales {

        private final double total;
        private final int numeroVentas;

        public Totales(double total, int numeroVentas) {
            this.total = total;
            this.numeroVentas = numeroVentas;
        }

        public double getTotal() {
            return total;
        }

        public int getNumeroVentas() {
            return numeroVentas;
        }

        public double getTicketPromedio() {
            return numeroVentas == 0 ? 0 : total / numeroVentas;
        }
    }

    public static class VentaReciente {

        private final String origen;   // "venta" o "pedido"
        private final int folio;
        private final String fecha;
        private final String hora;
        private final String metodo;
        private final double total;
        private final int productos;

        public VentaReciente(String origen, int folio, String fecha,
                String hora, String metodo, double total, int productos) {
            this.origen = origen;
            this.folio = folio;
            this.fecha = fecha;
            this.hora = hora;
            this.metodo = metodo;
            this.total = total;
            this.productos = productos;
        }

        public String getOrigen() { return origen; }
        public int getFolio() { return folio; }
        public String getFecha() { return fecha; }
        public String getHora() { return hora; }
        public String getMetodo() { return metodo; }
        public double getTotal() { return total; }
        public int getProductos() { return productos; }

        /** "#0045" para ventas de caja y "#PED-0012" para pedidos. */
        public String getEtiquetaFolio() {
            return "pedido".equals(origen)
                    ? String.format("#PED-%04d", folio)
                    : String.format("#%04d", folio);
        }
    }

    public static class ProductoTop {

        private final String nombre;
        private final int unidades;
        private final double total;

        public ProductoTop(String nombre, int unidades, double total) {
            this.nombre = nombre;
            this.unidades = unidades;
            this.total = total;
        }

        public String getNombre() { return nombre; }
        public int getUnidades() { return unidades; }
        public double getTotal() { return total; }
    }
}
