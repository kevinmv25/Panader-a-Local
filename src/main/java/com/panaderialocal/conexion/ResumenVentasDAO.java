package com.panaderialocal.conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Consultas para la pantalla "Resumen de ventas".
 * Solo LEE de las tablas ventas, detalle_venta y panes.
 */
public class ResumenVentasDAO {

    // SQLite guarda CURRENT_TIMESTAMP en UTC. "localtime" lo convierte a la
    // hora de tu computadora. Si las horas salen desfasadas porque tu
    // CajeroController ya guarda la hora local, cambia "localtime" por "+0 minutes".
    private static final String AJUSTE_HORA = "localtime";

    private static final String HORA =
            "datetime(v.fecha_hora, '" + AJUSTE_HORA + "')";

    private static final String FILTRO_FECHAS =
            "date(" + HORA + ") BETWEEN ? AND ?";

    // ---------------------------------------------------------
    // TOTAL DE VENTAS Y NUMERO DE VENTAS
    // ---------------------------------------------------------
    public Totales obtenerTotales(LocalDate desde, LocalDate hasta)
            throws SQLException {

        String sql = "SELECT COALESCE(SUM(v.total), 0), COUNT(*) "
                + "FROM ventas v WHERE " + FILTRO_FECHAS;

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
    // TOTAL POR METODO DE PAGO
    // ---------------------------------------------------------
    public Map<String, Double> totalesPorMetodo(
            LocalDate desde, LocalDate hasta) throws SQLException {

        Map<String, Double> resultado = new LinkedHashMap<>();
        resultado.put("Efectivo", 0.0);
        resultado.put("Tarjeta", 0.0);
        resultado.put("Transferencia", 0.0);

        double otros = 0;

        String sql = "SELECT v.metodo_pago, SUM(v.total) "
                + "FROM ventas v WHERE " + FILTRO_FECHAS
                + " GROUP BY v.metodo_pago";

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
    // VENTAS POR HORA DEL DIA (indice 0..23)
    // ---------------------------------------------------------
    public double[] ventasPorHora(LocalDate desde, LocalDate hasta)
            throws SQLException {

        double[] horas = new double[24];

        String sql = "SELECT CAST(strftime('%H', " + HORA + ") AS INTEGER), "
                + "SUM(v.total) FROM ventas v WHERE " + FILTRO_FECHAS
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
    // VENTAS POR DIA
    // ---------------------------------------------------------
    public Map<LocalDate, Double> ventasPorDia(
            LocalDate desde, LocalDate hasta) throws SQLException {

        Map<LocalDate, Double> dias = new TreeMap<>();

        String sql = "SELECT date(" + HORA + "), SUM(v.total) "
                + "FROM ventas v WHERE " + FILTRO_FECHAS
                + " GROUP BY date(" + HORA + ")";

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
    // ULTIMAS VENTAS (metodo = null significa "todos")
    // ---------------------------------------------------------
    public List<VentaReciente> ultimasVentas(
            LocalDate desde, LocalDate hasta,
            String metodo, int limite) throws SQLException {

        List<VentaReciente> ventas = new ArrayList<>();

        String sql = "SELECT v.id_venta, "
                + "strftime('%d/%m', " + HORA + "), "
                + "strftime('%H:%M', " + HORA + "), "
                + "v.metodo_pago, v.total, "
                + "COUNT(dv.id_detalle_venta) "
                + "FROM ventas v "
                + "LEFT JOIN detalle_venta dv ON dv.id_venta = v.id_venta "
                + "WHERE " + FILTRO_FECHAS
                + (metodo != null ? " AND v.metodo_pago = ?" : "")
                + " GROUP BY v.id_venta "
                + "ORDER BY v.fecha_hora DESC, v.id_venta DESC LIMIT ?";

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
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getDouble(5),
                            rs.getInt(6)
                    ));
                }
            }
        }

        return ventas;
    }

    // ---------------------------------------------------------
    // PRODUCTOS MAS VENDIDOS
    // ---------------------------------------------------------
    public List<ProductoTop> productosTop(
            LocalDate desde, LocalDate hasta, int limite)
            throws SQLException {

        List<ProductoTop> productos = new ArrayList<>();

        String sql = "SELECT p.nombre, SUM(dv.cantidad), SUM(dv.subtotal) "
                + "FROM detalle_venta dv "
                + "INNER JOIN ventas v ON v.id_venta = dv.id_venta "
                + "INNER JOIN panes p ON p.id_pan = dv.id_pan "
                + "WHERE " + FILTRO_FECHAS
                + " GROUP BY p.id_pan "
                + "ORDER BY SUM(dv.cantidad) DESC, SUM(dv.subtotal) DESC "
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
    // CONEXION
    // ---------------------------------------------------------
    private Connection conectar() throws SQLException {

        Connection con = ConexionBD.conectar();

        if (con == null) {
            throw new SQLException("No fue posible conectar con SQLite.");
        }

        return con;
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

        private final int folio;
        private final String fecha;
        private final String hora;
        private final String metodo;
        private final double total;
        private final int productos;

        public VentaReciente(int folio, String fecha, String hora,
                String metodo, double total, int productos) {
            this.folio = folio;
            this.fecha = fecha;
            this.hora = hora;
            this.metodo = metodo;
            this.total = total;
            this.productos = productos;
        }

        public int getFolio() { return folio; }
        public String getFecha() { return fecha; }
        public String getHora() { return hora; }
        public String getMetodo() { return metodo; }
        public double getTotal() { return total; }
        public int getProductos() { return productos; }
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
