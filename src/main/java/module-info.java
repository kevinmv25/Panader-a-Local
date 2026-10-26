module com.mycompany.panaderia.local {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires java.base;

    opens com.mycompany.panaderia.local to javafx.fxml;
    opens com.mycompany.panaderia.local.Controllers to javafx.fxml;
    opens com.panaderialocal.conexion to javafx.fxml;

    exports com.mycompany.panaderia.local;
    exports com.panaderialocal.conexion;
}