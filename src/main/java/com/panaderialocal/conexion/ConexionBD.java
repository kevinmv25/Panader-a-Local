package com.panaderialocal.conexion;
import java.sql.*;
import java.nio.file.*;
public class ConexionBD {
 public static Connection conectar() {
  try {
   Path archivo=Path.of(System.getProperty("panaderia.db","panaderia.db")).toAbsolutePath();
   if(!Files.isRegularFile(archivo)) throw new SQLException("No se encuentra la base de datos: "+archivo);
   Connection c=DriverManager.getConnection("jdbc:sqlite:"+archivo);
   try(Statement s=c.createStatement()) {
    s.execute("PRAGMA foreign_keys=ON"); s.execute("PRAGMA busy_timeout=5000"); EsquemaBD.actualizar(c); return c;
   } catch(SQLException e) { c.close(); throw e; }
  } catch(SQLException e) { throw new IllegalStateException("No se pudo abrir SQLite: "+e.getMessage(),e); }
 }
}
