package com.mycompany.panaderia.local;

import com.panaderialocal.conexion.DeudorDAO;
import com.panaderialocal.conexion.DeudorDAO.Deudor;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class DeudorController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtMonto;
    @FXML private TextArea txtDescripcion;

    @FXML private TableView<Deudor> tablaDeudores;
    @FXML private TableColumn<Deudor, String> colNombre;
    @FXML private TableColumn<Deudor, String> colTelefono;
    @FXML private TableColumn<Deudor, String> colMonto;
    @FXML private TableColumn<Deudor, String> colFecha;
    @FXML private TableColumn<Deudor, String> colDescripcion;
    @FXML private TableColumn<Deudor, String> colEstado;

    @FXML private Label lblTotalAdeudado;
    @FXML private Button btnGuardar;
    @FXML private Button btnCerrar;

    private final DeudorDAO dao = new DeudorDAO();

    private final ObservableList<Deudor> deudores =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        colNombre.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getNombre()));
        colTelefono.setCellValueFactory(d ->
                new SimpleStringProperty(texto(d.getValue().getTelefono())));
        colMonto.setCellValueFactory(d ->
                new SimpleStringProperty(moneda(d.getValue().getMonto())));
        colFecha.setCellValueFactory(d ->
                new SimpleStringProperty(texto(d.getValue().getFecha())));
        colDescripcion.setCellValueFactory(d ->
                new SimpleStringProperty(texto(d.getValue().getDescripcion())));
        colEstado.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getEstado()));

        tablaDeudores.setItems(deudores);
        tablaDeudores.setPlaceholder(new Label("Aún no hay deudores registrados"));

        cargarDeudores();
    }

    @FXML
    private void guardarDeudor() {

        String nombre = txtNombre.getText() == null
                ? "" : txtNombre.getText().trim();
        String telefono = txtTelefono.getText() == null
                ? "" : txtTelefono.getText().trim();
        String textoMonto = txtMonto.getText() == null
                ? "" : txtMonto.getText().trim().replace(",", ".");
        String descripcion = txtDescripcion.getText() == null
                ? "" : txtDescripcion.getText().trim();

        if (nombre.isEmpty()) {
            alerta(Alert.AlertType.WARNING, "Nombre requerido",
                    "Escribe el nombre del cliente.");
            return;
        }

        if (!telefono.isEmpty() && !telefono.matches("[0-9+\\-\\s]{7,20}")) {
            alerta(Alert.AlertType.WARNING, "Teléfono inválido",
                    "Usa solo números, espacios, + o - (mínimo 7 dígitos).");
            return;
        }

        BigDecimal monto;

        try {
            monto = new BigDecimal(textoMonto);
        } catch (NumberFormatException ex) {
            alerta(Alert.AlertType.WARNING, "Monto inválido",
                    "Escribe el monto adeudado, por ejemplo 85.50.");
            return;
        }

        if (monto.signum() <= 0 || monto.scale() > 2) {
            alerta(Alert.AlertType.WARNING, "Monto inválido",
                    "El monto debe ser mayor que cero y con máximo dos decimales.");
            return;
        }

        try {
            btnGuardar.setDisable(true);
            dao.registrarDeudor(nombre, telefono, monto, descripcion);

            alerta(Alert.AlertType.INFORMATION, "Deudor registrado",
                    nombre + " debe " + moneda(monto) + ".");

            limpiar();
            cargarDeudores();

        } catch (SQLException | IllegalStateException ex) {
            alerta(Alert.AlertType.ERROR,
                    "No se pudo registrar el deudor", ex.getMessage());
        } finally {
            btnGuardar.setDisable(false);
        }
    }

    @FXML
    private void limpiar() {
        txtNombre.clear();
        txtTelefono.clear();
        txtMonto.clear();
        txtDescripcion.clear();
        txtNombre.requestFocus();
    }

    @FXML
    private void cerrar() {
        Stage stage = (Stage) btnCerrar.getScene().getWindow();
        stage.close();
    }

    private void cargarDeudores() {
        try {
            List<Deudor> lista = dao.obtenerDeudores();
            deudores.setAll(lista);

            BigDecimal total = BigDecimal.ZERO;
            for (Deudor d : lista) {
                if ("Pendiente".equals(d.getEstado())) {
                    total = total.add(d.getMonto());
                }
            }
            lblTotalAdeudado.setText(moneda(total));

        } catch (SQLException | IllegalStateException ex) {
            alerta(Alert.AlertType.ERROR,
                    "Error al cargar los deudores", ex.getMessage());
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private String moneda(BigDecimal importe) {
        return String.format(Locale.US, "$%.2f", importe);
    }

    private void alerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}