package com.mycompany.panaderia.local.Controllers.AdminController;

import com.mycompany.panaderia.local.App;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class VentasAdminController implements Initializable {

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Inicializaciones de la vista de ventas si se requieren
    }

    @FXML
    private void abrirInicio(ActionEvent event) throws IOException {
        cambiarPantalla(event, "/com/mycompany/panaderia/local/Administrador/inicioAdmin.fxml");
    }

    @FXML
    private void abrirCatalogo(ActionEvent event) throws IOException {
        cambiarPantalla(event, "/com/mycompany/panaderia/local/Administrador/loginAdministrador.fxml");
    }

    @FXML
    private void abrirProduccion(ActionEvent event) throws IOException {
        cambiarPantalla(event, "/com/mycompany/panaderia/local/Administrador/ProduccionDiaria.fxml");
    }

    @FXML
    private void abrirMenuMateriales(ActionEvent event) throws IOException {
        cambiarPantalla(event, "/com/mycompany/panaderia/local/Administrador/menuMateriales.fxml");
    }

    @FXML
    private void abrirPedidos(ActionEvent event) throws IOException {
        // Cambiar si tienes una vista específica de pedidos
    }

    @FXML
    private void abrirResumenVentas(ActionEvent event) throws IOException {
        // Aquí carga la interfaz completa de gráficas y KPIs que hicimos antes
        cambiarPantalla(event, "/com/mycompany/panaderia/local/resumenVentas.fxml");
    }

    @FXML
    private void abrirPuntoVenta(ActionEvent event) throws IOException {
        // Opcional: si deseas conectar otra vista de caja
    }

    @FXML
    private void cerrarSesion(ActionEvent event) throws IOException {
        App.cerrarSesion();
    }

    private void cambiarPantalla(ActionEvent event, String ruta) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(ruta));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}