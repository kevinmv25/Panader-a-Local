package com.mycompany.panaderia.local;
 
/**
 * Clase de arranque para el .jar empaquetado.
 * No extiende Application a proposito: asi JavaFX puede iniciar
 * cuando se ejecuta desde el classpath.
 */
public class Launcher {
 
    public static void main(String[] args) {
        App.main(args);
    }
}
