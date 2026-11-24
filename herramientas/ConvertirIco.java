import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Convierte un PNG en un icono .ico de Windows con varios tamanos.
 *
 * Uso (con el JDK, sin compilar):
 *     java ConvertirIco.java logo.png logo.ico
 *
 * - Si el logo no es cuadrado, se centra sobre fondo transparente (no se deforma).
 * - Tamanos: 16, 24, 32, 48, 64 y 256 px.
 */
public class ConvertirIco {

    private static final int[] TAMANOS = {16, 24, 32, 48, 64, 256};

    public static void main(String[] args) throws Exception {

        System.setProperty("java.awt.headless", "true");

        if (args.length != 2) {
            System.out.println("Uso: java ConvertirIco.java logo.png salida.ico");
            System.exit(1);
        }

        BufferedImage origen = ImageIO.read(new File(args[0]));

        if (origen == null) {
            System.out.println("ERROR: no se pudo leer la imagen: " + args[0]);
            System.exit(1);
        }

        List<byte[]> imagenes = new ArrayList<>();

        for (int tamano : TAMANOS) {
            BufferedImage cuadro = ajustarACuadro(origen, tamano);
            imagenes.add(tamano >= 256 ? comoPng(cuadro) : comoBmp(cuadro));
        }

        escribirIco(new File(args[1]), imagenes);

        System.out.println("Icono creado: " + args[1]
                + " (" + origen.getWidth() + "x" + origen.getHeight() + " -> "
                + TAMANOS.length + " tamanos)");
    }

    /** Encaja la imagen dentro de un cuadro transparente sin deformarla. */
    private static BufferedImage ajustarACuadro(BufferedImage origen, int lado) {

        double escala = Math.min((double) lado / origen.getWidth(),
                                 (double) lado / origen.getHeight());

        int ancho = Math.max(1, (int) Math.round(origen.getWidth() * escala));
        int alto  = Math.max(1, (int) Math.round(origen.getHeight() * escala));

        BufferedImage reducida = escalar(origen, ancho, alto);

        BufferedImage cuadro = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = cuadro.createGraphics();
        g.drawImage(reducida, (lado - ancho) / 2, (lado - alto) / 2, null);
        g.dispose();
        return cuadro;
    }

    /** Reduce por mitades y al final al tamano exacto, para que quede nitido. */
    private static BufferedImage escalar(BufferedImage origen, int ancho, int alto) {

        BufferedImage actual = aArgb(origen);

        while (actual.getWidth() / 2 >= ancho && actual.getHeight() / 2 >= alto) {
            actual = redimensionar(actual, actual.getWidth() / 2, actual.getHeight() / 2);
        }

        return redimensionar(actual, ancho, alto);
    }

    private static BufferedImage redimensionar(BufferedImage origen, int ancho, int alto) {
        BufferedImage destino = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(origen, 0, 0, ancho, alto, null);
        g.dispose();
        return destino;
    }

    private static BufferedImage aArgb(BufferedImage origen) {
        if (origen.getType() == BufferedImage.TYPE_INT_ARGB) {
            return origen;
        }
        BufferedImage copia = new BufferedImage(origen.getWidth(), origen.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copia.createGraphics();
        g.drawImage(origen, 0, 0, null);
        g.dispose();
        return copia;
    }

    private static byte[] comoPng(BufferedImage imagen) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    /** Imagen DIB de 32 bits (BGRA, de abajo hacia arriba) + mascara AND vacia. */
    private static byte[] comoBmp(BufferedImage imagen) {

        int n = imagen.getWidth();
        int bytesMascaraFila = ((n + 31) / 32) * 4;

        ByteBuffer b = ByteBuffer.allocate(40 + n * n * 4 + bytesMascaraFila * n)
                .order(ByteOrder.LITTLE_ENDIAN);

        b.putInt(40);          // tamano del encabezado
        b.putInt(n);           // ancho
        b.putInt(n * 2);       // alto (imagen + mascara)
        b.putShort((short) 1); // planos
        b.putShort((short) 32);// bits por pixel
        b.putInt(0);           // sin compresion
        b.putInt(n * n * 4);   // tamano de los pixeles
        b.putInt(0);
        b.putInt(0);
        b.putInt(0);
        b.putInt(0);

        for (int y = n - 1; y >= 0; y--) {
            for (int x = 0; x < n; x++) {
                int argb = imagen.getRGB(x, y);
                b.put((byte) (argb & 0xFF));          // B
                b.put((byte) ((argb >> 8) & 0xFF));   // G
                b.put((byte) ((argb >> 16) & 0xFF));  // R
                b.put((byte) ((argb >> 24) & 0xFF));  // A
            }
        }

        // mascara AND: ceros (la transparencia real va en el canal alfa)
        return b.array();
    }

    private static void escribirIco(File destino, List<byte[]> imagenes) throws IOException {

        int cantidad = imagenes.size();
        int offset = 6 + 16 * cantidad;

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ByteBuffer cabecera = ByteBuffer.allocate(offset).order(ByteOrder.LITTLE_ENDIAN);

        cabecera.putShort((short) 0);          // reservado
        cabecera.putShort((short) 1);          // tipo: icono
        cabecera.putShort((short) cantidad);

        for (int i = 0; i < cantidad; i++) {
            int lado = TAMANOS[i];
            byte[] datos = imagenes.get(i);

            cabecera.put((byte) (lado >= 256 ? 0 : lado));  // ancho (0 = 256)
            cabecera.put((byte) (lado >= 256 ? 0 : lado));  // alto
            cabecera.put((byte) 0);                         // paleta
            cabecera.put((byte) 0);                         // reservado
            cabecera.putShort((short) 1);                   // planos
            cabecera.putShort((short) 32);                  // bits por pixel
            cabecera.putInt(datos.length);
            cabecera.putInt(offset);

            offset += datos.length;
        }

        salida.write(cabecera.array());
        for (byte[] datos : imagenes) {
            salida.write(datos);
        }

        File carpeta = destino.getAbsoluteFile().getParentFile();
        if (carpeta != null) {
            carpeta.mkdirs();
        }
        Files.write(destino.toPath(), salida.toByteArray());
    }
}
