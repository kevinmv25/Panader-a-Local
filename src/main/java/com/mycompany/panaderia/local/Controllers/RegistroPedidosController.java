
package com.mycompany.panaderia.local.Controllers;

import com.panaderialocal.conexion.RegistroPedidosDAO;
import com.panaderialocal.conexion.RegistroPedidosDAO.DetallePedido;
import com.panaderialocal.conexion.RegistroPedidosDAO.Pan;
import com.panaderialocal.conexion.RegistroPedidosDAO.PedidoReciente;
import com.mycompany.panaderia.local.App;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RegistroPedidosController implements Initializable {

    @FXML private TextField txtNombreCliente;
    @FXML private TextField txtTelefono;
    @FXML private DatePicker dpFechaEntrega;
    @FXML private ComboBox<String> cbTipoPedido;

    @FXML private ComboBox<Pan> cbTipoPan;
    @FXML private Spinner<Integer> spCantidad;
    @FXML private TextField txtPrecioUnitario;

    @FXML private Button btnAgregarProducto;
    @FXML private Button btnLimpiar;
    @FXML private Button btnGuardarPedido;
    @FXML private Button btnVerPedidosRecientes;
    @FXML private Button btnRegresar;

    @FXML private TableView<DetallePedido> tablaProductos;
    @FXML private TableColumn<DetallePedido, String> colProducto;
    @FXML private TableColumn<DetallePedido, String> colCantidad;
    @FXML private TableColumn<DetallePedido, String> colPrecio;
    @FXML private TableColumn<DetallePedido, String> colSubtotal;

    @FXML private Label lblTotal;
    @FXML private TextArea txtObservaciones;

    private final RegistroPedidosDAO dao = new RegistroPedidosDAO();

    private final ObservableList<DetallePedido> detalles =
            FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        tablaProductos.setItems(detalles);
        configurarTabla();

        spCantidad.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1)
        );

        txtPrecioUnitario.setEditable(false);

        cbTipoPedido.getItems().setAll(
                "Pendiente",
                "En preparación",
                "Listo",
                "Entregado",
                "Cancelado"
        );

        cbTipoPedido.getSelectionModel().selectFirst();

        cbTipoPan.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, pan) -> {
                    if (pan != null) {
                        txtPrecioUnitario.setText(
                                String.format(Locale.US, "%.2f", pan.getPrecio())
                        );
                    } else {
                        txtPrecioUnitario.clear();
                    }
                });

        btnAgregarProducto.setOnAction(e -> agregarProducto());
        btnLimpiar.setOnAction(e -> limpiarFormulario());
        btnGuardarPedido.setOnAction(e -> guardarPedido());
        btnVerPedidosRecientes.setOnAction(e -> mostrarPedidosRecientes());
        btnRegresar.setOnAction(e -> regresarAInterfazAnterior());

        cargarPanes();
        actualizarTotal();
    }

    private void configurarTabla() {

        colProducto.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        dato.getValue().getNombreProducto()
                )
        );

        colCantidad.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        String.valueOf(dato.getValue().getCantidad())
                )
        );

        colPrecio.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        String.format(
                                Locale.US,
                                "$ %.2f",
                                dato.getValue().getPrecioUnitario()
                        )
                )
        );

        colSubtotal.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        String.format(
                                Locale.US,
                                "$ %.2f",
                                dato.getValue().getSubtotal()
                        )
                )
        );
    }

    private void cargarPanes() {
        try {
            List<Pan> panes = dao.obtenerPanes();

            cbTipoPan.setItems(
                    FXCollections.observableArrayList(panes)
            );

            if (panes.isEmpty()) {
                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Sin productos",
                        "No hay panes registrados en la base de datos."
                );
            }

        } catch (SQLException ex) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error al cargar los panes",
                    ex.getMessage()
            );
        }
    }

    private void agregarProducto() {

        Pan pan = cbTipoPan.getValue();

        if (pan == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Producto requerido",
                    "Selecciona un tipo de pan."
            );
            return;
        }

        Integer cantidad = spCantidad.getValue();

        if (cantidad == null || cantidad < 1) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Cantidad inválida",
                    "La cantidad debe ser mayor que cero."
            );
            return;
        }

        detalles.add(new DetallePedido(pan, cantidad));
        actualizarTotal();

        cbTipoPan.getSelectionModel().clearSelection();
        spCantidad.getValueFactory().setValue(1);
        txtPrecioUnitario.clear();
    }

    private double calcularTotal() {
        return detalles.stream()
                .mapToDouble(DetallePedido::getSubtotal)
                .sum();
    }

    private void actualizarTotal() {
        lblTotal.setText(
                String.format(
                        Locale.US,
                        "Total: %.2f MXN",
                        calcularTotal()
                )
        );
    }

    private void guardarPedido() {

        String nombre = txtNombreCliente.getText().trim();
        String telefono = txtTelefono.getText().trim();

        if (nombre.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Falta el cliente",
                    "Escribe el nombre del cliente."
            );
            return;
        }

        if (dpFechaEntrega.getValue() == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Falta la fecha",
                    "Selecciona la fecha de entrega."
            );
            return;
        }

        if (cbTipoPedido.getValue() == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Falta el tipo de pedido",
                    "Selecciona una opción."
            );
            return;
        }

        if (detalles.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Pedido vacío",
                    "Agrega al menos un producto."
            );
            return;
        }

        try {
            dao.registrarPedido(
                    nombre,
                    telefono,
                    dpFechaEntrega.getValue(),
                    cbTipoPedido.getValue(),
                    txtObservaciones.getText().trim(),
                    calcularTotal(),
                    new ArrayList<>(detalles)
            );

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Pedido registrado",
                    "El pedido y sus productos se guardaron correctamente."
            );

            limpiarFormulario();

        } catch (SQLException ex) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo registrar el pedido",
                    ex.getMessage()
            );
        }
    }

    private void limpiarFormulario() {

        txtNombreCliente.clear();
        txtTelefono.clear();
        dpFechaEntrega.setValue(null);
        txtObservaciones.clear();

        cbTipoPan.getSelectionModel().clearSelection();
        cbTipoPedido.getSelectionModel().selectFirst();

        txtPrecioUnitario.clear();
        spCantidad.getValueFactory().setValue(1);

        detalles.clear();
        actualizarTotal();
    }

    private void mostrarPedidosRecientes() {

        try {
            List<PedidoReciente> pedidos =
                    dao.obtenerPedidosRecientes();

            TableView<PedidoReciente> tabla = new TableView<>();

            TableColumn<PedidoReciente, String> colId =
                    new TableColumn<>("N.º");
            colId.setCellValueFactory(
                    dato -> new SimpleStringProperty(
                            String.valueOf(dato.getValue().getIdPedido())
                    )
            );

            TableColumn<PedidoReciente, String> colCliente =
                    new TableColumn<>("Cliente");
            colCliente.setCellValueFactory(
                    dato -> new SimpleStringProperty(
                            textoSeguro(dato.getValue().getCliente())
                    )
            );

            TableColumn<PedidoReciente, String> colTelefono =
                    new TableColumn<>("Teléfono");
            colTelefono.setCellValueFactory(
                    dato -> new SimpleStringProperty(
                            textoSeguro(dato.getValue().getTelefono())
                    )
            );

            TableColumn<PedidoReciente, String> colFechaPedido =
                    new TableColumn<>("Fecha de registro");
            colFechaPedido.setCellValueFactory(
                    dato -> new SimpleStringProperty(
                            textoSeguro(dato.getValue().getFechaPedido())
                    )
            );

            TableColumn<PedidoReciente, String> colFechaEntrega =
                    new TableColumn<>("Fecha de entrega");
            colFechaEntrega.setCellValueFactory(
                    dato -> new SimpleStringProperty(
                            textoSeguro(dato.getValue().getFechaEntrega())
                    )
            );

            TableColumn<PedidoReciente, String> colEstado =
                    new TableColumn<>("Estado");
            colEstado.setCellValueFactory(
                    dato -> new SimpleStringProperty(
                            textoSeguro(dato.getValue().getEstado())
                    )
            );

            TableColumn<PedidoReciente, String> colTotal =
                    new TableColumn<>("Total");
            colTotal.setCellValueFactory(
                    dato -> new SimpleStringProperty(
                            String.format(
                                    Locale.US,
                                    "$ %.2f",
                                    dato.getValue().getTotal()
                            )
                    )
            );

            tabla.getColumns().addAll(
                    colId,
                    colCliente,
                    colTelefono,
                    colFechaPedido,
                    colFechaEntrega,
                    colEstado,
                    colTotal
            );

            tabla.setColumnResizePolicy(
                    TableView.CONSTRAINED_RESIZE_POLICY
            );

            tabla.setItems(
                    FXCollections.observableArrayList(pedidos)
            );

            Label titulo = new Label("Pedidos recientes");
            titulo.setStyle(
                    "-fx-font-size: 22px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #422817;"
            );

            Label mensaje = new Label(
                    pedidos.isEmpty()
                            ? "Todavía no hay pedidos registrados."
                            : "Se muestran los últimos "
                                    + pedidos.size()
                                    + " pedidos registrados."
            );

            mensaje.setStyle("-fx-text-fill: #6B4A2E;");

            VBox contenido = new VBox(12, titulo, mensaje, tabla);
            contenido.setStyle(
                    "-fx-background-color: #FFF8EC;" +
                    "-fx-padding: 20;"
            );

            VBox.setVgrow(tabla, Priority.ALWAYS);

            Stage ventana = new Stage();
            ventana.setTitle("El Lirio Rojo - Pedidos recientes");
            ventana.setScene(new Scene(contenido, 1050, 500));
            ventana.show();

        } catch (SQLException ex) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error al consultar pedidos",
                    ex.getMessage()
            );
        }
    }

    private void regresarAInterfazAnterior() {
        try {
            App.setRoot("cajeropedido");
        } catch (IOException ex) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo regresar",
                    "No fue posible volver a la interfaz del cajero. "
                            + ex.getMessage()
            );
        }
    }

    private String textoSeguro(String texto) {
        return texto == null ? "" : texto;
    }

    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensaje) {

        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
