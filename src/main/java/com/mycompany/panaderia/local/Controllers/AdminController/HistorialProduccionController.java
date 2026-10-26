package com.mycompany.panaderia.local.Controllers.AdminController;

import com.panaderialocal.conexion.DAO.ProduccionDAO;
import com.panaderialocal.conexion.modelos.Produccion;
import java.sql.SQLException;
import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

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
        if (dpDesde.getValue() == null || dpHasta.getValue() == null) {
            mostrarError("Selecciona las dos fechas.");
            return;
        }

        if (dpDesde.getValue().isAfter(dpHasta.getValue())) {
            mostrarError("La fecha inicial no puede ser posterior a la fecha final.");
            return;
        }

        try {
            tablaHistorial.setItems(
                FXCollections.observableArrayList(
                    produccionDAO.obtenerHistorial(
                        dpDesde.getValue(),
                        dpHasta.getValue()
                    )
                )
            );
        } catch (SQLException e) {
            mostrarError("No se pudo cargar el historial: " + e.getMessage());
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