package com.panaderialocal.conexion;
import java.sql.*;
public final class EsquemaBD {
 public static synchronized void actualizar(Connection c) throws SQLException {
  c.setAutoCommit(false);
  try(Statement s=c.createStatement()) {
   if(!columna(c,"detalle_pedido","precio_unitario")) {
    s.execute("ALTER TABLE detalle_pedido ADD COLUMN precio_unitario REAL");
    // Los pedidos antiguos no guardaban precio. Conservar el total y la proporcion del catalogo disponible.
    s.execute("UPDATE detalle_pedido SET precio_unitario=COALESCE((SELECT p.total * (SELECT precio FROM panes WHERE id_pan=detalle_pedido.id_pan) / NULLIF((SELECT SUM(d.cantidad*pa.precio) FROM detalle_pedido d JOIN panes pa ON pa.id_pan=d.id_pan WHERE d.id_pedido=p.id_pedido),0) FROM pedidos p WHERE p.id_pedido=detalle_pedido.id_pedido),0)");
   }
   if(!columna(c,"ventas","referencia_pago")) s.execute("ALTER TABLE ventas ADD COLUMN referencia_pago TEXT");
   if(!columna(c,"ventas","efectivo_recibido")) s.execute("ALTER TABLE ventas ADD COLUMN efectivo_recibido REAL");
   if(!columna(c,"pedidos","fecha_completado")) s.execute("ALTER TABLE pedidos ADD COLUMN fecha_completado TEXT");
   s.execute("CREATE TRIGGER IF NOT EXISTS trg_pedido_completado AFTER UPDATE OF estado ON pedidos WHEN NEW.estado IN ('Completado','Entregado') AND OLD.estado NOT IN ('Completado','Entregado') AND NEW.fecha_completado IS NULL BEGIN UPDATE pedidos SET fecha_completado=CURRENT_TIMESTAMP WHERE id_pedido=NEW.id_pedido; END");
   c.commit();
  } catch(SQLException e) { c.rollback(); throw e; } finally { c.setAutoCommit(true); }
 }
 private static boolean columna(Connection c,String tabla,String nombre) throws SQLException {
  try(Statement s=c.createStatement();ResultSet rs=s.executeQuery("PRAGMA table_info("+tabla+")")) {
   while(rs.next()) if(nombre.equals(rs.getString("name"))) return true; return false;
  }
 }
}
