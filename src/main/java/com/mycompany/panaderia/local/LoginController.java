
package com.mycompany.panaderia.local;

import java.io.IOException;
import java.io.InputStream;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class LoginController {

    @FXML private TextField txtCorreo;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnIniciar;
    @FXML private Button btnCajero;
    @FXML private Label lblMensaje;
    @FXML private ImageView imgLogoBarra;
    @FXML private ImageView imgLogoTarjeta;

    private static final String CARPETA_LOGOS =
            "/com/mycompany/panaderia/local/iconos/";

    @FXML
    public void initialize() {

        // LOGO: barra superior (junto a "El Lirio Rojo") y tarjeta central
        cargarLogo(imgLogoBarra, 56, "logo_el_lirio_rojo_barra.png");
        cargarLogo(imgLogoTarjeta, 120, "logo_el_lirio_rojo_tarjeta.png");

        // BOTÓN INICIAR SESIÓN COMO ADMINISTRADOR
        btnIniciar.setOnAction(event -> {

            String correo = txtCorreo.getText();
            String password = txtPassword.getText();

            if (correo.equals("admon@gmail.com")
                    && password.equals("1234")) {

                try {
                    App.mostrar("Administrador/loginAdministrador");

                } catch (IOException ex) {
                    System.getLogger(
                        LoginController.class.getName()
                    ).log(
                        System.Logger.Level.ERROR,
                        "Error al abrir administrador",
                        ex
                    );
                }

            } else {
                lblMensaje.setText(
                    "Credenciales incorrectas o usuario no autorizado."
                );
            }
        });

        // BOTÓN ENTRAR COMO CAJERO
        btnCajero.setOnAction(event -> {

            try {
                App.mostrar("cajeropedido");

            } catch (IOException ex) {
                System.getLogger(
                    LoginController.class.getName()
                ).log(
                    System.Logger.Level.ERROR,
                    "Error al abrir cajero",
                    ex
                );
            }
        });
    }

    // Carga el logo reducido al tamano que se muestra. Usa la copia liviana si
    // existe y, si no, el logo HD que ya trae el proyecto. Si ninguno carga,
    // avisa en consola y oculta el espacio para no dejar un hueco en pantalla.
    private void cargarLogo(ImageView vista, double alto, String liviano) {

        if (vista == null) {
            return;
        }

        for (String nombre : new String[] {liviano, "logo_el_lirio_rojo_hd.png"}) {

            try (InputStream in = LoginController.class
                    .getResourceAsStream(CARPETA_LOGOS + nombre)) {

                if (in == null) {
                    continue;
                }

                // x2 para que se vea nitido en pantallas de alta densidad
                Image imagen = new Image(in, 0, alto * 2, true, true);

                if (!imagen.isError()) {
                    vista.setImage(imagen);
                    vista.setFitHeight(alto);
                    vista.setPreserveRatio(true);
                    vista.setSmooth(true);
                    return;
                }

            } catch (IOException ex) {
                System.getLogger(LoginController.class.getName()).log(
                    System.Logger.Level.WARNING,
                    "No se pudo leer el logo " + nombre, ex);
            }
        }

        System.getLogger(LoginController.class.getName()).log(
            System.Logger.Level.WARNING,
            "No se encontro ningun logo en " + CARPETA_LOGOS);

        vista.setVisible(false);
        vista.setManaged(false);
    }
}
