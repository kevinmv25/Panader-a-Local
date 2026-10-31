
package com.mycompany.panaderia.local.Controllers.AdminController;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class MenuMaterialesController {

    /**
     * Abre el catálogo de panes.
     */
    @FXML
    private void abrirCatalogo(ActionEvent event) throws IOException {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/loginAdministrador.fxml"
        );
    }

    /**
     * Abre la pantalla de producción.
     */
    @FXML
    private void abrirProduccion(ActionEvent event) throws IOException {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/ProduccionDiaria.fxml"
        );
    }

    /**
     * Abre el formulario para registrar materiales.
     */
    @FXML
    private void abrirAgregarMateriales(ActionEvent event) throws IOException {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/registroMateriales.fxml"
        );
    }

    /**
     * Abre el inventario o tabla de materiales.
     */
    @FXML
    private void abrirTablaMateriales(ActionEvent event) throws IOException {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Cajero/Inventario.fxml"
        );
    }
    
    @FXML
    private void abrirInicio(ActionEvent event) throws IOException {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/inicioAdmin.fxml"
        );
    }

   

    /**
     * Acción para abrir ventas.
     */
    @FXML
    private void abrirVentas(ActionEvent event) {
        mostrarMensaje(
            "La pantalla de Ventas todavía no está configurada."
        );
    }

    /**
     * Acción para abrir pedidos.
     */
    @FXML
    private void abrirPedidos(ActionEvent event) {
        mostrarMensaje(
            "La pantalla de Pedidos todavía no está configurada."
        );
    }

    /**
     * Acción para cerrar sesión.
     */
    @FXML
    private void cerrarSesion(ActionEvent event) {
        mostrarMensaje(
            "La función de Cerrar Sesión todavía no está configurada."
        );
    }
    
    
    

    /**
     * Carga una pantalla en la ventana actual.
     */
    private void abrirPantalla(ActionEvent event, String ruta)
            throws IOException {

        URL recurso = getClass().getResource(ruta);

        if (recurso == null) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Pantalla no encontrada");
            alerta.setHeaderText("No se pudo abrir la pantalla.");
            alerta.setContentText("Verifica la ruta: " + ruta);
            alerta.showAndWait();
            return;
        }

        Parent root = FXMLLoader.load(recurso);

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }

    /**
     * Muestra mensajes informativos.
     */
    private void mostrarMensaje(String mensaje) {

        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("El Lirio Rojo");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
