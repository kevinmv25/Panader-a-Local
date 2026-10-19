package com.mycompany.panaderia.local;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField txtCorreo;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnIniciar;
    @FXML private Button btnCajero;
    @FXML private Label lblMensaje;

    @FXML
    public void initialize() {
        btnIniciar.setOnAction(event -> {
            String correo = txtCorreo.getText();
            String password = txtPassword.getText();

            // Validación temporal sin Base de Datos aún
            if (correo.equals("angel@gmail.com") && password.equals("1234")) {
                try {
                    App.mostrar("loginAdministrador"); 
                } catch (IOException ex) {
                    System.getLogger(LoginController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }
            } else {
                lblMensaje.setText("Credenciales incorrectas o usuario no autorizado.");
            }
        });

        // Botón Entrar como Cajero
        btnCajero.setOnAction(event -> {
            try {
                App.mostrar("cajeropedido"); 
            } catch (IOException ex) {
                System.getLogger(LoginController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        });
    }
}