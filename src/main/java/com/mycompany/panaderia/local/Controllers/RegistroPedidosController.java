
package com.mycompany.panaderia.local.Controllers;

import com.panaderialocal.conexion.RegistroPedidosDAO;
import com.panaderialocal.conexion.RegistroPedidosDAO.DetallePedido;
import com.panaderialocal.conexion.RegistroPedidosDAO.Pan;
import com.panaderialocal.conexion.RegistroPedidosDAO.PedidoReciente;
import com.mycompany.panaderia.local.App;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.time.LocalDate;

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

    // ADELANTO Y DETALLE EXTRA
    @FXML private TextField txtAdelanto;
    @FXML private Label lblSaldoPendiente;
    @FXML private Label lblAvisoAdelanto;

    // importes: hasta 7 enteros y 2 decimales
    private static final Pattern FORMATO_IMPORTE =
            Pattern.compile("\\d{0,7}([.]\\d{0,2})?");

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

        dpFechaEntrega.setValue(LocalDate.now());

        dpFechaEntrega.setDayCellFactory(param -> new DateCell() {

            @Override
            public void updateItem(LocalDate fecha, boolean empty) {
                super.updateItem(fecha, empty);

                if (empty || fecha.isBefore(LocalDate.now())) {
                    setDisable(true);
                }
            }
        });

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

        // ADELANTO: solo importes con hasta dos decimales
        txtAdelanto.setTextFormatter(new TextFormatter<String>(cambio ->
                FORMATO_IMPORTE.matcher(cambio.getControlNewText()).matches()
                        ? cambio : null));

        txtAdelanto.textProperty().addListener(
                (observable, anterior, nuevo) -> actualizarSaldo());

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

        if (pan.getPrecio() <= 0) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Precio inválido",
                    "El producto seleccionado tiene un precio inválido."
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

        double total = 0;

        for (DetallePedido detalle : detalles) {
            total += detalle.getSubtotal();
        }

        return total;
    }

    private void actualizarTotal() {
        lblTotal.setText(
                String.format(
                        Locale.US,
                        "Total: %.2f MXN",
                        calcularTotal()
                )
        );

        actualizarSaldo();
    }

    private BigDecimal totalDecimal() {
        return BigDecimal.valueOf(calcularTotal())
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Vacío = sin adelanto. Devuelve null si el texto no es un importe válido.
    private BigDecimal leerAdelanto() {

        String texto = txtAdelanto.getText() == null
                ? "" : txtAdelanto.getText().trim();

        if (texto.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }

        try {
            return new BigDecimal(texto).setScale(2, RoundingMode.UNNECESSARY);

        } catch (NumberFormatException | ArithmeticException ex) {
            return null;
        }
    }

    private String importe(BigDecimal valor) {
        return String.format(Locale.US, "$ %.2f", valor);
    }

    // Saldo por cobrar = total del pedido - adelanto (se ve mientras se captura)
    private void actualizarSaldo() {

        BigDecimal total = totalDecimal();
        BigDecimal adelanto = leerAdelanto();

        String aviso = "";

        if (adelanto == null) {
            adelanto = BigDecimal.ZERO;
            aviso = "Escribe el adelanto como importe, por ejemplo 150 o 150.50.";

        } else if (adelanto.compareTo(total) > 0) {
            aviso = "El adelanto no puede ser mayor que el total del pedido.";
        }

        lblSaldoPendiente.setText(
                importe(total.subtract(adelanto).max(BigDecimal.ZERO))
        );

        lblAvisoAdelanto.setText(aviso);
        lblAvisoAdelanto.setVisible(!aviso.isEmpty());
        lblAvisoAdelanto.setManaged(!aviso.isEmpty());
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

        for (int i = 0; i < nombre.length(); i++) {

            char letra = nombre.charAt(i);

            if (!((letra >= 'A' && letra <= 'Z')
                    || (letra >= 'a' && letra <= 'z')
                    || letra == ' '
                    || letra == 'á' || letra == 'é'
                    || letra == 'í' || letra == 'ó'
                    || letra == 'ú'
                    || letra == 'Á' || letra == 'É'
                    || letra == 'Í' || letra == 'Ó'
                    || letra == 'Ú'
                    || letra == 'ñ' || letra == 'Ñ'
                    || letra == 'ü' || letra == 'Ü')) {

                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Nombre inválido",
                        "El nombre del cliente solo debe contener letras."
                );
                return;
            }
        }

        if (!telefono.isEmpty()) {

            if (telefono.length() != 10) {
                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Teléfono inválido",
                        "El teléfono debe contener exactamente 10 números."
                );
                return;
            }

            for (int i = 0; i < telefono.length(); i++) {

                if (telefono.charAt(i) < '0'
                        || telefono.charAt(i) > '9') {

                    mostrarAlerta(
                            Alert.AlertType.WARNING,
                            "Teléfono inválido",
                            "El teléfono solo debe contener números."
                    );
                    return;
                }
            }
        }

        if (dpFechaEntrega.getValue() == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Falta la fecha",
                    "Selecciona la fecha de entrega."
            );
            return;
        }

        if (dpFechaEntrega.getValue().isBefore(LocalDate.now())) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Fecha inválida",
                    "La fecha de entrega no puede ser anterior a hoy."
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

        BigDecimal adelanto = leerAdelanto();

        if (adelanto == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Adelanto inválido",
                    "Escribe el adelanto como importe, por ejemplo 150 o 150.50."
            );
            return;
        }

        BigDecimal total = totalDecimal();

        if (adelanto.compareTo(total) > 0) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Adelanto inválido",
                    "El adelanto no puede ser mayor que el total del pedido."
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
                    adelanto.doubleValue(),
                    new ArrayList<>(detalles)
            );

            String resumen =
                    "El pedido y sus productos se guardaron correctamente.";

            if (adelanto.signum() > 0) {
                resumen += "\nAdelanto registrado: " + importe(adelanto)
                        + "\nSaldo por cobrar: "
                        + importe(total.subtract(adelanto));
            }

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Pedido registrado",
                    resumen
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
        dpFechaEntrega.setValue(LocalDate.now());
        txtObservaciones.clear();
        txtAdelanto.clear();

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

            TableColumn<PedidoReciente, String> colAdelanto =
                    new TableColumn<>("Adelanto");
            colAdelanto.setCellValueFactory(
                    dato -> new SimpleStringProperty(
                            String.format(
                                    Locale.US,
                                    "$ %.2f",
                                    dato.getValue().getAdelanto()
                            )
                    )
            );

            TableColumn<PedidoReciente, String> colSaldo =
                    new TableColumn<>("Saldo por cobrar");
            colSaldo.setCellValueFactory(
                    dato -> new SimpleStringProperty(
                            String.format(
                                    Locale.US,
                                    "$ %.2f",
                                    dato.getValue().getSaldo()
                            )
                    )
            );

            TableColumn<PedidoReciente, String> colDetalleExtra =
                    new TableColumn<>("Detalle extra");
            colDetalleExtra.setPrefWidth(260);
            colDetalleExtra.setCellValueFactory(
                    dato -> new SimpleStringProperty(
                            textoSeguro(dato.getValue().getDetalleExtra())
                                    .replace("\n", " · ")
                    )
            );

            tabla.getColumns().addAll(
                    colId,
                    colCliente,
                    colTelefono,
                    colFechaPedido,
                    colFechaEntrega,
                    colEstado,
                    colTotal,
                    colAdelanto,
                    colSaldo,
                    colDetalleExtra
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
            ventana.setScene(new Scene(
                    contenido,
                    Math.min(1320, Screen.getPrimary().getVisualBounds().getWidth() - 40),
                    500));
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
