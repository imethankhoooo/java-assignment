import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.ColumnText;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 报表导出服务 - 使用Apache POI和iText直接导出
 */
public class ReportExportService {
    
    private static final Logger logger = Logger.getLogger(ReportExportService.class.getName());
    
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";
    
    private final boolean hasApachePOI;
    private final boolean hasIText;
    
    private String dateFormat = DEFAULT_DATE_FORMAT;
    private String timestampFormat = DEFAULT_TIMESTAMP_FORMAT;
    private boolean autoSizeColumns = true;
    
    public ReportExportService() {
        this.hasApachePOI = checkLibrary("org.apache.poi.ss.usermodel.Workbook", "Apache POI");
        this.hasIText = checkLibrary("com.itextpdf.text.Document", "iText5");
    }
    
    private boolean checkLibrary(String className, String libraryName) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            logger.warning(libraryName + " 库未找到，请运行 'download_compatible_dependencies.bat' 脚本。");
            return false;
        }
    }
    
    public boolean exportToExcel(String reportTitle, List<String> headers, List<List<String>> data, String baseFilename) {
        if (!hasApachePOI) return false;
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Report");
            
            // 创建样式
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook, false);
            CellStyle alternateDataStyle = createDataStyle(workbook, true);
            
            int rowNum = 0;
            
            // 标题和时间戳
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(reportTitle);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.size() - 1));

            Row timestampRow = sheet.createRow(rowNum++);
            timestampRow.createCell(0).setCellValue("Generated on: " + getCurrentTimestamp());
            rowNum++; // 空行
            
            // 表头
            Row headerRow = sheet.createRow(rowNum++);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }
            
            // 数据行
            for (int i = 0; i < data.size(); i++) {
                Row dataRow = sheet.createRow(rowNum++);
                List<String> rowData = data.get(i);
                 for (int j = 0; j < rowData.size(); j++) {
                    Cell cell = dataRow.createCell(j);
                    cell.setCellValue(rowData.get(j));
                    cell.setCellStyle(i % 2 == 0 ? dataStyle : alternateDataStyle);
                }
            }
            
            // 自动调整列宽
            if (autoSizeColumns) {
            for (int i = 0; i < headers.size(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }
            
            // 保存文件
            String filename = generateFilename(baseFilename, ".xlsx");
            ensureReportsDirectory();
            try (FileOutputStream fileOut = new FileOutputStream("reports/" + filename)) {
                workbook.write(fileOut);
            }
            
            System.out.println(" Excel report exported successfully: reports/" + filename);
            return true;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Excel export failed", e);
            return false;
        }
    }
    
    public boolean exportToPDF(String reportTitle, List<String> headers, List<List<String>> data, String baseFilename) {
        if (!hasIText) return false;
        
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 72, 72); // 增加边距
        try {
            String filename = generateFilename(baseFilename, ".pdf");
            ensureReportsDirectory();
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("reports/" + filename));
            
            // 添加页脚事件
            writer.setPageEvent(new PdfFooter(timestampFormat));

            document.open();
            
            // 使用标准英文字体，避免中文字体问题
            BaseFont bfEnglish;
            try {
                // 使用标准字体
                bfEnglish = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                // 备用方案
                bfEnglish = BaseFont.createFont();
            }
            
            // 定义字体 - 使用英文字体，改善可读性
            Font titleFont = new Font(bfEnglish, 20, Font.BOLD, BaseColor.BLACK);
            Font subtitleFont = new Font(bfEnglish, 10, Font.NORMAL, BaseColor.GRAY);
            Font headerFont = new Font(bfEnglish, 10, Font.BOLD, BaseColor.WHITE);
            Font normalFont = new Font(bfEnglish, 9, Font.NORMAL, BaseColor.BLACK);
            Font alternateFont = new Font(bfEnglish, 9, Font.NORMAL, BaseColor.BLACK);
            
            // 标题
            Paragraph title = new Paragraph(reportTitle, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10f);
            document.add(title);
            
            // 副标题 - 生成时间
            Paragraph subtitle = new Paragraph("Generated on: " + getCurrentTimestamp(), subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20f);
            document.add(subtitle);
            
            // 数据统计信息
            Paragraph dataInfo = new Paragraph("Total Records: " + data.size(), subtitleFont);
            dataInfo.setAlignment(Element.ALIGN_LEFT);
            dataInfo.setSpacingAfter(15f);
            document.add(dataInfo);
            
            // 创建表格
            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            
            // 根据列数设置合适的列宽
            float[] columnWidths = calculateColumnWidths(headers.size());
            table.setWidths(columnWidths);
            
            // 表头样式
            for (String header : headers) {
                PdfPCell cell = new PdfPCell();
                
                // 创建支持换行的段落
                Paragraph headerParagraph = new Paragraph(header, headerFont);
                cell.addElement(headerParagraph);
                
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setBackgroundColor(new BaseColor(41, 128, 185)); // 蓝色
                cell.setPadding(8f);
                cell.setBorderColor(BaseColor.WHITE);
                cell.setBorderWidth(1f);
                cell.setMinimumHeight(30f);
                cell.setNoWrap(false); // 允许换行
                table.addCell(cell);
            }
            
            // 数据行
            for (int i = 0; i < data.size(); i++) {
                List<String> rowData = data.get(i);
                for (int j = 0; j < rowData.size(); j++) {
                    String cellData = rowData.get(j);
                    // 处理空值
                    if (cellData == null) cellData = "";
                    
                    PdfPCell cell = new PdfPCell();
                    
                    // 创建支持换行的段落
                    Font cellFont = i % 2 == 0 ? normalFont : alternateFont;
                    Paragraph cellParagraph = new Paragraph(cellData, cellFont);
                    
                    // 数字列右对齐
                    if (isNumeric(cellData)) {
                        cellParagraph.setAlignment(Element.ALIGN_RIGHT);
                        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    } else {
                        cellParagraph.setAlignment(Element.ALIGN_LEFT);
                        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                    }
                    
                    cell.addElement(cellParagraph);
                    cell.setPadding(6f);
                    cell.setBorderColor(new BaseColor(200, 200, 200));
                    cell.setBorderWidth(0.5f);
                    cell.setMinimumHeight(25f);
                    cell.setNoWrap(false); // 允许换行
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    
                    // 交替行颜色
                    if (i % 2 != 0) {
                        cell.setBackgroundColor(new BaseColor(248, 249, 250)); // 非常淡的灰色
                    }
                    
                    table.addCell(cell);
                }
            }
            
            document.add(table);
            
            // 添加页脚信息
            document.add(new Paragraph("\n"));
            Paragraph footer = new Paragraph("Generated by Vehicle Rental Management System", subtitleFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            
            System.out.println(" PDF report exported successfully: reports/" + filename);
            return true;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PDF export failed", e);
            System.err.println("PDF export failed: " + e.getMessage());
            return false;
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }
    
    /**
     * 计算列宽
     */
    private float[] calculateColumnWidths(int columnCount) {
        float[] widths = new float[columnCount];
        
        // 根据列数设置不同的宽度分配
        switch (columnCount) {
            case 2:
                widths[0] = 1f;
                widths[1] = 2f;
                break;
            case 3:
                widths[0] = 1f;
                widths[1] = 1.5f;
                widths[2] = 1f;
                break;
            case 4:
                widths[0] = 1f;
                widths[1] = 2f;
                widths[2] = 1f;
                widths[3] = 1f;
                break;
            case 5:
                widths[0] = 0.8f;
                widths[1] = 1.5f;
                widths[2] = 1.2f;
                widths[3] = 1f;
                widths[4] = 0.8f;
                break;
            case 6:
                widths[0] = 0.8f;
                widths[1] = 1.2f;
                widths[2] = 1.2f;
                widths[3] = 1f;
                widths[4] = 1f;
                widths[5] = 0.8f;
                break;
            case 7:
                widths[0] = 0.6f;
                widths[1] = 1f;
                widths[2] = 1f;
                widths[3] = 1f;
                widths[4] = 1f;
                widths[5] = 1f;
                widths[6] = 0.8f;
                break;
            case 8:
                widths[0] = 0.6f;
                widths[1] = 1f;
                widths[2] = 1f;
                widths[3] = 1f;
                widths[4] = 1f;
                widths[5] = 1f;
                widths[6] = 1f;
                widths[7] = 0.8f;
                break;
            default:
                // 默认等宽
                for (int i = 0; i < columnCount; i++) {
                    widths[i] = 1f;
                }
        }
        
        return widths;
    }
    
    /**
     * 检查字符串是否为数字
     */
    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        
        // 移除货币符号和空格
        String cleaned = str.replace("RM", "").replace(",", "").trim();
        
        try {
            Double.parseDouble(cleaned);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // === 辅助方法 ===
    
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 20);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
                    }

    private CellStyle createDataStyle(Workbook workbook, boolean isAlternate) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());

        if (isAlternate) {
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }
        return style;
    }
    
    private String generateFilename(String baseFilename, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(timestampFormat));
        return baseFilename + "_" + timestamp + extension;
    }
    
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(dateFormat));
    }
    
    private void ensureReportsDirectory() {
        File reportsDir = new File("reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
    }
    
    public void promptForExport(Scanner scanner, String reportTitle, List<String> headers, List<List<String>> data, String baseFilename) {
        System.out.println("\n--- Export Report ---");
        if (!hasApachePOI && !hasIText) {
            System.out.println("Error: No export libraries available.");
            return;
        }
        
        System.out.print("Do you want to export this report? (y/n): ");
        String response = scanner.nextLine().trim();
        
        if (response.equalsIgnoreCase("y")) {
            System.out.println("Export format selection:");
            if (hasApachePOI) System.out.println("1. Excel");
            if (hasIText) System.out.println("2. PDF");
            if (hasApachePOI && hasIText) System.out.println("3. Both");
            System.out.print("Please select: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    exportToExcel(reportTitle, headers, data, baseFilename);
                    break;
                case "2":
                    exportToPDF(reportTitle, headers, data, baseFilename);
                    break;
                case "3":
                    exportToExcel(reportTitle, headers, data, baseFilename);
                    exportToPDF(reportTitle, headers, data, baseFilename);
                    break;
                default:
                    System.out.println("Invalid selection.");
            }
        }
    }
} 

class PdfFooter extends PdfPageEventHelper {
    private final String timestampFormat;
    private Font footerFont;

    public PdfFooter(String timestampFormat) {
        this.timestampFormat = timestampFormat;
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            this.footerFont = new Font(bf, 8, Font.NORMAL, BaseColor.GRAY);
        } catch (Exception e) {
            // 备用方案
            this.footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY);
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(this.timestampFormat));
        
        // 创建页脚内容
        String footerText = String.format("Page %d | Generated on: %s", writer.getPageNumber(), timestamp);
        Phrase footer = new Phrase(footerText, footerFont);

        // 计算页脚位置
        float x = (document.right() - document.left()) / 2 + document.leftMargin();
        float y = document.bottom() - 20; // 稍微提高位置
        
        // 添加页脚文本
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footer, x, y, 0);
        
        // 添加分隔线
        cb.setLineWidth(0.5f);
        cb.setColorStroke(BaseColor.LIGHT_GRAY);
        cb.moveTo(document.leftMargin(), document.bottom() - 10);
        cb.lineTo(document.right() - document.rightMargin(), document.bottom() - 10);
        cb.stroke();
    }
} 