
package com.mycompany.panaderia.local;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

// Guarda las fotos de los panes dentro del proyecto (images/catalogo) para que
// la ruta que queda en la base de datos funcione en cualquier computadora.
public final class ImagenesCatalogo {

    // ruta de recurso que se guarda en panes.foto (siempre con "/")
    public static final String RUTA_RECURSO = "/images/catalogo/";

    private static final Path FUENTES =
            Path.of("src", "main", "resources", "images", "catalogo");

    private static final Path COMPILADO =
            Path.of("target", "classes", "images", "catalogo");

    private ImagenesCatalogo() {
    }

    // Copia la imagen elegida a images/catalogo y devuelve /images/catalogo/<archivo>.
    // Si no se puede copiar (por ejemplo, al ejecutar fuera de la carpeta del
    // proyecto) devuelve la ruta original para no perder la foto.
    public static String guardar(File origen) {
        return guardar(origen, FUENTES, COMPILADO);
    }

    static String guardar(File origen, Path fuentes, Path compilado) {

        if (origen == null) {
            return "";
        }

        if (!Files.isDirectory(fuentes)) {
            return origen.getAbsolutePath();
        }

        try {
            Path original = origen.toPath();
            String nombre = nombreSeguro(origen.getName());
            Path destino = fuentes.resolve(nombre);

            // Si ya hay otra imagen distinta con ese nombre, se agrega -2, -3...
            int numero = 2;

            while (Files.exists(destino)
                    && Files.mismatch(original, destino) != -1L) {

                destino = fuentes.resolve(conSufijo(nombre, numero++));
            }

            if (!Files.exists(destino)) {
                Files.copy(original, destino);
            }

            // Copia en target/classes para verla sin volver a compilar
            Path carpetaClases = compilado.getParent() == null
                    ? null : compilado.getParent().getParent();

            if (carpetaClases != null && Files.isDirectory(carpetaClases)) {
                Files.createDirectories(compilado);
                Files.copy(
                    destino,
                    compilado.resolve(destino.getFileName()),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }

            return RUTA_RECURSO + destino.getFileName();

        } catch (IOException ex) {
            System.err.println("No se pudo copiar la imagen: " + ex.getMessage());
            return origen.getAbsolutePath();
        }
    }

    // Extension en minusculas (.PNG -> .png) y sin caracteres que rompan la ruta
    static String nombreSeguro(String nombre) {

        int punto = nombre.lastIndexOf('.');
        String base = punto > 0 ? nombre.substring(0, punto) : nombre;
        String extension = punto > 0
                ? nombre.substring(punto).toLowerCase(Locale.ROOT) : "";

        base = base.replaceAll("[^\\p{L}\\p{N} ._()\\-]", "_").trim();

        if (base.isEmpty()) {
            base = "pan";
        }

        return base + extension;
    }

    private static String conSufijo(String nombre, int numero) {

        int punto = nombre.lastIndexOf('.');

        return punto > 0
                ? nombre.substring(0, punto) + "-" + numero + nombre.substring(punto)
                : nombre + "-" + numero;
    }
}
