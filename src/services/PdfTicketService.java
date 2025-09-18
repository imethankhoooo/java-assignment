package services;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import models.*;
/**
 * PDF Ticket Service - generates beautiful PDF tickets for rental confirmations
 */
public class PdfTicketService {
    private static final Logger logger = Logger.getLogger(PdfTicketService.class.getName());
    
    // Elegant and sophisticated color scheme - inspired by luxury brands
    private static final BaseColor PRIMARY_COLOR = new BaseColor(30, 41, 59);       // Deep slate gray - main brand
    private static final BaseColor SECONDARY_COLOR = new BaseColor(71, 85, 105);    // Medium slate - secondary sections
    private static final BaseColor ACCENT_COLOR = new BaseColor(100, 116, 139);     // Light slate - subtle accents
    private static final BaseColor GOLD_ACCENT = new BaseColor(180, 142, 83);       // Sophisticated gold
    private static final BaseColor BACKGROUND_COLOR = new BaseColor(248, 250, 252); // Cool white background
    private static final BaseColor TEXT_COLOR = new BaseColor(15, 23, 42);          // Deep text
    private static final BaseColor BORDER_COLOR = new BaseColor(203, 213, 225);     // Elegant border
    private static final BaseColor WARNING_COLOR = new BaseColor(185, 28, 28);      // Deep red for warnings
    
