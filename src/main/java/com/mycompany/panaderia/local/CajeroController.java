package com.mycompany.panaderia.local;


import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import com.panaderialocal.conexion.RegistroPedidosDAO.DetallePedido;
import java.io.File;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import panaderialocal.conexion.PanDAO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class CajeroController {

    @FXML private Button btnSalir;
    @FXML private Button btnEditarPedido;
    @FXML private Button btnIrRegistro;
    @FXML private Button btnDevolucion;

    @FXML private FlowPane contenedorPanes;
    @FXML private TableView<DetallePedido> tablaCuenta;
    @FXML private TableColumn<DetallePedido, String> colCantidad;
    @FXML private TableColumn<DetallePedido, String> colProducto;
    @FXML private TableColumn<DetallePedido, String> colSubtotal;
    @FXML private Label lblTotalCuenta;
    @FXML private Button btnQuitarUno;
    @FXML private Button btnVaciarCuenta;

    private static final int CANTIDAD_MAXIMA = 9999;

    private final PanDAO panDAO = new PanDAO();

    private final ObservableList<DetallePedido> cuenta =
            FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        btnSalir.setOnAction(event -> {
            try {
                App.mostrar("Login");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        btnEditarPedido.setOnAction(event -> abrirGestionPedidos());
        btnDevolucion.setOnAction(event -> abrirDevolucion());
        
        btnIrRegistro.setOnAction(event -> {
            try {
                App.setRoot("Cajero/RegistroPedidos");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        configurarCuenta();
        cargarPanes();
    }
    
    private void abrirDevolucion() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("devolucion.fxml")
            );

            Parent root = loader.load();

            Stage ventana = new Stage();
            ventana.initModality(Modality.APPLICATION_MODAL);
            ventana.setTitle("Devolución / Reembolso");
            ventana.setScene(new Scene(root, 850, 700));
            ventana.setResizable(true);
            ventana.showAndWait();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void abrirGestionPedidos() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("editarpedido.fxml")
            );

            Parent root = loader.load();

            Stage ventana = new Stage();
            ventana.initModality(Modality.APPLICATION_MODAL);
            ventana.setTitle("Gestión de pedidos");
            ventana.setScene(new Scene(root, 850, 600));
            ventana.setResizable(true);
            ventana.showAndWait();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private void configurarCuenta() {

        tablaCuenta.setItems(cuenta);
        tablaCuenta.setPlaceholder(
                new Label("Haz clic en un pan para agregarlo")
        );

        colCantidad.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        String.valueOf(dato.getValue().getCantidad())
                )
        );

        colProducto.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        dato.getValue().getNombreProducto()
                )
        );

        colSubtotal.setCellValueFactory(
                dato -> new SimpleStringProperty(
                        moneda(subtotalDe(dato.getValue()))
                )
        );

        // El total se recalcula solo ante CUALQUIER cambio de la cuenta
        // (agregar, cambiar cantidad, quitar o vaciar).
        cuenta.addListener(
                (ListChangeListener<DetallePedido>) cambio -> actualizarTotal()
        );

        btnQuitarUno.setOnAction(event -> quitarUno());
        btnVaciarCuenta.setOnAction(event -> cuenta.clear());

        actualizarTotal();
    }

    private void cargarPanes() {

        contenedorPanes.getChildren().clear();

        try {
            List<Pan> panes = panDAO.obtenerPanes();

            for (Pan pan : panes) {
                contenedorPanes.getChildren().add(crearTarjeta(pan));
            }

        } catch (Exception ex) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error al cargar los panes",
                    ex.getMessage()
            );
        }
    }

    private VBox crearTarjeta(Pan pan) {

        ImageView imagen = new ImageView();
        imagen.setFitWidth(110);
        imagen.setFitHeight(90);
        imagen.setPreserveRatio(true);

        Image foto = obtenerImagen(pan.getFoto());

        if (foto != null) {
            imagen.setImage(foto);
        }

        StackPane marco = new StackPane(imagen);
        marco.setPrefSize(120, 100);
        marco.setStyle(
                "-fx-background-color: #f5f5f5; -fx-background-radius: 6;"
        );

        Label lblNombre = new Label(pan.getNombre());
        lblNombre.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label lblPrecio = new Label(
                moneda(BigDecimal.valueOf(pan.getPrecio()))
        );
        lblPrecio.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");

        VBox tarjeta = new VBox(5, marco, lblNombre, lblPrecio);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPrefSize(140, 180);
        tarjeta.setStyle(
                "-fx-background-color: white;"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 10;"
                + "-fx-effect: dropshadow(three-pass-box, "
                + "rgba(0,0,0,0.05), 5, 0, 0, 2);"
        );
        tarjeta.setCursor(Cursor.HAND);
        tarjeta.setOnMouseClicked(event -> agregarPan(pan));

        return tarjeta;
    }

    private Image obtenerImagen(String ruta) {

        if (ruta == null || ruta.trim().isEmpty()) {
            return null;
        }

        try {
            String valor = ruta.trim();

            URL recurso = getClass().getResource(
                    valor.startsWith("/") ? valor : "/" + valor
            );

            if (recurso != null) {
                return new Image(
                        recurso.toExternalForm(), 220, 180, true, true, true
                );
            }

            File archivo = new File(valor);

            if (archivo.isFile()) {
                return new Image(
                        archivo.toURI().toString(), 220, 180, true, true, true
                );
            }

        } catch (Exception ex) {
            System.err.println(
                    "No se pudo cargar la imagen " + ruta + ": "
                    + ex.getMessage()
            );
        }

        return null;
    }

    private void agregarPan(Pan pan) {

        if (pan.getPrecio() <= 0) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Precio inválido",
                    "El producto seleccionado tiene un precio inválido."
            );
            return;
        }

        for (int i = 0; i < cuenta.size(); i++) {

            DetallePedido linea = cuenta.get(i);

            if (linea.getIdPan() == pan.getIdPan()) {

                if (linea.getCantidad() >= CANTIDAD_MAXIMA) {
                    mostrarAlerta(
                            Alert.AlertType.WARNING,
                            "Cantidad máxima",
                            "Solo se permiten " + CANTIDAD_MAXIMA
                            + " piezas por producto."
                    );
                    return;
                }

                cuenta.set(i, conCantidad(linea, linea.getCantidad() + 1));
                tablaCuenta.getSelectionModel().select(i);
                return;
            }
        }

        cuenta.add(new DetallePedido(
                pan.getIdPan(),
                pan.getNombre(),
                1,
                pan.getPrecio()
        ));
        tablaCuenta.getSelectionModel().selectLast();
    }

    private void quitarUno() {

        int indice = tablaCuenta.getSelectionModel().getSelectedIndex();

        if (indice < 0) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Producto requerido",
                    "Selecciona un producto de la cuenta."
            );
            return;
        }

        DetallePedido linea = cuenta.get(indice);

        if (linea.getCantidad() > 1) {
            cuenta.set(indice, conCantidad(linea, linea.getCantidad() - 1));
            tablaCuenta.getSelectionModel().select(indice);
        } else {
            cuenta.remove(indice);
        }
    }

    // DetallePedido es inmutable: se crea una copia con la nueva cantidad.
    private DetallePedido conCantidad(DetallePedido linea, int cantidad) {
        return new DetallePedido(
                linea.getIdPan(),
                linea.getNombreProducto(),
                cantidad,
                linea.getPrecioUnitario()
        );
    }

    // Subtotal = precio unitario x cantidad, redondeado a centavos.
    private BigDecimal subtotalDe(DetallePedido linea) {
        return BigDecimal.valueOf(linea.getPrecioUnitario())
                .multiply(BigDecimal.valueOf(linea.getCantidad()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Total = suma de los subtotales mostrados en la tabla.
    private BigDecimal calcularTotal() {

        BigDecimal total = BigDecimal.ZERO;

        for (DetallePedido linea : cuenta) {
            total = total.add(subtotalDe(linea));
        }

        return total;
    }

    private void actualizarTotal() {
        lblTotalCuenta.setText(moneda(calcularTotal()));
    }

    private String moneda(BigDecimal importe) {
        return String.format(Locale.US, "$%.2f", importe);
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