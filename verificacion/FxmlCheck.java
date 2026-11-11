import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;
import java.nio.file.*;
import java.util.concurrent.*;
public class FxmlCheck {
 static Boolean confirmarEliminacion;
 public static void main(String[] args) throws Exception {
  System.setProperty("panaderia.db",args[0]);
  var done=new CountDownLatch(1);var failures=new java.util.concurrent.atomic.AtomicInteger();
  Platform.startup(()->{});Platform.setImplicitExit(false);
  var watcher=Executors.newSingleThreadScheduledExecutor();
  watcher.scheduleAtFixedRate(()->Platform.runLater(()->{
   for(var w:java.util.List.copyOf(Window.getWindows())) if(w.isShowing()) {
    if(confirmarEliminacion!=null && w.getScene().getRoot() instanceof javafx.scene.control.DialogPane dialog && dialog.getButtonTypes().stream().anyMatch(t->t.getText().equals("Eliminar pan"))) {
     boolean confirmar=confirmarEliminacion; confirmarEliminacion=null;
     var tipo=dialog.getButtonTypes().stream().filter(t->confirmar ? t.getButtonData()==javafx.scene.control.ButtonBar.ButtonData.OK_DONE : t==javafx.scene.control.ButtonType.CANCEL).findFirst().orElseThrow();
     ((javafx.scene.control.Button)dialog.lookupButton(tipo)).fire();continue;
    }
    System.err.println("UNEXPECTED DIALOG: "+w.getScene().getRoot());failures.incrementAndGet();w.hide();}
  }),1,1,TimeUnit.SECONDS);
  Platform.runLater(()->{
   try(var paths=Files.walk(Path.of(args[1]))) {
    for(Path path:paths.filter(p->p.toString().endsWith(".fxml")).toList()) {
     try {
      FXMLLoader loader=new FXMLLoader(path.toUri().toURL());
      Parent root=loader.load();
      if(loader.getController() instanceof com.mycompany.panaderia.local.CajeroController caja) {
       var agregar=caja.getClass().getDeclaredMethod("cambiarCantidad",com.mycompany.panaderia.local.Pan.class,int.class);agregar.setAccessible(true);
       agregar.invoke(caja,new com.mycompany.panaderia.local.Pan(100001,"A","",2.5,""),3);
       agregar.invoke(caja,new com.mycompany.panaderia.local.Pan(100002,"B","",2.5,""),2);
       var campo=caja.getClass().getDeclaredField("productosPedido");campo.setAccessible(true);
       var grupos=(java.util.List<?>)campo.get(caja);
       if(grupos.size()!=2)throw new AssertionError("Mezcla panes distintos de igual precio");
       var cantidad=grupos.get(0).getClass().getDeclaredMethod("getCantidad");cantidad.setAccessible(true);
       if(!cantidad.invoke(grupos.get(0)).equals(3))throw new AssertionError("Cantidad por producto incorrecta");
       var vaciar=caja.getClass().getDeclaredMethod("vaciarCuenta");vaciar.setAccessible(true);vaciar.invoke(caja);
       if(!grupos.isEmpty())throw new AssertionError("No vacia la cuenta");
       System.out.println("CAJA OK: separa productos de igual precio y vacia la cuenta");
      }
      if(loader.getController() instanceof com.mycompany.panaderia.local.Controllers.FormularioPanController form) {
       var stage=new javafx.stage.Stage();stage.setScene(new Scene(root));
       var ns=loader.getNamespace();var delete=(javafx.scene.control.Button)ns.get("btnEliminar");
       form.configurar(null,()->{});
       if(delete.isVisible() || delete.isManaged())throw new AssertionError("Eliminar visible al registrar");
       var dao=new panaderialocal.conexion.PanDAO();dao.insertarPan("FORM TEST","",9,"");
       var pan=dao.obtenerPanes().stream().filter(p->p.getNombre().equals("FORM TEST")).findFirst().orElseThrow();
       var refresh=new java.util.concurrent.atomic.AtomicInteger();form.configurar(pan,refresh::incrementAndGet);
       if(!delete.isVisible())throw new AssertionError("Falta eliminar al modificar");
       ((javafx.scene.control.TextField)ns.get("txtNombre")).setText("FORM EDITADO");
       ((javafx.scene.control.TextField)ns.get("txtPrecio")).setText("14.50");
       ((javafx.scene.control.Button)ns.get("btnGuardar")).fire();
       var editado=dao.obtenerPanes().stream().filter(p->p.getIdPan()==pan.getIdPan()).findFirst().orElseThrow();
       if(!editado.getNombre().equals("FORM EDITADO") || editado.getPrecio()!=14.5 || refresh.get()!=1)throw new AssertionError("Formulario no guarda/refresca");
       for(boolean confirmar:new boolean[]{false,true}) {
        confirmarEliminacion=confirmar;
        delete.fire();
        boolean existe=dao.obtenerPanes().stream().anyMatch(p->p.getIdPan()==pan.getIdPan());
        if(existe==confirmar)throw new AssertionError("Confirmacion de eliminar incorrecta");
       }
       if(refresh.get()!=2)throw new AssertionError("No refresca tras eliminar");
       stage.close();System.out.println("FORMULARIO OK: edicion persistida, cancelar conserva y confirmar elimina en SQLite");
      }
      if(root.getScene()==null)new Scene(root,1280,720);root.applyCss();root.layout();
      System.out.println("FXML OK: "+path.getFileName());
     }catch(Throwable e){failures.incrementAndGet();System.err.println("FXML FAIL: "+path);e.printStackTrace();}
    }
   }catch(Exception e){failures.incrementAndGet();e.printStackTrace();}finally{done.countDown();}
  });
  if(!done.await(90,TimeUnit.SECONDS)) failures.incrementAndGet();
  watcher.shutdownNow();Platform.exit();System.out.println("FXML FAILURES: "+failures.get());System.exit(failures.get()==0?0:1);
 }
}
