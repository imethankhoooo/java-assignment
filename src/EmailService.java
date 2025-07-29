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
    
    // SMTP配置 - 从配置文件读取
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
            senderEmail = props.getProperty("sender.email", "your-rental-system@gmail.com");
            senderPassword = props.getProperty("sender.password", "your-app-password");
            useTLS = Boolean.parseBoolean(props.getProperty("smtp.use_tls", "true"));
            useAuth = Boolean.parseBoolean(props.getProperty("smtp.use_auth", "true"));
            
        } catch (IOException e) {
            // 只在真正失败时显示警告
            System.err.println("Warning: Could not load smtp_config.properties, using default values");
            // 使用默认值
            smtpHost = "smtp.gmail.com";
            smtpPort = 587;
            senderEmail = "your-rental-system@gmail.com";
            senderPassword = "your-app-password";
            useTLS = true;
            useAuth = true;
        }
    }
    
    public boolean sendEmail(String recipientEmail, String subject, String content) {
        return sendEmailWithAttachment(recipientEmail, subject, content, null, null);
    }
    
    public boolean sendEmailWithAttachment(String recipientEmail, String subject, String content, byte[] attachmentData, String attachmentName) {
        try {
            if (useAuth && (senderEmail.equals("your-rental-system@gmail.com") || senderPassword.equals("your-app-password"))) {
                // 如果还在使用默认配置，回退到控制台显示
                System.out.println("=== SMTP NOT CONFIGURED ===");
                System.out.println("Please update smtp_config.properties with your email settings");
                fallbackToConsoleDisplay(recipientEmail, subject, content);
                if (attachmentData != null) {
                    System.out.println("Attachment: " + attachmentName + " (" + attachmentData.length + " bytes)");
                }
                return false;
            }
            
            // 尝试SMTP发送
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
                // SMTP失败，回退到控制台显示
                fallbackToConsoleDisplay(recipientEmail, subject, content);
                if (attachmentData != null) {
                    System.out.println("Attachment: " + attachmentName + " (" + attachmentData.length + " bytes)");
                }
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to send email via SMTP to: " + recipientEmail, e);
            System.err.println("SMTP Email Error: " + e.getMessage());
            
            // 如果SMTP失败，回退到控制台显示
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
            // 连接到SMTP服务器
            socket = new Socket(smtpHost, smtpPort);
            socket.setSoTimeout(10000); // 10秒超时
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            
            // 读取服务器响应
            String response = reader.readLine();
            if (!response.startsWith("220")) {
                throw new IOException("SMTP服务器连接失败: " + response);
            }
            
            // EHLO命令
            writer.println("EHLO " + smtpHost);
            
            // 读取EHLO响应（可能有多行）
            response = reader.readLine();
            if (!response.startsWith("250")) {
                throw new IOException("EHLO命令失败: " + response);
            }
            
            // 读取剩余的EHLO响应行
            while (reader.ready()) {
                String line = reader.readLine();
                if (line.startsWith("250 ")) break; // 最后一行以"250 "开头
            }
            
            // 如果使用TLS，启动TLS并进行SSL握手
            if (useTLS) {
                writer.println("STARTTLS");
                response = reader.readLine();
                if (!response.startsWith("220")) {
                    // STARTTLS失败，尝试不使用TLS继续
                } else {
                    // 创建SSL上下文
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, null, null);
                    
                    // 创建SSL Socket
                    SSLSocketFactory factory = sslContext.getSocketFactory();
                    SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, smtpHost, smtpPort, true);
                    sslSocket.setUseClientMode(true);
                    sslSocket.startHandshake();
                    
                    // 更新reader和writer以使用SSL连接
                    reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                    writer = new PrintWriter(sslSocket.getOutputStream(), true);
                    socket = sslSocket; // 更新socket引用
                }
            }
            
            // 身份验证
            if (useAuth) {
                writer.println("AUTH LOGIN");
                response = reader.readLine();
                if (!response.startsWith("334")) {
                    throw new IOException("AUTH LOGIN命令失败: " + response);
                }
                
                // 发送用户名（Base64编码）
                String encodedUsername = Base64.getEncoder().encodeToString(senderEmail.getBytes());
                writer.println(encodedUsername);
                response = reader.readLine();
                if (!response.startsWith("334")) {
                    throw new IOException("用户名验证失败: " + response);
                }
                
                // 发送密码（Base64编码）
                String encodedPassword = Base64.getEncoder().encodeToString(senderPassword.getBytes());
                writer.println(encodedPassword);
                response = reader.readLine();
                if (!response.startsWith("235")) {
                    throw new IOException("密码验证失败: " + response);
                }
            }
            
            // MAIL FROM命令
            writer.println("MAIL FROM:<" + senderEmail + ">");
            response = reader.readLine();
            if (!response.startsWith("250")) {
                throw new IOException("MAIL FROM命令失败: " + response);
            }
            
            // RCPT TO命令
            writer.println("RCPT TO:<" + recipientEmail + ">");
            response = reader.readLine();
            if (!response.startsWith("250")) {
                throw new IOException("RCPT TO命令失败: " + response);
            }
            
            // DATA命令
            writer.println("DATA");
            response = reader.readLine();
            if (!response.startsWith("354")) {
                throw new IOException("DATA命令失败: " + response);
            }
            
            // 发送邮件头和内容
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
            writer.println("From: " + senderEmail);
            writer.println("To: " + recipientEmail);
            writer.println("Subject: " + subject);
            writer.println("Date: " + dateFormat.format(new Date()));
            
            if (attachmentData != null && attachmentName != null) {
                // 发送带附件的MIME邮件
                String boundary = "----=_NextPart_" + System.currentTimeMillis();
                writer.println("MIME-Version: 1.0");
                writer.println("Content-Type: multipart/mixed; boundary=\"" + boundary + "\"");
                writer.println();
                writer.println("This is a multi-part message in MIME format.");
                writer.println();
                
                // 文本部分
                writer.println("--" + boundary);
                writer.println("Content-Type: text/plain; charset=UTF-8");
                writer.println("Content-Transfer-Encoding: 8bit");
                writer.println();
                writer.println(content);
                writer.println();
                
                // 附件部分
                writer.println("--" + boundary);
                writer.println("Content-Type: application/pdf; name=\"" + attachmentName + "\"");
                writer.println("Content-Transfer-Encoding: base64");
                writer.println("Content-Disposition: attachment; filename=\"" + attachmentName + "\"");
                writer.println();
                
                // Base64编码附件
                String encodedAttachment = Base64.getEncoder().encodeToString(attachmentData);
                // 按每行76个字符分割base64数据
                for (int i = 0; i < encodedAttachment.length(); i += 76) {
                    int end = Math.min(i + 76, encodedAttachment.length());
                    writer.println(encodedAttachment.substring(i, end));
                }
                writer.println();
                writer.println("--" + boundary + "--");
            } else {
                // 发送纯文本邮件
                writer.println("Content-Type: text/plain; charset=UTF-8");
                writer.println();
                writer.println(content);
            }
            writer.println(".");
            
            response = reader.readLine();
            if (!response.startsWith("250")) {
                throw new IOException("邮件发送失败: " + response);
            }
            
            // QUIT命令
            writer.println("QUIT");
            response = reader.readLine();
            
            return true;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "SMTP发送失败", e);
            return false;
        } finally {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "关闭SMTP连接时出错", e);
            }
        }
    }
    
    // 备用方法：如果SMTP失败，在控制台显示邮件内容
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
        
        return sendEmail(recipientEmail, subject, content);
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
        
        return sendEmail(recipientEmail, subject, content);
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
        
        return sendEmail(recipientEmail, subject, content);
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
        
        return sendEmail(recipientEmail, subject, content);
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
        
        return sendEmail(recipientEmail, subject, content);
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
        
        return sendEmail(recipientEmail, subject, content);
    }
    
    // 测试邮件配置
    public boolean testEmailConfiguration() {
        try {
            String testSubject = "Test Email - Rental System SMTP";
            String testContent = "This is a test email to verify SMTP configuration.\n" +
            "If you receive this email, SMTP is working correctly!";
            return sendEmail(senderEmail, testSubject, testContent);
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
    
    // 重新加载配置
    public void reloadConfiguration() {
        loadConfiguration();
        logger.info("SMTP configuration reloaded");
    }
} 