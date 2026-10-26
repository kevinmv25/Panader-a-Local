package com.mycompany.panaderia.local;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CajeroController {

    @FXML private Button btnSalir;
    @FXML private Button btnEditarPedido;
    @FXML private Button btnIrRegistro;
    @FXML private Label lblPrecio1;

    @FXML
    public void initialize() {
        btnSalir.setOnAction(event -> {
            try {
                App.mostrar("Login");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        btnEditarPedido.setOnAction(event -> abrirGestionPedidos());

        btnIrRegistro.setOnAction(event -> {
            try {
                App.setRoot("Cajero/RegistroPedidos");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void abrirGestionPedidos() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("editarpedido.fxml")
            );

            Parent root = loader.load();

            Stage ventana = new Stage();
            ventana.initModality(Modality.APPLICATION_MODAL);
            ventana.setTitle("Gestión de pedidos");
            ventana.setScene(new Scene(root, 850, 600));
            ventana.setResizable(true);
            ventana.showAndWait();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}