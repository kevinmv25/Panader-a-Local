/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.panaderialocal.conexion;

import com.panaderialocal.conexion.RegistroPedidosDAO.DetallePedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author aleja
 */
public class DevolucionDAO {
    // CONSULTAR VENTAS QUE TODAVIA TIENEN SALDO POR DEVOLVER
    public List<VentaDevolucion> obtenerVentasConSaldo()
            throws SQLException {

        List<VentaDevolucion> ventas = new ArrayList<>();

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

                ventas.add(new VentaDevolucion(
                    rs.getInt("id_venta"),
                    rs.getString("fecha"),
                    rs.getString("metodo_pago"),
                    BigDecimal.valueOf(rs.getDouble("total")),
                    BigDecimal.valueOf(rs.getDouble("devuelto"))
                ));
            }
        }

        return ventas;
    }

    // CONSULTAR LOS PRODUCTOS DE UNA VENTA
    // El precio unitario se obtiene de subtotal / cantidad.
    public List<DetallePedido> obtenerLineasVenta(int idVenta)
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
    public void registrarDevolucion(
            int idVenta,
            BigDecimal monto,
            String motivo) throws SQLException {

        if (monto == null || monto.signum() <= 0) {
            throw new SQLException(
                "El monto a devolver debe ser mayor que cero."
            );
        }

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new SQLException(
                "El motivo de la devolucion es obligatorio."
            );
        }

        BigDecimal importe = monto.setScale(2, RoundingMode.HALF_UP);

        try (Connection con = conectar()) {

            boolean autocommit = con.getAutoCommit();
            con.setAutoCommit(false);

            try {

                // Se vuelve a validar contra la BD: nunca se devuelve
                // mas dinero del que se cobro en la venta.
                BigDecimal saldo = obtenerSaldo(con, idVenta);

                if (importe.compareTo(saldo) > 0) {
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

                try (PreparedStatement ps =
                        con.prepareStatement(insertar)) {

                    ps.setInt(1, idVenta);
                    ps.setString(2, motivo.trim());
                    ps.setDouble(3, importe.doubleValue());
                    ps.executeUpdate();
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

    // SALDO REEMBOLSABLE = TOTAL DE LA VENTA - LO YA DEVUELTO
    private BigDecimal obtenerSaldo(Connection con, int idVenta)
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

    // MODELO DE VENTA PARA DEVOLUCIONES
    public static class VentaDevolucion {

        private final int idVenta;
        private final String fecha;
        private final String metodoPago;
        private final BigDecimal total;
        private final BigDecimal devuelto;

        public VentaDevolucion(
                int idVenta,
                String fecha,
                String metodoPago,
                BigDecimal total,
                BigDecimal devuelto) {

            this.idVenta = idVenta;
            this.fecha = fecha;
            this.metodoPago = metodoPago;
            this.total = total.setScale(2, RoundingMode.HALF_UP);
            this.devuelto = devuelto.setScale(2, RoundingMode.HALF_UP);
        }

        public int getIdVenta() {
            return idVenta;
        }

        public String getFecha() {
            return fecha;
        }

        public String getMetodoPago() {
            return metodoPago;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public BigDecimal getDevuelto() {
            return devuelto;
        }

        public BigDecimal getSaldo() {
            return total.subtract(devuelto);
        }

        @Override
        public String toString() {
            return "Venta #" + idVenta
                    + " - " + (fecha == null ? "" : fecha)
                    + " - " + metodoPago
                    + " - Total $" + total
                    + " - Reembolsable $" + getSaldo();
        }
    }
    
}
