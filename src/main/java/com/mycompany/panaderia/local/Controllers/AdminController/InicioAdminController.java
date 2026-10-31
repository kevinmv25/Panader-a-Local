
package com.mycompany.panaderia.local.Controllers.AdminController;

import com.panaderialocal.conexion.RegistroPedidosDAO;
import com.panaderialocal.conexion.RegistroPedidosDAO.PedidoReciente;
import com.panaderialocal.conexion.RegistroPedidosDAO.DetallePedido;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javafx.stage.Stage;

public class InicioAdminController implements Initializable {

    @FXML
    private Label lblFechaActual;

    @FXML
    private Label lblCantidadAlertas;

    @FXML
    private Label lblSinPedidos;

    @FXML
    private VBox contenedorPedidos;

    private final RegistroPedidosDAO dao =
            new RegistroPedidosDAO();

    private final DateTimeFormatter formatoFecha =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ==========================================
    // INICIALIZAR INTERFAZ
    // ==========================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        configurarFecha();

        cargarAlertasPedidos();
    }

    // ==========================================
    // MOSTRAR FECHA ACTUAL
    // ==========================================

    private void configurarFecha() {

        Locale mexico = new Locale("es", "MX");

        DateTimeFormatter formato =
                DateTimeFormatter.ofPattern(
                        "EEEE, dd MMM, yyyy",
                        mexico
                );

        String fecha = LocalDate.now()
                .format(formato);

        if (!fecha.isEmpty()) {
            fecha = fecha.substring(0, 1).toUpperCase()
                    + fecha.substring(1);
        }

        lblFechaActual.setText(fecha);
    }

    // ==========================================
    // CONSULTAR PEDIDOS PRÓXIMOS A VENCER
    // ==========================================

    private void cargarAlertasPedidos() {

        try {

            LocalDate hoy = LocalDate.now();
            LocalDate limite = hoy.plusDays(2);

            List<PedidoReciente> todos =
                    dao.obtenerTodosLosPedidos();

            List<PedidoReciente> pendientes =
                    new ArrayList<>();

            for (PedidoReciente pedido : todos) {

                if (pedido == null
                        || pedido.getFechaEntrega() == null) {
                    continue;
                }

                String estado = pedido.getEstado();

                if (estado == null) {
                    continue;
                }

                // EXCLUIR PEDIDOS FINALIZADOS
                if (estado.equalsIgnoreCase("Cancelado")
                        || estado.equalsIgnoreCase("Completado")
                        || estado.equalsIgnoreCase("Entregado")) {
                    continue;
                }

                try {

                    LocalDate fechaEntrega =
                            LocalDate.parse(
                                    pedido.getFechaEntrega()
                            );

                    // MOSTRAR HOY, MAÑANA Y DENTRO DE 2 DÍAS
                    if (!fechaEntrega.isBefore(hoy)
                            && !fechaEntrega.isAfter(limite)) {

                        pendientes.add(pedido);
                    }

                } catch (Exception ex) {

                    System.err.println(
                            "Fecha incorrecta del pedido "
                            + pedido.getIdPedido()
                            + ": "
                            + pedido.getFechaEntrega()
                    );
                }
            }

            // ORDENAR POR FECHA DE ENTREGA
            pendientes.sort(
                    Comparator.comparing(
                            pedido -> LocalDate.parse(
                                    pedido.getFechaEntrega()
                            )
                    )
            );

            mostrarPedidos(pendientes);

            // UNA SOLA NOTIFICACIÓN AL ENTRAR
            if (!pendientes.isEmpty()) {
                mostrarNotificacion(pendientes.size());
            }

        } catch (SQLException ex) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error al consultar pedidos",
                    "No se pudieron cargar las alertas:\n"
                    + ex.getMessage()
            );
        }
    }

    // ==========================================
    // MOSTRAR TARJETAS DE PEDIDOS
    // ==========================================

    private void mostrarPedidos(
            List<PedidoReciente> pedidos) {

        contenedorPedidos.getChildren().clear();

        int cantidad = pedidos.size();

        lblCantidadAlertas.setText(
                "(" + cantidad
                + (cantidad == 1
                        ? " pedido pendiente)"
                        : " pedidos pendientes)")
        );

        boolean hayPedidos = !pedidos.isEmpty();

        lblSinPedidos.setVisible(!hayPedidos);
        lblSinPedidos.setManaged(!hayPedidos);

        for (PedidoReciente pedido : pedidos) {

            try {

                List<DetallePedido> detalles =
                        dao.obtenerDetallesPedido(
                                pedido.getIdPedido()
                        );

                HBox tarjeta =
                        crearTarjetaPedido(pedido, detalles);

                contenedorPedidos.getChildren().add(tarjeta);

            } catch (SQLException ex) {

                System.err.println(
                        "Error al consultar los detalles del pedido "
                        + pedido.getIdPedido()
                        + ": "
                        + ex.getMessage()
                );
            }
        }
    }

    // ==========================================
    // CREAR TARJETA DE PEDIDO
    // ==========================================

    private HBox crearTarjetaPedido(
            PedidoReciente pedido,
            List<DetallePedido> detalles) {

        LocalDate fechaEntrega =
                LocalDate.parse(pedido.getFechaEntrega());

        long diasRestantes =
                ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        fechaEntrega
                );

        int cantidadProductos = detalles.size();

        int totalPiezas = detalles.stream()
                .mapToInt(DetallePedido::getCantidad)
                .sum();

        String tiempoRestante;

        if (diasRestantes == 0) {
            tiempoRestante = "Hoy";
        } else if (diasRestantes == 1) {
            tiempoRestante = "Mañana";
        } else {
            tiempoRestante = "En " + diasRestantes + " días";
        }

        // --------------------------------------
        // INFORMACIÓN DEL PEDIDO
        // --------------------------------------

        VBox infoPedido = new VBox(7.0);

        Label numeroPedido = new Label(
                "Pedido #" + String.format(
                        "%06d",
                        pedido.getIdPedido()
                )
        );

        numeroPedido.setStyle(
                "-fx-font-family: 'Georgia';"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #1D1B1A;"
        );

        Label cliente = new Label(
                textoSeguro(pedido.getCliente())
        );

        Label telefono = new Label(
                textoSeguro(pedido.getTelefono())
        );

        cliente.setStyle(estiloTexto());
        telefono.setStyle(estiloTexto());

        infoPedido.getChildren().addAll(
                numeroPedido,
                cliente,
                telefono
        );

        // --------------------------------------
        // FECHA DE ENTREGA
        // --------------------------------------

        VBox infoFecha = new VBox(7.0);

        Label tituloFecha =
                new Label("Fecha de entrega:");

        Label fecha = new Label(
                fechaEntrega.format(formatoFecha)
        );

        Label tiempo = new Label(tiempoRestante);

        tituloFecha.setStyle(estiloTexto());
        fecha.setStyle(estiloTexto());

        tiempo.setStyle(
                "-fx-background-color: #FFF3E0;"
                + "-fx-background-radius: 4;"
                + "-fx-padding: 4 9 4 9;"
                + "-fx-text-fill: #B96A00;"
                + "-fx-font-size: 10px;"
                + "-fx-font-weight: bold;"
        );

        infoFecha.getChildren().addAll(
                tituloFecha,
                fecha,
                tiempo
        );

        // --------------------------------------
        // PRODUCTOS Y PIEZAS
        // --------------------------------------

        VBox infoProductos = new VBox(7.0);

        Label productos = new Label(
                cantidadProductos + " productos"
        );

        Label piezas = new Label(
                totalPiezas + " piezas"
        );

        productos.setStyle(estiloTexto());
        piezas.setStyle(estiloTexto());

        infoProductos.getChildren().addAll(
                productos,
                piezas
        );

        // --------------------------------------
        // TOTAL
        // --------------------------------------

        VBox infoTotal = new VBox(7.0);

        Label tituloTotal = new Label("Total:");

        Label total = new Label(
                String.format(
                        Locale.US,
                        "$%.2f",
                        pedido.getTotal()
                )
        );

        tituloTotal.setStyle(estiloTexto());

        total.setStyle(
                "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #1D1B1A;"
        );

        infoTotal.getChildren().addAll(
                tituloTotal,
                total
        );

        // --------------------------------------
        // BOTÓN VER PEDIDO
        // --------------------------------------

        Button btnVerPedido =
                new Button("Ver pedido");

        btnVerPedido.setPrefWidth(105.0);
        btnVerPedido.setMinWidth(95.0);

        btnVerPedido.setStyle(
                "-fx-background-color: #E68000;"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 6;"
                + "-fx-padding: 8 12;"
                + "-fx-font-size: 11px;"
                + "-fx-cursor: hand;"
        );

        btnVerPedido.setOnAction(event ->
                mostrarDetallePedido(pedido, detalles)
        );

        // --------------------------------------
        // TARJETA
        // --------------------------------------

        HBox tarjeta = new HBox(13.0);

        tarjeta.setAlignment(Pos.CENTER_LEFT);

        tarjeta.setPadding(
                new Insets(12.0, 10.0, 12.0, 10.0)
        );

        tarjeta.setMinHeight(75.0);
        tarjeta.setMaxWidth(Double.MAX_VALUE);

        tarjeta.setStyle(
                "-fx-background-color: #FFFFFF;"
                + "-fx-border-color: #E8E5E3;"
                + "-fx-border-radius: 7;"
                + "-fx-background-radius: 7;"
        );

        // DISTRIBUCIÓN DE LAS COLUMNAS
        infoPedido.setMinWidth(115.0);
        infoFecha.setMinWidth(145.0);
        infoProductos.setMinWidth(90.0);
        infoTotal.setMinWidth(75.0);

        HBox.setHgrow(infoPedido, Priority.ALWAYS);
        HBox.setHgrow(infoFecha, Priority.ALWAYS);
        HBox.setHgrow(infoProductos, Priority.ALWAYS);
        HBox.setHgrow(infoTotal, Priority.ALWAYS);

        tarjeta.getChildren().addAll(
                infoPedido,
                crearSeparador(),
                infoFecha,
                crearSeparador(),
                infoProductos,
                crearSeparador(),
                infoTotal,
                btnVerPedido
        );

        return tarjeta;
    }

    // ==========================================
    // SEPARADORES VERTICALES
    // ==========================================

    private Separator crearSeparador() {

        Separator separador =
                new Separator();

        separador.setOrientation(
                javafx.geometry.Orientation.VERTICAL
        );

        separador.setMinHeight(55.0);
        separador.setPrefHeight(65.0);

        return separador;
    }

    // ==========================================
    // DETALLE DEL PEDIDO
    // ==========================================

    private void mostrarDetallePedido(
            PedidoReciente pedido,
            List<DetallePedido> detalles) {

        StringBuilder mensaje = new StringBuilder();

        mensaje.append("Pedido #")
                .append(pedido.getIdPedido())
                .append("\n\n");

        mensaje.append("Cliente: ")
                .append(textoSeguro(pedido.getCliente()))
                .append("\n");

        mensaje.append("Teléfono: ")
                .append(textoSeguro(pedido.getTelefono()))
                .append("\n");

        mensaje.append("Fecha de entrega: ")
                .append(pedido.getFechaEntrega())
                .append("\n");

        mensaje.append("Estado: ")
                .append(textoSeguro(pedido.getEstado()))
                .append("\n\n");

        mensaje.append("PRODUCTOS\n");

        for (DetallePedido detalle : detalles) {

            mensaje.append("• ")
                    .append(detalle.getNombreProducto())
                    .append(" - ")
                    .append(detalle.getCantidad())
                    .append(" piezas - $")
                    .append(String.format(
                            Locale.US,
                            "%.2f",
                            detalle.getSubtotal()
                    ))
                    .append("\n");
        }

        mensaje.append("\nTotal: $")
                .append(String.format(
                        Locale.US,
                        "%.2f",
                        pedido.getTotal()
                ));

        mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Detalle del pedido",
                mensaje.toString()
        );
    }

    // ==========================================
    // NOTIFICACIÓN AUTOMÁTICA
    // ==========================================

    private void mostrarNotificacion(int cantidad) {

        Alert alerta = new Alert(
                Alert.AlertType.WARNING
        );

        alerta.setTitle("El Lirio Rojo");
        alerta.setHeaderText("¡Alerta de pedidos!");

        if (cantidad == 1) {

            alerta.setContentText(
                    "Tienes 1 pedido pendiente "
                    + "de entrega en los próximos 2 días."
            );

        } else {

            alerta.setContentText(
                    "Tienes " + cantidad
                    + " pedidos pendientes "
                    + "de entrega en los próximos 2 días."
            );
        }

        // UNA SOLA VENTANA POR CARGA DE ESTA INTERFAZ
        alerta.showAndWait();
    }

    // ==========================================
    // NAVEGACIÓN
    // ==========================================

    @FXML
    private void abrirCatalogo(ActionEvent event)
            throws IOException {

        abrirPantalla(
                event,
                "/com/mycompany/panaderia/local/Administrador/loginAdministrador.fxml"
        );
    }

    @FXML
    private void abrirProduccion(ActionEvent event)
            throws IOException {

        abrirPantalla(
                event,
                "/com/mycompany/panaderia/local/Administrador/ProduccionDiaria.fxml"
        );
    }

    @FXML
    private void abrirMenuMateriales(ActionEvent event)
            throws IOException {

        abrirPantalla(
                event,
                "/com/mycompany/panaderia/local/Administrador/menuMateriales.fxml"
        );
    }

    @FXML
    private void abrirVentas(ActionEvent event) {

        mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Ventas",
                "La pantalla de ventas todavía no está configurada."
        );
    }

    @FXML
    private void abrirPedidos(ActionEvent event) {

        mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Pedidos",
                "La navegación a la pantalla de pedidos "
                + "todavía no está configurada."
        );
    }

    @FXML
    private void cerrarSesion(ActionEvent event) {

        mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Cerrar sesión",
                "Configura aquí la navegación hacia el inicio de sesión."
        );
    }

    // ==========================================
    // CAMBIAR DE INTERFAZ
    // ==========================================

    private void abrirPantalla(
            ActionEvent event,
            String ruta) throws IOException {

        URL recurso = getClass().getResource(ruta);

        if (recurso == null) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Pantalla no encontrada",
                    "No se encontró la interfaz:\n" + ruta
            );

            return;
        }

        Parent root = FXMLLoader.load(recurso);

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }

    // ==========================================
    // UTILIDADES
    // ==========================================

    private String estiloTexto() {

        return "-fx-font-size: 10px;"
                + "-fx-text-fill: #1D1B1A;";
    }

    private String textoSeguro(String texto) {

        return texto == null || texto.trim().isEmpty()
                ? "No disponible"
                : texto;
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
