package com.mycompany.panaderia.local.Controllers.AdminController;

import com.panaderialocal.conexion.HistorialPedidosDAO;
import com.panaderialocal.conexion.HistorialPedidosDAO.ItemPedido;
import com.panaderialocal.conexion.HistorialPedidosDAO.PedidoHistorial;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class HistorialPedidosController implements Initializable {

    // ---------- Raiz ----------
    @FXML private StackPane raiz;

    // ---------- Encabezado y tarjetas ----------
    @FXML private Label lblTotalRegistrados;
    @FXML private Label lblTotalVisible;
    @FXML private Label lblCompletados;
    @FXML private Label lblPendientes;

    // ---------- Filtros ----------
    @FXML private Label lblFiltrosActivos;
    @FXML private HBox cajaBusqueda;
    @FXML private TextField txtBuscar;
    @FXML private Button btnBorrarBusqueda;
    @FXML private ComboBox<String> cmbFecha;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private ComboBox<String> cmbEntrega;

    // ---------- Tabla ----------
    @FXML private Label lblMostrando;
    @FXML private Label lblActualizado;
    @FXML private TableView<PedidoHistorial> tabla;
    @FXML private TableColumn<PedidoHistorial, PedidoHistorial> colFolio;
    @FXML private TableColumn<PedidoHistorial, PedidoHistorial> colFecha;
    @FXML private TableColumn<PedidoHistorial, PedidoHistorial> colEntrega;
    @FXML private TableColumn<PedidoHistorial, PedidoHistorial> colCliente;
    @FXML private TableColumn<PedidoHistorial, PedidoHistorial> colProductos;
    @FXML private TableColumn<PedidoHistorial, PedidoHistorial> colEstado;
    @FXML private TableColumn<PedidoHistorial, PedidoHistorial> colAdelanto;
    @FXML private TableColumn<PedidoHistorial, PedidoHistorial> colTotal;
    @FXML private TableColumn<PedidoHistorial, PedidoHistorial> colAcciones;

    // ---------- Panel de detalle ----------
    @FXML private StackPane overlayDetalle;
    @FXML private VBox cajon;
    @FXML private Label lblDetalleFolio;
    @FXML private Label lblDetalleEstado;
    @FXML private Label lblDetalleFecha;
    @FXML private Label lblDetalleIniciales;
    @FXML private Label lblDetalleCliente;
    @FXML private Label lblDetalleTelefono;
    @FXML private Label lblDetalleEntrega;
    @FXML private Label lblDetalleConceptos;
    @FXML private VBox vboxDetalleItems;
    @FXML private VBox seccionObservaciones;
    @FXML private Label lblDetalleObs;
    @FXML private Label lblEtiquetaSaldo;
    @FXML private Label lblDetalleAdelanto;
    @FXML private Label lblDetalleSaldo;
    @FXML private Label lblDetalleTotal;

    private final HistorialPedidosDAO dao = new HistorialPedidosDAO();

    private List<PedidoHistorial> todos = new ArrayList<>();

    private static final Locale ES = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter FMT_FECHA =
            DateTimeFormatter.ofPattern("dd MMM yyyy", ES);
    private static final double ALTO_FILA = 58;

    private static final PseudoClass ENFOCADO =
            PseudoClass.getPseudoClass("enfocado");

    // =========================================================
    // INICIALIZAR
    // =========================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        enlazarCss(url);

        cmbFecha.setItems(FXCollections.observableArrayList(
                "Todos los días", "Hoy", "Ayer", "Últimos 7 días", "Este mes"));
        cmbFecha.setValue("Todos los días");

        cmbEstado.setItems(FXCollections.observableArrayList(
                "Todos", "Pendiente", "Completado", "Cancelado"));
        cmbEstado.setValue("Todos");

        cmbEntrega.setItems(FXCollections.observableArrayList(
                "Todas", "Hoy", "Próximos 7 días", "Atrasadas"));
        cmbEntrega.setValue("Todas");

        cmbFecha.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        cmbEstado.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        cmbEntrega.valueProperty().addListener((o, a, b) -> aplicarFiltros());

        txtBuscar.textProperty().addListener((o, a, b) -> {
            boolean hayTexto = b != null && !b.isEmpty();
            btnBorrarBusqueda.setVisible(hayTexto);
            btnBorrarBusqueda.setManaged(hayTexto);
            aplicarFiltros();
        });
        txtBuscar.focusedProperty().addListener((o, a, enfocado) ->
                cajaBusqueda.pseudoClassStateChanged(ENFOCADO, enfocado));

        // Un clic dentro del panel de detalle no debe cerrarlo
        cajon.setOnMouseClicked(e -> e.consume());

        configurarTabla();
        cargar();
    }

    /** Carga historialPedidos.css desde la misma carpeta donde esta el FXML. */
    private void enlazarCss(URL ubicacionFxml) {

        final String css;

        try {
            css = ubicacionFxml.toURI().resolve("historialPedidos.css").toString();
        } catch (URISyntaxException | RuntimeException ex) {
            System.err.println("No se pudo ubicar historialPedidos.css: " + ex.getMessage());
            return;
        }

        System.out.println("historialPedidos.css -> " + css);

        raiz.sceneProperty().addListener((obs, anterior, escena) -> {
            if (escena != null
                    && !escena.getRoot().getStylesheets().contains(css)) {
                escena.getRoot().getStylesheets().add(css);
            }
        });
    }

    // =========================================================
    // TABLA
    // =========================================================
    private void configurarTabla() {

        VBox vacio = new VBox(6);
        vacio.setAlignment(Pos.CENTER);
        vacio.getStyleClass().add("estado-vacio");

        Label icono = new Label("⌕");
        icono.getStyleClass().add("vacio-icono");
        Label titulo = new Label("No encontramos pedidos");
        titulo.getStyleClass().add("vacio-titulo");
        Label texto = new Label("Prueba modificando o limpiando los filtros seleccionados.");
        texto.getStyleClass().add("vacio-texto");
        Button limpiar = new Button("Limpiar filtros");
        limpiar.getStyleClass().add("boton-vacio");
        limpiar.setOnAction(e -> limpiarFiltros());

        vacio.getChildren().addAll(icono, titulo, texto, limpiar);
        tabla.setPlaceholder(vacio);

        for (TableColumn<PedidoHistorial, PedidoHistorial> col : List.of(
                colFolio, colFecha, colEntrega, colCliente, colProductos,
                colEstado, colAdelanto, colTotal, colAcciones)) {
            col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        }

        colFolio.setCellFactory(c -> celda(p -> etiqueta(folio(p), "celda-folio")));

        colFecha.setCellFactory(c -> celda(p -> {
            Label dia = etiqueta(formatoFecha(p.getFechaPedido()), "celda-fecha");
            Label hora = etiqueta(p.getHoraPedido() == null ? "" : p.getHoraPedido(), "celda-pequena");
            return new VBox(2, dia, hora);
        }));

        colEntrega.setCellFactory(c -> celda(p -> {
            Label dia = etiqueta(formatoFecha(p.getFechaEntrega()), "celda-fecha");
            VBox caja = new VBox(2, dia);
            String relativo = textoEntrega(p, LocalDate.now());
            if (relativo != null) {
                caja.getChildren().add(etiqueta(relativo,
                        relativo.startsWith("Atrasado") ? "entrega-atrasada" : "entrega-proxima"));
            }
            return caja;
        }));

        colCliente.setCellFactory(c -> celda(p -> {
            Label avatar = etiqueta(iniciales(p.getCliente()), "avatar-mini");
            avatar.setAlignment(Pos.CENTER);
            avatar.setMinSize(28, 28);
            Label nombre = etiqueta(textoSeguro(p.getCliente()), "celda-cliente");
            HBox caja = new HBox(8, avatar, nombre);
            caja.setAlignment(Pos.CENTER_LEFT);
            return caja;
        }));

        colProductos.setCellFactory(c -> celda(p -> etiqueta(
                p.getArticulos() + (p.getArticulos() == 1 ? " artículo" : " artículos"),
                "celda-texto")));

        colEstado.setCellFactory(c -> celda(p -> {
            Label estado = new Label();
            aplicarEstado(estado, estadoVisible(p));
            return estado;
        }));

        colAdelanto.setCellFactory(c -> celda(p -> etiqueta(
                p.getAdelanto() > 0 ? moneda(p.getAdelanto()) : "—", "celda-texto")));

        colTotal.setCellFactory(c -> celda(p -> etiqueta(moneda(p.getTotal()), "celda-total")));

        colAcciones.setCellFactory(c -> celda(p -> {
            Button ver = new Button("Ver detalle");
            ver.getStyleClass().add("boton-detalle");
            ver.setOnAction(e -> abrirDetalle(p));
            return ver;
        }));

        tabla.setRowFactory(tv -> {
            TableRow<PedidoHistorial> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (!fila.isEmpty()) {
                    abrirDetalle(fila.getItem());
                }
            });
            return fila;
        });

        // Anchos proporcionales al ancho de la tabla
        ancho(colFolio, 0.10);
        ancho(colFecha, 0.13);
        ancho(colEntrega, 0.11);
        ancho(colCliente, 0.19);
        ancho(colProductos, 0.09);
        ancho(colEstado, 0.11);
        ancho(colAdelanto, 0.09);
        ancho(colTotal, 0.09);
        ancho(colAcciones, 0.09);
    }

    private void ancho(TableColumn<PedidoHistorial, PedidoHistorial> col, double proporcion) {
        col.prefWidthProperty().bind(tabla.widthProperty().subtract(22).multiply(proporcion));
    }

    private TableCell<PedidoHistorial, PedidoHistorial> celda(
            Function<PedidoHistorial, Node> constructor) {

        return new TableCell<PedidoHistorial, PedidoHistorial>() {
            @Override
            protected void updateItem(PedidoHistorial pedido, boolean empty) {
                super.updateItem(pedido, empty);

                if (empty || pedido == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setText(null);
                    setGraphic(constructor.apply(pedido));
                }
            }
        };
    }

    // =========================================================
    // CARGA Y FILTROS
    // =========================================================
    private void cargar() {

        try {
            todos = dao.obtenerPedidos();
        } catch (SQLException ex) {
            todos = new ArrayList<>();
            mostrarError(ex);
        }

        lblTotalRegistrados.setText(String.valueOf(todos.size()));
        lblActualizado.setText("Actualizado "
                + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        aplicarFiltros();
    }

    private void aplicarFiltros() {

        if (cmbFecha.getValue() == null
                || cmbEstado.getValue() == null
                || cmbEntrega.getValue() == null) {
            return;
        }

        String busqueda = txtBuscar.getText() == null
                ? "" : txtBuscar.getText().trim().toLowerCase(ES);

        LocalDate hoy = LocalDate.now();

        List<PedidoHistorial> visibles = new ArrayList<>();

        for (PedidoHistorial p : todos) {
            if (coincideBusqueda(p, busqueda)
                    && coincideFecha(p, hoy)
                    && coincideEstado(p)
                    && coincideEntrega(p, hoy)) {
                visibles.add(p);
            }
        }

        tabla.setItems(FXCollections.observableArrayList(visibles));
        ajustarAltura(visibles.size());

        double totalVisible = 0;
        int completados = 0;
        int pendientes = 0;

        for (PedidoHistorial p : visibles) {
            String estado = estadoVisible(p);
            if (!"Cancelado".equals(estado)) {
                totalVisible += p.getTotal();
            }
            if ("Completado".equals(estado)) {
                completados++;
            } else if (enCurso(p)) {
                pendientes++;
            }
        }

        lblTotalVisible.setText(moneda(totalVisible));
        lblCompletados.setText(String.valueOf(completados));
        lblPendientes.setText(String.valueOf(pendientes));
        lblMostrando.setText("Mostrando " + visibles.size()
                + " de " + todos.size() + " pedidos");

        int activos = 0;
        if (!"Todos los días".equals(cmbFecha.getValue())) activos++;
        if (!"Todos".equals(cmbEstado.getValue())) activos++;
        if (!"Todas".equals(cmbEntrega.getValue())) activos++;

        lblFiltrosActivos.setText(String.valueOf(activos));
        lblFiltrosActivos.setVisible(activos > 0);
        lblFiltrosActivos.setManaged(activos > 0);
    }

    private void ajustarAltura(int filas) {
        double alto = filas == 0 ? 240 : Math.min(filas, 8) * ALTO_FILA + 42;
        tabla.setMinHeight(alto);
        tabla.setPrefHeight(alto);
        tabla.setMaxHeight(alto);
    }

    private boolean coincideBusqueda(PedidoHistorial p, String busqueda) {
        if (busqueda.isEmpty()) {
            return true;
        }
        return folio(p).toLowerCase(ES).contains(busqueda)
                || textoSeguro(p.getCliente()).toLowerCase(ES).contains(busqueda);
    }

    private boolean coincideFecha(PedidoHistorial p, LocalDate hoy) {

        String filtro = cmbFecha.getValue();

        if ("Todos los días".equals(filtro)) {
            return true;
        }

        LocalDate fecha = parsearFecha(p.getFechaPedido());

        if (fecha == null) {
            return false;
        }

        switch (filtro) {
            case "Hoy":
                return fecha.equals(hoy);
            case "Ayer":
                return fecha.equals(hoy.minusDays(1));
            case "Últimos 7 días":
                return !fecha.isBefore(hoy.minusDays(6)) && !fecha.isAfter(hoy);
            case "Este mes":
                return fecha.getYear() == hoy.getYear()
                        && fecha.getMonth() == hoy.getMonth();
            default:
                return true;
        }
    }

    private boolean coincideEstado(PedidoHistorial p) {
        String filtro = cmbEstado.getValue();

        if ("Todos".equals(filtro)) {
            return true;
        }

        // "Pendiente" agrupa todo lo que aun no termina (En preparación, etc.)
        if ("Pendiente".equals(filtro)) {
            return enCurso(p);
        }

        return filtro.equals(estadoVisible(p));
    }

    private boolean coincideEntrega(PedidoHistorial p, LocalDate hoy) {

        String filtro = cmbEntrega.getValue();

        if ("Todas".equals(filtro)) {
            return true;
        }

        LocalDate entrega = parsearFecha(p.getFechaEntrega());

        if (entrega == null) {
            return false;
        }

        switch (filtro) {
            case "Hoy":
                return entrega.equals(hoy);
            case "Próximos 7 días":
                return !entrega.isBefore(hoy) && !entrega.isAfter(hoy.plusDays(7));
            case "Atrasadas":
                return entrega.isBefore(hoy) && enCurso(p);
            default:
                return true;
        }
    }

    @FXML
    private void limpiarFiltros() {
        txtBuscar.clear();
        cmbFecha.setValue("Todos los días");
        cmbEstado.setValue("Todos");
        cmbEntrega.setValue("Todas");
        aplicarFiltros();
    }

    @FXML
    private void borrarBusqueda() {
        txtBuscar.clear();
        txtBuscar.requestFocus();
    }

    // =========================================================
    // PANEL DE DETALLE
    // =========================================================
    private void abrirDetalle(PedidoHistorial p) {

        List<ItemPedido> items;

        try {
            items = dao.obtenerItems(p.getIdPedido());
        } catch (SQLException ex) {
            mostrarError(ex);
            return;
        }

        String estado = estadoVisible(p);

        lblDetalleFolio.setText(folio(p));
        aplicarEstado(lblDetalleEstado, estado);

        String fecha = formatoFecha(p.getFechaPedido());
        lblDetalleFecha.setText(p.getHoraPedido() == null
                ? fecha : fecha + " · " + p.getHoraPedido());

        lblDetalleIniciales.setText(iniciales(p.getCliente()));
        lblDetalleCliente.setText(textoSeguro(p.getCliente()));
        lblDetalleTelefono.setText(textoSeguro(p.getTelefono()));

        String entrega = formatoFecha(p.getFechaEntrega());
        String relativo = textoEntrega(p, LocalDate.now());
        lblDetalleEntrega.setText(relativo == null ? entrega : entrega + "  ·  " + relativo);

        lblDetalleConceptos.setText(items.size()
                + (items.size() == 1 ? " concepto" : " conceptos"));

        vboxDetalleItems.getChildren().clear();

        for (ItemPedido item : items) {

            Label cantidad = etiqueta(String.valueOf(item.getCantidad()), "item-cantidad");
            cantidad.setAlignment(Pos.CENTER);
            cantidad.setMinSize(26, 26);

            Label nombre = etiqueta(item.getNombre(), "item-nombre");

            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);

            Label subtotal = etiqueta(moneda(item.getSubtotal()), "item-total");

            HBox fila = new HBox(10, cantidad, nombre, espacio, subtotal);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("item-fila");

            vboxDetalleItems.getChildren().add(fila);
        }

        boolean hayObs = p.getObservaciones() != null
                && !p.getObservaciones().trim().isEmpty();
        seccionObservaciones.setVisible(hayObs);
        seccionObservaciones.setManaged(hayObs);
        lblDetalleObs.setText(hayObs ? p.getObservaciones().trim() : "");

        double saldo = Math.max(0, p.getTotal() - p.getAdelanto());

        if ("Completado".equals(estado)) {
            lblEtiquetaSaldo.setText("Saldo cobrado");
        } else if ("Cancelado".equals(estado)) {
            lblEtiquetaSaldo.setText("Saldo (pedido cancelado)");
        } else {
            lblEtiquetaSaldo.setText("Saldo pendiente");
        }

        lblDetalleAdelanto.setText(moneda(p.getAdelanto()));
        lblDetalleSaldo.setText(moneda(saldo));
        lblDetalleTotal.setText(moneda(p.getTotal()));

        overlayDetalle.setVisible(true);
        overlayDetalle.setManaged(true);
    }

    @FXML
    private void cerrarDetalle() {
        overlayDetalle.setVisible(false);
        overlayDetalle.setManaged(false);
    }

    // =========================================================
    // NAVEGACION
    // =========================================================
    @FXML
    private void volverInicio(ActionEvent event) throws IOException {

        Parent root = FXMLLoader.load(getClass().getResource(
                "/com/mycompany/panaderia/local/Administrador/inicioAdmin.fxml"));

        ((Node) event.getSource()).getScene().setRoot(root);
    }

    // =========================================================
    // UTILIDADES
    // =========================================================
    private Label etiqueta(String texto, String clase) {
        Label l = new Label(texto);
        l.getStyleClass().add(clase);
        return l;
    }

    private void aplicarEstado(Label etiqueta, String estado) {

        etiqueta.getStyleClass().removeAll(
                "estado-completado", "estado-pendiente", "estado-cancelado");

        if (!etiqueta.getStyleClass().contains("estado")) {
            etiqueta.getStyleClass().add("estado");
        }

        String clase = "Completado".equals(estado) ? "estado-completado"
                : "Cancelado".equals(estado) ? "estado-cancelado"
                : "estado-pendiente";

        etiqueta.getStyleClass().add(clase);
        etiqueta.setText("●  " + estado);
    }

    private String estadoVisible(PedidoHistorial p) {
        String e = p.getEstado();
        if (e == null || e.trim().isEmpty()) {
            return "Pendiente";
        }
        return "Entregado".equalsIgnoreCase(e) ? "Completado" : e;
    }

    /** Un pedido esta "en curso" mientras no este Completado/Entregado/Cancelado. */
    private boolean enCurso(PedidoHistorial p) {
        String estado = estadoVisible(p);
        return !"Completado".equals(estado) && !"Cancelado".equals(estado);
    }

    private String folio(PedidoHistorial p) {
        return String.format("#PED-%04d", p.getIdPedido());
    }

    private LocalDate parsearFecha(String texto) {
        if (texto == null || texto.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(texto.substring(0, 10));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String formatoFecha(String texto) {
        LocalDate fecha = parsearFecha(texto);
        return fecha == null ? "—" : fecha.format(FMT_FECHA);
    }

    /** Solo para pedidos pendientes: "Hoy", "Mañana", "Atrasado 2 días"... */
    private String textoEntrega(PedidoHistorial p, LocalDate hoy) {

        if (!enCurso(p)) {
            return null;
        }

        LocalDate entrega = parsearFecha(p.getFechaEntrega());

        if (entrega == null) {
            return null;
        }

        long dias = ChronoUnit.DAYS.between(hoy, entrega);

        if (dias < 0) {
            return "Atrasado " + (-dias) + (dias == -1 ? " día" : " días");
        }
        if (dias == 0) {
            return "Hoy";
        }
        if (dias == 1) {
            return "Mañana";
        }
        if (dias <= 7) {
            return "En " + dias + " días";
        }
        return null;
    }

    private String iniciales(String nombre) {

        if (nombre == null || nombre.trim().isEmpty()) {
            return "?";
        }

        String[] partes = nombre.trim().split("\\s+");
        String resultado = partes[0].substring(0, 1);

        if (partes.length > 1) {
            resultado += partes[1].substring(0, 1);
        }

        return resultado.toUpperCase(ES);
    }

    private String textoSeguro(String texto) {
        return texto == null || texto.trim().isEmpty() ? "No disponible" : texto;
    }

    private String moneda(double valor) {
        return String.format(Locale.US, "$%,.2f", valor);
    }

    private void mostrarError(SQLException ex) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Historial de pedidos");
        alerta.setHeaderText(null);
        alerta.setContentText("No se pudieron cargar los pedidos:\n" + ex.getMessage());
        alerta.showAndWait();
    }
}
