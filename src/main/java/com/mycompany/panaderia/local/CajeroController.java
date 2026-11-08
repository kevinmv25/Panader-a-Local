
package com.mycompany.panaderia.local;

import panaderialocal.conexion.PanDAO;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CajeroController {

    @FXML private Button btnSalir;
    @FXML private Button btnEditarPedido;
    @FXML private Button btnIrRegistro;
    @FXML private FlowPane contenedorPanes;
    @FXML private TextField txtBuscarProducto;
    @FXML private Label lblPrecio1;

    @FXML private TableView<DetalleVenta> tablaPedido;
    @FXML private TableColumn<DetalleVenta, String> colProducto;
    @FXML private TableColumn<DetalleVenta, String> colCantidad;
    @FXML private TableColumn<DetalleVenta, String> colSubtotal;
    @FXML private Label lblTotal;

    private final PanDAO panDAO = new PanDAO();

    private List<Pan> panes = new ArrayList<>();

    private final ObservableList<DetalleVenta> productosPedido =
            FXCollections.observableArrayList();

    private final Map<Integer, DetalleVenta> seleccionados =
            new LinkedHashMap<>();

    @FXML
    public void initialize() {

        configurarTabla();
        cargarPanesDinamicos();

        txtBuscarProducto.textProperty().addListener(
            (obs, anterior, nuevo) -> mostrarPanes(nuevo)
        );

        // boton salir
        btnSalir.setOnAction(e -> {
            try {
                App.mostrar("Login");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // Editar pedidos
        btnEditarPedido.setOnAction(
            e -> abrirGestionPedidos()
        );

        // Registrar pedidos 
        btnIrRegistro.setOnAction(e -> {
            try {
                App.setRoot("Cajero/RegistroPedidos");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void configurarTabla() {

        colProducto.setCellValueFactory(dato ->
            new ReadOnlyStringWrapper(
                dato.getValue().getPan().getNombre()
            )
        );

        colCantidad.setCellValueFactory(dato ->
            new ReadOnlyStringWrapper(
                String.valueOf(
                    dato.getValue().getCantidad()
                )
            )
        );

        colSubtotal.setCellValueFactory(dato ->
            new ReadOnlyStringWrapper(
                String.format(
                    Locale.US,
                    "$%.2f",
                    dato.getValue().getSubtotal()
                )
            )
        );

        tablaPedido.setItems(productosPedido);

        tablaPedido.setPlaceholder(
            new Label("Selecciona panes con los botones +")
        );

        actualizarTotal();
    }

    private void cargarPanesDinamicos() {//obtener los panes desde la base de datos

        panes = panDAO.obtenerPanes();

        if (panes == null) {
            panes = new ArrayList<>();
        }

        mostrarPanes("");
    }

   
    private void mostrarPanes(String filtro) {

        contenedorPanes.getChildren().clear();

        String busqueda = filtro == null
                ? ""
                : filtro.trim().toLowerCase(Locale.ROOT);

        for (Pan pan : panes) {

            if (pan.getNombre() != null &&
                pan.getNombre().toLowerCase(Locale.ROOT)
                    .contains(busqueda)) {

                contenedorPanes.getChildren().add(
                    crearTarjetaPan(pan)
                );
            }
        }
    }

    private VBox crearTarjetaPan(Pan pan) {

        VBox tarjeta = new VBox(8);

        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPrefSize(150, 190);
        tarjeta.setPadding(new Insets(8.5));

        tarjeta.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #E9BD6B;" +
            "-fx-border-radius: 12;"
        );

        
        ImageView imagen = new ImageView();

        imagen.setFitWidth(110);
        imagen.setFitHeight(75);
        imagen.setPreserveRatio(true);

        Image foto = cargarImagen(pan.getFoto());

        if (foto != null) {
            imagen.setImage(foto);
        }

        Label lblNombre = new Label(pan.getNombre());

        lblNombre.setAlignment(Pos.CENTER);
        lblNombre.setWrapText(true);

        lblNombre.setStyle(
            "-fx-font-family: Georgia;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #422817;"
        );

        // Precio sacar precios 
        Label lblPrecio = new Label(
            String.format(
                Locale.US,
                "$%.2f",
                pan.getPrecio()
            )
        );

        lblPrecio.setStyle(
            "-fx-font-weight: bold;" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #D97706;"
        );

        HBox controles = new HBox(8);
        controles.setAlignment(Pos.CENTER);

        Button btnMenos = new Button("-");

        btnMenos.setPrefSize(30, 25);
        btnMenos.setStyle(
            "-fx-background-color: #E9BD6B;" +
            "-fx-text-fill: #422817;" +
            "-fx-font-weight: bold;"
        );

        Label lblCantidad = new Label(
            String.valueOf(obtenerCantidad(pan))
        );

        lblCantidad.setStyle(
            "-fx-font-weight: bold;" +
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #422817;"
        );

        Button btnMas = new Button("+");

        btnMas.setPrefSize(30, 25);
        btnMas.setStyle(
            "-fx-background-color: #422817;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;"
        );

        btnMas.setOnAction(e -> {

            cambiarCantidad(pan, 1);

            lblCantidad.setText(
                String.valueOf(obtenerCantidad(pan))
            );
        });

        btnMenos.setOnAction(e -> {

            cambiarCantidad(pan, -1);

            lblCantidad.setText(
                String.valueOf(obtenerCantidad(pan))
            );
        });

        controles.getChildren().addAll(
            btnMenos,
            lblCantidad,
            btnMas
        );

        tarjeta.getChildren().addAll(
            imagen,
            lblNombre,
            lblPrecio,
            controles
        );

        return tarjeta;
    }

    private int obtenerCantidad(Pan pan) {

        DetalleVenta detalle =
                seleccionados.get(pan.getIdPan());

        return detalle == null
                ? 0
                : detalle.getCantidad();
    }

    private void cambiarCantidad(Pan pan, int cambio) {

        int id = pan.getIdPan();

        DetalleVenta detalle = seleccionados.get(id);

        int cantidadActual = detalle == null
                ? 0
                : detalle.getCantidad();

        int cantidadNueva = Math.max(
            0,
            cantidadActual + cambio
        );

        if (cantidadNueva == 0) {

            if (detalle != null) {

                productosPedido.remove(detalle);
                seleccionados.remove(id);
            }

        } else if (detalle == null) {

            // AGREGAR NUEVO PRODUCTO A LA TABLA
            DetalleVenta nuevo = new DetalleVenta(
                pan,
                cantidadNueva
            );

            seleccionados.put(id, nuevo);
            productosPedido.add(nuevo);

        } else {

            // ACTUALIZAR CANTIDAD EXISTENTE
            detalle.setCantidad(cantidadNueva);
        }

        // ACTUALIZAR TABLA Y TOTAL
        tablaPedido.refresh();
        actualizarTotal();
    }

    // CALCULAR TOTAL DEL PEDIDO
    private void actualizarTotal() {

        double total = 0;

        for (DetalleVenta detalle : productosPedido) {
            total += detalle.getSubtotal();
        }

        lblTotal.setText(
            String.format(Locale.US, "%.2f", total)
        );
    }

    // CARGAR IMAGEN DESDE RECURSOS
    private Image cargarImagen(String ruta) {

        if (ruta == null || ruta.isBlank()) {
            return null;
        }

        try {

            String origen;
            File archivo = new File(ruta);

            if (archivo.isFile()) {

                origen = archivo.toURI().toString();

            } else if (ruta.startsWith("file:") ||
                       ruta.startsWith("http:") ||
                       ruta.startsWith("https:")) {

                origen = ruta;

            } else {

                String recurso = ruta.startsWith("/")
                        ? ruta
                        : "/" + ruta;

                URL url = getClass().getResource(recurso);

                if (url == null) {
                    return null;
                }

                origen = url.toExternalForm();
            }

            Image imagen = new Image(
                origen,
                110,
                75,
                true,
                true,
                false
            );

            return imagen.isError() ? null : imagen;

        } catch (Exception e) {
            return null;
        }
    }

    // ABRIR GESTION DE PEDIDOS
    private void abrirGestionPedidos() {

        try {

            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                    "/com/mycompany/panaderia/local/editarpedido.fxml"
                )
            );

            Parent root = loader.load();

            Stage ventana = new Stage();

            ventana.initModality(
                Modality.APPLICATION_MODAL
            );

            ventana.setTitle("Gestion de pedidos");

            ventana.setScene(
                new Scene(root, 850, 600)
            );

            ventana.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class DetalleVenta {

        private final Pan pan;
        private int cantidad;

        public DetalleVenta(Pan pan, int cantidad) {
            this.pan = pan;
            this.cantidad = cantidad;
        }

        public Pan getPan() {
            return pan;
        }

        public int getCantidad() {
            return cantidad;
        }

        public void setCantidad(int cantidad) {
            this.cantidad = cantidad;
        }

        public double getSubtotal() {
            return cantidad * pan.getPrecio();
        }
    }
}