    /**
     * Generate PDF ticket and return as byte array for email attachment
     */
    public byte[] generatePdfTicket(Ticket ticket) {
        try {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, baos);//this cannot delete this line
            
            document.open();
            
            // Set up fonts
            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            Font labelFont = new Font(baseFont, 10, Font.BOLD, TEXT_COLOR);
            Font valueFont = new Font(baseFont, 12, Font.NORMAL, TEXT_COLOR);
            Font smallFont = new Font(baseFont, 9, Font.NORMAL, TEXT_COLOR);
            
            // Compact single-page layout
            addCompactHeader(document, ticket, baseFont);
            addCompactDetails(document, ticket, labelFont, valueFont);
            addCompactInstructions(document, baseFont, labelFont, valueFont);
            addCompactFooter(document, smallFont);
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to generate PDF ticket", e);
            return null;
        }
    }
    
    /**
     * Generate PDF ticket and save to file
     */
    public boolean generatePdfTicketFile(Ticket ticket, String filepath) {
        try {
            byte[] pdfData = generatePdfTicket(ticket);
            if (pdfData != null) {
                try (FileOutputStream fos = new FileOutputStream(filepath)) {
                    fos.write(pdfData);
                }
                return true;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save PDF ticket to file", e);
        }
        return false;
    }
    
    private void addCompactHeader(Document document, Ticket ticket, BaseFont baseFont) throws DocumentException {
        // Compact header with all essential info in one section
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2f, 1f, 1f});
        headerTable.setSpacingAfter(15f);
        
        // Company and title
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setBackgroundColor(PRIMARY_COLOR);
        titleCell.setPadding(15f);
        
        Paragraph companyName = new Paragraph("CARSEEK", 
                                            new Font(baseFont, 14, Font.BOLD, BaseColor.WHITE));
        companyName.setAlignment(Element.ALIGN_LEFT);
        titleCell.addElement(companyName);
        
        Paragraph subtitle = new Paragraph("RENTAL CONFIRMATION TICKET", 
                                         new Font(baseFont, 10, Font.NORMAL, new BaseColor(200, 220, 255)));
        subtitle.setSpacingBefore(2f);
        titleCell.addElement(subtitle);
        
        headerTable.addCell(titleCell);
        
        // Ticket ID
        PdfPCell ticketCell = new PdfPCell();
        ticketCell.setBorder(Rectangle.NO_BORDER);
        ticketCell.setBackgroundColor(TEXT_COLOR);
        ticketCell.setPadding(15f);
        ticketCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        Paragraph ticketLabel = new Paragraph("TICKET ID", 
                                            new Font(baseFont, 8, Font.BOLD, BaseColor.WHITE));
        ticketLabel.setAlignment(Element.ALIGN_CENTER);
        ticketCell.addElement(ticketLabel);
        
        Paragraph ticketId = new Paragraph(ticket.getTicketId(), 
                                         new Font(baseFont, 9, Font.BOLD, BaseColor.WHITE));
        ticketId.setAlignment(Element.ALIGN_CENTER);
        ticketId.setSpacingBefore(2f);
        ticketCell.addElement(ticketId);
        
        headerTable.addCell(ticketCell);
        
        // Status and Date
        PdfPCell statusCell = new PdfPCell();
        statusCell.setBorder(Rectangle.NO_BORDER);
        statusCell.setBackgroundColor(GOLD_ACCENT);
        statusCell.setPadding(15f);
        statusCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        Paragraph status = new Paragraph("CONFIRMED", 
                                       new Font(baseFont, 10, Font.BOLD, BaseColor.WHITE));
        status.setAlignment(Element.ALIGN_CENTER);
        statusCell.addElement(status);
        
        Paragraph date = new Paragraph(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), 
                                     new Font(baseFont, 8, Font.NORMAL, BaseColor.WHITE));
        date.setAlignment(Element.ALIGN_CENTER);
        date.setSpacingBefore(2f);
        statusCell.addElement(date);
        
        headerTable.addCell(statusCell);
        
        document.add(headerTable);
    }
    
    private void addCompactDetails(Document document, Ticket ticket, Font labelFont, Font valueFont) throws DocumentException {
        // Compact 3-column details layout (removed payment section)
        PdfPTable detailsTable = new PdfPTable(3);
        detailsTable.setWidthPercentage(100);
        detailsTable.setWidths(new float[]{1f, 1f, 1f});
        detailsTable.setSpacingAfter(12f);
        
        // Customer details
        PdfPCell customerCell = createCompactSection("CUSTOMER", PRIMARY_COLOR);
        addCompactRow(customerCell, "Name:", ticket.getCustomerName(), labelFont, valueFont);
        addCompactRow(customerCell, "Contact:", ticket.getCustomerContact(), labelFont, valueFont);
        addCompactRow(customerCell, "Rental ID:", "#" + ticket.getRentalId(), labelFont, valueFont);
        detailsTable.addCell(customerCell);
        
        // Vehicle details
        PdfPCell vehicleCell = createCompactSection("VEHICLE", SECONDARY_COLOR);
        addCompactRow(vehicleCell, "Model:", ticket.getVehicleInfo(), labelFont, valueFont);
        addCompactRow(vehicleCell, "Plate:", ticket.getCarPlate(), labelFont, valueFont);
        addCompactRow(vehicleCell, "Category:", "Premium", labelFont, valueFont);
        detailsTable.addCell(vehicleCell);
        
        // Rental period
        PdfPCell periodCell = createCompactSection("PERIOD", GOLD_ACCENT);
        addCompactRow(periodCell, "Start:", ticket.getStartDate(), labelFont, valueFont);
        addCompactRow(periodCell, "End:", ticket.getEndDate(), labelFont, valueFont);
        addCompactRow(periodCell, "Duration:", calculateDuration(ticket.getStartDate(), ticket.getEndDate()), labelFont, valueFont);
        detailsTable.addCell(periodCell);
        
        document.add(detailsTable);
    }
    
    private PdfPCell createCompactSection(String title, BaseColor color) throws DocumentException {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBorderWidth(0.5f);
        cell.setPadding(8f);
        cell.setBackgroundColor(BaseColor.WHITE);
        
        // Section header
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(5f);
        
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(color);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setPadding(5f);
        
        Paragraph headerPara = new Paragraph(title, new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE));
        headerPara.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(headerPara);
        
        headerTable.addCell(headerCell);
        cell.addElement(headerTable);
        
        return cell;
    }
    
    private void addCompactRow(PdfPCell cell, String label, String value, Font labelFont, Font valueFont) throws DocumentException {
        PdfPTable row = new PdfPTable(1);
        row.setWidthPercentage(100);
        row.setSpacingAfter(2f);
        
        // Label
        Paragraph labelPara = new Paragraph(label, new Font(labelFont.getBaseFont(), 8, Font.BOLD, new BaseColor(75, 85, 99)));
        labelPara.setSpacingAfter(1f);
        cell.addElement(labelPara);
        
        // Value
        Paragraph valuePara = new Paragraph(value, new Font(valueFont.getBaseFont(), 9, Font.NORMAL, TEXT_COLOR));
        valuePara.setSpacingAfter(3f);
        cell.addElement(valuePara);
    }
    
    private void addCompactInstructions(Document document, BaseFont baseFont, Font labelFont, Font valueFont) throws DocumentException {
        // Two-column layout for instructions and location
        PdfPTable instructionsTable = new PdfPTable(2);
        instructionsTable.setWidthPercentage(100);
        instructionsTable.setWidths(new float[]{1.5f, 1f});
        instructionsTable.setSpacingAfter(10f);
        
        // Left: Instructions
        PdfPCell instructionsCell = new PdfPCell();
        instructionsCell.setBorder(Rectangle.BOX);
        instructionsCell.setBorderColor(BORDER_COLOR);
        instructionsCell.setBorderWidth(0.5f);
        instructionsCell.setPadding(10f);
        instructionsCell.setBackgroundColor(BaseColor.WHITE);
        
        Paragraph instTitle = new Paragraph("PICKUP INSTRUCTIONS", 
                                          new Font(baseFont, 11, Font.BOLD, TEXT_COLOR));
        instTitle.setSpacingAfter(6f);
        instructionsCell.addElement(instTitle);
        
        String[] instructions = {
            "1. Bring valid ID (Driver's License)",
            "2. Present this ticket (digital/print)",
            "3. Arrive 15 mins early",
            "4. Complete vehicle inspection",
            "5. Have payment method ready"
        };
        
        for (String instruction : instructions) {
            Paragraph instPara = new Paragraph(instruction, new Font(baseFont, 8, Font.NORMAL, TEXT_COLOR));
            instPara.setSpacingAfter(2f);
            instructionsCell.addElement(instPara);
        }
        
        instructionsTable.addCell(instructionsCell);
        
        // Right: Location & Contact
        PdfPCell locationCell = new PdfPCell();
        locationCell.setBorder(Rectangle.BOX);
        locationCell.setBorderColor(BORDER_COLOR);
        locationCell.setBorderWidth(0.5f);
        locationCell.setPadding(10f);
        locationCell.setBackgroundColor(BACKGROUND_COLOR);
        
        Paragraph locTitle = new Paragraph("PICKUP LOCATION", 
                                         new Font(baseFont, 11, Font.BOLD, TEXT_COLOR));
        locTitle.setSpacingAfter(6f);
        locationCell.addElement(locTitle);
        
        Paragraph address = new Paragraph("Main Office\nVehicle Rental Center", 
                                        new Font(baseFont, 9, Font.NORMAL, TEXT_COLOR));
        address.setSpacingAfter(6f);
        locationCell.addElement(address);
        
        Paragraph hours = new Paragraph("HOURS\n8:00 AM - 6:00 PM\n(Monday - Sunday)", 
                                      new Font(baseFont, 8, Font.NORMAL, SECONDARY_COLOR));
        hours.setSpacingAfter(6f);
        locationCell.addElement(hours);
        
        Paragraph contact = new Paragraph("CONTACT\n+1-800-RENTAL\nsupport@premium.com", 
                                        new Font(baseFont, 8, Font.NORMAL, ACCENT_COLOR));
        locationCell.addElement(contact);
        
        instructionsTable.addCell(locationCell);
        
        document.add(instructionsTable);
        
        // Important notices in compact format
        PdfPTable noticesTable = new PdfPTable(1);
        noticesTable.setWidthPercentage(100);
        noticesTable.setSpacingAfter(10f);
        
        PdfPCell noticesCell = new PdfPCell();
        noticesCell.setBackgroundColor(new BaseColor(254, 249, 195)); // Light yellow
        noticesCell.setBorder(Rectangle.BOX);
        noticesCell.setBorderColor(WARNING_COLOR);
        noticesCell.setBorderWidth(0.5f);
        noticesCell.setPadding(8f);
        
        Paragraph noticesTitle = new Paragraph("IMPORTANT NOTICES", 
                                             new Font(baseFont, 9, Font.BOLD, WARNING_COLOR));
        noticesTitle.setSpacingAfter(4f);
        noticesCell.addElement(noticesTitle);
        
        Paragraph notices = new Paragraph("• This ticket is required for vehicle pickup • Late pickup may incur charges • Vehicle must be returned in same condition • Fuel level should match pickup level", 
                                        new Font(baseFont, 7, Font.NORMAL, TEXT_COLOR));
        noticesCell.addElement(notices);
        
        noticesTable.addCell(noticesCell);
        document.add(noticesTable);
    }
    
    private void addCompactFooter(Document document, Font smallFont) throws DocumentException {
        // Simple footer
        PdfPTable footerTable = new PdfPTable(1);
        footerTable.setWidthPercentage(100);
        
        PdfPCell footerCell = new PdfPCell();
        footerCell.setBackgroundColor(TEXT_COLOR);
        footerCell.setBorder(Rectangle.NO_BORDER);
        footerCell.setPadding(10f);
        
        Paragraph thankYou = new Paragraph("Thank you for choosing CarSeek!", 
                                         new Font(smallFont.getBaseFont(), 10, Font.BOLD, BaseColor.WHITE));
        thankYou.setAlignment(Element.ALIGN_CENTER);
        thankYou.setSpacingAfter(4f);
        footerCell.addElement(thankYou);
        
        Paragraph tagline = new Paragraph("Your Journey, Our Priority • CarSeek Since 2015", 
                                        new Font(smallFont.getBaseFont(), 8, Font.NORMAL, new BaseColor(200, 220, 255)));
        tagline.setAlignment(Element.ALIGN_CENTER);
        footerCell.addElement(tagline);
        
        footerTable.addCell(footerCell);
        document.add(footerTable);
    }
    
    private String calculateDuration(String startDate, String endDate) {
        try {
            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);
            long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            return days + " day" + (days > 1 ? "s" : "");
        } catch (Exception e) {
            return "Multiple days";
        }
    }
    
} 