
package com.mycompany.panaderia.local;

import com.panaderialocal.conexion.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InicializarDatos {

    public static void insertarDatosIniciales() throws SQLException {

        String sqlMateriales = """
            INSERT INTO materiales (nombre, stock_actual, unidad_medida)
            SELECT ?, ?, ?
            WHERE NOT EXISTS (
                SELECT 1 FROM materiales WHERE nombre = ?
            )
            """;

        String sqlPanes = """
            INSERT INTO panes (nombre, descripcion, precio)
            SELECT ?, ?, ?
            WHERE NOT EXISTS (
                SELECT 1 FROM panes WHERE nombre = ?
            )
            """;

        try (Connection conexion = ConexionBD.conectar()) {

            if (conexion == null) {
                throw new SQLException(
                    "No se pudo conectar a la base de datos."
                );
            }

            boolean autocommitAnterior = conexion.getAutoCommit();

            try {
                conexion.setAutoCommit(false);

                try (PreparedStatement material =
                        conexion.prepareStatement(sqlMateriales)) {

                    insertarMaterial(material, "Harina de trigo", 25, "kg");
                    insertarMaterial(material, "Azúcar", 10, "kg");
                    insertarMaterial(material, "Mantequilla", 5, "kg");
                    insertarMaterial(material, "Leche", 8, "litros");
                    insertarMaterial(material, "Huevos", 30, "piezas");
                    insertarMaterial(material, "Levadura", 2, "kg");
                    insertarMaterial(material, "Sal", 12, "kg");
                    insertarMaterial(material, "Canela", 3, "kg");
                }

                try (PreparedStatement pan =
                        conexion.prepareStatement(sqlPanes)) {

                    insertarPan(pan, "Bolillo",
                            "Pan tradicional", 2.50);

                    insertarPan(pan, "Concha",
                            "Pan dulce tradicional", 12.00);

                    insertarPan(pan, "Cuernito",
                            "Pan de masa suave", 15.00);

                    insertarPan(pan, "Dona",
                            "Dona tradicional", 14.00);

                    insertarPan(pan, "Oreja",
                            "Pan de hojaldre", 13.00);

                    insertarPan(pan, "Baguette",
                            "Pan de corteza crujiente", 25.00);
                }

                conexion.commit();

                System.out.println(
                    "Datos iniciales insertados correctamente."
                );

            } catch (SQLException e) {
                conexion.rollback();
                throw e;

            } finally {
                conexion.setAutoCommit(autocommitAnterior);
            }
        }
    }

    private static void insertarMaterial(
            PreparedStatement sentencia,
            String nombre,
            double stock,
            String unidad) throws SQLException {

        sentencia.setString(1, nombre);
        sentencia.setDouble(2, stock);
        sentencia.setString(3, unidad);
        sentencia.setString(4, nombre);
        sentencia.executeUpdate();
    }

    private static void insertarPan(
            PreparedStatement sentencia,
            String nombre,
            String descripcion,
            double precio) throws SQLException {

        sentencia.setString(1, nombre);
        sentencia.setString(2, descripcion);
        sentencia.setDouble(3, precio);
        sentencia.setString(4, nombre);
        sentencia.executeUpdate();
    }
}
