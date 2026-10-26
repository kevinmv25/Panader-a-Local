
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

    // CONSULTAR PANES
    public List<Pan> obtenerPanes() throws SQLException {

        List<Pan> panes = new ArrayList<>();

        String sql = "SELECT id_pan, nombre, precio "
                + "FROM panes ORDER BY nombre";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql);
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

    // REGISTRAR PEDIDO
    public void registrarPedido(
            String nombreCliente,
            String telefono,
            LocalDate fechaEntrega,
            String estado,
            String observaciones,
            double total,
            List<DetallePedido> detalles) throws SQLException {

        if (nombreCliente == null ||
            nombreCliente.trim().isEmpty()) {
            throw new SQLException(
                "El nombre del cliente es obligatorio."
            );
        }

        if (fechaEntrega == null) {
            throw new SQLException(
                "La fecha de entrega es obligatoria."
            );
        }

        if (estado == null || estado.trim().isEmpty()) {
            throw new SQLException(
                "El estado es obligatorio."
            );
        }

        if (detalles == null || detalles.isEmpty()) {
            throw new SQLException(
                "Agrega al menos un producto."
            );
        }

        if (!Double.isFinite(total) || total <= 0) {
            throw new SQLException(
                "El total debe ser mayor que cero."
            );
        }

        try (Connection con = conectar()) {

            boolean autocommit = con.getAutoCommit();
            con.setAutoCommit(false);

            try {

                int idCliente = obtenerOCrearCliente(
                    con, nombreCliente.trim(), telefono
                );

                String sqlPedido = """
                    INSERT INTO pedidos
                    (id_cliente, fecha_entrega, estado,
                     adelanto, detalle_extra, total)
                    VALUES (?, ?, ?, 0, ?, ?)
                    """;

                int idPedido;

                try (PreparedStatement ps =
                        con.prepareStatement(
                            sqlPedido,
                            Statement.RETURN_GENERATED_KEYS
                        )) {

                    ps.setInt(1, idCliente);
                    ps.setString(2, fechaEntrega.toString());
                    ps.setString(3, estado);

                    if (observaciones == null ||
                        observaciones.trim().isEmpty()) {

                        ps.setNull(4, java.sql.Types.VARCHAR);

                    } else {

                        ps.setString(
                            4, observaciones.trim()
                        );
                    }

                    ps.setDouble(5, total);
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {

                        if (rs.next()) {
                            idPedido = rs.getInt(1);

                        } else {

                            try (Statement st = con.createStatement();
                                 ResultSet generado = st.executeQuery(
                                     "SELECT last_insert_rowid()"
                                 )) {

                                if (!generado.next()) {
                                    throw new SQLException(
                                        "No se pudo obtener el ID del pedido."
                                    );
                                }

                                idPedido = generado.getInt(1);
                            }
                        }
                    }
                }

                String sqlDetalle = """
                    INSERT INTO detalle_pedido
                    (id_pedido, id_pan, cantidad)
                    VALUES (?, ?, ?)
                    """;

                try (PreparedStatement ps =
                        con.prepareStatement(sqlDetalle)) {

                    for (DetallePedido detalle : detalles) {

                        if (detalle == null ||
                            detalle.getCantidad() < 1) {

                            throw new SQLException(
                                "Las cantidades deben ser mayores que cero."
                            );
                        }

                        ps.setInt(1, idPedido);
                        ps.setInt(2, detalle.getIdPan());
                        ps.setInt(3, detalle.getCantidad());

                        ps.addBatch();
                    }

                    ps.executeBatch();
                }

                con.commit();

            } catch (SQLException | RuntimeException ex) {

                con.rollback();
                throw ex;

            } finally {

                con.setAutoCommit(autocommit);
            }
        }
    }

    // BUSCAR O REGISTRAR CLIENTE
    private int obtenerOCrearCliente(
            Connection con,
            String nombre,
            String telefono) throws SQLException {

        String tel = telefono == null
                ? "" : telefono.trim();

        String buscar = """
            SELECT id_cliente FROM clientes
            WHERE nombre = ?
              AND COALESCE(numero_contacto, '') = ?
            LIMIT 1
            """;

        try (PreparedStatement ps =
                con.prepareStatement(buscar)) {

            ps.setString(1, nombre);
            ps.setString(2, tel);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("id_cliente");
                }
            }
        }

        String insertar = """
            INSERT INTO clientes
            (nombre, numero_contacto)
            VALUES (?, ?)
            """;

        try (PreparedStatement ps =
                con.prepareStatement(insertar)) {

            ps.setString(1, nombre);

            if (tel.isEmpty()) {
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(2, tel);
            }

            ps.executeUpdate();
        }

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT last_insert_rowid()"
             )) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        throw new SQLException(
            "No se pudo obtener el identificador del cliente."
        );
    }

    // CONSULTAR PEDIDOS RECIENTES
    public List<PedidoReciente> obtenerPedidosRecientes()
            throws SQLException {

        return consultarPedidos(true);
    }

    // CONSULTAR TODOS LOS PEDIDOS
    public List<PedidoReciente> obtenerTodosLosPedidos()
            throws SQLException {

        return consultarPedidos(false);
    }

    // CONSULTA GENERAL DE PEDIDOS
    private List<PedidoReciente> consultarPedidos(
            boolean limitar) throws SQLException {

        List<PedidoReciente> pedidos = new ArrayList<>();

        String sql = """
            SELECT p.id_pedido,
                   c.nombre AS cliente,
                   c.numero_contacto,
                   p.fecha_pedido,
                   p.fecha_entrega,
                   p.estado,
                   p.total
            FROM pedidos p
            INNER JOIN clientes c
                ON p.id_cliente = c.id_cliente
            WHERE p.estado <> 'Cancelado'
            ORDER BY p.id_pedido DESC
            """ + (limitar ? " LIMIT 20" : "");

        try (Connection con = conectar();
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

    // CONSULTAR DETALLES DE UN PEDIDO
    public List<DetallePedido> obtenerDetallesPedido(
            int idPedido) throws SQLException {

        List<DetallePedido> detalles = new ArrayList<>();

        String sql = """
            SELECT dp.id_pan,
                   pa.nombre,
                   pa.precio,
                   dp.cantidad
            FROM detalle_pedido dp
            INNER JOIN panes pa
                ON pa.id_pan = dp.id_pan
            WHERE dp.id_pedido = ?
            ORDER BY dp.id_detalle_pedido
            """;

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPedido);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    detalles.add(new DetallePedido(
                        rs.getInt("id_pan"),
                        rs.getString("nombre"),
                        rs.getInt("cantidad"),
                        rs.getDouble("precio")
                    ));
                }
            }
        }

        return detalles;
    }

    // CONSULTAR UN PEDIDO POR ID
    public PedidoReciente obtenerPedido(int idPedido)
            throws SQLException {

        String sql = """
            SELECT p.id_pedido,
                   c.nombre AS cliente,
                   c.numero_contacto,
                   p.fecha_pedido,
                   p.fecha_entrega,
                   p.estado,
                   p.total
            FROM pedidos p
            INNER JOIN clientes c
                ON p.id_cliente = c.id_cliente
            WHERE p.id_pedido = ?
            """;

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPedido);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    return new PedidoReciente(
                        rs.getInt("id_pedido"),
                        rs.getString("cliente"),
                        rs.getString("numero_contacto"),
                        rs.getString("fecha_pedido"),
                        rs.getString("fecha_entrega"),
                        rs.getString("estado"),
                        rs.getDouble("total")
                    );
                }
            }
        }

        throw new SQLException(
            "No se encontro el pedido."
        );
    }

    // CONSULTAR ADELANTO
    public double obtenerAdelanto(int idPedido)
            throws SQLException {

        String sql = """
            SELECT adelanto FROM pedidos
            WHERE id_pedido = ?
            """;

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPedido);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getDouble("adelanto");
                }
            }
        }

        throw new SQLException(
            "No se encontro el pedido."
        );
    }

    // ACTUALIZAR ADELANTO
    public void actualizarAdelanto(
            int idPedido, double adelanto)
            throws SQLException {

        if (!Double.isFinite(adelanto) || adelanto < 0) {

            throw new SQLException(
                "El adelanto no puede ser negativo."
            );
        }

        String sql = """
            UPDATE pedidos
            SET adelanto = ?
            WHERE id_pedido = ?
              AND estado NOT IN
                  ('Cancelado', 'Completado', 'Entregado')
              AND ? <= total
            """;

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDouble(1, adelanto);
            ps.setInt(2, idPedido);
            ps.setDouble(3, adelanto);

            if (ps.executeUpdate() == 0) {

                throw new SQLException(
                    "No se pudo actualizar el adelanto. "
                    + "Verifica el pedido y el importe."
                );
            }
        }
    }

    // COMPLETAR PEDIDO
    public void completarPedido(
            int idPedido, String motivo)
            throws SQLException {

        actualizarEstado(
            idPedido, "Completado", motivo
        );
    }

    // CANCELAR PEDIDO
    public void cancelarPedido(
            int idPedido, String motivo)
            throws SQLException {

        actualizarEstado(
            idPedido, "Cancelado", motivo
        );
    }

    // ACTUALIZAR ESTADO DEL PEDIDO
    private void actualizarEstado(
            int idPedido,
            String estado,
            String motivo) throws SQLException {

        String comentario = motivo == null
                ? "" : motivo.trim();

        String sql = """
            UPDATE pedidos
            SET estado = ?,
                detalle_extra = CASE
                    WHEN ? = '' THEN detalle_extra
                    WHEN detalle_extra IS NULL
                        OR TRIM(detalle_extra) = ''
                        THEN ?
                    ELSE detalle_extra || char(10) || ?
                END
            WHERE id_pedido = ?
              AND estado NOT IN
                  ('Cancelado', 'Completado', 'Entregado')
            """;

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setString(2, comentario);
            ps.setString(3, estado + ": " + comentario);
            ps.setString(4, estado + ": " + comentario);
            ps.setInt(5, idPedido);

            if (ps.executeUpdate() == 0) {

                throw new SQLException(
                    "No se pudo actualizar el pedido. "
                    + "Puede que ya este cerrado."
                );
            }
        }
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

    // MODELO DE PAN
    public static class Pan {

        private final int idPan;
        private final String nombre;
        private final double precio;

        public Pan(
                int idPan,
                String nombre,
                double precio) {

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

        // Compatibilidad con vistas existentes.
        // La tabla panes no tiene columna de estado.
        public String getEstado() {
            return "Disponible";
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    // MODELO DE DETALLE DE PEDIDO
    public static class DetallePedido {

        private final int idPan;
        private final String nombreProducto;
        private final int cantidad;
        private final double precioUnitario;

        public DetallePedido(Pan pan, int cantidad) {

            this(
                pan.getIdPan(),
                pan.getNombre(),
                cantidad,
                pan.getPrecio()
            );
        }

        public DetallePedido(
                int idPan,
                String nombreProducto,
                int cantidad,
                double precioUnitario) {

            this.idPan = idPan;
            this.nombreProducto = nombreProducto;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
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

    // MODELO DE PEDIDOS RECIENTES
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

        @Override
        public String toString() {

            return "Pedido #" + idPedido
                + " - " + cliente
                + " - " + estado
                + " - $"
                + String.format(
                    java.util.Locale.US,
                    "%.2f",
                    total
                );
        }
    }
}
