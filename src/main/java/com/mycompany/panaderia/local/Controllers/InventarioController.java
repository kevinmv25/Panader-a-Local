package com.mycompany.panaderia.local.Controllers;

import com.panaderialocal.conexion.InventarioDAO;
import com.panaderialocal.conexion.InventarioDAO.Material;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class InventarioController implements Initializable {

    @FXML
    private TextField txtBuscarMaterial;

    @FXML
    private TableView<Material> tablaMateriales;

    @FXML
    private TableColumn<Material, String> colNombreMaterial;

    @FXML
    private TableColumn<Material, String> colUnidad;

    @FXML
    private TableColumn<Material, Number> colExistencia;

    private final ObservableList<Material> listaMateriales =
            FXCollections.observableArrayList();

    private final InventarioDAO inventarioDAO = new InventarioDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTabla();
        cargarMateriales();

        txtBuscarMaterial.textProperty().addListener(
            (observable, anterior, nuevo) -> filtrarMateriales(nuevo)
        );
    }

    private void configurarTabla() {
        colNombreMaterial.setCellValueFactory(
            dato -> new SimpleStringProperty(
                dato.getValue().getNombre()
            )
        );

        colUnidad.setCellValueFactory(
            dato -> new SimpleStringProperty(
                dato.getValue().getUnidad()
            )
        );

        colExistencia.setCellValueFactory(
            dato -> new SimpleDoubleProperty(
                dato.getValue().getExistencia()
            )
        );

        colNombreMaterial.setStyle(
            "-fx-alignment: CENTER-LEFT;"
        );

        colUnidad.setStyle(
            "-fx-alignment: CENTER;"
        );

        colExistencia.setCellFactory(columna ->
            new TableCell<Material, Number>() {
                private final Label etiqueta = new Label();

                {
                    etiqueta.setAlignment(Pos.CENTER);
                    etiqueta.setMinWidth(88);
                    etiqueta.setPrefWidth(88);
                    etiqueta.setMinHeight(38);
                    etiqueta.setStyle(
                        "-fx-background-color: #FFE7AD;"
                        + "-fx-background-radius: 22px;"
                        + "-fx-text-fill: #422817;"
                        + "-fx-font-size: 16px;"
                        + "-fx-padding: 5px 12px;"
                    );
                    setAlignment(Pos.CENTER);
                }

                @Override
                protected void updateItem(Number valor, boolean vacia) {
                    super.updateItem(valor, vacia);

                    if (vacia || valor == null) {
                        setGraphic(null);
                    } else {
                        double existencia = valor.doubleValue();

                        etiqueta.setText(
                            existencia == Math.rint(existencia)
                                ? String.valueOf((long) existencia)
                                : String.valueOf(existencia)
                        );

                        setGraphic(etiqueta);
                    }

                    setText(null);
                }
            }
        );
    }

    private void cargarMateriales() {
        try {
            listaMateriales.setAll(
                inventarioDAO.obtenerMateriales()
            );

            tablaMateriales.setItems(listaMateriales);

        } catch (SQLException e) {
            mostrarError(
                "No se pudo cargar el inventario:\n" + e.getMessage()
            );
        }
    }

    @FXML
    private void buscarMaterial() {
        filtrarMateriales(txtBuscarMaterial.getText());
    }

    private void filtrarMateriales(String texto) {
        String busqueda = texto == null
            ? ""
            : texto.trim().toLowerCase();

        ObservableList<Material> filtrados =
                FXCollections.observableArrayList();

        for (Material material : listaMateriales) {
            if (material.getNombre() != null
                    && material.getNombre()
                        .toLowerCase()
                        .contains(busqueda)) {
                filtrados.add(material);
            }
        }

        tablaMateriales.setItems(filtrados);
    }

    @FXML
    private void abrirCatalogo(ActionEvent event) {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/loginAdministrador.fxml"
        );
    }

    @FXML
    private void abrirProduccion(ActionEvent event) {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/ProduccionDiaria.fxml"
        );
    }

    @FXML
    private void abrirMenuMateriales(ActionEvent event) {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/menuMateriales.fxml"
        );
    }

    @FXML
    private void abrirInicio(ActionEvent event) {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/inicioAdmin.fxml"
        );
    }

    @FXML
    private void cerrarSesion(ActionEvent event) {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Login.fxml"
        );
    }
    
    @FXML
    private void abrirVentas(ActionEvent event) {
        abrirPantalla(
            event,
            "/com/mycompany/panaderia/local/Administrador/ventas.fxml"
        );
    }
    
    @FXML
    private void inicioAdmin(ActionEvent event) throws IOException {

        abrirPantalla(
                event,
                "/com/mycompany/panaderia/local/Administrador/inicioAdmin.fxml"
        );
    }

    private void abrirPantalla(ActionEvent event, String ruta) {

        try {
            URL recurso = getClass().getResource(ruta);

            if (recurso == null) {
                mostrarError(
                    "No se encontró el archivo: " + ruta
                );
                return;
            }

            Parent root = FXMLLoader.load(recurso);

            Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            mostrarError(
                "No se pudo abrir la pantalla:\n" + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error de inventario");
        alerta.setHeaderText("No se pudo completar la operación");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}

