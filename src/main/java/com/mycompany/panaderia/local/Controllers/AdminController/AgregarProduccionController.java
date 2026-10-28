package com.mycompany.panaderia.local.Controllers.AdminController;

import com.panaderialocal.conexion.DAO.ProduccionDAO;
import com.panaderialocal.conexion.modelos.Produccion;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AgregarProduccionController {

    @FXML
    private ComboBox<String> cmbTipoPan;

    @FXML
    private TextField txtCantidad;

    @FXML
    private DatePicker dpFecha;

    private final ProduccionDAO produccionDAO = new ProduccionDAO();

    @FXML
    public void initialize() {
        dpFecha.setValue(LocalDate.now());

        try {
            List<String[]> panes = produccionDAO.obtenerPanes();

            for (String[] pan : panes) {
                cmbTipoPan.getItems().add(
                    pan[0] + " - " + pan[1]
                );
            }

        } catch (SQLException e) {
            mostrarAlerta(
                "Error",
                "No se pudieron cargar los tipos de pan."
            );
            e.printStackTrace();
        }
    }

    @FXML
    private void guardarProduccion(ActionEvent event) {

        if (cmbTipoPan.getValue() == null
                || txtCantidad.getText().trim().isEmpty()
                || dpFecha.getValue() == null) {

            mostrarAlerta(
                "Datos incompletos",
                "Completa todos los campos."
            );
            return;
        }

        try {
            int cantidad = Integer.parseInt(
                txtCantidad.getText().trim()
            );

            if (cantidad <= 0) {
                mostrarAlerta(
                    "Cantidad inválida",
                    "La cantidad debe ser mayor que cero."
                );
                return;
            }

            int idPan = Integer.parseInt(
                cmbTipoPan.getValue().split(" - ", 2)[0]
            );

            Produccion produccion = new Produccion();
            produccion.setFecha(dpFecha.getValue());
            produccion.setIdPan(idPan);
            produccion.setCantidadProducida(cantidad);

            if (produccionDAO.registrarProduccion(produccion)) {

                mostrarAlerta(
                    "Éxito",
                    "La producción se registró correctamente."
                );

                cerrarVentana(event);

            } else {
                mostrarAlerta(
                    "Error",
                    "No se pudo registrar la producción."
                );
            }

        } catch (NumberFormatException e) {
            mostrarAlerta(
                "Cantidad inválida",
                "Introduce una cantidad entera."
            );

        } catch (SQLException e) {
            mostrarAlerta(
                "Error",
                "No se pudo guardar la producción: "
                    + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    /**
     * Regresa a la pantalla principal de producción.
     */
    @FXML
    private void volverProduccion(ActionEvent event) {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/ProduccionDiaria.fxml"
        );
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
     * Conserva la función del botón Cancelar.
     */
    @FXML
    private void cancelar(ActionEvent event) {
        cerrarVentana(event);
    }

    /**
     * Cambia de pantalla en la misma ventana.
     */
    private void abrirPantalla(ActionEvent event, String ruta) {

        try {
            URL recurso = getClass().getResource(ruta);

            if (recurso == null) {
                mostrarAlerta(
                    "Pantalla no encontrada",
                    "No se encontró el archivo: " + ruta
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
            mostrarAlerta(
                "Error de navegación",
                "No se pudo abrir la pantalla: "
                    + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    /**
     * Cierra la ventana actual.
     */
    private void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource())
            .getScene()
            .getWindow();

        stage.close();
    }

    /**
     * Muestra mensajes al usuario.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
