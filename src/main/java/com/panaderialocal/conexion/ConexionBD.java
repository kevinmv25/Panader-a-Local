package com.panaderialocal.conexion;
 
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
 
/**
 * Conexion a SQLite.
 *
 * Donde vive la base (en este orden):
 *   1) Si se arranca con -Dpanaderia.db=RUTA, se usa esa (debe existir).
 *   2) Si no: %APPDATA%\El Lirio Rojo\panaderia.db  (una ruta FIJA por usuario,
 *      sin importar desde donde se abra el programa).
 *
 * La primera vez, si esa base no existe, se crea asi:
 *   a) Si hay un panaderia.db junto al programa / en la carpeta de trabajo,
 *      se COPIA una sola vez (conserva los datos que ya tenias).
 *   b) Si no, y el programa trae /plantilla/panaderia.db en sus recursos,
 *      se copia esa plantilla.
 *   c) Si no, se crea un archivo vacio y EsquemaBD.actualizar() arma las tablas.
 */
public class ConexionBD {
 
    private static final String CARPETA_APP = "El Lirio Rojo";
    private static final String NOMBRE_BD = "panaderia.db";
    private static final String PLANTILLA = "/plantilla/panaderia.db";
 
    private static Path archivoListo;
 
    public static Connection conectar() {
        try {
            Path archivo = prepararArchivo();
            Connection c = DriverManager.getConnection("jdbc:sqlite:" + archivo);
            try (Statement s = c.createStatement()) {
                s.execute("PRAGMA foreign_keys=ON");
                s.execute("PRAGMA busy_timeout=5000");
                EsquemaBD.actualizar(c);
                return c;
            } catch (SQLException e) {
                c.close();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo abrir SQLite: " + e.getMessage(), e);
        }
    }
 
    /** Ruta de la base que usa el programa (la crea la primera vez). */
    public static synchronized Path prepararArchivo() throws SQLException {
 
        if (archivoListo != null) {
            return archivoListo;
        }
 
        try {
            String manual = System.getProperty("panaderia.db");
            Path archivo;
 
            if (manual != null && !manual.isBlank()) {
                archivo = Path.of(manual).toAbsolutePath();
 
                if (!Files.isRegularFile(archivo)) {
                    throw new SQLException("No se encuentra la base de datos: " + archivo);
                }
            } else {
                archivo = carpetaDatos().resolve(NOMBRE_BD);
 
                if (!Files.isRegularFile(archivo)) {
                    crearBaseInicial(archivo);
                }
            }
 
            archivoListo = archivo;
            return archivo;
 
        } catch (IOException e) {
            throw new SQLException("No se pudo preparar la base de datos: " + e.getMessage(), e);
        }
    }
 
    private static Path carpetaDatos() throws IOException {
 
        String appdata = System.getenv("APPDATA");
 
        Path base = (appdata != null && !appdata.isBlank())
                ? Path.of(appdata)
                : Path.of(System.getProperty("user.home"));
 
        Path carpeta = base.resolve(CARPETA_APP);
        Files.createDirectories(carpeta);
        return carpeta;
    }
 
    private static void crearBaseInicial(Path destino) throws IOException {
 
        // a) base anterior junto al programa (se copia una sola vez)
        Path anterior = Path.of(NOMBRE_BD).toAbsolutePath();
 
        if (Files.isRegularFile(anterior)) {
            Files.copy(anterior, destino);
            return;
        }
 
        // b) plantilla incluida dentro del programa
        try (InputStream plantilla = ConexionBD.class.getResourceAsStream(PLANTILLA)) {
            if (plantilla != null) {
                Files.copy(plantilla, destino);
                return;
            }
        }
 
        // c) archivo vacio: SQLite lo inicializa como base nueva
        Files.createFile(destino);
    }
}
