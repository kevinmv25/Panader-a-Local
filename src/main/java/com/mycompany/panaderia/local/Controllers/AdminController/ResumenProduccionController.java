package com.mycompany.panaderia.local.Controllers.AdminController;

import com.panaderialocal.conexion.DAO.ProduccionDAO;
import com.panaderialocal.conexion.modelos.Produccion;
import java.sql.SQLException;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

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
            Produccion resumen = produccionDAO.obtenerResumen(dpFecha.getValue());

            lblProducidos.setText(String.valueOf(resumen.getCantidadProducida()));
            lblVendidos.setText(String.valueOf(resumen.getCantidadVendida()));
            lblSobrantes.setText(String.valueOf(resumen.getCantidadSobrante()));
            lblMerma.setText(String.valueOf(resumen.getMerma()));

        } catch (SQLException e) {
            mostrarError("No se pudo cargar el resumen: " + e.getMessage());
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