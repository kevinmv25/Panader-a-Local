
package com.panaderialocal.conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventarioDAO {

   
    public static class Material {

        private final int id;
        private final String nombre;
        private final double existencia;
        private final String unidad;

        public Material(int id, String nombre,
                        double existencia, String unidad) {
            this.id = id;
            this.nombre = nombre;
            this.existencia = existencia;
            this.unidad = unidad;
        }

        public int getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public double getExistencia() {
            return existencia;
        }

        public String getUnidad() {
            return unidad;
        }
    }

    // ==========================================
    // MODELO PARA INSERTAR MATERIALES
    // ==========================================

    public static class MaterialNuevo {

        private final String nombre;
        private final double existencia;
        private final String unidad;

        public MaterialNuevo(String nombre,
                             double existencia,
                             String unidad) {
            this.nombre = nombre;
            this.existencia = existencia;
            this.unidad = unidad;
        }

        public String getNombre() {
            return nombre;
        }

        public double getExistencia() {
            return existencia;
        }

        public String getUnidad() {
            return unidad;
        }
    }

    // ==========================================
    // CONSULTAR MATERIALES
    // ==========================================

    public List<Material> obtenerMateriales() throws SQLException {

        String sql = """
            SELECT id_material, nombre, stock_actual, unidad_medida
            FROM materiales
            ORDER BY nombre
            """;

        List<Material> lista = new ArrayList<>();

        try (Connection conexion = ConexionBD.conectar()) {

            if (conexion == null) {
                throw new SQLException(
                    "No se pudo conectar con la base de datos SQLite."
                );
            }

            try (PreparedStatement consulta =
                    conexion.prepareStatement(sql);
                 ResultSet resultado = consulta.executeQuery()) {

                while (resultado.next()) {
                    lista.add(new Material(
                        resultado.getInt("id_material"),
                        resultado.getString("nombre"),
                        resultado.getDouble("stock_actual"),
                        resultado.getString("unidad_medida")
                    ));
                }
            }
        }

        return lista;
    }

    // ==========================================
    // INSERTAR VARIOS MATERIALES
    // ==========================================

    public void guardarMateriales(
            List<MaterialNuevo> materiales) throws SQLException {

        if (materiales == null || materiales.isEmpty()) {
            throw new SQLException(
                "No hay materiales para guardar."
            );
        }

        String sql = """
            INSERT INTO materiales
                (nombre, stock_actual, unidad_medida)
            VALUES (?, ?, ?)
            """;

        try (Connection conexion = ConexionBD.conectar()) {

            if (conexion == null) {
                throw new SQLException(
                    "No se pudo conectar con la base de datos SQLite."
                );
            }

            boolean autocommitAnterior = conexion.getAutoCommit();

            try {
                conexion.setAutoCommit(false);

                try (PreparedStatement consulta =
                        conexion.prepareStatement(sql)) {

                    for (MaterialNuevo material : materiales) {

                        consulta.setString(1, material.getNombre());
                        consulta.setDouble(2, material.getExistencia());
                        consulta.setString(3, material.getUnidad());

                        consulta.executeUpdate();
                    }
                }

                conexion.commit();

            } catch (SQLException e) {

                conexion.rollback();
                throw e;

            } finally {

                conexion.setAutoCommit(autocommitAnterior);
            }
        }
    }
}
