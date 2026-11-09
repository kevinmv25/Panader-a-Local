
package com.mycompany.panaderia.local.Controllers.AdminController;

import com.panaderialocal.conexion.InventarioDAO;
import com.panaderialocal.conexion.InventarioDAO.MaterialNuevo;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegistroMaterialesController implements Initializable {

    @FXML
    private TextField txtMaterial;

    @FXML
    private TextField txtCantidad;

    @FXML
    private ComboBox<String> cmbUnidad;

    @FXML
    private TableView<MaterialRegistro> tablaMateriales;

    @FXML
    private TableColumn<MaterialRegistro, String> colMaterial;

    @FXML
    private TableColumn<MaterialRegistro, String> colCantidad;

    @FXML
    private TableColumn<MaterialRegistro, String> colUnidadMedida;

    private final InventarioDAO inventarioDAO = new InventarioDAO();

    private final ObservableList<MaterialRegistro> listaMateriales =
            FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        colMaterial.setCellValueFactory(
                datos -> new ReadOnlyStringWrapper(
                        datos.getValue().getMaterial()
                )
        );

        colCantidad.setCellValueFactory(
                datos -> new ReadOnlyStringWrapper(
                        datos.getValue().getCantidad()
                )
        );

        colUnidadMedida.setCellValueFactory(
                datos -> new ReadOnlyStringWrapper(
                        datos.getValue().getUnidadMedida()
                )
        );

        tablaMateriales.setItems(listaMateriales);

        cmbUnidad.setItems(FXCollections.observableArrayList(
                "KG",
                "Gramos",
                "Litros",
                "Mililitros",
                "Piezas",
                "Paquetes",
                "Unidades"
        ));
    }

    @FXML
    private void agregarMaterial(ActionEvent event) {

        String material = txtMaterial.getText().trim();
        String cantidadTexto = txtCantidad.getText().trim();
        String unidad = cmbUnidad.getValue();

        if (material.isEmpty()
                || cantidadTexto.isEmpty()
                || unidad == null) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campos incompletos",
                    "Completa el nombre, la cantidad y la unidad de medida."
            );
            return;
        }

        for (int i = 0; i < material.length(); i++) {

            char letra = material.charAt(i);

            if (!((letra >= 'A' && letra <= 'Z')
                    || (letra >= 'a' && letra <= 'z')
                    || letra == ' '
                    || letra == 'á' || letra == 'é'
                    || letra == 'í' || letra == 'ó'
                    || letra == 'ú'
                    || letra == 'Á' || letra == 'É'
                    || letra == 'Í' || letra == 'Ó'
                    || letra == 'Ú'
                    || letra == 'ñ' || letra == 'Ñ'
                    || letra == 'ü' || letra == 'Ü')) {

                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Nombre inválido",
                        "El nombre del material solo debe contener letras."
                );
                return;
            }
        }

        double cantidad;

        try {
            cantidad = Double.parseDouble(
                    cantidadTexto.replace(",", ".")
            );

            if (Double.isNaN(cantidad)
                    || Double.isInfinite(cantidad)
                    || cantidad <= 0) {

                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Cantidad inválida",
                        "La cantidad debe ser un número mayor que cero."
                );
                return;
            }

        } catch (NumberFormatException e) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Cantidad inválida",
                    "Introduce una cantidad numérica válida."
            );
            return;
        }

        MaterialRegistro nuevoMaterial = new MaterialRegistro(
                material,
                cantidadTexto,
                unidad
        );

        listaMateriales.add(nuevoMaterial);

        tablaMateriales.refresh();
        tablaMateriales.scrollTo(nuevoMaterial);

        limpiarCampos();
    }

    @FXML
    private void guardarMateriales(ActionEvent event) {

        if (listaMateriales.isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Sin materiales",
                    "Primero agrega al menos un material a la tabla."
            );
            return;
        }

        List<MaterialNuevo> materialesGuardar = new ArrayList<>();

        try {

            for (MaterialRegistro material : listaMateriales) {

                double cantidad = Double.parseDouble(
                        material.getCantidad().replace(",", ".")
                );

                materialesGuardar.add(new MaterialNuevo(
                        material.getMaterial(),
                        cantidad,
                        material.getUnidadMedida()
                ));
            }

            inventarioDAO.guardarMateriales(materialesGuardar);

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Registro exitoso",
                    "Se guardaron correctamente "
                    + materialesGuardar.size()
                    + " materiales en el inventario."
            );

            listaMateriales.clear();
            limpiarCampos();

        } catch (SQLException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error al guardar",
                    "No se pudieron guardar los materiales.\n\n"
                    + e.getMessage()
            );

        } catch (NumberFormatException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error en los datos",
                    "Uno de los materiales contiene una cantidad inválida."
            );
        }
    }

    @FXML
    private void cancelarRegistro(ActionEvent event) {

        if (listaMateriales.isEmpty()) {
            limpiarCampos();
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Cancelar registro");
        confirmacion.setHeaderText("¿Deseas cancelar el registro?");
        confirmacion.setContentText(
                "Se eliminarán los materiales que agregaste a esta lista."
        );

        confirmacion.showAndWait().ifPresent(respuesta -> {

            if (respuesta == ButtonType.OK) {

                listaMateriales.clear();
                limpiarCampos();
            }
        });
    }

    private void limpiarCampos() {

        txtMaterial.clear();
        txtCantidad.clear();
        cmbUnidad.getSelectionModel().clearSelection();

        txtMaterial.requestFocus();
    }

    @FXML
    private void abrirCatalogo(ActionEvent event) throws IOException {

        abrirPantalla(
                event,
                "/com/mycompany/panaderia/local/Administrador/loginAdministrador.fxml"
        );
    }

    @FXML
    private void abrirProduccion(ActionEvent event) throws IOException {

        abrirPantalla(
                event,
                "/com/mycompany/panaderia/local/Administrador/ProduccionDiaria.fxml"
        );
    }

    @FXML
    private void abrirMenuMateriales(ActionEvent event) throws IOException {

        abrirPantalla(
                event,
                "/com/mycompany/panaderia/local/Administrador/menuMateriales.fxml"
        );
    }

    @FXML
    private void abrirInicio(ActionEvent event) throws IOException {

        abrirPantalla(
                event,
                "/com/mycompany/panaderia/local/Administrador/inicioAdmin.fxml"
        );
    }

    @FXML
    private void abrirVentas(ActionEvent event) throws IOException {

        abrirPantalla(
                event,
                "/com/mycompany/panaderia/local/Administrador/ventas.fxml"
        );
    }
    
    @FXML
    private void abrirPedidos(ActionEvent event) throws IOException {

        abrirPantalla(
                event,
                "/com/mycompany/panaderia/local/Administrador/historialPedidos.fxml"
        );
    }
    
    @FXML
    private void cerrarSesion(ActionEvent event) throws IOException {

        abrirPantalla(
                event,
                "/com/mycompany/panaderia/local/Login.fxml"
        );
    }
    
    @FXML
    private void inicioAdmin(ActionEvent event) throws IOException {

        abrirPantalla(
                event,
                "/com/mycompany/panaderia/local/Administrador/inicioAdmin.fxml"
        );
    }
    
    

    private void abrirPantalla(ActionEvent event, String ruta)
            throws IOException {

        URL recurso = getClass().getResource(ruta);

        if (recurso == null) {

            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Pantalla no encontrada");
            alerta.setHeaderText("No se pudo abrir la pantalla.");
            alerta.setContentText("Verifica la ruta: " + ruta);
            alerta.showAndWait();

            return;
        }

        Parent root = FXMLLoader.load(recurso);

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    }

    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensaje) {

        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class MaterialRegistro {

        private final String material;
        private final String cantidad;
        private final String unidadMedida;

        public MaterialRegistro(
                String material,
                String cantidad,
                String unidadMedida) {

            this.material = material;
            this.cantidad = cantidad;
            this.unidadMedida = unidadMedida;
        }

        public String getMaterial() {
            return material;
        }

        public String getCantidad() {
            return cantidad;
        }

        public String getUnidadMedida() {
            return unidadMedida;
        }
    }
}
