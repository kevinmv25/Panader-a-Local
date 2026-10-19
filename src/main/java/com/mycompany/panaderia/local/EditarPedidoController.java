/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.panaderia.local;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/**
 *
 * @author Angel
 */
public class EditarPedidoController {
    
    
    @FXML private Button btnCerrarX;
    @FXML private Button btnCerrar;
    @FXML private Button btnGuardar;

    @FXML
    public void initialize() {
        btnCerrarX.setOnAction(event -> cerrarModal());
        btnCerrar.setOnAction(event -> cerrarModal());
        
        btnGuardar.setOnAction(event -> {
            // Lógica provisional para guardar cambios sin BD
            cerrarModal();
        });
    }

    private void cerrarModal() {
        Stage stage = (Stage) btnCerrar.getScene().getWindow();
        stage.close();
    }
}
    

