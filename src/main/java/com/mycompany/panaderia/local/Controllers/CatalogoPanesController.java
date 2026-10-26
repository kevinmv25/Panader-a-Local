
package com.mycompany.panaderia.local.Controllers;

import com.mycompany.panaderia.local.Pan;
import panaderialocal.conexion.PanDAO;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public class CatalogoPanesController {

    @FXML private TextField txtBuscarPan;
    @FXML private Button btnAgregarPan;
    @FXML private FlowPane contenedorPanes;

    private final PanDAO panDAO = new PanDAO();
    private List<Pan> panes;

    @FXML
    public void initialize() {

        cargarPanes();

        txtBuscarPan.textProperty().addListener(
            (obs, anterior, nuevo) -> mostrarPanes(nuevo)
        );

        btnAgregarPan.setOnAction(e -> abrirFormulario(null));
    }

    // CARGAR DESDE SQLITE
    private void cargarPanes() {
        try {
            panes = panDAO.obtenerPanes();
            mostrarPanes(txtBuscarPan.getText());
        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    // BUSCAR PANES
    private void mostrarPanes(String filtro) {

        contenedorPanes.getChildren().clear();

        String busqueda = filtro == null ? ""
                : filtro.trim().toLowerCase(Locale.ROOT);

        if (panes == null) return;

        for (Pan pan : panes) {

            if (pan.getNombre() != null &&
                pan.getNombre().toLowerCase(Locale.ROOT).contains(busqueda)) {

                contenedorPanes.getChildren().add(crearTarjeta(pan));
            }
        }
    }

    // CREAR TARJETAS
    private VBox crearTarjeta(Pan pan) {

        VBox tarjeta = new VBox();
        tarjeta.setPrefWidth(180);
        tarjeta.setStyle(
            "-fx-background-color: white; -fx-background-radius: 12;" +
            "-fx-border-color: #E5E5E5; -fx-border-radius: 12;"
        );

        StackPane zonaFoto = new StackPane();
        zonaFoto.setPrefHeight(125);
        zonaFoto.setMinHeight(125);
        zonaFoto.setStyle("-fx-background-color: #E8D0AC;");

        ImageView imagen = obtenerImagen(pan.getFoto());

        if (imagen != null) {
            zonaFoto.getChildren().add(imagen);
        } else {
            zonaFoto.getChildren().add(new Label("Sin Imagen"));
        }

        VBox informacion = new VBox(8);
        informacion.setPadding(new Insets(12));

        Label nombre = new Label(pan.getNombre());
        nombre.setWrapText(true);
        nombre.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

        Label precio = new Label(
            String.format(Locale.US, "$%.2f", pan.getPrecio())
        );
        precio.setStyle("-fx-text-fill: #E68000; -fx-font-weight: bold;");

        Label descripcion = new Label(
            pan.getdescripcion() == null ? "" : pan.getdescripcion()
        );
        descripcion.setWrapText(true);

        Label estado = new Label("Disponible");
        estado.setStyle("-fx-text-fill: #00A779;");

        Button modificar = new Button("Modificar");
        modificar.setMaxWidth(Double.MAX_VALUE);
        modificar.setOnAction(e -> abrirFormulario(pan));

        informacion.getChildren().addAll(
            nombre, precio, descripcion, estado, modificar
        );

        tarjeta.getChildren().addAll(zonaFoto, informacion);

        return tarjeta;
    }

    // MOSTRAR FOTOGRAFIAS
    private ImageView obtenerImagen(String ruta) {

        if (ruta == null || ruta.isBlank()) return null;

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
                String recurso = ruta.startsWith("/") ? ruta : "/" + ruta;
                URL url = getClass().getResource(recurso);
                if (url == null) return null;
                origen = url.toExternalForm();
            }

            Image imagen = new Image(origen, 180, 125, true, true, false);

            if (imagen.isError()) return null;

            ImageView vista = new ImageView(imagen);
            vista.setFitWidth(180);
            vista.setFitHeight(125);
            vista.setPreserveRatio(true);

            return vista;

        } catch (Exception e) {
            return null;
        }
    }

    // ABRIR FORMULARIO PARA REGISTRAR O MODIFICAR
    private void abrirFormulario(Pan pan) {

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                    "/com/mycompany/panaderia/local/FormularioPan.fxml"
                )
            );

            Parent root = loader.load();

            FormularioPanController controller = loader.getController();
//Hola
            // Enviar el pan seleccionado y la accion de actualizar
            controller.configurar(pan, this::cargarPanes);

            Stage ventana = new Stage();
            ventana.setTitle(pan == null ? "Registrar Pan" : "Modificar Pan");

            ventana.initModality(Modality.WINDOW_MODAL);
            ventana.initOwner(btnAgregarPan.getScene().getWindow());

            ventana.setScene(new Scene(root));
            ventana.setResizable(false);
            ventana.showAndWait();

        } catch (Exception e) {
            mostrarError(e.getMessage());
            e.printStackTrace();
        }
    }

    // MENSAJES DE ERROR
    private void mostrarError(String mensaje) {

        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Catalogo de Panes");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje == null ? "Error desconocido" : mensaje);
        alerta.showAndWait();
    }
    
    
    @FXML
    private void cerrarSesion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(           
                            "/com/mycompany/panaderia/local/Login.fxml"         
                    )      
            );       
            Parent root = loader.load();       
            Stage ventana = (Stage)
                ((Button) event.getSource())
                .getScene().getWindow();       
            ventana.setScene(new Scene(root));     
            ventana.setTitle("El Lirio Rojo - Iniciar Sesion");     
            ventana.centerOnScreen();   
        } catch (Exception e) {        
            e.printStackTrace();   
        }
    }

    
    
}
