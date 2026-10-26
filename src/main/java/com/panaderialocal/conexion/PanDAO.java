
package panaderialocal.conexion;

import com.mycompany.panaderia.local.Pan;
import com.panaderialocal.conexion.ConexionBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PanDAO {

    public List<Pan> obtenerPanes() {
        List<Pan> panes = new ArrayList<>();
        String sql = "SELECT * FROM panes";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                panes.add(new Pan(
                    rs.getInt("id_pan"),
                    rs.getString("nombre"),
                    rs.getString("descripcion"),
                    rs.getDouble("precio"),
                    rs.getString("foto")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar panes", e);
        }

        return panes;
    }

    // registar pan 
    public boolean insertarPan(String nombre, String descripcion,
                               double precio, String foto) throws SQLException {

        String sql = "INSERT INTO panes (nombre, descripcion, precio, foto) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, descripcion);
            ps.setDouble(3, precio);
            ps.setString(4, foto);

            return ps.executeUpdate() > 0;
        }
    }

    // modificar unn pan 
    public boolean actualizarPan(int id, String nombre, String descripcion,
                                 double precio, String foto) throws SQLException {

        String sql = "UPDATE panes SET nombre=?, descripcion=?, "
                   + "precio=?, foto=? WHERE id_pan=?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, descripcion);
            ps.setDouble(3, precio);
            ps.setString(4, foto);
            ps.setInt(5, id);

            return ps.executeUpdate() > 0;
        }
    }
}
