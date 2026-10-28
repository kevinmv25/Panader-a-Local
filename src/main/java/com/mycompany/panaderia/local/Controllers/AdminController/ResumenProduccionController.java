
package com.mycompany.panaderia.local.Controllers.AdminController;

import com.panaderialocal.conexion.DAO.ProduccionDAO;
import com.panaderialocal.conexion.modelos.Produccion;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ResumenProduccionController {

    @FXML
    private DatePicker dpFecha;

    @FXML
    private Label lblProducidos;

    @FXML
    private Label lblVendidos;

    @FXML
    private Label lblSobrantes;

    @FXML
    private Label lblMerma;

    private final ProduccionDAO produccionDAO = new ProduccionDAO();

    @FXML
    public void initialize() {
        dpFecha.setValue(LocalDate.now());
        dpFecha.setOnAction(event -> cargarResumen());
        cargarResumen();
    }

    private void cargarResumen() {
        if (dpFecha.getValue() == null) {
            return;
        }

        try {
            Produccion resumen =
                produccionDAO.obtenerResumen(dpFecha.getValue());

            lblProducidos.setText(
                String.valueOf(resumen.getCantidadProducida())
            );

            lblVendidos.setText(
                String.valueOf(resumen.getCantidadVendida())
            );

            lblSobrantes.setText(
                String.valueOf(resumen.getCantidadSobrante())
            );

            lblMerma.setText(
                String.valueOf(resumen.getMerma())
            );

        } catch (SQLException e) {
            mostrarError(
                "No se pudo cargar el resumen: " + e.getMessage()
            );
        }
    }

    /**
     * Abre el catálogo de panes.
     */
    @FXML
    private void abrirCatalogo(ActionEvent event) {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/loginAdministrador.fxml"
        );
    }

    /**
     * Regresa a la pantalla de producción diaria.
     */
    @FXML
    private void volverProduccion(ActionEvent event) {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/ProduccionDiaria.fxml"
        );
    }

    /**
     * Navega a otra pantalla en la misma ventana.
     */
    private void abrirPantalla(ActionEvent event, String ruta) {
        try {
            URL recurso = getClass().getResource(ruta);

            if (recurso == null) {
                mostrarError(
                    "No se encontró la pantalla: " + ruta
                );
                return;
            }

            Parent root = FXMLLoader.load(recurso);

            Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            mostrarError(
                "No se pudo abrir la pantalla: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
