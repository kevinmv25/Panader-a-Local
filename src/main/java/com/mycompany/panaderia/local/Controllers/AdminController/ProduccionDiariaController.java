package com.mycompany.panaderia.local.Controllers.AdminController;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class ProduccionDiariaController {

    @FXML
    private void abrirRegistro(ActionEvent event) throws IOException {
        abrirPantalla(event, "/com/mycompany/panaderia/local/Administrador/agregarProduccion.fxml");
    }

    @FXML
    private void abrirResumen(ActionEvent event) throws IOException {
        abrirPantalla(event, "/com/mycompany/panaderia/local/Administrador/resumenProduccion.fxml");
    }

    @FXML
    private void abrirHistorial(ActionEvent event) throws IOException {
        abrirPantalla(event, "/com/mycompany/panaderia/local/Administrador/historialProduccion.fxml");
    }

    private void abrirPantalla(ActionEvent event, String ruta) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(ruta));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}