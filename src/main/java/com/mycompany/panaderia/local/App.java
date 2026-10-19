
package com.mycompany.panaderia.local;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                App.class.getResource("/com/mycompany/panaderia/local/Login.fxml")
        );

        Parent root = loader.load();

        scene = new Scene(root, 1280, 720); // Medidas originales de tu ventana de login

        stage.setTitle("El Lirio Rojo - Iniciar Sesión");
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                App.class.getResource(fxml + ".fxml")
        );
        return loader.load();
    }


    private static boolean administrador = false;

    public static void mostrar(String archivo) throws IOException {
        if (archivo.equalsIgnoreCase("loginAdministrador") && !administrador) {
            throw new IllegalStateException("No existe una sesion administrativa.");
        }
        setRoot(archivo);
    }

    public static boolean validarAdministrador(String correo, String contrasena) {
        boolean valido = correo.equalsIgnoreCase("angel@gmail.com") && contrasena.equals("1234");
        administrador = valido;
        return valido;
    }

    public static void entrarCajero() throws IOException {
        administrador = false;
        setRoot("cajeroPedido"); 
    }

    public static void cerrarSesion() throws IOException {
        administrador = false;
        setRoot("Login");
    }

    public static void main(String[] args) {
        launch(args);
    }
}