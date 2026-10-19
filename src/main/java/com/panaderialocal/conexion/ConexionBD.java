package com.panaderialocal.conexion;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author Sinuhe
 */
public class ConexionBD {
    // El archivo de la base de datos se guardará en la carpeta del proyecto
    private static final String URL = "jdbc:sqlite:panaderia.db";

    public static Connection conectar() {
        Connection conexion = null;
        try {
            conexion = DriverManager.getConnection(URL);
            // Habilitar claves foráneas en SQLite
            conexion.createStatement().execute("PRAGMA foreign_keys = ON;");
            System.out.println("Conexión a SQLite exitosa.");
        } catch (SQLException e) {
            System.err.println("Error al conectar a la base de datos: " + e.getMessage());
        }
        return conexion;
    }
}