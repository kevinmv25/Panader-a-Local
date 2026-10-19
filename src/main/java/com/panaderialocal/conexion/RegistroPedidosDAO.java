
package com.panaderialocal.conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RegistroPedidosDAO {

    // =========================================================
    // 1. CONSULTAR LOS PANES REGISTRADOS EN LA BASE DE DATOS
    // =========================================================
    public List<Pan> obtenerPanes() throws SQLException {

        List<Pan> panes = new ArrayList<>();
        Connection conexion = ConexionBD.conectar();

        if (conexion == null) {
            throw new SQLException("No fue posible conectar con SQLite.");
        }

        try (Connection con = conexion;
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id_pan, nombre, precio FROM panes ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                panes.add(new Pan(
                        rs.getInt("id_pan"),
                        rs.getString("nombre"),
                        rs.getDouble("precio")
                ));
            }
        }

        return panes;
    }

    // =========================================================
    // 2. REGISTRAR CLIENTE, PEDIDO Y DETALLES DEL PEDIDO
    // =========================================================
    public void registrarPedido(
            String nombreCliente,
            String telefono,
            LocalDate fechaEntrega,
            String estado,
            String observaciones,
            double total,
            List<DetallePedido> detalles) throws SQLException {

        if (nombreCliente == null || nombreCliente.trim().isEmpty()) {
            throw new SQLException("El nombre del cliente es obligatorio.");
        }

        if (fechaEntrega == null) {
            throw new SQLException("La fecha de entrega es obligatoria.");
        }

        if (estado == null || estado.trim().isEmpty()) {
            throw new SQLException("El estado del pedido es obligatorio.");
        }

        if (detalles == null || detalles.isEmpty()) {
            throw new SQLException("El pedido debe incluir al menos un producto.");
        }

        if (total <= 0) {
            throw new SQLException("El total debe ser mayor que cero.");
        }

        Connection conexion = ConexionBD.conectar();

        if (conexion == null) {
            throw new SQLException("No fue posible conectar con SQLite.");
        }

        try (Connection con = conexion) {

            boolean autoCommitOriginal = con.getAutoCommit();
            con.setAutoCommit(false);

            try {
                // A. Buscar al cliente o registrarlo.
                int idCliente = obtenerOCrearCliente(
                        con,
                        nombreCliente.trim(),
                        telefono
                );

                // B. Insertar el pedido.
                String sqlPedido =
                        "INSERT INTO pedidos " +
                        "(id_cliente, fecha_entrega, estado, adelanto, " +
                        "detalle_extra, total) " +
                        "VALUES (?, ?, ?, 0, ?, ?)";

                try (PreparedStatement ps = con.prepareStatement(sqlPedido)) {
                    ps.setInt(1, idCliente);
                    ps.setString(2, fechaEntrega.toString());
                    ps.setString(3, estado);

                    if (observaciones == null || observaciones.trim().isEmpty()) {
                        ps.setNull(4, java.sql.Types.VARCHAR);
                    } else {
                        ps.setString(4, observaciones.trim());
                    }

                    ps.setDouble(5, total);
                    ps.executeUpdate();
                }

                // C. Obtener el identificador del pedido recién insertado.
                int idPedido;

                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT last_insert_rowid()")) {

                    if (!rs.next()) {
                        throw new SQLException(
                                "No se pudo obtener el identificador del pedido."
                        );
                    }

                    idPedido = rs.getInt(1);
                }

                // D. Insertar los productos asociados al pedido.
                String sqlDetalle =
                        "INSERT INTO detalle_pedido " +
                        "(id_pedido, id_pan, cantidad) VALUES (?, ?, ?)";

                try (PreparedStatement psDetalle =
                             con.prepareStatement(sqlDetalle)) {

                    for (DetallePedido detalle : detalles) {

                        if (detalle.getCantidad() < 1) {
                            throw new SQLException(
                                    "La cantidad de cada producto debe ser mayor que cero."
                            );
                        }

                        psDetalle.setInt(1, idPedido);
                        psDetalle.setInt(2, detalle.getIdPan());
                        psDetalle.setInt(3, detalle.getCantidad());
                        psDetalle.addBatch();
                    }

                    psDetalle.executeBatch();
                }

                // E. Confirmar todos los cambios.
                con.commit();

            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;

            } finally {
                con.setAutoCommit(autoCommitOriginal);
            }
        }
    }

    // =========================================================
    // 3. BUSCAR UN CLIENTE EXISTENTE O INSERTAR UNO NUEVO
    // =========================================================
    private int obtenerOCrearCliente(
            Connection con,
            String nombre,
            String telefono) throws SQLException {

        String tel = telefono == null ? "" : telefono.trim();

        String sqlBuscar =
                "SELECT id_cliente FROM clientes " +
                "WHERE nombre = ? " +
                "AND COALESCE(numero_contacto, '') = ? " +
                "LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setString(1, nombre);
            ps.setString(2, tel);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_cliente");
                }
            }
        }

        String sqlInsertar =
                "INSERT INTO clientes (nombre, numero_contacto) VALUES (?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sqlInsertar)) {
            ps.setString(1, nombre);

            if (tel.isEmpty()) {
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(2, tel);
            }

            ps.executeUpdate();
        }

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        throw new SQLException("No se pudo obtener el identificador del cliente.");
    }

    // =========================================================
    // 4. CONSULTAR LOS 20 PEDIDOS MÁS RECIENTES
    // =========================================================
    public List<PedidoReciente> obtenerPedidosRecientes()
            throws SQLException {

        List<PedidoReciente> pedidos = new ArrayList<>();
        Connection conexion = ConexionBD.conectar();

        if (conexion == null) {
            throw new SQLException("No fue posible conectar con SQLite.");
        }

        String sql =
                "SELECT p.id_pedido, c.nombre AS cliente, " +
                "c.numero_contacto, p.fecha_pedido, p.fecha_entrega, " +
                "p.estado, p.total " +
                "FROM pedidos p " +
                "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                "ORDER BY p.id_pedido DESC LIMIT 20";

        try (Connection con = conexion;
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                pedidos.add(new PedidoReciente(
                        rs.getInt("id_pedido"),
                        rs.getString("cliente"),
                        rs.getString("numero_contacto"),
                        rs.getString("fecha_pedido"),
                        rs.getString("fecha_entrega"),
                        rs.getString("estado"),
                        rs.getDouble("total")
                ));
            }
        }

        return pedidos;
    }

    // =========================================================
    // 5. MODELO DE LOS PANES
    // =========================================================
    public static class Pan {

        private final int idPan;
        private final String nombre;
        private final double precio;

        public Pan(int idPan, String nombre, double precio) {
            this.idPan = idPan;
            this.nombre = nombre;
            this.precio = precio;
        }

        public int getIdPan() {
            return idPan;
        }

        public String getNombre() {
            return nombre;
        }

        public double getPrecio() {
            return precio;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    // =========================================================
    // 6. MODELO DE LOS PRODUCTOS DE UN PEDIDO
    // =========================================================
    public static class DetallePedido {

        private final int idPan;
        private final String nombreProducto;
        private final int cantidad;
        private final double precioUnitario;

        public DetallePedido(Pan pan, int cantidad) {
            this.idPan = pan.getIdPan();
            this.nombreProducto = pan.getNombre();
            this.cantidad = cantidad;
            this.precioUnitario = pan.getPrecio();
        }

        public int getIdPan() {
            return idPan;
        }

        public String getNombreProducto() {
            return nombreProducto;
        }

        public int getCantidad() {
            return cantidad;
        }

        public double getPrecioUnitario() {
            return precioUnitario;
        }

        public double getSubtotal() {
            return cantidad * precioUnitario;
        }
    }

    // =========================================================
    // 7. MODELO PARA MOSTRAR PEDIDOS RECIENTES
    // =========================================================
    public static class PedidoReciente {

        private final int idPedido;
        private final String cliente;
        private final String telefono;
        private final String fechaPedido;
        private final String fechaEntrega;
        private final String estado;
        private final double total;

        public PedidoReciente(
                int idPedido,
                String cliente,
                String telefono,
                String fechaPedido,
                String fechaEntrega,
                String estado,
                double total) {

            this.idPedido = idPedido;
            this.cliente = cliente;
            this.telefono = telefono;
            this.fechaPedido = fechaPedido;
            this.fechaEntrega = fechaEntrega;
            this.estado = estado;
            this.total = total;
        }

        public int getIdPedido() {
            return idPedido;
        }

        public String getCliente() {
            return cliente;
        }

        public String getTelefono() {
            return telefono;
        }

        public String getFechaPedido() {
            return fechaPedido;
        }

        public String getFechaEntrega() {
            return fechaEntrega;
        }

        public String getEstado() {
            return estado;
        }

        public double getTotal() {
            return total;
        }
    }
}
