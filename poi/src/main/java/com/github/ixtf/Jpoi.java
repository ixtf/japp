package com.github.ixtf;

import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyles;
import org.zwobble.mammoth.DocumentConverter;
import org.zwobble.mammoth.Result;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;
import static java.util.Spliterators.spliteratorUnknownSize;

public final class Jpoi {

    public static Workbook wb(String file) {
        return wb(J.getFile(file));
    }

    @SneakyThrows({IOException.class,InvalidFormatException.class})
    public static Workbook wb(File file) {
        final var ext = FilenameUtils.getExtension(file.getName());
        if ("xlsx".equalsIgnoreCase(ext)) {
            return new XSSFWorkbook(file);
        }
        return new HSSFWorkbook(POIFSFileSystem.create(file));
    }

    public static Collection<CellRangeAddress> listCellRangeAddress(Sheet sheet) {
        return IntStream.range(0, sheet.getNumMergedRegions())
                .parallel()
                .mapToObj(sheet::getMergedRegion)
                .toList();
    }

    public static Optional<CellRangeAddress> getCellRangeAddress(Collection<CellRangeAddress> rangeList, Cell cell) {
        final var rowIndex = cell.getRowIndex();
        final var columnIndex = cell.getColumnIndex();
        return rangeList.parallelStream().filter(range -> {
            if (rowIndex < range.getFirstRow()) {
                return false;
            }
            if (rowIndex > range.getLastRow()) {
                return false;
            }
            if (columnIndex < range.getFirstColumn()) {
                return false;
            }
            return columnIndex <= range.getLastColumn();
        }).findFirst();
    }

    public static Stream<Row> rowStream(Sheet sheet) {
        return StreamSupport.stream(spliteratorUnknownSize(sheet.rowIterator(), Spliterator.ORDERED), false);
    }

    public static Optional<Row> prevOpt(Row row) {
        final var sheet = row.getSheet();
        final var prev = row.getRowNum() - 1;
        return prev < 0 ? Optional.empty() : ofNullable(sheet.getRow(prev));
    }

    public static Optional<Row> nextOpt(Row row) {
        final var sheet = row.getSheet();
        return ofNullable(sheet.getRow(row.getRowNum() + 1));
    }

    public static final Cell cell(Row row, char c) {
        return CellUtil.getCell(row, c - 'A');
    }

    public static final Cell cell(Sheet sheet, int rowIndex, char c) {
        final var row = CellUtil.getRow(rowIndex, sheet);
        return CellUtil.getCell(row, c - 'A');
    }

    public static Cell cell(Sheet sheet, int rowIndex, int columnIndex) {
        final var row = CellUtil.getRow(rowIndex, sheet);
        return CellUtil.getCell(row, columnIndex);
    }

    public static boolean isColumn(Cell cell, char c) {
        return c - 'A' == cell.getColumnIndex();
    }

    public static boolean isFirst(CellRangeAddress range, Cell cell) {
        return cell.getRowIndex() == range.getFirstRow() && cell.getColumnIndex() == range.getFirstColumn();
    }

    public static Optional<String> stringOpt(Cell cell) {
        return ofNullable(cell).map(__ -> switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                final Number numeric = cell.getNumericCellValue();
                yield "" + numeric.intValue();
            }
            default -> null;
        });
    }

    public static Optional<String> stringOpt(Row row, char c) {
        return stringOpt(cell(row, c));
    }

    public static Optional<String> idOpt(Row row, char c) {
        return stringOpt(row, c)
                .map(J::deleteWhitespace)
                .filter(J::nonBlank)
                .filter(StringUtils::isAsciiPrintable)
                .map(String::toUpperCase);
    }

    public static final String toHtml(File file) throws IOException {
        DocumentConverter converter = new DocumentConverter();
        Result<String> result = converter.convertToHtml(file);
        return result.getValue(); // The generated HTML
    }

    public static void append(XWPFDocument dest, File... files) throws IOException {
        for (File file : files)
            append(dest, file);
    }

    public static void append(XWPFDocument dest, Iterable<File> files) throws IOException {
        for (File file : files)
            append(dest, file);
    }

//    public static void append(XWPFDocument dest, File file) throws IOException {
//        try (FileInputStream fis = new FileInputStream(file); XWPFDocument src = new XWPFDocument(fis)) {
//            append(dest, src);
//        }
//    }

//    public static void append(XWPFDocument dest, XWPFDocument src) {
//        if (dest.getStyles() == null) {
//            copyXWPFStyles(dest, src);
//        }
//
//        for (XWPFParagraph srcParagraph : CollectionUtils.emptyIfNull(src.getParagraphs())) {
//            copyParagraph(dest.createParagraph(), srcParagraph);
//        }
//    }

    private static void copyXWPFStyles(XWPFDocument dest, XWPFDocument src) {
        try {
            CTStyles style = src.getStyle();
            if (style != null) {
                XWPFStyles xwpfStyles = dest.createStyles();
                xwpfStyles.setStyles(style);
            }
        } catch (Exception e) {
            // 没有style没关系的
        }
    }

