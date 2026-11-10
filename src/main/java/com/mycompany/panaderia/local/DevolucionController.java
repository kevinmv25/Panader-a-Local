package com.mycompany.panaderia.local;

import com.panaderialocal.conexion.DevolucionDAO;
import com.panaderialocal.conexion.DevolucionDAO.LineaDevuelta;
import com.panaderialocal.conexion.DevolucionDAO.OrigenDevolucion;
import com.panaderialocal.conexion.RegistroPedidosDAO.DetallePedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class DevolucionController {

    @FXML private ComboBox<OrigenDevolucion> cmbOrigen;

    @FXML private TableView<DetallePedido> tablaVenta;
    @FXML private TableColumn<DetallePedido, String> colProducto;
    @FXML private TableColumn<DetallePedido, String> colPrecio;
    @FXML private TableColumn<DetallePedido, String> colCantidad;
    @FXML private TableColumn<DetallePedido, String> colSubtotal;

    @FXML private Spinner<Integer> spCantidad;
    @FXML private Button btnAgregar;

    @FXML private TableView<LineaDevuelta> tablaDevolucion;
    @FXML private TableColumn<LineaDevuelta, String> colDevProducto;
    @FXML private TableColumn<LineaDevuelta, String> colDevCantidad;
    @FXML private TableColumn<LineaDevuelta, String> colDevImporte;

    @FXML private Label lblSaldo;
    @FXML private Label lblTotalDevolver;
    @FXML private Label lblPiezas;
    @FXML private TextArea txtMotivo;
    @FXML private Button btnConfirmar;
    @FXML private Button btnCerrar;

    // Hoja de estilos de esta pantalla (tambien se aplica a sus alertas).
    private static final String ESTILOS = DevolucionController.class
            .getResource("/com/mycompany/panaderia/local/estilos/devolucion.css")
            .toExternalForm();

    private final DevolucionDAO dao = new DevolucionDAO();

    private final ObservableList<DetallePedido> lineasOrigen =
            FXCollections.observableArrayList();

    private final ObservableList<LineaDevuelta> devolucion =
            FXCollections.observableArrayList();

    private OrigenDevolucion origenActual;

    @FXML
    public void initialize() {

        colProducto.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        dato.getValue().getNombreProducto()
                )
        );
        colPrecio.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        moneda(BigDecimal.valueOf(
                                dato.getValue().getPrecioUnitario()
                        ).setScale(2, RoundingMode.HALF_UP))
                )
        );
        colCantidad.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        String.valueOf(dato.getValue().getCantidad())
                )
        );
        colSubtotal.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        moneda(subtotalDe(dato.getValue()))
                )
        );

        colDevProducto.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        dato.getValue().getProducto()
                )
        );
        colDevCantidad.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        String.valueOf(dato.getValue().getCantidad())
                )
        );
        colDevImporte.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        moneda(dato.getValue().getImporte())
                )
        );

        tablaVenta.setItems(lineasOrigen);
        tablaVenta.setPlaceholder(
                new Label("Selecciona un pedido o una venta")
        );

        tablaDevolucion.setItems(devolucion);
        tablaDevolucion.setPlaceholder(
                new Label("Aún no hay productos en la devolución")
        );

        // El total a devolver se recalcula solo ante CUALQUIER cambio.
        devolucion.addListener(
                (ListChangeListener<LineaDevuelta>) cambio -> {
                    actualizarTotal();
                    prepararCantidad(
                            tablaVenta.getSelectionModel().getSelectedItem()
                    );
                }
        );

        cmbOrigen.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, origen) ->
                        cargarOrigen(origen));

        tablaVenta.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, linea) ->
                        prepararCantidad(linea));

        cargarOrigenes();
        cargarOrigen(null);
        actualizarTotal();
    }

    private void cargarOrigenes() {
        try {
            cmbOrigen.getSelectionModel().clearSelection();

            ObservableList<OrigenDevolucion> origenes =
                    FXCollections.observableArrayList(
                            dao.obtenerOrigenes()
                    );

            cmbOrigen.setItems(origenes);

            cmbOrigen.setPromptText(
                    origenes.isEmpty()
                            ? "No hay pedidos ni ventas por devolver"
                            : "Selecciona un pedido o una venta"
            );

        } catch (SQLException ex) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error al cargar los pedidos",
                    ex.getMessage()
            );
        }
    }

    private void cargarOrigen(OrigenDevolucion origen) {

        devolucion.clear();
        lineasOrigen.clear();
        origenActual = origen;

        if (origen == null) {
            lblSaldo.setText(moneda(BigDecimal.ZERO));
            prepararCantidad(null);
            return;
        }

        try {
            lineasOrigen.setAll(dao.obtenerLineas(origen));
            lblSaldo.setText(moneda(origen.getSaldo()));

        } catch (SQLException ex) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error al cargar los productos",
                    ex.getMessage()
            );
        }
    }

    // Ajusta el Spinner: solo se puede devolver lo que aún no se agregó.
    private void prepararCantidad(DetallePedido linea) {

        int disponible = linea == null ? 0 : cantidadDisponible(linea);
        boolean hay = disponible > 0;

        spCantidad.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, Math.max(1, disponible), 1
                )
        );
        spCantidad.setDisable(!hay);
        btnAgregar.setDisable(!hay);
    }

    @FXML
    private void agregarADevolucion() {

        DetallePedido linea = tablaVenta.getSelectionModel().getSelectedItem();

        if (origenActual == null || linea == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Producto requerido",
                    "Selecciona en la tabla el pan que se va a devolver."
            );
            return;
        }

        Integer cantidad = spCantidad.getValue();
        int disponible = cantidadDisponible(linea);

        if (cantidad == null || cantidad < 1 || cantidad > disponible) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Cantidad inválida",
                    "Solo quedan " + disponible
                            + " pieza(s) de este producto por devolver."
            );
            return;
        }

        int indice = indiceEnDevolucion(linea.getIdPan());
        int nuevaCantidad = cantidad
                + (indice >= 0 ? devolucion.get(indice).getCantidad() : 0);

        LineaDevuelta nueva = new LineaDevuelta(
                linea.getIdPan(),
                linea.getNombreProducto(),
                nuevaCantidad,
                importeDe(linea, nuevaCantidad)
        );

        BigDecimal totalPrevisto = calcularTotal()
                .subtract(indice >= 0
                        ? devolucion.get(indice).getImporte()
                        : BigDecimal.ZERO)
                .add(nueva.getImporte());

        if (totalPrevisto.compareTo(origenActual.getSaldo()) > 0) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Importe excedido",
                    "La devolución no puede superar el máximo a devolver ("
                            + moneda(origenActual.getSaldo()) + ")."
            );
            return;
        }

        if (indice >= 0) {
            devolucion.set(indice, nueva);
        } else {
            devolucion.add(nueva);
        }
    }

    @FXML
    private void quitarDeDevolucion() {

        int indice = tablaDevolucion.getSelectionModel().getSelectedIndex();

        if (indice < 0) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Producto requerido",
                    "Selecciona un producto de la lista a devolver."
            );
            return;
        }

        devolucion.remove(indice);
    }

    @FXML
    private void confirmarDevolucion() {

        if (origenActual == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Pedido requerido",
                    "Selecciona el pedido al que pertenece el pan."
            );
            return;
        }

        if (devolucion.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Devolución vacía",
                    "Agrega al menos un producto a devolver."
            );
            return;
        }

        String motivo = txtMotivo.getText() == null
                ? "" : txtMotivo.getText().trim();

        if (motivo.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Motivo requerido",
                    "Escribe el motivo (por ejemplo: pan duro o con moho)."
            );
            return;
        }

        BigDecimal total = calcularTotal();

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar devolución");
        confirmacion.setHeaderText("Devolver al cliente: " + moneda(total));
        confirmacion.setContentText(textoConfirmacion(total));
        aplicarEstilo(confirmacion);

        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }

        try {
            // El DAO devuelve el importe realmente registrado.
            BigDecimal devuelto = dao.registrarDevolucion(
                    origenActual,
                    new ArrayList<>(devolucion),
                    descripcionMotivo(motivo)
            );

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Devolución registrada",
                    (origenActual.esPedido()
                            ? "El pan devuelto se descontó del pedido #"
                                    + origenActual.getId() + ".\n"
                            : "")
                            + "Devuelve " + moneda(devuelto) + " al cliente."
            );

            txtMotivo.clear();
            cargarOrigenes();

        } catch (SQLException ex) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo registrar la devolución",
                    ex.getMessage()
            );
        }
    }

    @FXML
    private void cerrar() {
        Stage stage = (Stage) btnCerrar.getScene().getWindow();
        stage.close();
    }

    // ---------------------------------------------------------
    // CÁLCULOS
    // ---------------------------------------------------------

    private int cantidadDisponible(DetallePedido linea) {

        int indice = indiceEnDevolucion(linea.getIdPan());
        int yaAgregada = indice >= 0 ? devolucion.get(indice).getCantidad() : 0;

        return cantidadEnOrigen(linea.getIdPan()) - yaAgregada;
    }

    // Un pedido puede traer el mismo pan en varios renglones: el DAO descuenta
    // de todos ellos, asi que el maximo a devolver es la suma de las piezas.
    private int cantidadEnOrigen(int idPan) {

        int total = 0;

        for (DetallePedido linea : lineasOrigen) {
            if (linea.getIdPan() == idPan) {
                total += linea.getCantidad();
            }
        }

        return total;
    }

    private int indiceEnDevolucion(int idPan) {

        for (int i = 0; i < devolucion.size(); i++) {
            if (devolucion.get(i).getIdPan() == idPan) {
                return i;
            }
        }

        return -1;
    }

    private BigDecimal subtotalDe(DetallePedido linea) {
        return BigDecimal.valueOf(linea.getSubtotal())
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Importe = piezas devueltas x precio con el que se cobró cada renglón.
    // Se toman los renglones del mismo pan en orden, igual que el DAO, para que
    // lo que muestra la pantalla sea exactamente lo que se descuenta aunque el
    // mismo pan tenga renglones con precios distintos.
    private BigDecimal importeDe(DetallePedido linea, int cantidad) {

        BigDecimal importe = BigDecimal.ZERO;
        int pendiente = cantidad;

        for (DetallePedido renglon : lineasOrigen) {

            if (renglon.getIdPan() != linea.getIdPan() || pendiente == 0) {
                continue;
            }

            int tomar = Math.min(pendiente, renglon.getCantidad());

            importe = importe.add(
                    BigDecimal.valueOf(renglon.getPrecioUnitario())
                            .multiply(BigDecimal.valueOf(tomar)));

            pendiente -= tomar;
        }

        return importe.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularTotal() {

        BigDecimal total = BigDecimal.ZERO;

        for (LineaDevuelta linea : devolucion) {
            total = total.add(linea.getImporte());
        }

        return total;
    }

    private void actualizarTotal() {

        int piezas = 0;

        for (LineaDevuelta linea : devolucion) {
            piezas += linea.getCantidad();
        }

        lblTotalDevolver.setText(moneda(calcularTotal()));
        lblPiezas.setText(String.valueOf(piezas));
        btnConfirmar.setDisable(devolucion.isEmpty());
    }

    private String textoConfirmacion(BigDecimal totalDevolver) {

        if (origenActual.esPedido()) {
            return "Pedido #" + origenActual.getId()
                    + ": el pan devuelto se descontará del pedido y su "
                    + "total pasará de " + moneda(origenActual.getSaldo())
                    + " a "
                    + moneda(origenActual.getSaldo().subtract(totalDevolver))
                    + ".\nEntrega el dinero solo después de confirmar.";
        }

        return "Venta #" + origenActual.getId()
                + ". Entrega el dinero solo después de confirmar.";
    }

    // Queda registrado qué pan se devolvió y por qué.
    private String descripcionMotivo(String motivo) {

        StringBuilder texto = new StringBuilder("Devolución de: ");

        for (int i = 0; i < devolucion.size(); i++) {

            LineaDevuelta linea = devolucion.get(i);

            if (i > 0) {
                texto.append(", ");
            }

            texto.append(linea.getCantidad())
                    .append(" x ")
                    .append(linea.getProducto());
        }

        return texto.append(". Motivo: ").append(motivo).toString();
    }

    private String moneda(BigDecimal importe) {
        return String.format(Locale.US, "$%.2f", importe);
    }

    private void mostrarAlerta(
            Alert.AlertType tipo, String titulo, String mensaje) {

        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        aplicarEstilo(alerta);
        alerta.showAndWait();
    }

    // Aplica la paleta El Lirio Rojo a los cuadros de dialogo.
    private void aplicarEstilo(Alert alerta) {

        DialogPane panel = alerta.getDialogPane();

        if (!panel.getStylesheets().contains(ESTILOS)) {
            panel.getStylesheets().add(ESTILOS);
        }

        panel.getStyleClass().add("dialogo-devolucion");
    }
}