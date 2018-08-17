package com.github.ixtf.japp.zxing;

import com.google.common.collect.Maps;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author jzb 2018-08-17
 */
public final class Jzxing {
    public final static BufferedImage toImage(final BitMatrix bitMatrix) {
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    public final static BitMatrix code128(final String content, final int widthPix, final int heightPix) throws WriterException {
        //配置参数
        Map<EncodeHintType, Object> hints = Maps.newHashMap();
        hints.put(EncodeHintType.CHARACTER_SET, UTF_8);
        // 容错级别 这里选择最高H级别
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

        MultiFormatWriter writer = new MultiFormatWriter();
        return writer.encode(content, BarcodeFormat.CODE_128, widthPix, heightPix, hints);
    }

    public final static BufferedImage code128Image(final String content, final int widthPix, final int heightPix) throws WriterException {
        return toImage(code128(content, widthPix, heightPix));
    }

    public final static BitMatrix qrCode(final String content, final int widthPix, final int heightPix) throws WriterException {
        //配置参数
        Map<EncodeHintType, Object> hints = Maps.newHashMap();
        hints.put(EncodeHintType.CHARACTER_SET, UTF_8);
        // 容错级别 这里选择最高H级别
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);//设置二维码边的空度，非负数
        MultiFormatWriter writer = new MultiFormatWriter();
        // 图像数据转换，使用了矩阵转换 参数顺序分别为：编码内容，编码类型，生成图片宽度，生成图片高度，设置参数
        return writer.encode(content, BarcodeFormat.QR_CODE, widthPix, heightPix, hints);
    }

    public final static BufferedImage qrCodeImage(final String content, final int widthPix, final int heightPix) throws WriterException {
        return toImage(qrCode(content, widthPix, heightPix));
    }
}
