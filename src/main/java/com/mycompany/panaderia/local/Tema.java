package com.mycompany.panaderia.local;

import javafx.scene.Scene;
import javafx.scene.control.Alert;

public final class Tema {

    private static final String CSS = Tema.class
            .getResource("/com/mycompany/panaderia/local/estilos/tema.css")
            .toExternalForm();

    private Tema() {}

    public static void aplicar(Scene scene) {
        if (!scene.getStylesheets().contains(CSS)) {
            scene.getStylesheets().add(CSS);
        }
    }

    public static void aplicar(Alert alerta) {
        var panel = alerta.getDialogPane();
        if (!panel.getStylesheets().contains(CSS)) {
            panel.getStylesheets().add(CSS);
        }
    }
}