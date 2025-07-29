import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final BaseColor LIGHT_ACCENT = new BaseColor(241, 245, 249);     // Very light slate
    private static final BaseColor BORDER_COLOR = new BaseColor(203, 213, 225);     // Elegant border
    private static final BaseColor WARNING_COLOR = new BaseColor(185, 28, 28);      // Deep red for warnings
    
    /**
     * Generate PDF ticket and return as byte array for email attachment
     */
    public byte[] generatePdfTicket(Ticket ticket) {
        try {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Set up fonts
            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 24, Font.BOLD, PRIMARY_COLOR);
            Font headerFont = new Font(baseFont, 16, Font.BOLD, TEXT_COLOR);
            Font labelFont = new Font(baseFont, 10, Font.BOLD, TEXT_COLOR);
            Font valueFont = new Font(baseFont, 12, Font.NORMAL, TEXT_COLOR);
            Font smallFont = new Font(baseFont, 9, Font.NORMAL, TEXT_COLOR);
            Font warningFont = new Font(baseFont, 10, Font.BOLD, WARNING_COLOR);
            
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
        
        Paragraph companyName = new Paragraph("PREMIUM VEHICLE RENTAL", 
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
        
        Paragraph instTitle = new Paragraph("📋 PICKUP INSTRUCTIONS", 
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
        
        Paragraph locTitle = new Paragraph("📍 PICKUP LOCATION", 
                                         new Font(baseFont, 11, Font.BOLD, TEXT_COLOR));
        locTitle.setSpacingAfter(6f);
        locationCell.addElement(locTitle);
        
        Paragraph address = new Paragraph("Main Office\nVehicle Rental Center", 
                                        new Font(baseFont, 9, Font.NORMAL, TEXT_COLOR));
        address.setSpacingAfter(6f);
        locationCell.addElement(address);
        
        Paragraph hours = new Paragraph("🕐 HOURS\n8:00 AM - 6:00 PM\n(Monday - Sunday)", 
                                      new Font(baseFont, 8, Font.NORMAL, SECONDARY_COLOR));
        hours.setSpacingAfter(6f);
        locationCell.addElement(hours);
        
        Paragraph contact = new Paragraph("📞 CONTACT\n+1-800-RENTAL\nsupport@premium.com", 
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
        
        Paragraph noticesTitle = new Paragraph("⚠️ IMPORTANT NOTICES", 
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
        
        Paragraph thankYou = new Paragraph("✨ Thank you for choosing Premium Vehicle Rental! ✨", 
                                         new Font(smallFont.getBaseFont(), 10, Font.BOLD, BaseColor.WHITE));
        thankYou.setAlignment(Element.ALIGN_CENTER);
        thankYou.setSpacingAfter(4f);
        footerCell.addElement(thankYou);
        
        Paragraph tagline = new Paragraph("Your Journey, Our Priority • Premium Service Since 2020", 
                                        new Font(smallFont.getBaseFont(), 8, Font.NORMAL, new BaseColor(200, 220, 255)));
        tagline.setAlignment(Element.ALIGN_CENTER);
        footerCell.addElement(tagline);
        
        footerTable.addCell(footerCell);
        document.add(footerTable);
    }
    

    
    private void addTicketDetails(Document document, Ticket ticket, Font labelFont, Font valueFont) throws DocumentException {
        // Modern invoice-style details section
        PdfPTable mainDetailsTable = new PdfPTable(1);
        mainDetailsTable.setWidthPercentage(100);
        mainDetailsTable.setSpacingBefore(0f);
        mainDetailsTable.setSpacingAfter(20f);
        
        // Details container
        PdfPCell containerCell = new PdfPCell();
        containerCell.setBorder(Rectangle.BOX);
        containerCell.setBorderColor(BORDER_COLOR);
        containerCell.setBorderWidth(1f);
        containerCell.setPadding(0f);
        containerCell.setBackgroundColor(BaseColor.WHITE);
        
        // Create inner table for organized layout
        PdfPTable innerTable = new PdfPTable(2);
        innerTable.setWidthPercentage(100);
        innerTable.setWidths(new float[]{1f, 1f});
        
        // Left side details
        PdfPCell leftDetails = createModernDetailsCell();
        
        // Customer section
        addModernSection(leftDetails, "CUSTOMER DETAILS", TEXT_COLOR, labelFont);
        addModernDetailRow(leftDetails, "Full Name", ticket.getCustomerName(), labelFont, valueFont);
        addModernDetailRow(leftDetails, "Contact", ticket.getCustomerContact(), labelFont, valueFont);
        addModernDetailRow(leftDetails, "Rental ID", "#" + String.valueOf(ticket.getRentalId()), labelFont, valueFont);
        
        addSectionSpacer(leftDetails);
        
        // Rental period section
        addModernSection(leftDetails, "RENTAL PERIOD", ACCENT_COLOR, labelFont);
        addModernDetailRow(leftDetails, "Start Date", ticket.getStartDate(), labelFont, valueFont);
        addModernDetailRow(leftDetails, "End Date", ticket.getEndDate(), labelFont, valueFont);
        addModernDetailRow(leftDetails, "Duration", calculateDuration(ticket.getStartDate(), ticket.getEndDate()), labelFont, valueFont);
        
        innerTable.addCell(leftDetails);
        
        // Right side details
        PdfPCell rightDetails = createModernDetailsCell();
        
        // Vehicle section
        addModernSection(rightDetails, "VEHICLE DETAILS", SECONDARY_COLOR, labelFont);
        addModernDetailRow(rightDetails, "Vehicle", ticket.getVehicleInfo(), labelFont, valueFont);
        addModernDetailRow(rightDetails, "License Plate", ticket.getCarPlate(), labelFont, valueFont);
        addModernDetailRow(rightDetails, "Category", "Premium Rental", labelFont, valueFont);
        
        addSectionSpacer(rightDetails);
        
        // Payment section with enhanced styling
        addModernSection(rightDetails, "PAYMENT SUMMARY", new BaseColor(17, 24, 39), labelFont);
        addModernDetailRow(rightDetails, "Rental Fee", String.format("RM %.2f", ticket.getTotalFee()), labelFont, valueFont);
        addModernDetailRow(rightDetails, "Insurance", ticket.isInsuranceIncluded() ? "✓ Included" : "✗ Not Included", labelFont, valueFont);
        
        // Total in highlighted box
        PdfPTable totalBox = new PdfPTable(1);
        totalBox.setWidthPercentage(100);
        totalBox.setSpacingBefore(8f);
        
        PdfPCell totalCell = new PdfPCell();
        totalCell.setBackgroundColor(TEXT_COLOR);
        totalCell.setBorder(Rectangle.NO_BORDER);
        totalCell.setPadding(8f);
        
        Paragraph totalPara = new Paragraph(String.format("TOTAL: RM %.2f", ticket.getTotalFee()), 
                                          new Font(labelFont.getBaseFont(), 12, Font.BOLD, BaseColor.WHITE));
        totalPara.setAlignment(Element.ALIGN_CENTER);
        totalCell.addElement(totalPara);
        
        totalBox.addCell(totalCell);
        rightDetails.addElement(totalBox);
        
        innerTable.addCell(rightDetails);
        
        containerCell.addElement(innerTable);
        mainDetailsTable.addCell(containerCell);
        
        document.add(mainDetailsTable);
    }
    
    private PdfPCell createModernDetailsCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(20f);
        cell.setBackgroundColor(BaseColor.WHITE);
        return cell;
    }
    
    private void addModernSection(PdfPCell cell, String title, BaseColor color, Font labelFont) throws DocumentException {
        PdfPTable sectionHeader = new PdfPTable(1);
        sectionHeader.setWidthPercentage(100);
        sectionHeader.setSpacingBefore(0f);
        sectionHeader.setSpacingAfter(8f);
        
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(color);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setPadding(8f);
        
        Paragraph headerPara = new Paragraph(title, new Font(labelFont.getBaseFont(), 10, Font.BOLD, BaseColor.WHITE));
        headerPara.setAlignment(Element.ALIGN_LEFT);
        headerCell.addElement(headerPara);
        
        sectionHeader.addCell(headerCell);
        cell.addElement(sectionHeader);
    }
    
    private void addModernDetailRow(PdfPCell cell, String label, String value, Font labelFont, Font valueFont) throws DocumentException {
        PdfPTable row = new PdfPTable(2);
        row.setWidthPercentage(100);
        row.setWidths(new float[]{1f, 1.5f});
        row.setSpacingAfter(3f);
        
        PdfPCell labelCell = new PdfPCell(new Phrase(label + ":", labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(2f);
        labelCell.setBackgroundColor(LIGHT_ACCENT);
        labelCell.setPadding(5f);
        row.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(2f);
        valueCell.setPadding(5f);
        row.addCell(valueCell);
        
        cell.addElement(row);
    }
    
    private void addSectionSpacer(PdfPCell cell) {
        Paragraph spacer = new Paragraph(" ", new Font(Font.FontFamily.HELVETICA, 8));
        spacer.setSpacingAfter(5f);
        cell.addElement(spacer);
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
    
    private PdfPCell createDetailsCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BaseColor.LIGHT_GRAY);
        cell.setPadding(15f);
        cell.setBackgroundColor(BaseColor.WHITE);
        return cell;
    }
    
    private void addDetailRow(PdfPCell cell, String label, String value, Font labelFont, Font valueFont, boolean isHeader) {
        try {
            if (isHeader) {
                Paragraph headerPara = new Paragraph(label, new Font(labelFont.getBaseFont(), 12, Font.BOLD, PRIMARY_COLOR));
                headerPara.setSpacingAfter(8f);
                cell.addElement(headerPara);
            } else if (value != null) {
                PdfPTable row = new PdfPTable(2);
                row.setWidthPercentage(100);
                row.setWidths(new float[]{1f, 1.5f});
                
                PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
                labelCell.setBorder(Rectangle.NO_BORDER);
                labelCell.setPaddingBottom(3f);
                row.addCell(labelCell);
                
                PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
                valueCell.setBorder(Rectangle.NO_BORDER);
                valueCell.setPaddingBottom(3f);
                row.addCell(valueCell);
                
                cell.addElement(row);
            } else {
                cell.addElement(new Paragraph(" ", valueFont));
            }
        } catch (DocumentException e) {
            logger.log(Level.WARNING, "Failed to add detail row", e);
        }
    }
    
    private void addPickupInstructions(Document document, BaseFont baseFont, Font labelFont, Font valueFont) throws DocumentException {
        // Modern pickup instructions with icon-style layout
        PdfPTable mainInstructionsTable = new PdfPTable(2);
        mainInstructionsTable.setWidthPercentage(100);
        mainInstructionsTable.setWidths(new float[]{1.2f, 0.8f});
        mainInstructionsTable.setSpacingBefore(10f);
        mainInstructionsTable.setSpacingAfter(20f);
        
        // Left side - Instructions
        PdfPCell instructionsCell = new PdfPCell();
        instructionsCell.setBorder(Rectangle.BOX);
        instructionsCell.setBorderColor(BORDER_COLOR);
        instructionsCell.setBorderWidth(1f);
        instructionsCell.setBackgroundColor(BaseColor.WHITE);
        instructionsCell.setPadding(20f);
        
        // Instructions header
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(15f);
        
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(TEXT_COLOR);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setPadding(10f);
        
        Paragraph headerPara = new Paragraph("📋 PICKUP INSTRUCTIONS", 
                                           new Font(baseFont, 14, Font.BOLD, BaseColor.WHITE));
        headerPara.setAlignment(Element.ALIGN_LEFT);
        headerCell.addElement(headerPara);
        
        headerTable.addCell(headerCell);
        instructionsCell.addElement(headerTable);
        
        // Step-by-step instructions with icons
        String[][] instructionSteps = {
            {"🆔", "STEP 1: BRING IDENTIFICATION", "Valid government-issued ID (Driver's License required)"},
            {"📱", "STEP 2: PRESENT TICKET", "Show this PDF ticket (digital or printed copy)"},
            {"⏰", "STEP 3: ARRIVE ON TIME", "Come 15 minutes before your rental start time"},
            {"🔍", "STEP 4: VEHICLE INSPECTION", "Complete inspection will be conducted before handover"},
            {"💳", "STEP 5: PAYMENT READY", "Have your payment method from booking available"},
            {"📞", "STEP 6: CHANGES?", "Contact us 2+ hours in advance for rescheduling"}
        };
        
        for (String[] step : instructionSteps) {
            addInstructionStep(instructionsCell, step[0], step[1], step[2], baseFont, labelFont, valueFont);
        }
        
        mainInstructionsTable.addCell(instructionsCell);
        
        // Right side - Location and contact
        PdfPCell locationCell = new PdfPCell();
        locationCell.setBorder(Rectangle.BOX);
        locationCell.setBorderColor(BORDER_COLOR);
        locationCell.setBorderWidth(1f);
        locationCell.setBackgroundColor(BACKGROUND_COLOR);
        locationCell.setPadding(20f);
        
        // Location header
        PdfPTable locationHeaderTable = new PdfPTable(1);
        locationHeaderTable.setWidthPercentage(100);
        locationHeaderTable.setSpacingAfter(15f);
        
        PdfPCell locationHeaderCell = new PdfPCell();
        locationHeaderCell.setBackgroundColor(SECONDARY_COLOR);
        locationHeaderCell.setBorder(Rectangle.NO_BORDER);
        locationHeaderCell.setPadding(10f);
        
        Paragraph locationHeaderPara = new Paragraph("📍 PICKUP LOCATION", 
                                                   new Font(baseFont, 12, Font.BOLD, BaseColor.WHITE));
        locationHeaderPara.setAlignment(Element.ALIGN_CENTER);
        locationHeaderCell.addElement(locationHeaderPara);
        
        locationHeaderTable.addCell(locationHeaderCell);
        locationCell.addElement(locationHeaderTable);
        
        // Location details
        Paragraph locationName = new Paragraph("Main Office", 
                                             new Font(baseFont, 14, Font.BOLD, TEXT_COLOR));
        locationName.setAlignment(Element.ALIGN_CENTER);
        locationName.setSpacingAfter(5f);
        locationCell.addElement(locationName);
        
        Paragraph locationAddress = new Paragraph("Vehicle Rental Center", 
                                                new Font(baseFont, 11, Font.NORMAL, TEXT_COLOR));
        locationAddress.setAlignment(Element.ALIGN_CENTER);
        locationAddress.setSpacingAfter(15f);
        locationCell.addElement(locationAddress);
        
        // Business hours box
        PdfPTable hoursBox = new PdfPTable(1);
        hoursBox.setWidthPercentage(100);
        hoursBox.setSpacingAfter(15f);
        
        PdfPCell hoursCell = new PdfPCell();
        hoursCell.setBackgroundColor(BaseColor.WHITE);
        hoursCell.setBorder(Rectangle.BOX);
        hoursCell.setBorderColor(BORDER_COLOR);
        hoursCell.setPadding(10f);
        
        Paragraph hoursTitle = new Paragraph("🕐 BUSINESS HOURS", 
                                           new Font(baseFont, 10, Font.BOLD, TEXT_COLOR));
        hoursTitle.setAlignment(Element.ALIGN_CENTER);
        hoursTitle.setSpacingAfter(5f);
        hoursCell.addElement(hoursTitle);
        
        Paragraph hoursText = new Paragraph("8:00 AM - 6:00 PM\n(Monday - Sunday)", 
                                          new Font(baseFont, 10, Font.NORMAL, TEXT_COLOR));
        hoursText.setAlignment(Element.ALIGN_CENTER);
        hoursCell.addElement(hoursText);
        
        hoursBox.addCell(hoursCell);
        locationCell.addElement(hoursBox);
        
        // Contact box
        PdfPTable contactBox = new PdfPTable(1);
        contactBox.setWidthPercentage(100);
        
        PdfPCell contactCell = new PdfPCell();
        contactCell.setBackgroundColor(ACCENT_COLOR);
        contactCell.setBorder(Rectangle.NO_BORDER);
        contactCell.setPadding(10f);
        
        Paragraph contactTitle = new Paragraph("📞 NEED HELP?", 
                                             new Font(baseFont, 10, Font.BOLD, BaseColor.WHITE));
        contactTitle.setAlignment(Element.ALIGN_CENTER);
        contactTitle.setSpacingAfter(5f);
        contactCell.addElement(contactTitle);
        
        Paragraph contactText = new Paragraph("+1-800-RENTAL\nsupport@premium.com", 
                                            new Font(baseFont, 9, Font.NORMAL, BaseColor.WHITE));
        contactText.setAlignment(Element.ALIGN_CENTER);
        contactCell.addElement(contactText);
        
        contactBox.addCell(contactCell);
        locationCell.addElement(contactBox);
        
        mainInstructionsTable.addCell(locationCell);
        
        document.add(mainInstructionsTable);
    }
    
    private void addInstructionStep(PdfPCell parentCell, String icon, String title, String description, BaseFont baseFont, Font labelFont, Font valueFont) throws DocumentException {
        PdfPTable stepTable = new PdfPTable(2);
        stepTable.setWidthPercentage(100);
        stepTable.setWidths(new float[]{0.15f, 0.85f});
        stepTable.setSpacingAfter(8f);
        
        // Icon cell
        PdfPCell iconCell = new PdfPCell();
        iconCell.setBorder(Rectangle.NO_BORDER);
        iconCell.setPadding(5f);
        iconCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        Paragraph iconPara = new Paragraph(icon, new Font(baseFont, 16, Font.NORMAL, SECONDARY_COLOR));
        iconPara.setAlignment(Element.ALIGN_CENTER);
        iconCell.addElement(iconPara);
        
        stepTable.addCell(iconCell);
        
        // Content cell
        PdfPCell contentCell = new PdfPCell();
        contentCell.setBorder(Rectangle.NO_BORDER);
        contentCell.setPadding(5f);
        
        Paragraph titlePara = new Paragraph(title, new Font(baseFont, 10, Font.BOLD, TEXT_COLOR));
        titlePara.setSpacingAfter(2f);
        contentCell.addElement(titlePara);
        
        Paragraph descPara = new Paragraph(description, new Font(baseFont, 9, Font.NORMAL, new BaseColor(107, 114, 128)));
        contentCell.addElement(descPara);
        
        stepTable.addCell(contentCell);
        
        parentCell.addElement(stepTable);
    }
    
    private void addImportantNotices(Document document, Font warningFont, Font smallFont) throws DocumentException {
        // Important notices
        PdfPTable noticesTable = new PdfPTable(1);
        noticesTable.setWidthPercentage(100);
        noticesTable.setSpacingBefore(15f);
        
        PdfPCell noticesCell = new PdfPCell();
        noticesCell.setBackgroundColor(new BaseColor(255, 248, 225)); // Light yellow
        noticesCell.setPadding(12f);
        noticesCell.setBorder(Rectangle.BOX);
        noticesCell.setBorderColor(WARNING_COLOR);
        
        Paragraph noticesTitle = new Paragraph("IMPORTANT NOTICES", warningFont);
        noticesTitle.setSpacingAfter(8f);
        noticesCell.addElement(noticesTitle);
        
        String[] notices = {
            "• This ticket is required for vehicle pickup - do not lose it",
            "• Late pickup may result in additional charges",
            "• Vehicle must be returned in the same condition as received",
            "• Fuel tank should be returned at the same level as pickup",
            "• Any damages will be assessed and charged accordingly"
        };
        
        for (String notice : notices) {
            Paragraph noticePara = new Paragraph(notice, smallFont);
            noticePara.setSpacingAfter(3f);
            noticesCell.addElement(noticePara);
        }
        
        noticesTable.addCell(noticesCell);
        document.add(noticesTable);
    }
    
    private void addFooter(Document document, Font smallFont) throws DocumentException {
        // Modern footer with gradient-style design
        PdfPTable footerTable = new PdfPTable(1);
        footerTable.setWidthPercentage(100);
        footerTable.setSpacingBefore(20f);
        
        PdfPCell footerCell = new PdfPCell();
        footerCell.setBackgroundColor(TEXT_COLOR);
        footerCell.setBorder(Rectangle.NO_BORDER);
        footerCell.setPadding(20f);
        
        // Thank you message
        Paragraph thankYou = new Paragraph("✨ Thank you for choosing Premium Vehicle Rental! ✨", 
                                         new Font(smallFont.getBaseFont(), 12, Font.BOLD, BaseColor.WHITE));
        thankYou.setAlignment(Element.ALIGN_CENTER);
        thankYou.setSpacingAfter(8f);
        footerCell.addElement(thankYou);
        
        // Slogan
        Paragraph slogan = new Paragraph("Your Journey, Our Priority • Premium Service Since 2020", 
                                       new Font(smallFont.getBaseFont(), 9, Font.NORMAL, new BaseColor(200, 220, 255)));
        slogan.setAlignment(Element.ALIGN_CENTER);
        slogan.setSpacingAfter(12f);
        footerCell.addElement(slogan);
        
        // Contact info in structured format
        PdfPTable contactTable = new PdfPTable(3);
        contactTable.setWidthPercentage(100);
        contactTable.setWidths(new float[]{1f, 1f, 1f});
        
        // Phone
        PdfPCell phoneCell = new PdfPCell();
        phoneCell.setBorder(Rectangle.NO_BORDER);
        phoneCell.setPadding(5f);
        
        Paragraph phoneTitle = new Paragraph(" HOTLINE", 
                                           new Font(smallFont.getBaseFont(), 8, Font.BOLD, BaseColor.WHITE));
        phoneTitle.setAlignment(Element.ALIGN_CENTER);
        phoneTitle.setSpacingAfter(2f);
        phoneCell.addElement(phoneTitle);
        
        Paragraph phoneNumber = new Paragraph("+1-800-RENTAL", 
                                            new Font(smallFont.getBaseFont(), 9, Font.NORMAL, new BaseColor(200, 220, 255)));
        phoneNumber.setAlignment(Element.ALIGN_CENTER);
        phoneCell.addElement(phoneNumber);
        
        contactTable.addCell(phoneCell);
        
        // Email
        PdfPCell emailCell = new PdfPCell();
        emailCell.setBorder(Rectangle.NO_BORDER);
        emailCell.setPadding(5f);
        
        Paragraph emailTitle = new Paragraph("📧 SUPPORT", 
                                           new Font(smallFont.getBaseFont(), 8, Font.BOLD, BaseColor.WHITE));
        emailTitle.setAlignment(Element.ALIGN_CENTER);
        emailTitle.setSpacingAfter(2f);
        emailCell.addElement(emailTitle);
        
        Paragraph emailAddress = new Paragraph("support@premium.com", 
                                             new Font(smallFont.getBaseFont(), 9, Font.NORMAL, new BaseColor(200, 220, 255)));
        emailAddress.setAlignment(Element.ALIGN_CENTER);
        emailCell.addElement(emailAddress);
        
        contactTable.addCell(emailCell);
        
        // Website
        PdfPCell websiteCell = new PdfPCell();
        websiteCell.setBorder(Rectangle.NO_BORDER);
        websiteCell.setPadding(5f);
        
        Paragraph websiteTitle = new Paragraph("🌐 WEBSITE", 
                                             new Font(smallFont.getBaseFont(), 8, Font.BOLD, BaseColor.WHITE));
        websiteTitle.setAlignment(Element.ALIGN_CENTER);
        websiteTitle.setSpacingAfter(2f);
        websiteCell.addElement(websiteTitle);
        
        Paragraph websiteUrl = new Paragraph("www.premium-rental.com", 
                                           new Font(smallFont.getBaseFont(), 9, Font.NORMAL, new BaseColor(200, 220, 255)));
        websiteUrl.setAlignment(Element.ALIGN_CENTER);
        websiteCell.addElement(websiteUrl);
        
        contactTable.addCell(websiteCell);
        
        footerCell.addElement(contactTable);
        
        // Divider line
        PdfPTable dividerTable = new PdfPTable(1);
        dividerTable.setWidthPercentage(80);
        dividerTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        dividerTable.setSpacingBefore(10f);
        dividerTable.setSpacingAfter(8f);
        
        PdfPCell dividerCell = new PdfPCell();
        dividerCell.setBorder(Rectangle.NO_BORDER);
        dividerCell.setBackgroundColor(new BaseColor(100, 116, 139));
        dividerCell.setFixedHeight(1f);
        dividerTable.addCell(dividerCell);
        
        footerCell.addElement(dividerTable);
        
        // Final note
        Paragraph finalNote = new Paragraph("📱 Save this ticket to your phone • Print or show digitally for pickup", 
                                          new Font(smallFont.getBaseFont(), 8, Font.ITALIC, new BaseColor(156, 163, 175)));
        finalNote.setAlignment(Element.ALIGN_CENTER);
        footerCell.addElement(finalNote);
        
        footerTable.addCell(footerCell);
        document.add(footerTable);
    }
} 