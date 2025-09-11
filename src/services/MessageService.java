package services;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import enums.*;
import models.*;

public class MessageService {
    private static final String MESSAGES_FILE = "messages.json";
    private List<Message> messages;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // Message center
    public static void messageCenter(RentalSystem system, Scanner scanner, String username) {
        while (true) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                         MESSAGE CENTER                           ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. View Messages                                                 ║");
            System.out.println("║ 2. Send Message                                                  ║");
            System.out.println("║ 3. Delete Message                                                ║");
            System.out.println("║ 4. Message Statistics                                            ║");
            System.out.println("║ 0. Back to Main Menu                                             ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    viewMessagesMenu(system, scanner, username);
                    break;
                case "2":
                    sendMessage(system, scanner, username);
                    break;
                case "3":
                    deleteMessage(system, scanner, username);
                    break;
                case "4":
                    viewMessageStatistics(system, username);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    public MessageService() {
        this.messages = new ArrayList<>();
        loadMessages();
    }
    
    // Send message
    public boolean sendMessage(String sender, String recipient, String subject, String content, MessageType type) {
        Message message = new Message(sender, recipient, subject, content, type);
        messages.add(message);
        saveMessages();
        return true;
    }
    
    // Send rental-related messages
    public boolean sendRentalMessage(String sender, String recipient, String subject, String content, MessageType type, String rentalId) {
        Message message = new Message(sender, recipient, subject, content, type, rentalId);
        messages.add(message);
        saveMessages();
        return true;
    }
    
    // Get all messages for a user
    public List<Message> getMessagesByUser(String username) {
        return messages.stream()
                .filter(msg -> msg.getRecipient().equals(username) || msg.getSender().equals(username))
                .sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    // Get user received messages
    public List<Message> getReceivedMessages(String username) {
        return messages.stream()
                .filter(msg -> msg.getRecipient().equals(username))
                .sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    // Get user sent messages
    public List<Message> getSentMessages(String username) {
        return messages.stream()
                .filter(msg -> msg.getSender().equals(username))
                .sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    // Get unread messages
    public List<Message> getUnreadMessages(String username) {
        return messages.stream()
                .filter(msg -> msg.getRecipient().equals(username) && !msg.isRead())
                .sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    // Get messages by type
    public List<Message> getMessagesByType(String username, MessageType type) {
        return messages.stream()
                .filter(msg -> (msg.getRecipient().equals(username) || msg.getSender().equals(username)) 
                        && msg.getType() == type)
                .sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    // Mark message as read
    public boolean markAsRead(String messageId) {
        for (Message message : messages) {
            if (message.getId().equals(messageId)) {
                message.setRead(true);
                saveMessages();
                return true;
            }
        }
        return false;
    }
    
    // Delete message
    public boolean deleteMessage(String messageId) {
        boolean removed = messages.removeIf(msg -> msg.getId().equals(messageId));
        if (removed) {
            saveMessages();
        }
        return removed;
    }
    
    // Update email sent status for a message
    public boolean updateEmailSentStatus(String sender, String recipient, String subject, boolean emailSent) {
        // Find the most recent message with matching criteria
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message.getSender().equals(sender) && 
                message.getRecipient().equals(recipient) && 
                message.getSubject().equals(subject)) {
                message.setEmailSent(emailSent);
                saveMessages();
                return true;
            }
        }
        return false;
    }
    
    // Get message statistics
    public Map<String, Integer> getMessageStats(String username) {
        Map<String, Integer> stats = new HashMap<>();
        List<Message> userMessages = getMessagesByUser(username);
        
        stats.put("total", userMessages.size());
        stats.put("unread", (int) userMessages.stream().filter(msg -> !msg.isRead() && msg.getRecipient().equals(username)).count());
        stats.put("sent", (int) userMessages.stream().filter(msg -> msg.getSender().equals(username)).count());
        stats.put("received", (int) userMessages.stream().filter(msg -> msg.getRecipient().equals(username)).count());
        
        return stats;
    }
    
    // Save messages to file
    private void saveMessages() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(MESSAGES_FILE))) {
            writer.println("[");
            for (int i = 0; i < messages.size(); i++) {
                Message msg = messages.get(i);
                writer.println("  {");
                writer.println("    \"id\": \"" + msg.getId() + "\",");
                writer.println("    \"sender\": \"" + msg.getSender() + "\",");
                writer.println("    \"recipient\": \"" + msg.getRecipient() + "\",");
                writer.println("    \"subject\": \"" + escapeJson(msg.getSubject()) + "\",");
                writer.println("    \"content\": \"" + escapeJson(msg.getContent()) + "\",");
                writer.println("    \"type\": \"" + msg.getType() + "\",");
                writer.println("    \"timestamp\": \"" + msg.getTimestamp().format(formatter) + "\",");
                writer.println("    \"isRead\": " + msg.isRead() + ",");
                writer.println("    \"relatedRentalId\": \"" + (msg.getRelatedRentalId() != null ? msg.getRelatedRentalId() : "") + "\",");
                writer.println("    \"emailSent\": " + msg.isEmailSent());
                writer.print("  }");
                if (i < messages.size() - 1) {
                    writer.println(",");
                } else {
                    writer.println();
                }
            }
            writer.println("]");
        } catch (IOException e) {
            System.err.println("Error saving messages: " + e.getMessage());
        }
    }
    
    // Load messages from file
    private void loadMessages() {
        File file = new File(MESSAGES_FILE);
        if (!file.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line).append("\n");
            }
            
            parseMessages(jsonContent.toString());
        } catch (IOException e) {
            System.err.println("Error loading messages: " + e.getMessage());
        }
    }
    
