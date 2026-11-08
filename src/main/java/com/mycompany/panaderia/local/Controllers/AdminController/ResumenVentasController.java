package com.mycompany.panaderia.local.Controllers.AdminController;

import com.panaderialocal.conexion.ResumenVentasDAO;
import com.panaderialocal.conexion.ResumenVentasDAO.ProductoTop;
import com.panaderialocal.conexion.ResumenVentasDAO.Totales;
import com.panaderialocal.conexion.ResumenVentasDAO.VentaReciente;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class ResumenVentasController implements Initializable {

    // ---------- Encabezado ----------
    @FXML private Label lblFecha;
    @FXML private ComboBox<String> cmbPeriodo;
    @FXML private ComboBox<String> cmbMetodo;

    // ---------- Tarjetas ----------
    @FXML private Label lblTotalVentas;
    @FXML private Label lblVariacion;
    @FXML private Label lblMetodoPrincipal;
    @FXML private Label lblMetodoPct;
    @FXML private Label lblNumVentas;
    @FXML private Label lblTicketPromedio;

    // ---------- Grafica ----------
    @FXML private Label lblTituloGrafica;
    @FXML private Label lblTotalAcumulado;
    @FXML private AreaChart<String, Number> chartVentas;
    @FXML private CategoryAxis ejeX;
    @FXML private NumberAxis ejeY;

    // ---------- Metodos de pago ----------
    @FXML private PieChart pieMetodos;
    @FXML private Label lblDonutTotal;
    @FXML private VBox vboxMetodos;

    // ---------- Tabla ----------
    
    @FXML private TableView<VentaReciente> tablaVentas;
    @FXML private TableColumn<VentaReciente, String> colFolio;
    @FXML private TableColumn<VentaReciente, String> colHora;
    @FXML private TableColumn<VentaReciente, String> colDetalle;
    @FXML private TableColumn<VentaReciente, String> colMetodo;
    @FXML private TableColumn<VentaReciente, String> colTotal;

    // ---------- Productos ----------
    @FXML private VBox vboxProductos;

    private final ResumenVentasDAO dao = new ResumenVentasDAO();

    private LocalDate desde = LocalDate.now();
    private LocalDate hasta = LocalDate.now();
    private boolean periodoDeUnDia = true;

    private static final DateTimeFormatter FMT_DIA_MES =
            DateTimeFormatter.ofPattern("dd/MM");

    // =========================================================
    // INICIALIZAR
    // =========================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        URL css = getClass().getResource(
        "/com/mycompany/panaderia/local/resumenVentas.css");

        System.out.println("resumenVentas.css -> " + css);

        if (css != null) {
            lblFecha.sceneProperty().addListener((obs, anterior, escena) -> {
                if (escena != null
                && !escena.getRoot().getStylesheets().contains(css.toExternalForm())) {
                escena.getRoot().getStylesheets().add(css.toExternalForm());
                }
            });
        }
        configurarFecha();

        cmbPeriodo.setItems(FXCollections.observableArrayList(
                "Hoy", "Ayer", "Últimos 7 días", "Este mes"));
        cmbPeriodo.setValue("Hoy");

        cmbMetodo.setItems(FXCollections.observableArrayList(
                "Todos", "Efectivo", "Tarjeta", "Transferencia", "Pedidos"));
        cmbMetodo.setValue("Todos");

        cmbPeriodo.valueProperty().addListener(
                (obs, anterior, nuevo) -> actualizar());
        cmbMetodo.valueProperty().addListener(
                (obs, anterior, nuevo) -> refrescarTabla());

        configurarGrafica();
        configurarTabla();

        actualizar();
    }

    private void configurarFecha() {

        DateTimeFormatter formato = DateTimeFormatter.ofPattern(
                "EEEE, d 'de' MMMM 'de' yyyy",
                Locale.forLanguageTag("es-MX"));

        String fecha = LocalDate.now().format(formato);

        if (!fecha.isEmpty()) {
            fecha = fecha.substring(0, 1).toUpperCase() + fecha.substring(1);
        }

        lblFecha.setText(fecha);
    }

    private void configurarGrafica() {

        chartVentas.setAnimated(false);
        chartVentas.setLegendVisible(false);
        ejeX.setAnimated(false);
        ejeY.setAnimated(false);

        ejeY.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number valor) {
                return String.format(Locale.US, "$%,.0f", valor.doubleValue());
            }

            @Override
            public Number fromString(String texto) {
                return 0;
            }
        });
    }

    private void configurarTabla() {

        tablaVentas.setPlaceholder(new Label(
                "No hay ventas para este periodo o método de pago."));

        colFolio.setCellValueFactory(c -> new SimpleStringProperty(
        c.getValue().getEtiquetaFolio()));

        colHora.setCellValueFactory(c -> new SimpleStringProperty(
                periodoDeUnDia
                        ? c.getValue().getHora()
                        : c.getValue().getFecha() + "  " + c.getValue().getHora()));

        colDetalle.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getProductos()
                + (c.getValue().getProductos() == 1 ? " producto" : " productos")));

        colMetodo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getMetodo()));

        colTotal.setCellValueFactory(c -> new SimpleStringProperty(
                moneda(c.getValue().getTotal())));

        colMetodo.setCellFactory(col -> new TableCell<VentaReciente, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label insignia = new Label(item);
                insignia.getStyleClass().addAll(
                        "insignia",
                        "insignia-" + item.toLowerCase(Locale.ROOT));

                setText(null);
                setGraphic(insignia);
            }
        });

        colTotal.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Anchos proporcionales al ancho de la tabla
        colFolio.prefWidthProperty().bind(tablaVentas.widthProperty().multiply(0.14));
        colHora.prefWidthProperty().bind(tablaVentas.widthProperty().multiply(0.22));
        colDetalle.prefWidthProperty().bind(tablaVentas.widthProperty().multiply(0.22));
        colMetodo.prefWidthProperty().bind(tablaVentas.widthProperty().multiply(0.22));
        colTotal.prefWidthProperty().bind(tablaVentas.widthProperty().multiply(0.17));
    }

    // =========================================================
    // PERIODO
    // =========================================================
    private void calcularPeriodo() {

        LocalDate hoy = LocalDate.now();

        desde = hoy;
        hasta = hoy;

        String periodo = cmbPeriodo.getValue();

        if ("Ayer".equals(periodo)) {
            desde = hoy.minusDays(1);
            hasta = desde;
        } else if ("Últimos 7 días".equals(periodo)) {
            desde = hoy.minusDays(6);
        } else if ("Este mes".equals(periodo)) {
            desde = hoy.withDayOfMonth(1);
        }

        periodoDeUnDia = desde.equals(hasta);
    }

    // =========================================================
    // ACTUALIZAR TODA LA PANTALLA
    // =========================================================
    private void actualizar() {

        calcularPeriodo();

        try {
            Totales actual = dao.obtenerTotales(desde, hasta);

            long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
            LocalDate anteriorHasta = desde.minusDays(1);
            LocalDate anteriorDesde = anteriorHasta.minusDays(dias - 1);
            Totales anterior = dao.obtenerTotales(anteriorDesde, anteriorHasta);

            Map<String, Double> metodos = dao.totalesPorMetodo(desde, hasta);

            actualizarTarjetas(actual, anterior, metodos);
            actualizarGrafica(actual);
            actualizarMetodos(metodos);
            actualizarTabla();
            actualizarProductos();

        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void refrescarTabla() {
        try {
            actualizarTabla();
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    // =========================================================
    // TARJETAS
    // =========================================================
    private void actualizarTarjetas(Totales actual, Totales anterior,
            Map<String, Double> metodos) {

        lblTotalVentas.setText(moneda(actual.getTotal()));

        if (anterior.getTotal() > 0) {
            double variacion = (actual.getTotal() - anterior.getTotal())
                    / anterior.getTotal() * 100;

            lblVariacion.setText(
                    (variacion >= 0 ? "▲ " : "▼ ")
                    + String.format(Locale.US, "%.1f%%", Math.abs(variacion))
                    + " vs. periodo anterior");
        } else {
            lblVariacion.setText("Sin datos del periodo anterior");
        }

        String principal = "—";
        double mayor = 0;
        double suma = 0;

        for (Map.Entry<String, Double> e : metodos.entrySet()) {
            suma += e.getValue();
            if (e.getValue() > mayor) {
                mayor = e.getValue();
                principal = e.getKey();
            }
        }

        lblMetodoPrincipal.setText(principal);
        lblMetodoPct.setText(suma > 0
                ? String.format(Locale.US, "%.0f%% del total cobrado", mayor / suma * 100)
                : "Sin ventas en el periodo");

        lblNumVentas.setText(String.valueOf(actual.getNumeroVentas()));
        lblTicketPromedio.setText(moneda(actual.getTicketPromedio()) + " ticket promedio");
    }

    // =========================================================
    // GRAFICA
    // =========================================================
    private void actualizarGrafica(Totales actual) throws SQLException {

        XYChart.Series<String, Number> serie = new XYChart.Series<>();

        if (periodoDeUnDia) {

            lblTituloGrafica.setText("Ventas por hora");

            double[] horas = dao.ventasPorHora(desde, hasta);

            int primera = 7;
            int ultima = 21;

            for (int h = 0; h < 24; h++) {
                if (horas[h] > 0) {
                    primera = Math.min(primera, h);
                    ultima = Math.max(ultima, h);
                }
            }

            for (int h = primera; h <= ultima; h++) {
                serie.getData().add(
                        new XYChart.Data<String, Number>(h + ":00", horas[h]));
            }

        } else {

            lblTituloGrafica.setText("Ventas por día");

            Map<LocalDate, Double> porDia = dao.ventasPorDia(desde, hasta);

            for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
                serie.getData().add(new XYChart.Data<String, Number>(
                        dia.format(FMT_DIA_MES),
                        porDia.getOrDefault(dia, 0.0)));
            }
        }

        chartVentas.getData().clear();
        chartVentas.getData().add(serie);

        lblTotalAcumulado.setText(moneda(actual.getTotal()));
    }

    // =========================================================
    // METODOS DE PAGO (DONUT + LISTA)
    // =========================================================
    private void actualizarMetodos(Map<String, Double> metodos) {

        pieMetodos.getData().clear();
        vboxMetodos.getChildren().clear();

        double suma = 0;
        for (double valor : metodos.values()) {
            suma += valor;
        }

        int indice = 0;

        for (Map.Entry<String, Double> e : metodos.entrySet()) {

            if (suma > 0) {
                pieMetodos.getData().add(
                        new PieChart.Data(e.getKey(), e.getValue()));
            }

            vboxMetodos.getChildren().add(
                    crearFilaMetodo(indice, e.getKey(), e.getValue(), suma));

            indice++;
        }

        lblDonutTotal.setText(compacto(suma));
    }

    private HBox crearFilaMetodo(int indice, String nombre,
            double monto, double suma) {

        Region marcador = new Region();
        marcador.setMinSize(12, 12);
        marcador.setPrefSize(12, 12);
        marcador.setMaxSize(12, 12);
        marcador.getStyleClass().addAll(
                "marcador", "marcador-" + Math.min(indice, 3));

        Label lblNombre = new Label(nombre);
        lblNombre.getStyleClass().add("metodo-nombre");

        Label lblMonto = new Label(moneda(monto));
        lblMonto.getStyleClass().add("metodo-monto");

        VBox textos = new VBox(1, lblNombre, lblMonto);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        Label lblPct = new Label(suma > 0
                ? String.format(Locale.US, "%.0f%%", monto / suma * 100)
                : "0%");
        lblPct.getStyleClass().add("porcentaje");

        HBox fila = new HBox(10, marcador, textos, espacio, lblPct);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    // =========================================================
    // TABLA: ULTIMAS VENTAS
    // =========================================================
    private void actualizarTabla() throws SQLException {

        String filtro = cmbMetodo.getValue();
        String metodo = (filtro == null || "Todos".equals(filtro)) ? null : filtro;

        tablaVentas.setItems(FXCollections.observableArrayList(
                dao.ultimasVentas(desde, hasta, metodo, 6)));

        // Obliga a reescribir la columna de hora cuando cambia el periodo
        tablaVentas.refresh();
    }

    // =========================================================
    // PRODUCTOS MAS VENDIDOS
    // =========================================================
    private void actualizarProductos() throws SQLException {

        vboxProductos.getChildren().clear();

        List<ProductoTop> top = dao.productosTop(desde, hasta, 4);

        if (top.isEmpty()) {
            Label vacio = new Label("Sin ventas en este periodo.");
            vacio.getStyleClass().add("aviso-vacio");
            vboxProductos.getChildren().add(vacio);
            return;
        }

        int posicion = 1;

        for (ProductoTop producto : top) {

            Label rango = new Label(String.valueOf(posicion));
            rango.setMinSize(29, 29);
            rango.setAlignment(Pos.CENTER);
            rango.getStyleClass().addAll(
                    "rango", "rango-" + Math.min(posicion, 4));

            Label nombre = new Label(producto.getNombre());
            nombre.getStyleClass().add("producto-nombre");

            Label unidades = new Label(producto.getUnidades() + " vendidos");
            unidades.getStyleClass().add("producto-detalle");

            VBox info = new VBox(2, nombre, unidades);

            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);

            Label total = new Label(moneda(producto.getTotal()));
            total.getStyleClass().add("producto-total");

            HBox fila = new HBox(13, rango, info, espacio, total);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("producto-fila");

            vboxProductos.getChildren().add(fila);
            posicion++;
        }
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
    private String moneda(double valor) {
        return String.format(Locale.US, "$%,.2f", valor);
    }

    private String compacto(double valor) {
        return valor >= 1000
                ? String.format(Locale.US, "$%.1fk", valor / 1000)
                : String.format(Locale.US, "$%.0f", valor);
    }

    private void mostrarError(SQLException ex) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Resumen de ventas");
        alerta.setHeaderText(null);
        alerta.setContentText("No se pudieron cargar las ventas:\n" + ex.getMessage());
        alerta.showAndWait();
    }
}
