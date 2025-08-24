package services;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import enums.MessageType;
import enums.RentalStatus;

import java.util.HashMap;
import models.*;

public class NotificationService {
    private EmailService emailService;
    private MessageService messageService;
    private Map<String, String> userEmails; // User email mapping
    
    public NotificationService() {
        this.emailService = new EmailService();
        this.messageService = new MessageService();
        this.userEmails = new HashMap<>();
        loadUserEmails();
    }
    
    private void loadUserEmails() {
        // Load user emails from AccountService when accounts are available
        if (AccountService.getAccounts() != null && !AccountService.getAccounts().isEmpty()) {
            loadUserEmailsFromAccounts(AccountService.getAccounts());
        }
    }
    
    // Load user emails from accounts
    public void loadUserEmailsFromAccounts(List<Account> accounts) {
        userEmails.clear();
        for (Account account : accounts) {
            if (account.getEmail() != null && !account.getEmail().isEmpty()) {
                userEmails.put(account.getUsername(), account.getEmail());
            }
        }
    }
    
    // Set user email
    public void setUserEmail(String username, String email) {
        userEmails.put(username, email);
    }
    
    // Get user email
    public String getUserEmail(String username) {
        return userEmails.get(username);
    }
    
    // Send rental confirmation notification
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
        
        // Send system message
        messageService.sendMessage("system", username, subject, content, MessageType.RENTAL_CONFIRMATION);
        
        // Send email (if user has email)
        String email = getUserEmail(username);
        if (email != null) {
            return emailService.sendRentalConfirmation(email, username, vehicleModel, startDate, endDate, totalFee);
        }
        