    // Parse JSON message
    private void parseMessages(String jsonContent) {
        messages.clear();
        
        // Simple JSON parsing (similar to method in RentalSystem)
        String[] messageBlocks = jsonContent.split("\\},\\s*\\{");
        
        for (String block : messageBlocks) {
            block = block.trim();
            if (block.isEmpty() || block.equals("[") || block.equals("]")) continue;
            
            // Clean up JSON block
            block = block.replaceAll("^[\\[\\{]+", "").replaceAll("[\\}\\]]+$", "");
            
            try {
                Message message = parseMessage(block);
                if (message != null) {
                    messages.add(message);
                }
            } catch (Exception e) {
                System.err.println("Error parsing message block: " + e.getMessage());
            }
        }
    }
    
    // Parse single message
    private Message parseMessage(String messageBlock) {
        try {
            Message message = new Message();
            
            String id = extractJsonValue(messageBlock, "id");
            String sender = extractJsonValue(messageBlock, "sender");
            String recipient = extractJsonValue(messageBlock, "recipient");
            String subject = extractJsonValue(messageBlock, "subject");
            String content = extractJsonValue(messageBlock, "content");
            String typeStr = extractJsonValue(messageBlock, "type");
            String timestampStr = extractJsonValue(messageBlock, "timestamp");
            String isReadStr = extractJsonValue(messageBlock, "isRead");
            String relatedRentalId = extractJsonValue(messageBlock, "relatedRentalId");
            String emailSentStr = extractJsonValue(messageBlock, "emailSent");
            
            message.setId(id);
            message.setSender(sender);
            message.setRecipient(recipient);
            message.setSubject(subject);
            message.setContent(content);
            
            // Safely parse MessageType
            try {
                message.setType(MessageType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown MessageType: " + typeStr + ", using SYSTEM_MESSAGE as default");
                message.setType(MessageType.SYSTEM_MESSAGE);
            }
            
            // Safely parse timestamp
            try {
                message.setTimestamp(LocalDateTime.parse(timestampStr, formatter));
            } catch (Exception e) {
                System.err.println("Failed to parse timestamp: " + timestampStr + ", using current time");
                message.setTimestamp(LocalDateTime.now());
            }
            
            message.setRead(Boolean.parseBoolean(isReadStr));
            message.setRelatedRentalId(relatedRentalId.isEmpty() ? null : relatedRentalId);
            message.setEmailSent(Boolean.parseBoolean(emailSentStr));
            
            return message;
        } catch (Exception e) {
            System.err.println("Error parsing message: " + e.getMessage());
            return null;
        }
    }
    
    // Extract JSON value
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\\s*\"([^\"]*)\"|\"" + key + "\":\\s*([^,}]+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1) != null ? m.group(1) : m.group(2).trim();
        }
        return "";
    }
    
    // Escape JSON string
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    // Get all messages (for admin)
    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
    
    // Clean up old messages (optional feature)
    public void cleanupOldMessages(int daysOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        messages.removeIf(msg -> msg.getTimestamp().isBefore(cutoff));
        saveMessages();
    }
     // View messages menu
    public static void viewMessagesMenu(RentalSystem system, Scanner scanner, String username) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                         VIEW MESSAGES                            ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 1. View All Messages                                             ║");
        System.out.println("║ 2. View Received Messages                                        ║");
        System.out.println("║ 3. View Sent Messages                                            ║");
        System.out.println("║ 4. View Unread Messages                                          ║");
        System.out.println("║ 0. Back                                                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.print("Select option: ");
        
        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                viewAllMessages(system, username);
                break;
            case "2":
                viewReceivedMessages(system, username);
                break;
            case "3":
                viewSentMessages(system, username);
                break;
            case "4":
                viewUnreadMessages(system, username);
                break;
            case "0":
                return;
            default:
                System.out.println("Invalid option. Please try again.");
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
                return;
        }
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }
    
    // View all messages
     public static void viewAllMessages(RentalSystem system, String username) {
        List<Message> messages = system.getUserMessages(username);
        if (messages.isEmpty()) {
            System.out.println("No messages found.");
            return;
        }
        
        System.out.println("\n=== All Messages ===");
        for (Message message : messages) {
            System.out.println("ID: " + message.getId());
            System.out.println("From: " + message.getSender());
            System.out.println("To: " + message.getRecipient());
            System.out.println("Subject: " + message.getSubject());
            System.out.println("Content: " + message.getContent());
            System.out.println("Type: " + message.getType());
            System.out.println("Time: " + message.getTimestamp());
            System.out.println("Status: " + (message.isRead() ? "Read" : "Unread"));
            System.out.println("---");
        }
    }
    
    // View received messages
    public static void viewReceivedMessages(RentalSystem system, String username) {
        List<Message> messages = system.getUserMessages(username);
        List<Message> receivedMessages = messages.stream()
            .filter(msg -> msg.getRecipient().equals(username))
            .collect(java.util.stream.Collectors.toList());
            
        if (receivedMessages.isEmpty()) {
            System.out.println("No received messages found.");
            return;
        }
        
        System.out.println("\n=== Received Messages ===");
        for (Message message : receivedMessages) {
            System.out.println("ID: " + message.getId());
            System.out.println("From: " + message.getSender());
            System.out.println("Subject: " + message.getSubject());
            System.out.println("Content: " + message.getContent());
            System.out.println("Type: " + message.getType());
            System.out.println("Time: " + message.getTimestamp());
            System.out.println("Status: " + (message.isRead() ? "Read" : "Unread"));
            System.out.println("---");
        }
    }
    
    // View sent messages
    public static void viewSentMessages(RentalSystem system, String username) {
        List<Message> messages = system.getUserMessages(username);
        List<Message> sentMessages = messages.stream()
            .filter(msg -> msg.getSender().equals(username))
            .collect(java.util.stream.Collectors.toList());
            
        if (sentMessages.isEmpty()) {
            System.out.println("No sent messages found.");
            return;
        }
        
        System.out.println("\n=== Sent Messages ===");
        for (Message message : sentMessages) {
            System.out.println("ID: " + message.getId());
            System.out.println("To: " + message.getRecipient());
            System.out.println("Subject: " + message.getSubject());
            System.out.println("Content: " + message.getContent());
            System.out.println("Type: " + message.getType());
            System.out.println("Time: " + message.getTimestamp());
            System.out.println("Email Sent: " + (message.isEmailSent() ? "Yes" : "No"));
            System.out.println("---");
        }
    }
    
    // View unread messages
    public static void viewUnreadMessages(RentalSystem system, String username) {
        List<Message> unreadMessages = system.getNotificationService().getUnreadMessages(username);
        if (unreadMessages.isEmpty()) {
            System.out.println("No unread messages.");
            return;
        }
        
        System.out.println("\n=== Unread Messages ===");
        for (Message message : unreadMessages) {
            System.out.println("ID: " + message.getId());
            System.out.println("From: " + message.getSender());
            System.out.println("Subject: " + message.getSubject());
            System.out.println("Content: " + message.getContent());
            System.out.println("Time: " + message.getTimestamp());
            System.out.println("---");
        }
    }
    
    // Send message
    public static void sendMessage(RentalSystem system, Scanner scanner, String fromUser) {
        System.out.print("Enter recipient username: ");
        String toUser = scanner.nextLine();
        
        System.out.print("Enter subject: ");
        String subject = scanner.nextLine();
        
        System.out.print("Enter message content: ");
        String content = scanner.nextLine();
        
        if (system.sendUserMessage(fromUser, toUser, subject, content)) {
            System.out.println("Message sent successfully!");
        } else {
            System.out.println("Failed to send message.");
        }
    }
    
    // Mark message as read
    // Delete message
    public static void deleteMessage(RentalSystem system, Scanner scanner, String username) {
        List<Message> messages = system.getUserMessages(username);
        if (messages.isEmpty()) {
            System.out.println("No messages found.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        viewAllMessages(system, username);
        System.out.print("Enter message ID to delete: ");
        String messageId = scanner.nextLine().trim();
        
        if (messageId.isEmpty()) {
            System.out.println("No message ID entered. Operation cancelled.");
            return;
        }
        
        if (system.getNotificationService().deleteMessage(messageId)) {
            System.out.println("Message deleted successfully.");
        } else {
            System.out.println("Failed to delete message. Please check the message ID.");
        }
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }
    
    // View message statistics
    public static void viewMessageStatistics(RentalSystem system, String username) {
        Map<String, Integer> stats = system.getNotificationService().getMessageStats(username);
        
        System.out.println("\n=== Message Statistics ===");
        System.out.println("Total messages: " + stats.get("total"));
        System.out.println("Unread messages: " + stats.get("unread"));
        System.out.println("Sent messages: " + stats.get("sent"));
        System.out.println("Received messages: " + stats.get("received"));
    }
} 