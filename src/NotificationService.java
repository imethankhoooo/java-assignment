import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class NotificationService {
    private EmailService emailService;
    private MessageService messageService;
    private Map<String, String> userEmails; // 用户邮箱映射
    
    public NotificationService() {
        this.emailService = new EmailService();
        this.messageService = new MessageService();
        this.userEmails = new HashMap<>();
        loadUserEmails();
    }
    
    // 加载用户邮箱信息（实际应用中应从用户数据库获取）
    private void loadUserEmails() {
        // 示例邮箱映射，实际应从用户系统获取
        userEmails.put("admin", "admin@rental-system.com");
        userEmails.put("user1", "user1@example.com");
        userEmails.put("user2", "user2@example.com");
        // 可以从配置文件或数据库加载更多用户邮箱
    }
    
    // 从账户列表加载用户邮箱
    public void loadUserEmailsFromAccounts(List<Account> accounts) {
        for (Account account : accounts) {
            if (account.getEmail() != null && !account.getEmail().isEmpty()) {
                userEmails.put(account.getUsername(), account.getEmail());
            }
        }
    }
    
    // 设置用户邮箱
    public void setUserEmail(String username, String email) {
        userEmails.put(username, email);
    }
    
    // 获取用户邮箱
    public String getUserEmail(String username) {
        return userEmails.get(username);
    }
    
    // 发送租赁确认通知
    public boolean sendRentalConfirmation(String username, String vehicleModel, String startDate, String endDate, double totalFee) {
        String subject = "Rental Confirmation - " + vehicleModel;
        String content = String.format(
            "Your rental booking has been confirmed!\r\n\r\n" +
            "Vehicle: %s\r\n" +
            "Rental Period: %s to %s\r\n" +
            "Total Fee: RM%.2f\r\n\r\n" +
            "Please bring a valid driver's license when picking up the vehicle.",
            vehicleModel, startDate, endDate, totalFee
        );
        
        // 发送系统消息
        messageService.sendMessage("system", username, subject, content, MessageType.RENTAL_CONFIRMATION);
        
        // 发送邮件（如果用户有邮箱）
        String email = getUserEmail(username);
        if (email != null) {
            return emailService.sendRentalConfirmation(email, username, vehicleModel, startDate, endDate, totalFee);
        }
        
        return true;
    }
    
    // 发送租赁批准通知
    public boolean sendRentalApproval(String username, String vehicleModel, String rentalId) {
        String subject = "Rental Approved - " + vehicleModel;
        String content = String.format(
            "Great news! Your rental request for %s has been approved.\n" +
            "You can now proceed to pick up the vehicle during our business hours.\n" +
            "Please bring your driver's license and payment confirmation.",
            vehicleModel
        );
        
        // 发送系统消息
        messageService.sendRentalMessage("admin", username, subject, content, MessageType.RENTAL_APPROVAL, rentalId);
        
        // 发送邮件
        String email = getUserEmail(username);
        if (email != null) {
            return emailService.sendRentalApproval(email, username, vehicleModel);
        }
        
        return true;
    }
    
    // 发送租赁批准通知（带票据信息）
    public boolean sendRentalApprovalWithTicket(String username, String vehicleModel, String rentalId, String ticketId) {
        String subject = "Rental Approved - Ticket Generated - " + vehicleModel;
        String content = String.format(
            "Great news! Your rental request for %s has been approved.\n" +
            "Your rental ticket has been generated: %s\n" +
            "IMPORTANT: Please save this ticket ID. You will need it for vehicle pickup.\n" +
            "Pickup Instructions:\n" +
            "- Bring valid government-issued ID\n" +
            "- Present your ticket ID: %s\n" +
            "- Arrive 15 minutes before rental start time\n" +
            "- Vehicle inspection will be conducted before handover\n" +
            "You can view your ticket details in the customer portal.",
            vehicleModel, ticketId, ticketId
        );
        
        // 发送系统消息
        messageService.sendRentalMessage("admin", username, subject, content, MessageType.RENTAL_APPROVAL, rentalId);
        
        // 发送邮件
        String email = getUserEmail(username);
        if (email != null) {
            return emailService.sendRentalApprovalWithTicket(email, username, vehicleModel, ticketId);
        }
        
        return true;
    }
    
    // 发送租赁批准通知（带PDF票据）
    public boolean sendRentalApprovalWithPdfTicket(String username, String vehicleModel, String rentalId, String ticketId, byte[] pdfTicket) {
        String subject = "Rental Approved - PDF Ticket Attached - " + vehicleModel;
        String content = String.format(
            "Excellent news! Your rental request for %s has been approved!\n" +
            "Your rental ticket has been generated: %s\n" +
            "A beautiful PDF ticket has been sent to your email with all the details.\n" +
            "IMPORTANT: Please save the PDF ticket. You will need it for vehicle pickup.\n" +
            "Pickup Instructions:\n" +
            "- Bring valid government-issued ID\n" +
            "- Present your PDF ticket (digital or printed copy)\n" +
            "- Arrive 15 minutes before rental start time\n" +
            "- Vehicle inspection will be conducted before handover\n" +
            "Check your email for the complete PDF ticket!",
            vehicleModel, ticketId
        );
        
        // 发送系统消息
        messageService.sendRentalMessage("admin", username, subject, content, MessageType.RENTAL_APPROVAL, rentalId);
        
        // 发送带PDF附件的邮件
        String email = getUserEmail(username);
        if (email != null) {
            return emailService.sendRentalApprovalWithPdfTicket(email, username, vehicleModel, ticketId, pdfTicket);
        }
        
        return true;
    }
    
    // 发送租赁拒绝通知
    public boolean sendRentalRejection(String username, String vehicleModel, String reason, String rentalId) {
        String subject = "Rental Request Declined - " + vehicleModel;
        String content = String.format(
            "We regret to inform you that your rental request for %s has been declined.\n" +
            "Reason: %s\n" +
            "Please contact us if you have any questions.",
            vehicleModel, reason
        );
        
        // 发送系统消息
        messageService.sendRentalMessage("admin", username, subject, content, MessageType.RENTAL_REJECTION, rentalId);
        
        // 发送邮件
        String email = getUserEmail(username);
        if (email != null) {
            return emailService.sendRentalRejection(email, username, vehicleModel, reason);
        }
        
        return true;
    }
    
    // 发送租赁提醒
    public boolean sendRentalReminder(String username, String vehicleModel, String dueDate, String rentalId) {
        String subject = "Rental Reminder - " + vehicleModel;
        String content = String.format(
            "This is a reminder that your rental of %s is due on %s.\n" +
            "Please ensure to return the vehicle on time to avoid additional charges.",
            vehicleModel, dueDate
        );
        
        // 发送系统消息
        messageService.sendRentalMessage("system", username, subject, content, MessageType.RENTAL_REMINDER, rentalId);
        
        // 发送邮件
        String email = getUserEmail(username);
        if (email != null) {
            return emailService.sendRentalReminder(email, username, vehicleModel, dueDate);
        }
        
        return true;
    }
    
    // 发送逾期通知
    public boolean sendOverdueNotification(String username, String vehicleModel, String dueDate, String rentalId) {
        String subject = "OVERDUE RENTAL - " + vehicleModel;
        String content = String.format(
            "Your rental of %s was due on %s and is now OVERDUE.\n" +
            "Please return the vehicle immediately to avoid additional penalties.\n" +
            "Late fees may apply according to our rental agreement.",
            vehicleModel, dueDate
        );
        
        // 发送系统消息
        messageService.sendRentalMessage("system", username, subject, content, MessageType.OVERDUE_NOTIFICATION, rentalId);
        
        // 发送邮件
        String email = getUserEmail(username);
        if (email != null) {
            return emailService.sendOverdueNotification(email, username, vehicleModel, dueDate);
        }
        
        return true;
    }
    
    // 发送管理员通知
    public boolean sendAdminNotification(String subject, String content) {
        messageService.sendMessage("system", "admin", subject, content, MessageType.ADMIN_NOTIFICATION);
        
        String adminEmail = getUserEmail("admin");
        if (adminEmail != null) {
            return emailService.sendEmail(adminEmail, subject, content);
        }
        
        return true;
    }
    
    // 发送用户消息
    public boolean sendUserMessage(String fromUser, String toUser, String subject, String content) {
        // 发送内部消息
        boolean messageSuccess = messageService.sendMessage(fromUser, toUser, subject, content, MessageType.USER_MESSAGE);
        
        // 同时发送邮件通知
        String recipientEmail = getUserEmail(toUser);
        boolean emailSuccess = true;
        
        if (recipientEmail != null && !recipientEmail.isEmpty()) {
            String emailSubject = "New Message from " + fromUser + " - " + subject;
            String emailContent = String.format(
                "You have received a new message in the Vehicle Rental System.\n\n" +
                "From: %s\n" +
                "Subject: %s\n" +
                "Message: %s\n\n" +
                "Please log in to the system to view and respond to this message.\n\n" +
                "Best regards,\n" +
                "Vehicle Rental System",
                fromUser, subject, content
            );
            
            emailSuccess = emailService.sendEmail(recipientEmail, emailSubject, emailContent);
            
            if (emailSuccess) {
                System.out.println("Email notification sent to " + recipientEmail);
            } else {
                System.out.println("Failed to send email notification to " + recipientEmail);
            }
        } else {
            System.out.println("No email address found for user: " + toUser);
        }
        
        return messageSuccess; // 返回内部消息发送状态
    }
    
    // 检查并发送到期提醒
    public void checkAndSendReminders(List<Rental> rentals) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        
        for (Rental rental : rentals) {
            if (rental.getStatus() == RentalStatus.ACTIVE) {
                LocalDate endDate = rental.getEndDate();
                
                // 发送明天到期提醒 (只发送一次)
                if (endDate.equals(tomorrow) && !rental.isDueSoonReminderSent()) {
                    sendRentalReminder(rental.getUsername(), rental.getVehicle().getModel(), 
                                    rental.getEndDate().toString(), String.valueOf(rental.getId()));
                    rental.setDueSoonReminderSent(true);
                }
                
                // 发送逾期通知 (只发送一次)
                if (endDate.isBefore(today) && !rental.isOverdueReminderSent()) {
                    sendOverdueNotification(rental.getUsername(), rental.getVehicle().getModel(), 
                                          rental.getEndDate().toString(), String.valueOf(rental.getId()));
                    rental.setOverdueReminderSent(true);
                }
            }
        }
    }
    
    // 获取用户消息
    public List<Message> getUserMessages(String username) {
        return messageService.getMessagesByUser(username);
    }
    
    // 获取未读消息
    public List<Message> getUnreadMessages(String username) {
        return messageService.getUnreadMessages(username);
    }
    
    // 标记消息为已读
    public boolean markMessageAsRead(String messageId) {
        return messageService.markAsRead(messageId);
    }
    
    // 删除消息
    public boolean deleteMessage(String messageId) {
        return messageService.deleteMessage(messageId);
    }
    
    // 获取消息统计
    public Map<String, Integer> getMessageStats(String username) {
        return messageService.getMessageStats(username);
    }
    
    // 测试邮件配置
    public boolean testEmailConfiguration() {
        return emailService.testEmailConfiguration();
    }
    
    // 获取所有消息（管理员用）
    public List<Message> getAllMessages() {
        return messageService.getAllMessages();
    }
    
    // 获取EmailService实例（用于配置管理）
    public EmailService getEmailService() {
        return emailService;
    }
} 