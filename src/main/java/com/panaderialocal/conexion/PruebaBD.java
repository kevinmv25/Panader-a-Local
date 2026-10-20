package com.panaderialocal.conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PruebaBD {

    public static void main(String[] args) {
        // 1. Probar la conexión
        Connection conn = ConexionBD.conectar();


        if (conn != null) {
            try {
                // 2. Opcional: Asegurar que la tabla 'panes' exista
                String sqlCrearTabla = "CREATE TABLE IF NOT EXISTS panes ("
                        + "id_pan INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "nombre TEXT NOT NULL, "
                        + "descripcion TEXT, "
                        + "precio REAL NOT NULL, "                       
                        + ");";
                
                Statement stmt = conn.createStatement();
                stmt.execute(sqlCrearTabla);

                // 3. Insertar el "Birote"
                String sqlInsert = "INSERT INTO panes (nombre, descripcion, precio, stock) VALUES (?, ?, ?, ?)";
                PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert);
                pstmtInsert.setString(1, "Birote");
                pstmtInsert.setString(2, "Pan tradicional salado y crujiente");
                pstmtInsert.setDouble(3, 8.50); // Precio ej. $8.50


                int filasAfectadas = pstmtInsert.executeUpdate();
                if (filasAfectadas > 0) {
                    System.out.println("¡Birote insertado correctamente en la base de datos!");
                }

                // 4. Consultar los panes registrados para verificar que sí quedó guardado
                System.out.println("\n--- Lista de Panes en la Base de Datos ---");
                String sqlSelect = "SELECT * FROM panes";
                Statement stmtSelect = conn.createStatement();
                ResultSet rs = stmtSelect.executeQuery(sqlSelect);

                while (rs.next()) {
                    int id = rs.getInt("id_pan");
                    String nombre = rs.getString("nombre");
                    double precio = rs.getDouble("precio");
                    int stock = rs.getInt("stock");

                    System.out.println("ID: " + id + " | Nombre: " + nombre + " | Precio: $" + precio + " | Stock: " + stock);
                }
                System.out.println("Ruta real de la BD: " + new java.io.File("panaderia.db").getAbsolutePath());
                // 5. Cerrar la conexión
                conn.close();
                System.out.println("\nConexión cerrada.");

            } catch (SQLException e) {
                System.err.println("Error durante la prueba de base de datos: " + e.getMessage());
            }
        } else {
            System.err.println("No se pudo establecer la conexión.");
            
        }
    }
}
