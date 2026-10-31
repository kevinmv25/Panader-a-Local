
package com.mycompany.panaderia.local.Controllers;

import com.mycompany.panaderia.local.Pan;
import panaderialocal.conexion.PanDAO;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CatalogoPanesController {

    @FXML
    private TextField txtBuscarPan;

    @FXML
    private Button btnAgregarPan;

    @FXML
    private Button btnProduccion;

    @FXML
    private Button btnCerrarSesion;

    @FXML
    private FlowPane contenedorPanes;

    private final PanDAO panDAO = new PanDAO();

    private List<Pan> panes;

    @FXML
    public void initialize() {
        cargarPanes();

        txtBuscarPan.textProperty().addListener(
            (observable, anterior, nuevo) -> mostrarPanes(nuevo)
        );
    }

    @FXML
    private void abrirFormularioNuevo(ActionEvent event) {
        abrirFormulario(null);
    }

    /**
     * Navega desde loginAdministrador.fxml
     * hasta Administrador/ProduccionDiaria.fxml.
     */
    @FXML
    private void abrirProduccion(ActionEvent event) {
        cambiarPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/ProduccionDiaria.fxml"
        );
    }
    
    @FXML
    private void abrirMenuMateriales(ActionEvent event) {
        cambiarPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/menuMateriales.fxml"
        );
    }

    /**
     * Regresa a Login.fxml al cerrar sesión.
     */
    @FXML
    private void cerrarSesion(ActionEvent event) {
        cambiarPantalla(
            event,
            "/com/mycompany/panaderia/local/Login.fxml"
        );
    }

    private void cambiarPantalla(ActionEvent event, String rutaFXML) {
        try {
            URL recurso = getClass().getResource(rutaFXML);

            if (recurso == null) {
                mostrarError(
                    "Pantalla no encontrada",
                    "No se encontró el archivo FXML.",
                    "Ruta buscada: " + rutaFXML
                );
                return;
            }

            FXMLLoader loader = new FXMLLoader(recurso);
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            mostrarError(
                "Error de navegación",
                "No fue posible abrir la pantalla solicitada.",
                e.getMessage()
            );

            e.printStackTrace();
        }
    }

    private void cargarPanes() {
        try {
            panes = panDAO.obtenerPanes();

            mostrarPanes(
                txtBuscarPan == null ? "" : txtBuscarPan.getText()
            );

        } catch (Exception e) {
            mostrarError(
                "Error al cargar los panes",
                "No fue posible consultar los panes registrados.",
                e.getMessage()
            );

            e.printStackTrace();
        }
    }

    private void mostrarPanes(String textoBusqueda) {
        contenedorPanes.getChildren().clear();

        if (panes == null) {
            return;
        }

        String filtro = textoBusqueda == null
                ? ""
                : textoBusqueda.trim().toLowerCase(Locale.ROOT);

        for (Pan pan : panes) {
            if (pan == null || pan.getNombre() == null) {
                continue;
            }

            if (pan.getNombre().toLowerCase(Locale.ROOT).contains(filtro)) {
                contenedorPanes.getChildren().add(crearTarjeta(pan));
            }
        }
    }

    private VBox crearTarjeta(Pan pan) {
        VBox tarjeta = new VBox(10);
        tarjeta.setPadding(new Insets(15));
        tarjeta.setPrefWidth(220);
        tarjeta.setMaxWidth(220);

        tarjeta.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #E5E5E5;" +
            "-fx-border-radius: 12;"
        );

        ImageView imagen = new ImageView();
        imagen.setFitWidth(190);
        imagen.setFitHeight(130);
        imagen.setPreserveRatio(true);
        imagen.setSmooth(true);

        Image imagenPan = obtenerImagen(pan.getFoto());

        if (imagenPan != null) {
            imagen.setImage(imagenPan);
        }

        Label lblNombre = new Label(pan.getNombre());
        lblNombre.setWrapText(true);
        lblNombre.setStyle(
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #1D1B1A;"
        );

        Label lblPrecio = new Label("$" + pan.getPrecio());
        lblPrecio.setStyle(
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #E68000;"
        );

        Label lblDescripcion = new Label(
            pan.getdescripcion() == null ? "" : pan.getdescripcion()
        );
        lblDescripcion.setWrapText(true);
        lblDescripcion.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #666666;"
        );

        Button btnModificar = new Button("Modificar");
        btnModificar.setMaxWidth(Double.MAX_VALUE);
        btnModificar.setStyle(
            "-fx-background-color: #E68000;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );

        btnModificar.setOnAction(event -> abrirFormulario(pan));

        tarjeta.getChildren().addAll(
            imagen,
            lblNombre,
            lblPrecio,
            lblDescripcion,
            btnModificar
        );

        return tarjeta;
    }

    private Image obtenerImagen(String ruta) {
        if (ruta == null || ruta.trim().isEmpty()) {
            return null;
        }

        try {
            String valor = ruta.trim();

            if (valor.startsWith("http://")
                    || valor.startsWith("https://")
                    || valor.startsWith("file:")) {
                return new Image(valor, true);
            }

            File archivo = new File(valor);

            if (archivo.exists() && archivo.isFile()) {
                return new Image(archivo.toURI().toString(), true);
            }

            String rutaRecurso = valor.startsWith("/")
                    ? valor
                    : "/" + valor;

            URL recurso = getClass().getResource(rutaRecurso);

            if (recurso != null) {
                return new Image(recurso.toExternalForm(), true);
            }

        } catch (Exception e) {
            System.err.println(
                "No se pudo cargar la imagen " + ruta + ": "
                + e.getMessage()
            );
        }

        return null;
    }

    private void abrirFormulario(Pan pan) {
        try {
            URL recurso = getClass().getResource(
                "/com/mycompany/panaderia/local/FormularioPan.fxml"
            );

            if (recurso == null) {
                mostrarError(
                    "Formulario no encontrado",
                    "No se encontró FormularioPan.fxml.",
                    "Verifica la ubicación del archivo."
                );
                return;
            }

            FXMLLoader loader = new FXMLLoader(recurso);
            Parent root = loader.load();

            FormularioPanController controller =
                    loader.getController();

            controller.configurar(pan, this::cargarPanes);

            Stage stage = new Stage();
            stage.setTitle(
                pan == null ? "Registrar pan" : "Modificar pan"
            );
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (Exception e) {
            mostrarError(
                "Error al abrir el formulario",
                "No fue posible abrir el formulario del pan.",
                e.getMessage()
            );

            e.printStackTrace();
        }
    }

    private void mostrarError(
            String titulo,
            String mensaje,
            String detalle) {

        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle(titulo);
        alerta.setHeaderText(mensaje);
        alerta.setContentText(detalle == null ? "" : detalle);
        alerta.showAndWait();
    }
}
