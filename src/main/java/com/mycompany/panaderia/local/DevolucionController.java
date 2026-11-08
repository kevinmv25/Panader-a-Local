/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.panaderia.local;

import com.panaderialocal.conexion.DevolucionDAO;
import com.panaderialocal.conexion.DevolucionDAO.VentaDevolucion;
import com.panaderialocal.conexion.RegistroPedidosDAO.DetallePedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
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
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

/**
 *
 * @author aleja
 */
public class DevolucionController {
    @FXML private ComboBox<VentaDevolucion> cmbVentas;

    @FXML private TableView<DetallePedido> tablaVenta;
    @FXML private TableColumn<DetallePedido, String> colProducto;
    @FXML private TableColumn<DetallePedido, String> colPrecio;
    @FXML private TableColumn<DetallePedido, String> colCantidad;
    @FXML private TableColumn<DetallePedido, String> colSubtotal;

    @FXML private Spinner<Integer> spCantidad;
    @FXML private Button btnAgregar;

    @FXML private TableView<LineaDevolucion> tablaDevolucion;
    @FXML private TableColumn<LineaDevolucion, String> colDevProducto;
    @FXML private TableColumn<LineaDevolucion, String> colDevCantidad;
    @FXML private TableColumn<LineaDevolucion, String> colDevImporte;

    @FXML private Label lblSaldo;
    @FXML private Label lblTotalDevolver;
    @FXML private TextArea txtMotivo;
    @FXML private Button btnConfirmar;
    @FXML private Button btnCerrar;

    private final DevolucionDAO dao = new DevolucionDAO();

    private final ObservableList<DetallePedido> lineasVenta =
            FXCollections.observableArrayList();

    private final ObservableList<LineaDevolucion> devolucion =
            FXCollections.observableArrayList();

    private VentaDevolucion ventaActual;

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

        tablaVenta.setItems(lineasVenta);
        tablaVenta.setPlaceholder(new Label("Selecciona una venta"));

        tablaDevolucion.setItems(devolucion);
        tablaDevolucion.setPlaceholder(
                new Label("Aún no hay productos en la devolución")
        );

        // El total a devolver se recalcula solo ante CUALQUIER cambio.
        devolucion.addListener(
                (ListChangeListener<LineaDevolucion>) cambio -> {
                    actualizarTotal();
                    prepararCantidad(
                            tablaVenta.getSelectionModel().getSelectedItem()
                    );
                }
        );

        cmbVentas.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, venta) ->
                        cargarVenta(venta));

        tablaVenta.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, linea) ->
                        prepararCantidad(linea));

        cargarVentas();
        cargarVenta(null);
    }

    private void cargarVentas() {
        try {
            cmbVentas.getSelectionModel().clearSelection();

            ObservableList<VentaDevolucion> ventas =
                    FXCollections.observableArrayList(
                            dao.obtenerVentasConSaldo()
                    );

            cmbVentas.setItems(ventas);

            cmbVentas.setPromptText(
                    ventas.isEmpty()
                            ? "No hay ventas con saldo por devolver"
                            : "Selecciona una venta"
            );

        } catch (SQLException ex) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error al cargar las ventas",
                    ex.getMessage()
            );
        }
    }

    private void cargarVenta(VentaDevolucion venta) {

        devolucion.clear();
        lineasVenta.clear();
        ventaActual = venta;

        if (venta == null) {
            lblSaldo.setText(moneda(BigDecimal.ZERO));
            prepararCantidad(null);
            return;
        }

        try {
            lineasVenta.setAll(dao.obtenerLineasVenta(venta.getIdVenta()));
            lblSaldo.setText(moneda(venta.getSaldo()));

        } catch (SQLException ex) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error al cargar la venta",
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

        if (ventaActual == null || linea == null) {
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

        LineaDevolucion nueva = new LineaDevolucion(
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

        if (totalPrevisto.compareTo(ventaActual.getSaldo()) > 0) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Importe excedido",
                    "La devolución no puede superar el saldo reembolsable "
                            + "de la venta (" + moneda(ventaActual.getSaldo())
                            + ")."
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

        if (ventaActual == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Venta requerida",
                    "Selecciona la venta a la que pertenece el pan."
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
        confirmacion.setContentText(
                "Venta #" + ventaActual.getIdVenta()
                        + ". Entrega el dinero solo después de confirmar."
        );

        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }

        try {
            dao.registrarDevolucion(
                    ventaActual.getIdVenta(),
                    total,
                    descripcionMotivo(motivo)
            );

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Devolución registrada",
                    "Devuelve " + moneda(total) + " al cliente."
            );

            txtMotivo.clear();
            cargarVentas();

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

        return linea.getCantidad() - yaAgregada;
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

    // Importe = subtotal de la línea x piezas devueltas / piezas vendidas.
    // Si se devuelven todas las piezas, es exactamente lo que se cobró.
    private BigDecimal importeDe(DetallePedido linea, int cantidad) {
        return subtotalDe(linea)
                .multiply(BigDecimal.valueOf(cantidad))
                .divide(BigDecimal.valueOf(linea.getCantidad()),
                        2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularTotal() {

        BigDecimal total = BigDecimal.ZERO;

        for (LineaDevolucion linea : devolucion) {
            total = total.add(linea.getImporte());
        }

        return total;
    }

    private void actualizarTotal() {
        lblTotalDevolver.setText(moneda(calcularTotal()));
        btnConfirmar.setDisable(devolucion.isEmpty());
    }

    // Queda en la BD qué pan se devolvió, ya que la tabla devoluciones
    // solo guarda venta, motivo y monto.
    private String descripcionMotivo(String motivo) {

        StringBuilder texto = new StringBuilder("Devolución de: ");

        for (int i = 0; i < devolucion.size(); i++) {

            LineaDevolucion linea = devolucion.get(i);

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
        alerta.showAndWait();
    }

    // Línea de la devolución en curso (producto, piezas e importe).
    private static final class LineaDevolucion {

        private final int idPan;
        private final String producto;
        private final int cantidad;
        private final BigDecimal importe;

        private LineaDevolucion(
                int idPan, String producto, int cantidad, BigDecimal importe) {

            this.idPan = idPan;
            this.producto = producto;
            this.cantidad = cantidad;
            this.importe = importe;
        }

        private int getIdPan() {
            return idPan;
        }

        private String getProducto() {
            return producto;
        }

        private int getCantidad() {
            return cantidad;
        }

        private BigDecimal getImporte() {
            return importe;
        }
    }
    
}
