import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;
import java.nio.file.*;
import java.util.concurrent.*;
public class FxmlCheck {
 public static void main(String[] args) throws Exception {
  System.setProperty("panaderia.db",args[0]);
  var done=new CountDownLatch(1);var failures=new java.util.concurrent.atomic.AtomicInteger();
  Platform.startup(()->{});Platform.setImplicitExit(false);
  var watcher=Executors.newSingleThreadScheduledExecutor();
  watcher.scheduleAtFixedRate(()->Platform.runLater(()->{
   for(var w:java.util.List.copyOf(Window.getWindows())) if(w.isShowing()) {System.err.println("UNEXPECTED DIALOG: "+w.getScene().getRoot());failures.incrementAndGet();w.hide();}
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
      }new Scene(root,1280,720);root.applyCss();root.layout();
      System.out.println("FXML OK: "+path.getFileName());
     }catch(Throwable e){failures.incrementAndGet();System.err.println("FXML FAIL: "+path);e.printStackTrace();}
    }
   }catch(Exception e){failures.incrementAndGet();e.printStackTrace();}finally{done.countDown();}
  });
  if(!done.await(90,TimeUnit.SECONDS)) failures.incrementAndGet();
  watcher.shutdownNow();Platform.exit();System.out.println("FXML FAILURES: "+failures.get());System.exit(failures.get()==0?0:1);
 }
}
