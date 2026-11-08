
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
import javafx.scene.control.DateCell;
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

        // Establecer la fecha actual.
        dpFecha.setValue(LocalDate.now());

        // Bloquear los días anteriores a la fecha actual.
        dpFecha.setDayCellFactory(param -> new DateCell() {

            @Override
            public void updateItem(LocalDate fecha, boolean empty) {
                super.updateItem(fecha, empty);

                if (empty || fecha.isBefore(LocalDate.now())) {
                    setDisable(true);
                }
            }
        });

        // Cargar los tipos de pan.
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

        // Validar campos vacíos.
        if (cmbTipoPan.getValue() == null
                || txtCantidad.getText().trim().isEmpty()
                || dpFecha.getValue() == null) {

            mostrarAlerta(
                "Datos incompletos",
                "Completa todos los campos."
            );
            return;
        }

        // Validar que la fecha no sea anterior a hoy.
        if (dpFecha.getValue().isBefore(LocalDate.now())) {

            mostrarAlerta(
                "Fecha inválida",
                "No puedes registrar una producción con una fecha anterior a hoy."
            );
            return;
        }

        // Validar que solamente se introduzcan números.
        String cantidadTexto = txtCantidad.getText().trim();

        for (int i = 0; i < cantidadTexto.length(); i++) {

            if (!Character.isDigit(cantidadTexto.charAt(i))) {

                mostrarAlerta(
                    "Cantidad inválida",
                    "Solo se permiten números enteros."
                );
                return;
            }
        }

        try {
            int cantidad = Integer.parseInt(cantidadTexto);

            // Validar que la cantidad sea mayor que cero.
            if (cantidad <= 0) {

                mostrarAlerta(
                    "Cantidad inválida",
                    "La cantidad debe ser mayor que cero."
                );
                return;
            }

            // Obtener el ID del pan seleccionado.
            int idPan = Integer.parseInt(
                cmbTipoPan.getValue().split(" - ", 2)[0]
            );

            Produccion produccion = new Produccion();
            produccion.setFecha(dpFecha.getValue());
            produccion.setIdPan(idPan);
            produccion.setCantidadProducida(cantidad);

            // Registrar la producción.
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
                "Introduce una cantidad entera válida."
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

    @FXML
    private void abrirMenuMateriales(ActionEvent event) {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/menuMateriales.fxml"
        );
    }
    
    @FXML
    private void cerrarSesion(ActionEvent event) {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Login.fxml"
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

    @FXML
    private void abrirInicio(ActionEvent event) {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/inicioAdmin.fxml"
        );
    }

    /**
     * Acción para abrir ventas.
     */
    @FXML
    private void abrirVentas(ActionEvent event) throws IOException {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/ventas.fxml"
        );
    }

    @FXML
    private void abrirPedidos(ActionEvent event) throws IOException {

        abrirPantalla(
                event,
                "/com/mycompany/panaderia/local/Administrador/historialPedidos.fxml"
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
