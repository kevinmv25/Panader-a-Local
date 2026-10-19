package com.mycompany.panaderia.local;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class AdminController {

    @FXML private Button btnCerrarSesion;

    @FXML
    public void initialize() {
        btnCerrarSesion.setOnAction(event -> {
            try {
                App.mostrar("Login");
            } catch (IOException ex) {
                System.getLogger(AdminController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        });
    }
}