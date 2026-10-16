module com.mycompany.panaderia.local {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.mycompany.panaderia.local to javafx.fxml;
    exports com.mycompany.panaderia.local;

    opens com.panaderialocal.conexion to javafx.fxml;
    exports com.panaderialocal.conexion;
}
