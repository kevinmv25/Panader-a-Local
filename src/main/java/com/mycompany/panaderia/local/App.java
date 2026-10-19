
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
                App.class.getResource("/com/mycompany/panaderia/local/Cajero/RegistroPedidos.fxml")
        );

        Parent root = loader.load();

        scene = new Scene(root, 1200, 760);

        stage.setTitle("El Lirio Rojo - Inventario");
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                App.class.getResource(fxml + ".fxml")
        );
        return loader.load();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
