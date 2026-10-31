
package com.mycompany.panaderia.local.Controllers.AdminController;

import com.panaderialocal.conexion.DAO.ProduccionDAO;
import com.panaderialocal.conexion.modelos.Produccion;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class HistorialProduccionController {

    @FXML
    private DatePicker dpDesde;

    @FXML
    private DatePicker dpHasta;

    @FXML
    private TableView<Produccion> tablaHistorial;

    @FXML
    private TableColumn<Produccion, LocalDate> colFecha;

    @FXML
    private TableColumn<Produccion, Integer> colProducidos;

    @FXML
    private TableColumn<Produccion, Integer> colVendidos;

    @FXML
    private TableColumn<Produccion, Integer> colSobrante;

    @FXML
    private TableColumn<Produccion, Integer> colMerma;

    private final ProduccionDAO produccionDAO = new ProduccionDAO();

    @FXML
    public void initialize() {
        colFecha.setCellValueFactory(
            new PropertyValueFactory<>("fecha")
        );

        colProducidos.setCellValueFactory(
            new PropertyValueFactory<>("cantidadProducida")
        );

        colVendidos.setCellValueFactory(
            new PropertyValueFactory<>("cantidadVendida")
        );

        colSobrante.setCellValueFactory(
            new PropertyValueFactory<>("cantidadSobrante")
        );

        colMerma.setCellValueFactory(
            new PropertyValueFactory<>("merma")
        );

        dpHasta.setValue(LocalDate.now());
        dpDesde.setValue(LocalDate.now().minusDays(7));

        cargarHistorial();
    }

    @FXML
    private void aplicarFiltros() {
        cargarHistorial();
    }

    private void cargarHistorial() {
        if (dpDesde.getValue() == null
                || dpHasta.getValue() == null) {

            mostrarError("Selecciona las dos fechas.");
            return;
        }

        if (dpDesde.getValue().isAfter(dpHasta.getValue())) {
            mostrarError(
                "La fecha inicial no puede ser posterior a la fecha final."
            );
            return;
        }

        try {
            List<Produccion> registros =
                produccionDAO.obtenerHistorial(
                    dpDesde.getValue(),
                    dpHasta.getValue()
                );

            tablaHistorial.setItems(
                FXCollections.observableArrayList(registros)
            );

        } catch (SQLException e) {
            mostrarError(
                "No se pudo cargar el historial: " + e.getMessage()
            );
            e.printStackTrace();
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
    
    @FXML
    private void abrirInicio(ActionEvent event) {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/inicioAdmin.fxml"
        );
    }

    /**
     * Cambia de pantalla en la misma ventana.
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
