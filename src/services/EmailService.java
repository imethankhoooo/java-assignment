package services;
import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.net.ssl.*;

public class EmailService {
    private static final Logger logger = Logger.getLogger(EmailService.class.getName());
    
    // SMTP Configuration - Read from Configuration File
    private String smtpHost;
    private int smtpPort;
    private String senderEmail;
    private String senderPassword;
    private boolean useTLS;
    private boolean useAuth;
    
    public EmailService() {
        loadConfiguration();
    }
    
    private void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("smtp_config.properties")) {
            props.load(input);
            
            smtpHost = props.getProperty("smtp.host", "smtp.gmail.com");
            smtpPort = Integer.parseInt(props.getProperty("smtp.port", "587"));
            senderEmail = props.getProperty("sender.email", "ethankhoo09@gmail.com");
            senderPassword = props.getProperty("sender.password", "wkxeqfqsvwldtodm");
            useTLS = Boolean.parseBoolean(props.getProperty("smtp.use_tls", "true"));
            useAuth = Boolean.parseBoolean(props.getProperty("smtp.use_auth", "true"));
            
        } catch (IOException e) {
            // Show warnings only on true failures
            System.err.println("Warning: Could not load smtp_config.properties, using default values");
            // Use default values
            smtpHost = "smtp.gmail.com";
            smtpPort = 587;
            senderEmail = "ethankhoo09@gmail.com";
            senderPassword = "wkxeqfqsvwldtodm";
            useTLS = true;
            useAuth = true;
        }
    }
    

    
    public boolean sendEmailWithAttachment(String recipientEmail, String subject, String content, byte[] attachmentData, String attachmentName) {
        try {
            
            // Attempt SMTP sending
            boolean success = sendViaSMTP(recipientEmail, subject, content, attachmentData, attachmentName);
            
            if (success) {
                System.out.println("=== EMAIL SENT VIA SMTP ===");
                System.out.println("To: " + recipientEmail);
                System.out.println("Subject: " + subject);
                if (attachmentData != null) {
                    System.out.println("Attachment: " + attachmentName);
                }
                System.out.println("===========================");
                System.out.println("Email sent successfully via SMTP to: " + recipientEmail);
                return true;
            } else {
                // SMTP failed, fallback to console display
                fallbackToConsoleDisplay(recipientEmail, subject, content);
                if (attachmentData != null) {
                    System.out.println("Attachment: " + attachmentName + " (" + attachmentData.length + " bytes)");
                }
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to send email via SMTP to: " + recipientEmail, e);
            System.err.println("SMTP Email Error: " + e.getMessage());
            
            // If SMTP fails, fallback to console display
            fallbackToConsoleDisplay(recipientEmail, subject, content);
            if (attachmentData != null) {
                System.out.println("Attachment: " + attachmentName + " (" + attachmentData.length + " bytes)");
            }
            return false;
        }
    }
    
    private boolean sendViaSMTP(String recipientEmail, String subject, String content, byte[] attachmentData, String attachmentName) {
        Socket socket = null;
        BufferedReader reader = null;
        PrintWriter writer = null;
        
        try {
            // Connect to SMTP server
            socket = new Socket(smtpHost, smtpPort);
            socket.setSoTimeout(10000); // 10 seconds timeout
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            
            // Read server response
            String response = reader.readLine();
            if (!response.startsWith("220")) {
                throw new IOException("SMTP server connection failure: " + response);
            }
            
            // EHLO command
            writer.println("EHLO " + smtpHost);
            
            // Read EHLO response (may have multiple lines)
            response = reader.readLine();
            if (!response.startsWith("250")) {
                throw new IOException("EHLO command failed: " + response);
            }
            
            // Read remaining EHLO response lines
            while (reader.ready()) {
                String line = reader.readLine();
                if (line.startsWith("250 ")) break; // Last line starts with "250 "
            }
            
            // If using TLS, start TLS and perform SSL handshake
            if (useTLS) {
                writer.println("STARTTLS");
                response = reader.readLine();
                if (!response.startsWith("220")) {
                    // STARTTLS failed, try to continue without TLS
                } else {
                    // Create SSL context
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, null, null);
                    
                    // Create SSL Socket
                    SSLSocketFactory factory = sslContext.getSocketFactory();
                    SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, smtpHost, smtpPort, true);
                    sslSocket.setUseClientMode(true);
                    sslSocket.startHandshake();
                    
                    // Update reader and writer to use SSL connection
                    reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                    writer = new PrintWriter(sslSocket.getOutputStream(), true);
                    socket = sslSocket; // Update socket reference
                }
            }
            
            // Authentication
            if (useAuth) {
                writer.println("AUTH LOGIN");
                response = reader.readLine();
                if (!response.startsWith("334")) {
                    throw new IOException("AUTH LOGIN command failed: " + response);
                }
                
                // Send username (Base64 encoded)
                String encodedUsername = Base64.getEncoder().encodeToString(senderEmail.getBytes());
                writer.println(encodedUsername);
                response = reader.readLine();
                if (!response.startsWith("334")) {
                    throw new IOException("Username authentication failed: " + response);
                }
                
                // Send password (Base64 encoded)
                String encodedPassword = Base64.getEncoder().encodeToString(senderPassword.getBytes());
                writer.println(encodedPassword);
                response = reader.readLine();
                if (!response.startsWith("235")) {
                    throw new IOException("Password authentication failed: " + response);
                }
            }
            
            // MAIL FROM command
            writer.println("MAIL FROM:<" + senderEmail + ">");
            response = reader.readLine();
            if (!response.startsWith("250")) {
                throw new IOException("MAIL FROM command failed: " + response);
            }
            
            // RCPT TO command
            writer.println("RCPT TO:<" + recipientEmail + ">");
            response = reader.readLine();
            if (!response.startsWith("250")) {
                throw new IOException("RCPT TO command failed: " + response);
            }
            
            // DATA command
            writer.println("DATA");
            response = reader.readLine();
            if (!response.startsWith("354")) {
                throw new IOException("DATA command failed: " + response);
            }
            
            // Send email headers and content
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
            writer.println("From: " + senderEmail);
            writer.println("To: " + recipientEmail);
            writer.println("Subject: " + subject);
            writer.println("Date: " + dateFormat.format(new Date()));
            
            if (attachmentData != null && attachmentName != null) {
                // Send MIME email with attachment
                String boundary = "----=_NextPart_" + System.currentTimeMillis();
                writer.println("MIME-Version: 1.0");
                writer.println("Content-Type: multipart/mixed; boundary=\"" + boundary + "\"");
                writer.println();
                writer.println("This is a multi-part message in MIME format.");
                writer.println();
                
                // Text part
                writer.println("--" + boundary);
                writer.println("Content-Type: text/plain; charset=UTF-8");
                writer.println("Content-Transfer-Encoding: 8bit");
                writer.println();
                writer.println(content);
                writer.println();
                
                // Attachment part
                writer.println("--" + boundary);
                writer.println("Content-Type: application/pdf; name=\"" + attachmentName + "\"");
                writer.println("Content-Transfer-Encoding: base64");
                writer.println("Content-Disposition: attachment; filename=\"" + attachmentName + "\"");
                writer.println();
                
                // Base64 encode attachment
                String encodedAttachment = Base64.getEncoder().encodeToString(attachmentData);
                // Split base64 data into 76 characters per line
                for (int i = 0; i < encodedAttachment.length(); i += 76) {
                    int end = Math.min(i + 76, encodedAttachment.length());
                    writer.println(encodedAttachment.substring(i, end));
                }
                writer.println();
                writer.println("--" + boundary + "--");
            } else {
                // Send plain text email
                writer.println("Content-Type: text/plain; charset=UTF-8");
                writer.println();
                writer.println(content);
            }
            writer.println(".");
            
            response = reader.readLine();
            if (!response.startsWith("250")) {
                throw new IOException("Email sending failed: " + response);
            }
            
            // QUIT command
            writer.println("QUIT");
            response = reader.readLine();
            
            return true;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "SMTP sending failed", e);
            return false;
        } finally {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing SMTP connection", e);
            }
        }
    }
    
    // Fallback method: If SMTP fails, display email content in console
    private void fallbackToConsoleDisplay(String recipientEmail, String subject, String content) {
        System.out.println("=== EMAIL (FALLBACK MODE) ===");
        System.out.println("To: " + recipientEmail);
        System.out.println("Subject: " + subject);
        System.out.println("Content: " + content);
        System.out.println("==============================");
    }
    
    public boolean sendRentalReminder(String recipientEmail, String username, String vehicleModel, String dueDate) {
        String subject = "Rental Reminder - " + vehicleModel;
        String content = String.format(
            "Dear %s,\n\n" +
            "This is a reminder that your rental of %s is due on %s.\n\n" +
            "Please ensure to return the vehicle on time to avoid additional charges.\n\n" +
            "Thank you for choosing our rental service!\n\n" +
            "Best regards,\n" +
            "Rental Management System",
            username, vehicleModel, dueDate
        );
        
        return sendEmailWithAttachment(recipientEmail, subject, content, null, null);
    }
    
    public boolean sendOverdueNotification(String recipientEmail, String username, String vehicleModel, String dueDate) {
        String subject = "OVERDUE RENTAL - " + vehicleModel;
        String content = String.format(
            "Dear %s,\n\n" +
            "Your rental of %s was due on %s and is now OVERDUE.\n\n" +
            "Please return the vehicle immediately to avoid additional penalties.\n" +
            "Late fees may apply according to our rental agreement.\n\n" +
            "Contact us immediately if you need assistance.\n\n" +
            "Best regards,\n" +
            "Rental Management System",
            username, vehicleModel, dueDate
        );
        
        return sendEmailWithAttachment(recipientEmail, subject, content, null, null);
    }
    
    public boolean sendRentalConfirmation(String recipientEmail, String username, String vehicleModel, String startDate, String endDate, double totalFee) {
        String subject = "Rental Confirmation - " + vehicleModel;
        String content = String.format(
            "Dear %s,\n" +
            "Your rental booking has been confirmed!\n" +
            "Vehicle: %s\n" +
            "Rental Period: %s to %s\n" +
            "Total Fee: RM%.2f\n" +
            "Please bring a valid driver's license and payment method when picking up the vehicle.\n" +
            "Thank you for choosing our rental service!\n" +
            "Best regards,\n" +
            "Rental Management System",
            username, vehicleModel, startDate, endDate, totalFee
        );
        
        return sendEmailWithAttachment(recipientEmail, subject, content, null, null);
    }
    
    public boolean sendRentalApproval(String recipientEmail, String username, String vehicleModel) {
        String subject = "Rental Approved - " + vehicleModel;
        String content = String.format(
            "Dear %s,\n" +
            "Great news! Your rental request for %s has been approved.\n" +
            "You can now proceed to pick up the vehicle during our business hours.\n" +
            "Please bring your driver's license and payment confirmation.\n" +
            "Thank you for choosing our rental service!\n" +
            "Best regards,\n" +
            "Rental Management System",
            username, vehicleModel
        );
        
        return sendEmailWithAttachment(recipientEmail, subject, content, null, null);
    }
    
    public boolean sendRentalApprovalWithTicket(String recipientEmail, String username, String vehicleModel, String ticketId) {
        String subject = "Rental Approved - Ticket Generated - " + vehicleModel;
        String content = String.format(
            "Dear %s,\n" +
            "Great news! Your rental request for %s has been approved!\n" +
            "Your rental confirmation ticket has been generated: %s\n" +
            "IMPORTANT PICKUP INSTRUCTIONS:\n" +
            "- Bring valid government-issued ID\n" +
            "- Present your ticket ID: %s\n" +
            "- Arrive 15 minutes before rental start time\n" +
            "- Vehicle inspection will be conducted before handover\n" +
            "You can view your complete ticket details in the customer portal.\n" +
            "Thank you for choosing our rental service!\n" +
            "Best regards,\n" +
            "Vehicle Rental Management System",
            username, vehicleModel, ticketId, ticketId
        );
        
        return sendEmailWithAttachment(recipientEmail, subject, content, null, null);
    }
    
    public boolean sendRentalApprovalWithPdfTicket(String recipientEmail, String username, String vehicleModel, String ticketId, byte[] pdfTicket) {
        String subject = "Rental Approved - PDF Ticket Attached - " + vehicleModel;
        String content = String.format(
            "Dear %s,\n\n" +
            " Excellent news! Your rental request for %s has been approved!\n\n" +
            " CONFIRMATION SUMMARY:\n" +
            "• Ticket ID: %s\n" +
            "• Vehicle: %s\n" +
            "• Status: APPROVED \n\n" +
            " Your beautifully designed PDF ticket is attached to this email with all the detailed information, pickup instructions, and important notes.\n\n" +
            " QUICK REMINDER:\n" +
            "• Save the PDF ticket to your phone\n" +
            "• All details are in the attached PDF\n" +
            "• Contact us if you need assistance\n\n" +
            "Thank you for choosing Premium Vehicle Rental!\n" +
            "Your journey, our priority. \n\n" +
            "Best regards,\n" +
            "Premium Vehicle Rental Team\n" +
            " support@premiumrental.com |  +1-800-RENTAL",
            username, vehicleModel, ticketId, vehicleModel
        );
        
        String pdfFilename = "rental_ticket_" + ticketId + ".pdf";
        return sendEmailWithAttachment(recipientEmail, subject, content, pdfTicket, pdfFilename);
    }
    
    public boolean sendRentalRejection(String recipientEmail, String username, String vehicleModel, String reason) {
        String subject = "Rental Request Declined - " + vehicleModel;
        String content = String.format(
            "Dear %s,\n" +
            "We regret to inform you that your rental request for %s has been declined.\n" +
            "Reason: %s\n" +
            "Please contact us if you have any questions or would like to make alternative arrangements.\n" +
            "Thank you for your understanding.\n" +
            "Best regards,\n" +
            "Rental Management System",
            username, vehicleModel, reason
        );
        
        return sendEmailWithAttachment(recipientEmail, subject, content, null, null);
    }
    
    // Test email configuration
    public boolean testEmailConfiguration() {
        try {
            String testSubject = "Test Email - Rental System SMTP";
            String testContent = "This is a test email to verify SMTP configuration.\n" +
            "If you receive this email, SMTP is working correctly!";
            return sendEmailWithAttachment(senderEmail, testSubject, testContent, null, null);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Email configuration test failed", e);
            return false;
        }
    }
    
    public void printConfiguration() {
        System.out.println("=== SMTP Configuration ===");
        System.out.println("Host: " + smtpHost);
        System.out.println("Port: " + smtpPort);
        System.out.println("Sender: " + senderEmail);
        System.out.println("TLS: " + useTLS);
        System.out.println("Auth: " + useAuth);
        System.out.println("==========================");
    }
    
    // Reload configuration
    public void reloadConfiguration() {
        loadConfiguration();
        logger.info("SMTP configuration reloaded");
    }
} 