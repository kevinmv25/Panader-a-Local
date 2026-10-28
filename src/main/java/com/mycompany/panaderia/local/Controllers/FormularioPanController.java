
package com.mycompany.panaderia.local.Controllers;

import com.mycompany.panaderia.local.Pan;
import panaderialocal.conexion.PanDAO;

import java.io.File;
import java.net.URL;
import java.util.Locale;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class FormularioPanController {

    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private Label lblSinImagen;

    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtFoto;

    @FXML private ImageView imgVistaPrevia;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    private final PanDAO panDAO = new PanDAO();

    private Pan panSeleccionado;
    private Runnable actualizarCatalogo;

    // RECIBIR EL PAN DEL CATALOGO
    public void configurar(Pan pan, Runnable actualizar) {

        this.panSeleccionado = pan;
        this.actualizarCatalogo = actualizar;

        boolean modificar = pan != null;

        lblTitulo.setText(modificar
                ? "Modificar Pan" : "Registrar Nuevo Pan");

        lblSubtitulo.setText(modificar
                ? "Actualiza la informacion del producto"
                : "Registra un producto nuevo");

        btnGuardar.setText(modificar
                ? "Guardar Cambios" : "Registrar Pan");

        // MOSTRAR INFORMACION DEL PAN
        txtNombre.setText(modificar ? texto(pan.getNombre()) : "");
        txtDescripcion.setText(modificar ? texto(pan.getdescripcion()) : "");

        txtPrecio.setText(modificar
                ? String.format(Locale.US, "%.2f", pan.getPrecio())
                : "");

        txtFoto.setText(modificar ? texto(pan.getFoto()) : "");

        mostrarImagen(txtFoto.getText());
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }

    // BOTON CANCELAR
    @FXML
    private void cancelar() {
        cerrarVentana();
    }

    // BOTON SELECCIONAR IMAGEN
    @FXML
    private void seleccionarFoto() {

        FileChooser selector = new FileChooser();

        selector.setTitle("Seleccionar fotografia");

        selector.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(
                "Imagenes", "*.jpg", "*.jpeg", "*.png"
            )
        );

        File archivo = selector.showOpenDialog(
            btnGuardar.getScene().getWindow()
        );

        if (archivo != null) {
            txtFoto.setText(archivo.getAbsolutePath());
            mostrarImagen(txtFoto.getText());
        }
    }

    // MOSTRAR FOTO EN EL FORMULARIO
    private void mostrarImagen(String ruta) {

        imgVistaPrevia.setImage(null);
        lblSinImagen.setVisible(true);

        if (ruta == null || ruta.isBlank()) {
            return;
        }

        try {
            String origen;
            File archivo = new File(ruta);

            if (archivo.isFile()) {
                origen = archivo.toURI().toString();

            } else if (ruta.startsWith("file:")
                    || ruta.startsWith("http:")
                    || ruta.startsWith("https:")) {
                origen = ruta;

            } else {
                String recurso = ruta.startsWith("/")
                        ? ruta : "/" + ruta;

                URL url = getClass().getResource(recurso);

                if (url == null) return;

                origen = url.toExternalForm();
            }

            Image imagen = new Image(origen);

            if (!imagen.isError()) {
                imgVistaPrevia.setImage(imagen);
                lblSinImagen.setVisible(false);
            }

        } catch (Exception e) {
            System.out.println("No se pudo cargar la imagen.");
        }
    }

    // BOTON GUARDAR CAMBIOS / REGISTRAR
    @FXML
    private void guardarPan() {

        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        String foto = txtFoto.getText().trim();

        // VALIDAR NOMBRE
        if (nombre.isEmpty()) {
            mostrarMensaje("Debes ingresar el nombre del pan.");
            return;
        }

        // VALIDAR PRECIO
        double precio;

        try {
            precio = Double.parseDouble(
                txtPrecio.getText().trim().replace(",", ".")
            );

            if (!Double.isFinite(precio) || precio < 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {
            mostrarMensaje("Ingresa un precio valido.");
            return;
        }

        // GUARDAR EN SQLITE
        try {

            boolean guardado;

            if (panSeleccionado == null) {

                // REGISTRAR NUEVO PAN
                guardado = panDAO.insertarPan(
                    nombre, descripcion, precio, foto
                );

            } else {

                // MODIFICAR PAN EXISTENTE
                guardado = panDAO.actualizarPan(
                    panSeleccionado.getIdPan(),
                    nombre, descripcion, precio, foto
                );
            }

            if (guardado) {

                // VOLVER A CARGAR LOS PANES
                if (actualizarCatalogo != null) {
                    actualizarCatalogo.run();
                }

                cerrarVentana();

            } else {
                mostrarMensaje("No se pudo guardar el pan.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarMensaje("Error al guardar: " + e.getMessage());
        }
    }

    // CERRAR FORMULARIO
    private void cerrarVentana() {

        Stage ventana = (Stage)
                btnCancelar.getScene().getWindow();

        ventana.close();
    }

    // MOSTRAR MENSAJE
    private void mostrarMensaje(String mensaje) {

        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle("Catalogo de Panes");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
