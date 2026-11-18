package com.panaderialocal.conexion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DeudorDAO {

    // REGISTRAR UN DEUDOR
    public void registrarDeudor(
            String nombre,
            String telefono,
            BigDecimal monto,
            String descripcion) throws SQLException {

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new SQLException("El nombre del cliente es obligatorio.");
        }

        if (monto == null || monto.signum() <= 0) {
            throw new SQLException("El monto debe ser mayor que cero.");
        }

        BigDecimal importe = monto.setScale(2, RoundingMode.HALF_UP);

        String sql = """
            INSERT INTO deudores (nombre, telefono, monto, descripcion)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre.trim());
            ps.setString(2, vacioANull(telefono));
            ps.setDouble(3, importe.doubleValue());
            ps.setString(4, vacioANull(descripcion));
            ps.executeUpdate();
        }
    }

    // CONSULTAR TODOS LOS DEUDORES (pendientes primero)
    public List<Deudor> obtenerDeudores() throws SQLException {

        List<Deudor> lista = new ArrayList<>();

        String sql = """
            SELECT id_deudor,
                   nombre,
                   telefono,
                   monto,
                   datetime(fecha_deuda, 'localtime') AS fecha,
                   descripcion,
                   estado
            FROM deudores
            ORDER BY CASE estado WHEN 'Pendiente' THEN 0 ELSE 1 END,
                     id_deudor DESC
            """;

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new Deudor(
                    rs.getInt("id_deudor"),
                    rs.getString("nombre"),
                    rs.getString("telefono"),
                    BigDecimal.valueOf(rs.getDouble("monto")),
                    rs.getString("fecha"),
                    rs.getString("descripcion"),
                    rs.getString("estado")
                ));
            }
        }

        return lista;
    }

    private String vacioANull(String texto) {
        return (texto == null || texto.trim().isEmpty())
                ? null : texto.trim();
    }

    private Connection conectar() throws SQLException {

        Connection con = ConexionBD.conectar();

        if (con == null) {
            throw new SQLException("No fue posible conectar con SQLite.");
        }

        return con;
    }

    // MODELO DE DEUDOR
    public static class Deudor {

        private final int idDeudor;
        private final String nombre;
        private final String telefono;
        private final BigDecimal monto;
        private final String fecha;
        private final String descripcion;
        private final String estado;

        public Deudor(int idDeudor, String nombre, String telefono,
                      BigDecimal monto, String fecha,
                      String descripcion, String estado) {

            this.idDeudor = idDeudor;
            this.nombre = nombre;
            this.telefono = telefono;
            this.monto = monto.setScale(2, RoundingMode.HALF_UP);
            this.fecha = fecha;
            this.descripcion = descripcion;
            this.estado = estado;
        }

        public int getIdDeudor() { return idDeudor; }
        public String getNombre() { return nombre; }
        public String getTelefono() { return telefono; }
        public BigDecimal getMonto() { return monto; }
        public String getFecha() { return fecha; }
        public String getDescripcion() { return descripcion; }
        public String getEstado() { return estado; }
    }
}