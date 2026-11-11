import java.sql.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.nio.file.*;
import com.panaderialocal.conexion.*;
import com.panaderialocal.conexion.RegistroPedidosDAO.*;
import panaderialocal.conexion.PanDAO;
public class IntegrationCheck {
 static int checks;
 static void ok(boolean b,String message) {if(!b)throw new AssertionError(message);System.out.println("OK "+(++checks)+": "+message);}
 interface Action {void run() throws Exception;}
 static void reject(Action a,String message)throws Exception {try{a.run();}catch(SQLException|IllegalArgumentException e){ok(true,message);return;}throw new AssertionError(message);}
 static double scalar(String sql)throws Exception {try(Connection c=ConexionBD.conectar();Statement s=c.createStatement();ResultSet rs=s.executeQuery(sql)){rs.next();return rs.getDouble(1);}}
 public static void main(String[] args)throws Exception {
  if(Path.of(args[0]).getFileName().toString().equalsIgnoreCase("panaderia.db")) throw new IllegalArgumentException("Usa una copia de pruebas.");
  System.setProperty("panaderia.db",args[0]);
  var panes=new PanDAO();var dao=new RegistroPedidosDAO();var ventas=new VentasDAO();
  panes.insertarPan("PRUEBA A","original",2.5,"");panes.insertarPan("PRUEBA B","original",2.5,"");
  int a=(int)scalar("SELECT max(id_pan)-1 FROM panes"), b=a+1;
  var lineas=List.of(new DetallePedido(a,"A",3,2.5),new DetallePedido(b,"B",2,2.5));
  ok(VentasDAO.total(lineas).compareTo(new BigDecimal("12.50"))==0,"total de cinco piezas al mismo precio");
  int id=ventas.registrarVenta(lineas,"Efectivo",new BigDecimal("20.00"),null);
  ok(scalar("SELECT total FROM ventas WHERE id_venta="+id)==12.5,"venta efectivo persistida");
  ok(scalar("SELECT SUM(cantidad) FROM detalle_venta WHERE id_venta="+id)==5,"detalle conserva productos y cantidades");
  ventas.registrarVenta(lineas,"Tarjeta",null,"AUT-123");ventas.registrarVenta(lineas,"Transferencia",null,"TR-456");
  ok(scalar("SELECT COUNT(*) FROM ventas WHERE referencia_pago IN ('AUT-123','TR-456')")==2,"tarjeta y transferencia con referencia");
  double count=scalar("SELECT COUNT(*) FROM ventas");
  reject(()->ventas.registrarVenta(lineas,"Efectivo",BigDecimal.ONE,null),"rechaza efectivo insuficiente");
  reject(()->ventas.registrarVenta(lineas,"Tarjeta",null,""),"rechaza referencia vacia");
  reject(()->ventas.registrarVenta(List.of(),"Efectivo",BigDecimal.TEN,null),"rechaza venta vacia");
  reject(()->ventas.registrarVenta(List.of(new DetallePedido(999999,"inexistente",1,2.5)),"Efectivo",BigDecimal.TEN,null),"rechaza pan inexistente");
  ok(scalar("SELECT COUNT(*) FROM ventas")==count,"ventas fallidas no dejan registros parciales");
  dao.registrarPedido("PRUEBA CLIENTE","",LocalDate.now().plusDays(1),"Pendiente","nota",999,lineas);
  int pedido=(int)scalar("SELECT max(id_pedido) FROM pedidos");
  ok(dao.obtenerPedido(pedido).getTotal()==12.5,"pedido recalcula total en base de datos");
  panes.actualizarPan(a,"PRUEBA A EDITADO","nueva descripcion",3.75,"foto.png");
  ok(dao.obtenerDetallesPedido(pedido).get(0).getPrecioUnitario()==2.5,"cambiar pan conserva precio del pedido");
  ok(new DevolucionDAO().obtenerLineas(new DevolucionDAO.OrigenDevolucion(false,id,"prueba",new BigDecimal("12.50"))).get(0).getPrecioUnitario()==2.5,"cambiar pan conserva precio de venta");
  ok(new HistorialPedidosDAO().obtenerItems(pedido).get(0).getPrecioUnitario()==2.5,"historial nuevo conserva el precio original del pedido");
  reject(()->ventas.registrarVenta(lineas,"Efectivo",new BigDecimal("20.00"),null),"rechaza precio de caja desactualizado");
  dao.editarPedido(pedido,List.of(new DetallePedido(a,"A",4,2.5)),LocalDate.now().plusDays(2),"editado");
  ok(dao.obtenerPedido(pedido).getTotal()==10 && dao.obtenerDetallesPedido(pedido).size()==1,"edicion persiste lineas y total");
  var historico=new HistorialPedidosDAO().obtenerPedidos().stream().filter(p->p.getIdPedido()==pedido).findFirst().orElseThrow();
  ok(historico.getTotal()==10 && historico.getArticulos()==4 && historico.getConceptos()==1 && historico.getFechaEntrega().equals(LocalDate.now().plusDays(2).toString()) && historico.getObservaciones().equals("editado"),"historial refleja cantidades, total, fecha y notas editadas");
  ok(new HistorialPedidosDAO().obtenerItems(pedido).size()==1 && new HistorialPedidosDAO().obtenerItems(pedido).get(0).getSubtotal()==10,"historial elimina lineas retiradas y recalcula subtotal");
  var panActual=dao.obtenerPanes().stream().filter(p->p.getIdPan()==a).findFirst().orElseThrow();
  ok(panActual.getNombre().equals("PRUEBA A EDITADO") && panActual.getPrecio()==3.75,"registro de pedidos usa nombre y precio actualizados");
  ok(new com.panaderialocal.conexion.DAO.ProduccionDAO().obtenerPanes().stream().anyMatch(p->p[0].equals(String.valueOf(a)) && p[1].equals("PRUEBA A EDITADO")),"produccion usa el nombre actualizado del pan");
  int nuevaVenta=ventas.registrarVenta(List.of(new DetallePedido(panActual,2)),"Efectivo",new BigDecimal("10"),null);
  ok(scalar("SELECT total FROM ventas WHERE id_venta="+nuevaVenta)==7.5,"nueva venta aplica precio modificado");
  double ingresosAntes=new ResumenVentasDAO().obtenerTotales(LocalDate.now().minusDays(30),LocalDate.now().plusDays(1)).getTotal();
  dao.actualizarAdelanto(pedido,8);
  reject(()->dao.editarPedido(pedido,List.of(new DetallePedido(a,"A",1,2.5)),LocalDate.now(),"no"),"rechaza total menor al adelanto");
  ok(dao.obtenerPedido(pedido).getTotal()==10,"edicion fallida conserva pedido");
  reject(()->dao.editarPedido(pedido,List.of(new DetallePedido(999999,"X",10,2.5)),LocalDate.now(),"no"),"rollback ante referencia invalida");
  ok(dao.obtenerDetallesPedido(pedido).get(0).getCantidad()==4,"rollback conserva detalles originales");
  dao.cancelarPedido(pedido,"cliente cancelo");
  ok(dao.obtenerPedido(pedido).getEstado().equals("Cancelado"),"cancelacion persistida");
  var cancelado=new HistorialPedidosDAO().obtenerPedidos().stream().filter(p->p.getIdPedido()==pedido).findFirst().orElseThrow();
  ok(cancelado.getEstado().equals("Cancelado") && cancelado.getAdelanto()==8 && cancelado.getObservaciones().contains("cliente cancelo"),"cancelacion refleja estado, adelanto y motivo en historial");
  ok(new ResumenVentasDAO().obtenerTotales(LocalDate.now().minusDays(30),LocalDate.now().plusDays(1)).getTotal()==ingresosAntes,"pedido cancelado no suma ingresos de venta");
  ok(new DevolucionDAO().obtenerOrigenes().stream().noneMatch(o->o.esPedido() && o.getId()==pedido),"pedido cancelado no aparece como pedido disponible en devoluciones");
  reject(()->dao.actualizarAdelanto(pedido,1),"no permite cambiar adelanto de cancelados");
  reject(()->dao.editarPedido(pedido,lineas,LocalDate.now(),""),"no permite editar cancelados");
  reject(()->dao.completarPedido(pedido,""),"no completa cancelados");
  dao.registrarPedido("PRUEBA CLIENTE","",LocalDate.now(),"Pendiente","",12.5,lineas);
  int completo=(int)scalar("SELECT max(id_pedido) FROM pedidos");dao.completarPedido(completo,"");
  ok(scalar("SELECT COUNT(*) FROM pedidos WHERE fecha_completado IS NOT NULL AND id_pedido="+completo)==1,"fecha de completado registrada sin abrir reportes");
  var resumen=new ResumenVentasDAO();LocalDate desde=LocalDate.now().minusDays(30),hasta=LocalDate.now().plusDays(1);
  resumen.obtenerTotales(desde,hasta);resumen.totalesPorMetodo(desde,hasta);resumen.ventasPorHora(desde,hasta);resumen.ventasPorDia(desde,hasta);resumen.ultimasVentas(desde,hasta,null,50);resumen.productosTop(desde,hasta,50);
  new InventarioDAO().obtenerMateriales();var produccion=new com.panaderialocal.conexion.DAO.ProduccionDAO();produccion.obtenerPanes();produccion.obtenerResumen(LocalDate.now());produccion.obtenerHistorial(desde,hasta);
  new DevolucionDAO().registrarDevolucion(new DevolucionDAO.OrigenDevolucion(false,id,"prueba",new BigDecimal("12.50")), List.of(new DevolucionDAO.LineaDevuelta(a,"A",1,new BigDecimal("2.50"))),"prueba");new DevolucionDAO().obtenerOrigenes();
  ok(true,"consultas de reportes, materiales, produccion y devoluciones");
  try(Connection c=ConexionBD.conectar();Statement st=c.createStatement()) {
   try(ResultSet rs=st.executeQuery("PRAGMA integrity_check")){ok(rs.next()&&rs.getString(1).equals("ok"),"integridad de SQLite");}
   try(ResultSet rs=st.executeQuery("PRAGMA foreign_key_check")){ok(!rs.next(),"claves foraneas sin errores");}
  }
  ok(scalar("SELECT COUNT(*) FROM panes WHERE id_pan="+a+" AND nombre='PRUEBA A EDITADO' AND descripcion='nueva descripcion' AND precio=3.75 AND foto='foto.png'")==1,"edicion persiste todos los campos al volver a consultar SQLite");
  reject(()->panes.eliminarPan(a),"impide eliminar pan con historial");
  ok(scalar("SELECT COUNT(*) FROM panes WHERE id_pan="+a)==1,"rechazo conserva el pan y sus referencias");
  panes.insertarPan("BORRABLE","prueba",12,"");
  int borrable=(int)scalar("SELECT max(id_pan) FROM panes");
  ok(panes.eliminarPan(borrable),"elimina pan sin referencias");
  ok(scalar("SELECT COUNT(*) FROM panes WHERE id_pan="+borrable)==0,"eliminacion persistida en SQLite");
  ok(!panes.eliminarPan(borrable),"detecta pan previamente eliminado");
  reject(()->panes.actualizarPan(a,"","",1, ""),"rechaza nombre vacio");
 }
}

