package com.panaderialocal.conexion;
import java.sql.*;
import java.math.*;
import java.util.*;
import com.panaderialocal.conexion.RegistroPedidosDAO.DetallePedido;
public class VentasDAO {
 public static BigDecimal importe(double n) throws SQLException {
  if(!Double.isFinite(n)||n<=0) throw new SQLException("Precio invalido.");
  try { return BigDecimal.valueOf(n).setScale(2,RoundingMode.UNNECESSARY); }
  catch(ArithmeticException e) { throw new SQLException("Usa hasta dos decimales."); }
 }
 public static BigDecimal total(List<DetallePedido> lineas) throws SQLException {
  if(lineas==null||lineas.isEmpty()) throw new SQLException("Agrega al menos un producto.");
  BigDecimal t=BigDecimal.ZERO; Set<Integer> ids=new HashSet<>();
  for(DetallePedido d:lineas) {
   if(d==null||d.getCantidad()<1||d.getCantidad()>9999||!ids.add(d.getIdPan())) throw new SQLException("Cantidad o producto invalido/repetido.");
   if(!Double.isFinite(d.getPrecioUnitario())||d.getPrecioUnitario()<=0) throw new SQLException("Precio invalido.");
   t=t.add(BigDecimal.valueOf(d.getPrecioUnitario()).multiply(BigDecimal.valueOf(d.getCantidad())));
  } return t.setScale(2,RoundingMode.HALF_UP);
 }
 public int registrarVenta(List<DetallePedido> lineas,String metodo,BigDecimal recibido,String referencia) throws SQLException {
  BigDecimal t=total(lineas);
  if(metodo==null||!List.of("Efectivo","Tarjeta","Transferencia").contains(metodo)) throw new SQLException("Selecciona una forma de pago.");
  if("Efectivo".equals(metodo)) {
   if(recibido==null||recibido.scale()>2||recibido.compareTo(t)<0) throw new SQLException("El efectivo debe cubrir el total, con hasta dos decimales.");
  } else if(referencia==null||referencia.isBlank()) throw new SQLException("Indica la referencia del pago confirmado.");
  try(Connection c=ConexionBD.conectar()) {
   c.setAutoCommit(false);
   try {
    for(DetallePedido d:lineas) try(PreparedStatement ps=c.prepareStatement("SELECT precio FROM panes WHERE id_pan=?")) {
     ps.setInt(1,d.getIdPan());try(ResultSet rs=ps.executeQuery()) {
      if(!rs.next()||importe(rs.getDouble(1)).compareTo(importe(d.getPrecioUnitario()))!=0) throw new SQLException("El catalogo cambio. Recarga la caja antes de cobrar.");
     }
    }
    try(PreparedStatement ps=c.prepareStatement("INSERT INTO ventas(total,metodo_pago,efectivo_recibido,referencia_pago) VALUES(?,?,?,?)")) {
     ps.setDouble(1,t.doubleValue());ps.setString(2,metodo);
     if("Efectivo".equals(metodo)) ps.setDouble(3,recibido.doubleValue());else ps.setNull(3,Types.REAL);
     ps.setString(4,"Efectivo".equals(metodo)?null:referencia.trim());ps.executeUpdate();
    }
    int id;try(Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT last_insert_rowid()")) { rs.next();id=rs.getInt(1); }
    try(PreparedStatement ps=c.prepareStatement("INSERT INTO detalle_venta(id_venta,id_pan,cantidad,subtotal) VALUES(?,?,?,?)")) {
     for(DetallePedido d:lineas) {ps.setInt(1,id);ps.setInt(2,d.getIdPan());ps.setInt(3,d.getCantidad());ps.setDouble(4,importe(d.getPrecioUnitario()).multiply(BigDecimal.valueOf(d.getCantidad())).doubleValue());ps.addBatch();} ps.executeBatch();
    }
    c.commit();return id;
   } catch(SQLException|RuntimeException e) {c.rollback();throw e;}
  }
 }
}
