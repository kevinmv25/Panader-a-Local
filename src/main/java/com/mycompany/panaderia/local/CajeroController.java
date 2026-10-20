package com.mycompany.panaderia.local;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import javafx.scene.control.Label;

public class CajeroController {

    @FXML private Button btnSalir;
    @FXML private Button btnEditarPedido;
    @FXML private Button btnIrRegistro;
    @FXML private Label lblPrecio1;
    

    @FXML
    public void initialize() {
        // Salir al Login
        btnSalir.setOnAction(event -> {
            try {
                App.mostrar("Login");
            } catch (IOException ex) {
                System.getLogger(CajeroController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        });

        // Abrir modal de Editar Pedido
        btnEditarPedido.setOnAction(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("editarpedido.fxml"));
                Parent root = loader.load();
                Stage modalStage = new Stage();
                modalStage.initModality(Modality.APPLICATION_MODAL);
                modalStage.setTitle("EditarPedido");
                modalStage.setScene(new Scene(root, 1280, 720));
                modalStage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        
btnIrRegistro.setOnAction(event -> {
            try {
                // Apunta a la subcarpeta Cajero donde está el archivo de tu amigo
                App.setRoot("Cajero/RegistroPedidos");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}