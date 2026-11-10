package com.panaderialocal.conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas para la pantalla "Historial de pedidos".
 * Solo LEE de las tablas pedidos, clientes, detalle_pedido y panes.
 */
public class HistorialPedidosDAO {

    // fecha_pedido se guarda con CURRENT_TIMESTAMP (UTC), por eso se
    // convierte a la hora de la computadora con 'localtime'.
    private static final String FECHA_PEDIDO_LOCAL =
            "datetime(p.fecha_pedido, 'localtime')";

    // ---------------------------------------------------------
    // TODOS LOS PEDIDOS (del mas nuevo al mas viejo)
    // ---------------------------------------------------------
    public List<PedidoHistorial> obtenerPedidos() throws SQLException {

        List<PedidoHistorial> pedidos = new ArrayList<>();

        String sql = "SELECT p.id_pedido, c.nombre, c.numero_contacto, "
                + "date(" + FECHA_PEDIDO_LOCAL + "), "
                + "strftime('%H:%M', " + FECHA_PEDIDO_LOCAL + "), "
                + "p.fecha_entrega, p.estado, "
                + "COALESCE(p.adelanto, 0), p.total, p.detalle_extra, "
                + "COALESCE(SUM(dp.cantidad), 0), "
                + "COUNT(dp.id_detalle_pedido) "
                + "FROM pedidos p "
                + "INNER JOIN clientes c ON c.id_cliente = p.id_cliente "
                + "LEFT JOIN detalle_pedido dp ON dp.id_pedido = p.id_pedido "
                + "GROUP BY p.id_pedido "
                + "ORDER BY p.id_pedido DESC";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                pedidos.add(new PedidoHistorial(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7),
                        rs.getDouble(8),
                        rs.getDouble(9),
                        rs.getString(10),
                        rs.getInt(11),
                        rs.getInt(12)
                ));
            }
        }

        return pedidos;
    }

    // ---------------------------------------------------------
    // PRODUCTOS DE UN PEDIDO
    // ---------------------------------------------------------
    public List<ItemPedido> obtenerItems(int idPedido) throws SQLException {

        List<ItemPedido> items = new ArrayList<>();

        String sql = "SELECT pa.nombre, dp.cantidad, dp.precio_unitario "
                + "FROM detalle_pedido dp "
                + "INNER JOIN panes pa ON pa.id_pan = dp.id_pan "
                + "WHERE dp.id_pedido = ? "
                + "ORDER BY dp.id_detalle_pedido";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPedido);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new ItemPedido(
                            rs.getString(1),
                            rs.getInt(2),
                            rs.getDouble(3)
                    ));
                }
            }
        }

        return items;
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
    public static class PedidoHistorial {

        private final int idPedido;
        private final String cliente;
        private final String telefono;
        private final String fechaPedido;   // yyyy-MM-dd (hora local)
        private final String horaPedido;    // HH:mm (hora local)
        private final String fechaEntrega;  // yyyy-MM-dd
        private final String estado;
        private final double adelanto;
        private final double total;
        private final String observaciones;
        private final int articulos;        // suma de cantidades
        private final int conceptos;        // numero de renglones

        public PedidoHistorial(int idPedido, String cliente, String telefono,
                String fechaPedido, String horaPedido, String fechaEntrega,
                String estado, double adelanto, double total,
                String observaciones, int articulos, int conceptos) {
            this.idPedido = idPedido;
            this.cliente = cliente;
            this.telefono = telefono;
            this.fechaPedido = fechaPedido;
            this.horaPedido = horaPedido;
            this.fechaEntrega = fechaEntrega;
            this.estado = estado;
            this.adelanto = adelanto;
            this.total = total;
            this.observaciones = observaciones;
            this.articulos = articulos;
            this.conceptos = conceptos;
        }

        public int getIdPedido() { return idPedido; }
        public String getCliente() { return cliente; }
        public String getTelefono() { return telefono; }
        public String getFechaPedido() { return fechaPedido; }
        public String getHoraPedido() { return horaPedido; }
        public String getFechaEntrega() { return fechaEntrega; }
        public String getEstado() { return estado; }
        public double getAdelanto() { return adelanto; }
        public double getTotal() { return total; }
        public String getObservaciones() { return observaciones; }
        public int getArticulos() { return articulos; }
        public int getConceptos() { return conceptos; }
    }

    public static class ItemPedido {

        private final String nombre;
        private final int cantidad;
        private final double precioUnitario;

        public ItemPedido(String nombre, int cantidad, double precioUnitario) {
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
        }

        public String getNombre() { return nombre; }
        public int getCantidad() { return cantidad; }
        public double getPrecioUnitario() { return precioUnitario; }
        public double getSubtotal() { return cantidad * precioUnitario; }
    }
}
