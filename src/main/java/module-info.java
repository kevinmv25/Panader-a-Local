module com.mycompany.panaderia.local {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.mycompany.panaderia.local to javafx.fxml;
    exports com.mycompany.panaderia.local;
}
