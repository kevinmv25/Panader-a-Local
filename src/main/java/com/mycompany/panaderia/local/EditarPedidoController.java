package com.mycompany.panaderia.local;

import com.panaderialocal.conexion.RegistroPedidosDAO;
import com.panaderialocal.conexion.RegistroPedidosDAO.DetallePedido;
import com.panaderialocal.conexion.RegistroPedidosDAO.PedidoReciente;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class EditarPedidoController {

    @FXML private Button btnCerrarX;
    @FXML private Button btnCerrar;
    @FXML private Button btnCargar;
    @FXML private Button btnGuardarAdelanto;
    @FXML private Button btnCancelarPedido;
    @FXML private Button btnCompletar;

    @FXML private ComboBox<PedidoReciente> cmbPedidos;
    @FXML private TableView<DetallePedido> tablaDetalles;
    @FXML private TableColumn<DetallePedido, String> colProducto;
    @FXML private TableColumn<DetallePedido, Double> colPrecio;
    @FXML private TableColumn<DetallePedido, Integer> colCantidad;
    @FXML private TableColumn<DetallePedido, Double> colSubtotal;

    @FXML private Label lblTotal;
    @FXML private Label lblSaldo;
    @FXML private TextField txtAdelanto;
    @FXML private TextArea txtMotivo;

    private final RegistroPedidosDAO dao = new RegistroPedidosDAO();
    private PedidoReciente pedidoActual;
    private double adelantoActual;

    @FXML
    public void initialize() {
        colProducto.setCellValueFactory(
            new PropertyValueFactory<>("nombreProducto")
        );
        colPrecio.setCellValueFactory(
            new PropertyValueFactory<>("precioUnitario")
        );
        colCantidad.setCellValueFactory(
            new PropertyValueFactory<>("cantidad")
        );
        colSubtotal.setCellValueFactory(
            new PropertyValueFactory<>("subtotal")
        );

        cargarPedidosDisponibles();
        limpiarSeleccion();
    }

    private void cargarPedidosDisponibles() {
        try {
            cmbPedidos.setItems(
                FXCollections.observableArrayList(
                    dao.obtenerTodosLosPedidos()
                )
            );
        } catch (SQLException e) {
            mostrarAlerta(
                Alert.AlertType.ERROR,
                "Error",
                "No se pudieron cargar los pedidos: " + e.getMessage()
            );
        }
    }

    @FXML
    private void cargarPedido() {
        PedidoReciente seleccionado = cmbPedidos.getValue();

        if (seleccionado == null) {
            mostrarAlerta(
                Alert.AlertType.WARNING,
                "Pedido requerido",
                "Selecciona un pedido."
            );
            return;
        }

        try {
            pedidoActual = dao.obtenerPedido(seleccionado.getIdPedido());

            tablaDetalles.setItems(
                FXCollections.observableArrayList(
                    dao.obtenerDetallesPedido(pedidoActual.getIdPedido())
                )
            );

            adelantoActual = dao.obtenerAdelanto(
                pedidoActual.getIdPedido()
            );

            txtAdelanto.setText(
                String.format(Locale.US, "%.2f", adelantoActual)
            );

            actualizarImportes();
            actualizarControles();

        } catch (SQLException e) {
            mostrarAlerta(
                Alert.AlertType.ERROR,
                "Error",
                e.getMessage()
            );
        }
    }

    @FXML
    private void guardarAdelanto() {
        if (!validarPedidoEditable()) {
            return;
        }

        try {
            double adelanto = Double.parseDouble(
                txtAdelanto.getText().trim()
            );

            if (!Double.isFinite(adelanto)
                    || adelanto < 0
                    || adelanto > pedidoActual.getTotal()) {
                mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Adelanto inválido",
                    "El adelanto debe estar entre cero y el total del pedido."
                );
                return;
            }

            dao.actualizarAdelanto(
                pedidoActual.getIdPedido(),
                adelanto
            );

            adelantoActual = adelanto;
            actualizarImportes();

            mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Adelanto guardado",
                "El adelanto se guardó correctamente."
            );

        } catch (NumberFormatException e) {
            mostrarAlerta(
                Alert.AlertType.WARNING,
                "Importe inválido",
                "Introduce una cantidad numérica válida."
            );
        } catch (SQLException e) {
            mostrarAlerta(
                Alert.AlertType.ERROR,
                "Error",
                e.getMessage()
            );
        }
    }

    @FXML
    private void completarPedido() {
        if (!validarPedidoEditable()) {
            return;
        }

        double saldo = pedidoActual.getTotal() - adelantoActual;

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Completar pedido");
        confirmacion.setHeaderText("Saldo pendiente: " + moneda(saldo));
        confirmacion.setContentText(
            "Confirma que el pedido fue entregado y que se realizó el cobro pendiente."
        );

        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isEmpty()
                || respuesta.get() != ButtonType.OK) {
            return;
        }

        try {
            dao.completarPedido(
                pedidoActual.getIdPedido(),
                txtMotivo.getText()
            );

            mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Pedido completado",
                "Total: " + moneda(pedidoActual.getTotal())
                + "\nAdelanto: " + moneda(adelantoActual)
                + "\nSaldo calculado: " + moneda(saldo)
                + "\nEstado actualizado a Completado."
            );

            cargarPedidosDisponibles();
            limpiarSeleccion();

        } catch (SQLException e) {
            mostrarAlerta(
                Alert.AlertType.ERROR,
                "Error",
                e.getMessage()
            );
        }
    }

    @FXML
    private void cancelarPedido() {
        if (!validarPedidoEditable()) {
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cancelar pedido");
        confirmacion.setHeaderText("¿Deseas cancelar este pedido?");
        confirmacion.setContentText(
            "El pedido cambiará al estado Cancelado."
        );

        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isEmpty()
                || respuesta.get() != ButtonType.OK) {
            return;
        }

        try {
            dao.cancelarPedido(
                pedidoActual.getIdPedido(),
                txtMotivo.getText()
            );

            mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Pedido cancelado",
                "El pedido se canceló correctamente."
            );

            cargarPedidosDisponibles();
            limpiarSeleccion();

        } catch (SQLException e) {
            mostrarAlerta(
                Alert.AlertType.ERROR,
                "Error",
                e.getMessage()
            );
        }
    }

    @FXML
    private void cerrar() {
        Stage stage = (Stage) btnCerrar.getScene().getWindow();
        stage.close();
    }

    private boolean validarPedidoEditable() {
        if (pedidoActual == null) {
            mostrarAlerta(
                Alert.AlertType.WARNING,
                "Pedido requerido",
                "Primero selecciona y carga un pedido."
            );
            return false;
        }

        String estado = pedidoActual.getEstado();

        if ("Cancelado".equalsIgnoreCase(estado)
                || "Completado".equalsIgnoreCase(estado)
                || "Entregado".equalsIgnoreCase(estado)) {
            mostrarAlerta(
                Alert.AlertType.WARNING,
                "Pedido cerrado",
                "Este pedido ya no se puede modificar."
            );
            return false;
        }

        return true;
    }

    private void actualizarImportes() {
        if (pedidoActual == null) {
            lblTotal.setText("$0.00");
            lblSaldo.setText("$0.00");
            return;
        }

        lblTotal.setText(moneda(pedidoActual.getTotal()));
        lblSaldo.setText(
            moneda(pedidoActual.getTotal() - adelantoActual)
        );
    }

    private void actualizarControles() {
        boolean editable = pedidoActual != null
            && !"Cancelado".equalsIgnoreCase(pedidoActual.getEstado())
            && !"Completado".equalsIgnoreCase(pedidoActual.getEstado())
            && !"Entregado".equalsIgnoreCase(pedidoActual.getEstado());

        txtAdelanto.setDisable(!editable);
        btnGuardarAdelanto.setDisable(!editable);
        btnCancelarPedido.setDisable(!editable);
        btnCompletar.setDisable(!editable);
    }

    private void limpiarSeleccion() {
        pedidoActual = null;
        adelantoActual = 0;

        if (tablaDetalles != null) {
            tablaDetalles.getItems().clear();
        }

        if (cmbPedidos != null) {
            cmbPedidos.getSelectionModel().clearSelection();
        }

        if (txtAdelanto != null) {
            txtAdelanto.clear();
        }

        if (txtMotivo != null) {
            txtMotivo.clear();
        }

        actualizarImportes();

        if (btnCompletar != null) {
            actualizarControles();
        }
    }

    private String moneda(double importe) {
        return String.format(Locale.US, "$%.2f", importe);
    }

    private void mostrarAlerta(
            Alert.AlertType tipo, String titulo, String mensaje) {

        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}