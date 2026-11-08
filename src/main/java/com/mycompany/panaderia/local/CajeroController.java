
package com.mycompany.panaderia.local;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import panaderialocal.conexion.PanDAO;

public class CajeroController {
    @FXML private javafx.scene.control.ComboBox<String> cmbPago;
    @FXML private TextField txtRecibido;
    @FXML private TextField txtReferencia;
    @FXML private Button btnFinalizar;

    @FXML private void finalizarVenta() {
        try {
            var lineas = productosPedido.stream().map(d -> new com.panaderialocal.conexion.RegistroPedidosDAO.DetallePedido(
                d.pan.getIdPan(),d.pan.getNombre(),d.cantidad,d.pan.getPrecio())).toList();
            BigDecimal total = com.panaderialocal.conexion.VentasDAO.total(lineas);
            String metodo = cmbPago.getValue();
            BigDecimal recibido = "Efectivo".equals(metodo) ? new BigDecimal(txtRecibido.getText().trim().replace(",",".")) : null;
            if(recibido!=null && (recibido.scale()>2 || recibido.compareTo(total)<0)) throw new IllegalArgumentException("El efectivo recibido no cubre el total o tiene mas de dos decimales.");
            if(!"Efectivo".equals(metodo) && txtReferencia.getText().isBlank()) throw new IllegalArgumentException("Indica la referencia del pago.");
            Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
            confirmar.setHeaderText("Cobrar " + moneda(total) + " con " + metodo);
            confirmar.setContentText(recibido!=null ? "Cambio: " + moneda(recibido.subtract(total)) : "Confirma que el pago fue aprobado en la terminal o recibido en la cuenta bancaria.");
            if(confirmar.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)!=javafx.scene.control.ButtonType.OK) return;
            btnFinalizar.setDisable(true);
            int id = new com.panaderialocal.conexion.VentasDAO().registrarVenta(lineas,metodo,recibido,txtReferencia.getText());
            vaciarCuenta(); txtRecibido.clear(); txtReferencia.clear();
            mostrarAlerta(Alert.AlertType.INFORMATION,"Venta registrada","Venta #"+id+" — Total: "+moneda(total)+(recibido!=null?" — Cambio: "+moneda(recibido.subtract(total)):""));
        } catch(Exception e) { mostrarAlerta(Alert.AlertType.ERROR,"No se registro la venta", e instanceof NumberFormatException ? "Introduce el efectivo recibido." : e.getMessage()); }
        finally { btnFinalizar.setDisable(false); }
    }


    // BOTONES PRINCIPALES
    @FXML private Button btnSalir;
    @FXML private Button btnEditarPedido;
    @FXML private Button btnIrRegistro;
    @FXML private Button btnDevolucion;

    // PRODUCTOS
    @FXML private FlowPane contenedorPanes;
    @FXML private TextField txtBuscarProducto;
    @FXML private Label lblPrecio1;

    // TABLAS: COMPATIBILIDAD CON AMBOS FXML
    @FXML private TableView<DetalleVenta> tablaPedido;
    @FXML private TableView<DetalleVenta> tablaCuenta;

    @FXML private TableColumn<DetalleVenta, String> colProducto;
    @FXML private TableColumn<DetalleVenta, String> colCantidad;
    @FXML private TableColumn<DetalleVenta, String> colSubtotal;

    // TOTALES
    @FXML private Label lblTotal;
    @FXML private Label lblTotalCuenta;

    // CONTROLES DE CUENTA
    @FXML private Button btnQuitarUno;
    @FXML private Button btnVaciarCuenta;

    private static final int CANTIDAD_MAXIMA = 9999;

    private final PanDAO panDAO = new PanDAO();

    private List<Pan> panes = new ArrayList<>();

    private final ObservableList<DetalleVenta> productosPedido =
            FXCollections.observableArrayList();

    private final Map<Integer, DetalleVenta> seleccionados =
            new LinkedHashMap<>();

    // INICIALIZAR PANTALLA
    @FXML
    public void initialize() {

        if(cmbPago!=null) {
            cmbPago.getItems().setAll("Efectivo","Tarjeta","Transferencia"); cmbPago.setValue("Efectivo");
            txtReferencia.setDisable(true);
            cmbPago.valueProperty().addListener((o,a,b)-> { txtRecibido.setDisable(!"Efectivo".equals(b));txtReferencia.setDisable("Efectivo".equals(b)); });
        }
        configurarTabla();
        cargarPanesDinamicos();

        // BUSCAR PRODUCTOS
        if (txtBuscarProducto != null) {
            txtBuscarProducto.textProperty().addListener(
                (obs, anterior, nuevo) -> mostrarPanes(nuevo)
            );
        }

        // SALIR
        if (btnSalir != null) {
            btnSalir.setOnAction(e -> {
                try {
                    App.mostrar("Login");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        }

        // EDITAR PEDIDOS
        if (btnEditarPedido != null) {
            btnEditarPedido.setOnAction(
                e -> abrirGestionPedidos()
            );
        }

        // DEVOLUCIONES
        if (btnDevolucion != null) {
            btnDevolucion.setOnAction(
                e -> abrirDevolucion()
            );
        }

        // REGISTRAR PEDIDOS
        if (btnIrRegistro != null) {
            btnIrRegistro.setOnAction(e -> {
                try {
                    App.setRoot("Cajero/RegistroPedidos");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        }

        // QUITAR UNA PIEZA
        if (btnQuitarUno != null) {
            btnQuitarUno.setOnAction(
                e -> quitarUno()
            );
        }

        // VACIAR CUENTA
        if (btnVaciarCuenta != null) {
            btnVaciarCuenta.setOnAction(
                e -> vaciarCuenta()
            );
        }

        actualizarTotal();
    }

    // OBTENER TABLA DISPONIBLE
    private TableView<DetalleVenta> obtenerTabla() {

        if (tablaPedido != null) {
            return tablaPedido;
        }

        return tablaCuenta;
    }

    // CONFIGURAR TABLA
    private void configurarTabla() {

        if (colProducto != null) {
            colProducto.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                    dato.getValue().getPan().getNombre() + " " + moneda(BigDecimal.valueOf(dato.getValue().getPan().getPrecio()))
                )
            );
        }

        if (colCantidad != null) {
            colCantidad.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                    String.valueOf(
                        dato.getValue().getCantidad()
                    )
                )
            );
        }

        if (colSubtotal != null) {
            colSubtotal.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                    moneda(dato.getValue().getSubtotal())
                )
            );
        }

        if (tablaPedido != null) {
            tablaPedido.setItems(productosPedido);
            tablaPedido.setPlaceholder(
                new Label("Selecciona panes con los botones +")
            );
        }

        if (tablaCuenta != null) {
            tablaCuenta.setItems(productosPedido);
            tablaCuenta.setPlaceholder(
                new Label("Selecciona un pan para agregarlo")
            );
        }

        actualizarTotal();
    }

    // CARGAR PANES DESDE LA BASE DE DATOS
    private void cargarPanesDinamicos() {

        try {

            panes = panDAO.obtenerPanes();

            if (panes == null) {
                panes = new ArrayList<>();
            }

            mostrarPanes("");

        } catch (Exception ex) {

            panes = new ArrayList<>();

            mostrarAlerta(
                Alert.AlertType.ERROR,
                "Error al cargar los panes",
                ex.getMessage()
            );
        }
    }

    // MOSTRAR PRODUCTOS Y FILTRAR BUSQUEDA
    private void mostrarPanes(String filtro) {

        if (contenedorPanes == null) {
            return;
        }

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

    // CREAR TARJETA DE CADA PAN
    private VBox crearTarjetaPan(Pan pan) {

        VBox tarjeta = new VBox(8);

        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPrefSize(165, 285);
        tarjeta.setPadding(new Insets(8.5));

        tarjeta.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #E9BD6B;" +
            "-fx-border-radius: 12;"
        );

        tarjeta.setCursor(Cursor.HAND);

        // IMAGEN
        ImageView imagen = new ImageView();

        imagen.setFitWidth(110);
        imagen.setFitHeight(75);
        imagen.setPreserveRatio(true);

        Image foto = cargarImagen(pan.getFoto());

        if (foto != null) {
            imagen.setImage(foto);
        }

        // NOMBRE
        Label lblNombre = new Label(pan.getNombre());

        lblNombre.setAlignment(Pos.CENTER);
        lblNombre.setWrapText(true);

        lblNombre.setStyle(
            "-fx-font-family: Georgia;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #422817;"
        );

        // PRECIO
        Label lblPrecio = new Label(
            moneda(BigDecimal.valueOf(pan.getPrecio()))
        );

        lblPrecio.setStyle(
            "-fx-font-weight: bold;" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #D97706;"
        );

        // CONTROLES + Y -
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

        // AGREGAR PRODUCTO
        btnMas.setOnAction(e -> {

            cambiarCantidad(pan, 1);

            lblCantidad.setText(
                String.valueOf(obtenerCantidad(pan))
            );
        });

        // DISMINUIR PRODUCTO
        btnMenos.setOnAction(e -> {

            cambiarCantidad(pan, -1);

            lblCantidad.setText(
                String.valueOf(obtenerCantidad(pan))
            );
        });

        // PERMITIR AGREGAR AL PULSAR LA TARJETA
        tarjeta.setOnMouseClicked(event -> {

            Node origen = event.getTarget() instanceof Node
                    ? (Node) event.getTarget()
                    : null;

            // EVITAR SUMAR DOS VECES AL PULSAR UN BOTON
            while (origen != null && origen != tarjeta) {

                if (origen instanceof Button || origen instanceof TextField) {
                    return;
                }

                origen = origen.getParent();
            }

            cambiarCantidad(pan, 1);

            lblCantidad.setText(
                String.valueOf(obtenerCantidad(pan))
            );
        });

        controles.getChildren().addAll(
            btnMenos,
            lblCantidad,
            btnMas
        );

        TextField piezas = new TextField();
        piezas.setPromptText("Cantidad de piezas");
        piezas.setMaxWidth(145);
        piezas.setAlignment(Pos.CENTER);
        piezas.setPrefHeight(30);
        piezas.setStyle("-fx-background-color: #FFF8EC; -fx-border-color: #E9BD6B; -fx-background-radius: 8; -fx-border-radius: 8; -fx-text-fill: #422817; -fx-font-size: 12px;");
        Button agregar = new Button("Agregar cantidad");
        agregar.setMaxWidth(145);
        agregar.setPrefHeight(32);
        agregar.setCursor(Cursor.HAND);
        String estiloAgregar = "-fx-text-fill: #FFF8EC; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #E9BD6B;";
        agregar.setStyle(estiloAgregar + "-fx-background-color: #422817;");
        agregar.setOnMouseEntered(e -> agregar.setStyle(estiloAgregar + "-fx-background-color: #6B4A2E;"));
        agregar.setOnMouseExited(e -> agregar.setStyle(estiloAgregar + "-fx-background-color: #422817;"));
        agregar.setOnAction(e -> {
            try {
                int n=Integer.parseInt(piezas.getText().trim());
                if(n<1 || n+obtenerCantidad(pan)>CANTIDAD_MAXIMA) throw new NumberFormatException();
                cambiarCantidad(pan,n); actualizarTarjetas();
            } catch(NumberFormatException ex) {mostrarAlerta(Alert.AlertType.WARNING,"Cantidad invalida","Introduce de 1 a 9999 piezas, sin superar el limite por pan.");}
        });
        piezas.setOnAction(e -> agregar.fire());
        tarjeta.getChildren().addAll(
            imagen,
            lblNombre,
            lblPrecio,
            controles, piezas, agregar
        );
        if(foto == null) {
            Label sinFoto = new Label("Imagen pendiente");
            sinFoto.setMinHeight(75);
            sinFoto.setStyle("-fx-text-fill: #9A7955; -fx-font-size: 11px;");
            tarjeta.getChildren().set(0, sinFoto);
        }

        return tarjeta;
    }

    // OBTENER CANTIDAD ACTUAL
    private int obtenerCantidad(Pan pan) {

        DetalleVenta detalle =
                seleccionados.get(pan.getIdPan());

        return detalle == null
                ? 0
                : detalle.getCantidad();
    }

    // AUMENTAR O DISMINUIR CANTIDAD
    private void cambiarCantidad(Pan pan, int cambio) {

        if (!Double.isFinite(pan.getPrecio()) ||
            pan.getPrecio() <= 0) {

            mostrarAlerta(
                Alert.AlertType.WARNING,
                "Precio inválido",
                "El producto tiene un precio inválido."
            );

            return;
        }

        int id = pan.getIdPan();

        DetalleVenta detalle = seleccionados.get(id);

        int cantidadActual = detalle == null
                ? 0
                : detalle.getCantidad();

        if (cambio > 0 &&
            cantidadActual >= CANTIDAD_MAXIMA) {

            mostrarAlerta(
                Alert.AlertType.WARNING,
                "Cantidad máxima",
                "Solo se permiten " + CANTIDAD_MAXIMA +
                " piezas por producto."
            );

            return;
        }

        int cantidadNueva = Math.max(
            0,
            Math.min(
                CANTIDAD_MAXIMA,
                cantidadActual + cambio
            )
        );

        if (cantidadNueva == 0) {

            if (detalle != null) {
                productosPedido.remove(detalle);
                seleccionados.remove(id);
            }

        } else if (detalle == null) {

            // NUEVO PRODUCTO
            DetalleVenta nuevo = new DetalleVenta(
                pan,
                cantidadNueva
            );

            seleccionados.put(id, nuevo);
            productosPedido.add(nuevo);

        } else {

            // ACTUALIZAR PRODUCTO EXISTENTE
            detalle.setCantidad(cantidadNueva);
        }

        // ACTUALIZAR TABLAS
        if (tablaPedido != null) {
            tablaPedido.refresh();
        }

        if (tablaCuenta != null) {
            tablaCuenta.refresh();
        }

        actualizarTotal();
    }

    // QUITAR UNA PIEZA DE LA CUENTA
    private void quitarUno() {

        TableView<DetalleVenta> tabla = obtenerTabla();

        if (tabla == null) {
            return;
        }

        DetalleVenta seleccionado =
                tabla.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {

            mostrarAlerta(
                Alert.AlertType.WARNING,
                "Producto requerido",
                "Selecciona un producto de la cuenta."
            );

            return;
        }

        cambiarCantidad(
            seleccionado.getPan(),
            -1
        );

        // ACTUALIZAR CANTIDADES EN TARJETAS
        actualizarTarjetas();
    }

    // VACIAR CUENTA COMPLETA
    private void vaciarCuenta() {

        productosPedido.clear();
        seleccionados.clear();

        actualizarTotal();
        actualizarTarjetas();
    }

    // ACTUALIZAR LAS TARJETAS DE PANES
    private void actualizarTarjetas() {

        String filtro = txtBuscarProducto == null
                ? ""
                : txtBuscarProducto.getText();

        mostrarPanes(filtro);
    }

    // CALCULAR TOTAL
    private void actualizarTotal() {

        BigDecimal total = BigDecimal.ZERO;

        for (DetalleVenta detalle : productosPedido) {

            total = total.add(
                detalle.getSubtotal()
            );
        }

        String textoTotal = moneda(total);

        if (lblTotal != null) {
            lblTotal.setText(textoTotal);
        }

        if (lblTotalCuenta != null) {
            lblTotalCuenta.setText(textoTotal);
        }
    }

    // FORMATO DE MONEDA
    private String moneda(BigDecimal importe) {

        return String.format(
            Locale.US,
            "$%.2f",
            importe
        );
    }

    // CARGAR IMAGEN DEL PAN
    private Image cargarImagen(String ruta) {

        if (ruta == null || ruta.isBlank()) {
            return null;
        }

        try {

            String origen;
            File archivo = new File(ruta);

            if (archivo.isFile()) {

                origen = archivo.toURI().toString();

            } else if (
                ruta.startsWith("file:") ||
                ruta.startsWith("http:") ||
                ruta.startsWith("https:")
            ) {

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

            System.err.println(
                "No se pudo cargar la imagen: " + ruta
            );

            return null;
        }
    }

    // ABRIR GESTION DE PEDIDOS
    @FXML private void abrirGestionPedidos() {

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

            ventana.setTitle("Gestión de pedidos");

            ventana.setScene(
                new Scene(root, 850, 600)
            );

            ventana.showAndWait();

        } catch (IOException e) {

            mostrarAlerta(
                Alert.AlertType.ERROR,
                "Error",
                "No se pudo abrir la gestión de pedidos."
            );

            e.printStackTrace();
        }
    }

    // ABRIR DEVOLUCIONES
    private void abrirDevolucion() {

        try {

            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("devolucion.fxml")
            );

            Parent root = loader.load();

            Stage ventana = new Stage();

            ventana.initModality(
                Modality.APPLICATION_MODAL
            );

            ventana.setTitle("Devolución / Reembolso");

            ventana.setScene(
                new Scene(root, 850, 700)
            );

            ventana.setResizable(true);
            ventana.showAndWait();

        } catch (IOException e) {

            mostrarAlerta(
                Alert.AlertType.ERROR,
                "Error",
                "No se pudo abrir la pantalla de devoluciones."
            );

            e.printStackTrace();
        }
    }

    // MOSTRAR ALERTAS
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

    // MODELO INTERNO DE PRODUCTO EN LA CUENTA
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

        public BigDecimal getSubtotal() {

            return BigDecimal.valueOf(pan.getPrecio())
                    .multiply(BigDecimal.valueOf(cantidad))
                    .setScale(
                        2,
                        RoundingMode.HALF_UP
                    );
        }
    }
}
