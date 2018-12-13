package com.github.ixtf.japp.image;

import org.apache.commons.lang3.Validate;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

/**
 * @author jzb 2018-08-17
 */
public final class Jimage {
    public static final BufferedImage resize(File file, int newWidth, float quality) throws IOException {
        Validate.inclusiveBetween(0, 1, quality);
        final ImageIcon ii = new ImageIcon(file.getCanonicalPath());
        final Image i = ii.getImage();
        Image resizedImage = null;

        final int iWidth = i.getWidth(null);
        final int iHeight = i.getHeight(null);
        if (iWidth > iHeight) {
            resizedImage = i.getScaledInstance(newWidth, (newWidth * iHeight) / iWidth, Image.SCALE_SMOOTH);
        } else {
            resizedImage = i.getScaledInstance((newWidth * iWidth) / iHeight, newWidth, Image.SCALE_SMOOTH);
        }

        // This code ensures that all the pixels in the image are loaded.
        final Image temp = new ImageIcon(resizedImage).getImage();

        // Create the buffered image.
        BufferedImage bufferedImage = new BufferedImage(temp.getWidth(null), temp.getHeight(null), BufferedImage.TYPE_INT_RGB);
        // Copy image to buffered image.
        final Graphics g = bufferedImage.createGraphics();

        // Clear background and paint the image.
        g.setColor(Color.white);
        g.fillRect(0, 0, temp.getWidth(null), temp.getHeight(null));
        g.drawImage(temp, 0, 0, null);
        g.dispose();

        // Soften.
        final float softenFactor = 0.05f;
        final float[] softenArray = {0, softenFactor, 0, softenFactor,
                1 - (softenFactor * 4), softenFactor, 0, softenFactor, 0};
        final Kernel kernel = new Kernel(3, 3, softenArray);
        final ConvolveOp cOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        bufferedImage = cOp.filter(bufferedImage, null);
        return bufferedImage;
    }

    public static final BufferedImage base64(String s) throws IOException {
        String base64Image = s.split(",")[1];
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        return ImageIO.read(new ByteArrayInputStream(imageBytes));
    }
}