        return true;
    }
    
    // Send rental approval notification
    public boolean sendRentalApproval(String username, String vehicleModel, String rentalId) {
        String subject = "Rental Approved - " + vehicleModel;
        String content = String.format(
            "Great news! Your rental request for %s has been approved.\n" +
            "You can now proceed to pick up the vehicle during our business hours.\n" +
            "Please bring your driver's license and payment confirmation.",
            vehicleModel
        );
        
        // Send system message
        messageService.sendRentalMessage("admin", username, subject, content, MessageType.RENTAL_APPROVAL, rentalId);
        
        // Send email
        String email = getUserEmail(username);
        if (email != null) {
            return emailService.sendRentalApproval(email, username, vehicleModel);
        }
        
        return true;
    }
    
    // Send rental approval notification (with ticket information)
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
        
        // Send system message
        messageService.sendRentalMessage("admin", username, subject, content, MessageType.RENTAL_APPROVAL, rentalId);
        
        // Send email
        String email = getUserEmail(username);
        if (email != null) {
            return emailService.sendRentalApprovalWithTicket(email, username, vehicleModel, ticketId);
        }
        
        return true;
    }
    
    // Send rental approval notification (with PDF ticket)
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
        
        // Send system message
        messageService.sendRentalMessage("admin", username, subject, content, MessageType.RENTAL_APPROVAL, rentalId);
        
        // Send email with PDF attachment
        String email = getUserEmail(username);
        if (email != null) {
            return emailService.sendRentalApprovalWithPdfTicket(email, username, vehicleModel, ticketId, pdfTicket);
        }
        
        return true;
    }
    
    // Send rental rejection notification
    public boolean sendRentalRejection(String username, String vehicleModel, String reason, String rentalId) {
        String subject = "Rental Request Declined - " + vehicleModel;
        String content = String.format(
            "We regret to inform you that your rental request for %s has been declined.\n" +
            "Reason: %s\n" +
            "Please contact us if you have any questions.",
            vehicleModel, reason
        );
        
        // Send system message
        messageService.sendRentalMessage("admin", username, subject, content, MessageType.RENTAL_REJECTION, rentalId);
        
        // Send email
        String email = getUserEmail(username);
        if (email != null) {
            return emailService.sendRentalRejection(email, username, vehicleModel, reason);
        }
        
        return true;
    }
    
    // Send rental reminder
    public boolean sendRentalReminder(String username, String vehicleModel, String dueDate, String rentalId) {
        String subject = "Rental Reminder - " + vehicleModel;
        String content = String.format(
            "This is a reminder that your rental of %s is due on %s.\n" +
            "Please ensure to return the vehicle on time to avoid additional charges.",
            vehicleModel, dueDate
        );
        
        // Send system message
        messageService.sendRentalMessage("system", username, subject, content, MessageType.RENTAL_REMINDER, rentalId);
        
        // Send email
        String email = getUserEmail(username);
        if (email != null) {
            return emailService.sendRentalReminder(email, username, vehicleModel, dueDate);
        }
        
        return true;
    }
    
    // Send overdue notification
    public boolean sendOverdueNotification(String username, String vehicleModel, String dueDate, String rentalId) {
        String subject = "OVERDUE RENTAL - " + vehicleModel;
        String content = String.format(
            "Your rental of %s was due on %s and is now OVERDUE.\n" +
            "Please return the vehicle immediately to avoid additional penalties.\n" +
            "Late fees may apply according to our rental agreement.",
            vehicleModel, dueDate
        );
        
        // Send system message
        messageService.sendRentalMessage("system", username, subject, content, MessageType.OVERDUE_NOTIFICATION, rentalId);
        
        // Send email
        String email = getUserEmail(username);
        if (email != null) {
            return emailService.sendOverdueNotification(email, username, vehicleModel, dueDate);
        }
        
        return true;
    }
    
    // Send admin notification
    public boolean sendAdminNotification(String subject, String content) {
        messageService.sendMessage("system", "admin", subject, content, MessageType.ADMIN_NOTIFICATION);
        
        String adminEmail = getUserEmail("admin");
        if (adminEmail != null) {
            return emailService.sendEmailWithAttachment(adminEmail, subject, content, null, null);
        }
        
        return true;
    }
    
    // Send user message
    public boolean sendUserMessage(String fromUser, String toUser, String subject, String content) {
        // Send internal message
        boolean messageSuccess = messageService.sendMessage(fromUser, toUser, subject, content, MessageType.USER_MESSAGE);
        
        // Send email notification
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
            
            emailSuccess = emailService.sendEmailWithAttachment(recipientEmail, emailSubject, emailContent, null, null);
            
            if (emailSuccess) {
                System.out.println("Email notification sent to " + recipientEmail);
                // Update the email sent status for the message
                messageService.updateEmailSentStatus(fromUser, toUser, subject, true);
            } else {
                System.out.println("Failed to send email notification to " + recipientEmail);
                messageService.updateEmailSentStatus(fromUser, toUser, subject, false);
            }
        } else {
            System.out.println("No email address found for user: " + toUser);
            messageService.updateEmailSentStatus(fromUser, toUser, subject, false);
        }
        
        return messageSuccess; // Return internal message send status
    }
    
    // Check and send overdue reminder
    public void checkAndSendReminders(List<Rental> rentals) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        
        for (Rental rental : rentals) {
            if (rental.getStatus() == RentalStatus.ACTIVE) {
                LocalDate endDate = rental.getEndDate();
                
                // Send tomorrow overdue reminder (only send once)
                if (endDate.equals(tomorrow) && !rental.isDueSoonReminderSent()) {
                    sendRentalReminder(rental.getUsername(), rental.getVehicle().getModel(), 
                                    rental.getEndDate().toString(), String.valueOf(rental.getId()));
                    rental.setDueSoonReminderSent(true);
                }
                
                // Send overdue notification (only send once)
                if (endDate.isBefore(today) && !rental.isOverdueReminderSent()) {
                    sendOverdueNotification(rental.getUsername(), rental.getVehicle().getModel(), 
                                          rental.getEndDate().toString(), String.valueOf(rental.getId()));
                    rental.setOverdueReminderSent(true);
                }
            }
        }
    }
    
    // Get user messages
    public List<Message> getUserMessages(String username) {
        return messageService.getMessagesByUser(username);
    }
    
    // Get unread messages
    public List<Message> getUnreadMessages(String username) {
        return messageService.getUnreadMessages(username);
    }
    
    // Mark message as read
    public boolean markMessageAsRead(String messageId) {
        return messageService.markAsRead(messageId);
    }
    
    // Delete message
    public boolean deleteMessage(String messageId) {
        return messageService.deleteMessage(messageId);
    }
    
    // Get message statistics
    public Map<String, Integer> getMessageStats(String username) {
        return messageService.getMessageStats(username);
    }
    
    // Test email configuration
    public boolean testEmailConfiguration() {
        return emailService.testEmailConfiguration();
    }
    
    // Get all messages (for admin)
    public List<Message> getAllMessages() {
        return messageService.getAllMessages();
    }
    
    // Get EmailService instance (for configuration management)
    public EmailService getEmailService() {
        return emailService;
    }
} 