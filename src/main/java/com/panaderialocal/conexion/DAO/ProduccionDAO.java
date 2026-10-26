package com.panaderialocal.conexion.DAO;

import com.panaderialocal.conexion.ConexionBD;
import com.panaderialocal.conexion.modelos.Produccion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProduccionDAO {

    public List<String[]> obtenerPanes() throws SQLException {
        List<String[]> panes = new ArrayList<>();

        String sql = """
            SELECT id_pan, nombre
            FROM panes
            ORDER BY nombre
            """;

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                panes.add(new String[]{
                    String.valueOf(rs.getInt("id_pan")),
                    rs.getString("nombre")
                });
            }
        }

        return panes;
    }

    public boolean registrarProduccion(Produccion produccion)
            throws SQLException {

        String sql = """
            INSERT INTO produccion_diaria
                (fecha, id_pan, cantidad_producida,
                 cantidad_vendida, cantidad_sobrante)
            VALUES (?, ?, ?, 0, 0)
            """;

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, produccion.getFecha().toString());
            ps.setInt(2, produccion.getIdPan());
            ps.setInt(3, produccion.getCantidadProducida());

            return ps.executeUpdate() > 0;
        }
    }

    public Produccion obtenerResumen(LocalDate fecha)
            throws SQLException {

        String sql = """
            SELECT
                COALESCE(SUM(cantidad_producida), 0) AS producidos,
                COALESCE(SUM(cantidad_vendida), 0) AS vendidos,
                COALESCE(SUM(cantidad_sobrante), 0) AS sobrantes
            FROM produccion_diaria
            WHERE fecha = ?
            """;

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, fecha.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int producidos = rs.getInt("producidos");
                    int vendidos = rs.getInt("vendidos");
                    int sobrantes = rs.getInt("sobrantes");
                    int merma = Math.max(
                        0, producidos - vendidos - sobrantes
                    );

                    return new Produccion(
                        0, fecha, 0, null, producidos,
                        vendidos, sobrantes, merma
                    );
                }
            }
        }

        return new Produccion(
            0, fecha, 0, null, 0, 0, 0, 0
        );
    }

    public List<Produccion> obtenerHistorial(
            LocalDate desde, LocalDate hasta) throws SQLException {

        List<Produccion> historial = new ArrayList<>();

        String sql = """
            SELECT
                fecha,
                SUM(cantidad_producida) AS producidos,
                SUM(cantidad_vendida) AS vendidos,
                SUM(cantidad_sobrante) AS sobrantes
            FROM produccion_diaria
            WHERE fecha BETWEEN ? AND ?
            GROUP BY fecha
            ORDER BY fecha DESC
            """;

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate fecha = LocalDate.parse(
                        rs.getString("fecha")
                    );

                    int producidos = rs.getInt("producidos");
                    int vendidos = rs.getInt("vendidos");
                    int sobrantes = rs.getInt("sobrantes");
                    int merma = Math.max(
                        0, producidos - vendidos - sobrantes
                    );

                    historial.add(new Produccion(
                        0, fecha, 0, null, producidos,
                        vendidos, sobrantes, merma
                    ));
                }
            }
        }

        return historial;
    }
}