//    private static void copyParagraph(XWPFParagraph dest, XWPFParagraph src) {
//        dest.setAlignment(src.getAlignment());
//        dest.setStyle(src.getStyle());
//        dest.setBorderBottom(src.getBorderBottom());
//        dest.setBorderTop(src.getBorderTop());
//        dest.setBorderLeft(src.getBorderLeft());
//        dest.setBorderRight(src.getBorderRight());
//        dest.setBorderBetween(src.getBorderBetween());
//        dest.setVerticalAlignment(src.getVerticalAlignment());
//        dest.setFontAlignment(src.getFontAlignment());
//        dest.setFirstLineIndent(src.getFirstLineIndent());
//        dest.setIndentationHanging(src.getIndentationHanging());
//        dest.setIndentationFirstLine(src.getIndentationFirstLine());
//        dest.setIndentationLeft(src.getIndentationLeft());
//        dest.setIndentationRight(src.getIndentationRight());
//        dest.setIndentFromLeft(src.getIndentFromLeft());
//        dest.setIndentFromRight(src.getIndentFromRight());
//        dest.setSpacingAfter(src.getSpacingAfter());
//        dest.setSpacingAfterLines(src.getSpacingAfterLines());
//        dest.setSpacingBefore(src.getSpacingBefore());
//        dest.setSpacingBeforeLines(src.getSpacingBeforeLines());
//
//        copyRuns(dest, src.getRuns());
//    }

//    private static void copyRuns(XWPFParagraph paragraph, Collection<XWPFRun> runs) {
//        for (XWPFRun run : CollectionUtils.emptyIfNull(runs)) {
//            copyRun(paragraph, run);
//        }
//    }

//    private static void copyRun(XWPFParagraph paragraph, XWPFRun src) {
//        XWPFRun dest = paragraph.createRun();
//        dest.setText(src.getText(0));
//        dest.setTextPosition(src.getTextPosition());
//        dest.setFontFamily(src.getFontFamily());
//        dest.setColor(src.getColor());
//        dest.setBold(src.isBold());
//        dest.setUnderline(src.getUnderline());
//        dest.setEmbossed(src.isEmbossed());
//        dest.setImprinted(src.isImprinted());
//        dest.setItalic(src.isItalic());
//        dest.setDoubleStrikethrough(src.isDoubleStrikeThrough());
//        dest.setCapitalized(src.isCapitalized());
//        dest.setCharacterSpacing(src.getCharacterSpacing());
//        dest.setKerning(src.getKerning());
//        dest.setShadow(src.isShadowed());
//        dest.setSmallCaps(src.isSmallCaps());
////        dest.setSubscript(src.getSubscript());
//        if (src.getFontSize() > 0) {
//            dest.setFontSize(src.getFontSize());
//        }
//
//        ofNullable(src.getCTR())
//                .ifPresent(ctr -> copyCTR(dest.getCTR(), ctr));
//
//        copyEmbeddedPictures(paragraph, src.getEmbeddedPictures());
//    }

//    private static void copyCTR(CTR dest, CTR src) {
//        ofNullable(src.getRPr())
//                .ifPresent(ctrPr -> copyCTRPr(dest.addNewRPr(), ctrPr));
//        dest.setNoBreakHyphenArray(src.getNoBreakHyphenList());
//        Optional.ofNullable(src.getBrList())
//                .ifPresent(ctrPr -> copyCTRPr(dest.addNewRPr(), ctrPr));
//    }

//    private static void copyCTRPr(CTRPr dest, CTRPr src) {
//        dest.setRtl(src.getRtl());
//        dest.setHighlight(src.getHighlight());
//        dest.setColor(src.getColor());
//        dest.setEffect(src.getEffect());
//        dest.setShadow(src.getShadow());

//        dest.setB(src.getB());
//        dest.setBdr(src.getBdr());
//        dest.setBCs(src.getBCs());
//        dest.setCaps(src.getCaps());
//        dest.setDstrike(src.getDstrike());
//        dest.setEastAsianLayout(src.getEastAsianLayout());
//        dest.setEm(src.getEm());
//        dest.setEmboss(src.getEmboss());
//        dest.setFitText(src.getFitText());
//        dest.setI(src.getI());
//        dest.setICs(src.getICs());
//        dest.setImprint(src.getImprint());
//        dest.setKern(src.getKern());
//        dest.setLang(src.getLang());
//        dest.setNoProof(src.getNoProof());
//        dest.setOMath(src.getOMath());
//        dest.setOutline(src.getOutline());
//        dest.setPosition(src.getPosition());
//        dest.setRFonts(src.getRFonts());
//        dest.setRPrChange(src.getRPrChange());
//        dest.setRStyle(src.getRStyle());
//        dest.setSmallCaps(src.getSmallCaps());
//        dest.setSnapToGrid(src.getSnapToGrid());
//        dest.setSpacing(src.getSpacing());
//        dest.setSpecVanish(src.getSpecVanish());
//        dest.setStrike(src.getStrike());
//        dest.setSz(src.getSz());
//        dest.setSzCs(src.getSzCs());
//        dest.setShd(src.getShd());
//        dest.setU(src.getU());
//        dest.setVanish(src.getVanish());
//        dest.setVertAlign(src.getVertAlign());
//        dest.setW(src.getW());
//        dest.setWebHidden(src.getWebHidden());
//    }

    private static void copyEmbeddedPictures(XWPFParagraph paragraph, Collection<XWPFPicture> pictures) {
        for (XWPFPicture picture : CollectionUtils.emptyIfNull(pictures)) {
            copyEmbeddedPicture(paragraph, picture);
        }
    }

    private static void copyEmbeddedPicture(XWPFParagraph paragraph, XWPFPicture add) {
        XWPFRun dest = paragraph.createRun();
        CTPicture ctPicture = add.getCTPicture();
//        picture.setText(old.getDescription());
        XWPFPictureData pictureData = add.getPictureData();
        byte[] bytes = pictureData.getData();
        InputStream in = new ByteArrayInputStream(bytes);
        CTPositiveSize2D ext = ctPicture.getSpPr().getXfrm().getExt();
        int width = (int) ext.getCx();
        int height = (int) ext.getCy();
        try {
            dest.addPicture(in, pictureData.getPictureType(), pictureData.getFileName(), width, height);
        } catch (InvalidFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
