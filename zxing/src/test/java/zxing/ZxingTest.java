package zxing;

import com.google.common.collect.Maps;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author jzb 2018-08-18
 */
public class ZxingTest {
    public static void main(String[] args) throws Exception {
        final String code = "6000P123456";
        final int width = 500;
        final int height = 200;
        final int fontSize = 16;

        //配置参数
        Map<EncodeHintType, Object> hints = Maps.newHashMap();
        hints.put(EncodeHintType.CHARACTER_SET, UTF_8);
        // 容错级别 这里选择最高H级别
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);//设置二维码边的空度，非负数

        MultiFormatWriter writer = new MultiFormatWriter();
        // 图像数据转换，使用了矩阵转换 参数顺序分别为：编码内容，编码类型，生成图片宽度，生成图片高度，设置参数
        final BitMatrix matrix = writer.encode(code, BarcodeFormat.CODE_128, width, height, hints);

        // Create buffered image to draw to
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Iterate through the matrix and draw the pixels to the image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int grayValue = (matrix.get(x, y) ? 0 : 1) & 0xff;
                image.setRGB(x, y, (grayValue == 0 ? 0 : 0xFFFFFF));
            }
        }
        Graphics graphics = image.getGraphics();
        graphics.drawImage(image, 0, 0, null);

        Font f = new Font("Arial", Font.PLAIN, fontSize);
        FontRenderContext frc = image.getGraphics().getFontMetrics().getFontRenderContext();
        Rectangle2D rect = f.getStringBounds(code, frc);
        graphics.setColor(Color.WHITE);
        //add 10 pixels to width to get 5 pixels of padding in left/right
        //add 6 pixels to height to get 3 pixels of padding in top/bottom
        graphics.fillRect(
                0,
                (int) Math.ceil(image.getHeight() - (rect.getHeight() + 6)),
                width,height);
        // add the watermark text
        graphics.setFont(f);
        graphics.setColor(Color.BLACK);
        graphics.drawString(code,
                (int) Math.ceil((image.getWidth() / 2) - ((rect.getWidth()) / 2)),
                (int) Math.ceil(image.getHeight() - 6));
        graphics.dispose();

        ImageIO.write(image, "png", Paths.get("/home/jzb/zxing.png").toFile());

//        MatrixToImageWriter.writeToPath(bitMatrix, "png", Paths.get("/home/jzb/zxing.png"));
    }
}
