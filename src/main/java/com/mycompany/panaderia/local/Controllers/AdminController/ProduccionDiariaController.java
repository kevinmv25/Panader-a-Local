
package com.mycompany.panaderia.local.Controllers.AdminController;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class ProduccionDiariaController {

    /**
     * Abre el formulario para registrar producción.
     */
    @FXML
    private void abrirRegistro(ActionEvent event) throws IOException {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/agregarProduccion.fxml"
        );
    }

    /**
     * Abre el resumen de producción del día.
     */
    @FXML
    private void abrirResumen(ActionEvent event) throws IOException {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/resumenProduccion.fxml"
        );
    }

    /**
     * Abre el historial de producción.
     */
    @FXML
    private void abrirHistorial(ActionEvent event) throws IOException {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/historialProduccion.fxml"
        );
    }

    /**
     * Regresa al catálogo de panes.
     */
    @FXML
    private void abrirCatalogo(ActionEvent event) throws IOException {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/loginAdministrador.fxml"
        );
    }

    /**
     * Carga una pantalla en la ventana actual.
     */
    private void abrirPantalla(ActionEvent event, String ruta)
            throws IOException {

        URL recurso = getClass().getResource(ruta);

        if (recurso == null) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Pantalla no encontrada");
            alerta.setHeaderText("No se pudo abrir la pantalla.");
            alerta.setContentText("Verifica la ruta: " + ruta);
            alerta.showAndWait();
            return;
        }

        Parent root = FXMLLoader.load(recurso);

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }
}
