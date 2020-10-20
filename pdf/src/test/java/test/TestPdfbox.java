package test;

import lombok.Cleanup;
import lombok.SneakyThrows;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

import java.util.Calendar;
import java.util.stream.IntStream;

public class TestPdfbox {
    @SneakyThrows
    public static void main(String[] args) {
        @Cleanup final var document = new PDDocument();
        IntStream.range(0, 10).forEach(it -> {
            final PDPage blankPage = new PDPage();

//            try (final PDPageContentStream cs = new PDPageContentStream(document, blankPage, AppendMode.APPEND, true, true)) {
//                String ts = "测试";
//                float fontSize = 80.0f;
//                final var r0 = new PDExtendedGraphicsState();
//                r0.setNonStrokingAlphaConstant(0.3f);
//                r0.setAlphaSourceFlag(true);
//                cs.setGraphicsStateParameters(r0);
//                cs.setNonStrokingColor(188.0f, 188.0f, 188.0f);
//                cs.beginText();
//                cs.setFont(PDType1Font.TIMES_ROMAN, fontSize);
//
//                cs.setTextMatrix(Matrix.getRotateInstance(45, 320f, 150f));
//                cs.showText(ts);
//                cs.endText();
//            } catch (Exception e) {
//            }

            document.addPage(blankPage);
        });
        final var documentInformation = document.getDocumentInformation();
        documentInformation.setAuthor("medipath");
        documentInformation.setTitle("");
        documentInformation.setCreationDate(Calendar.getInstance());
        documentInformation.setCreator("jzb");

        PDPage page = document.getPage(0);
        try (final var contentStream = new PDPageContentStream(document, page)) {
            final var pdImage = PDImageXObject.createFromFile("/data/labelme/glomerulus/K2018-0794_HE - 2018-06-11 09.07.55_id0_106x5130.jpg", document);
            contentStream.drawImage(pdImage, 70, 250);
        }

        page = document.getPage(1);
        try (final var contentStream = new PDPageContentStream(document, page)) {
            contentStream.beginText();
            contentStream.setFont(PDType1Font.TIMES_ROMAN, 12);
            contentStream.newLineAtOffset(25, 500);
            contentStream.setLeading(14.5f);
            final String text1 = "This is an example of adding text to a page in the pdf document. ";
            final String text2 = "we can add as many lines as we want like this using the <b class=\"notranslate\">showText()</b> method of the ContentStream class";
            contentStream.showText(text1);
            contentStream.newLine();
            contentStream.showText(text2);
            contentStream.endText();
        }

//        final var accessPermission = new AccessPermission();
//        final var standardProtectionPolicy = new StandardProtectionPolicy("1234", "1234", accessPermission);
//        standardProtectionPolicy.setEncryptionKeyLength(128);
//        document.protect(standardProtectionPolicy);

        for (PDPage everyPage : document.getPages()) {
            try (final PDPageContentStream contentStream = new PDPageContentStream(document, everyPage, AppendMode.APPEND, true, true)) {
                final String ts = "Medipath";
                final float fontSize = 80.0f;
                final var graphicsState = new PDExtendedGraphicsState();
                graphicsState.setNonStrokingAlphaConstant(0.3f);
                graphicsState.setAlphaSourceFlag(true);
                contentStream.setGraphicsStateParameters(graphicsState);
                contentStream.setNonStrokingColor(188f / 255, 188f / 255, 188f / 255);
                contentStream.beginText();
                contentStream.setFont(PDType1Font.TIMES_ROMAN, fontSize);

                contentStream.setTextMatrix(Matrix.getRotateInstance(45, 320f, 150f));
                contentStream.showText(ts);
                contentStream.endText();
            }
        }

        document.save("/tmp/test.pdf");
    }
}
