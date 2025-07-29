import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MessageService {
    private static final String MESSAGES_FILE = "messages.json";
    private List<Message> messages;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public MessageService() {
        this.messages = new ArrayList<>();
        loadMessages();
    }
    
    // 发送消息
    public boolean sendMessage(String sender, String recipient, String subject, String content, MessageType type) {
        Message message = new Message(sender, recipient, subject, content, type);
        messages.add(message);
        saveMessages();
        return true;
    }
    
    // 发送与租赁相关的消息
    public boolean sendRentalMessage(String sender, String recipient, String subject, String content, MessageType type, String rentalId) {
        Message message = new Message(sender, recipient, subject, content, type, rentalId);
        messages.add(message);
        saveMessages();
        return true;
    }
    
    // 获取用户的所有消息
    public List<Message> getMessagesByUser(String username) {
        return messages.stream()
                .filter(msg -> msg.getRecipient().equals(username) || msg.getSender().equals(username))
                .sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    // 获取用户收到的消息
    public List<Message> getReceivedMessages(String username) {
        return messages.stream()
                .filter(msg -> msg.getRecipient().equals(username))
                .sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    // 获取用户发送的消息
    public List<Message> getSentMessages(String username) {
        return messages.stream()
                .filter(msg -> msg.getSender().equals(username))
                .sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    // 获取未读消息
    public List<Message> getUnreadMessages(String username) {
        return messages.stream()
                .filter(msg -> msg.getRecipient().equals(username) && !msg.isRead())
                .sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    // 按消息类型获取消息
    public List<Message> getMessagesByType(String username, MessageType type) {
        return messages.stream()
                .filter(msg -> (msg.getRecipient().equals(username) || msg.getSender().equals(username)) 
                        && msg.getType() == type)
                .sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    // 标记消息为已读
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
    
    // 删除消息
    public boolean deleteMessage(String messageId) {
        boolean removed = messages.removeIf(msg -> msg.getId().equals(messageId));
        if (removed) {
            saveMessages();
        }
        return removed;
    }
    
    // 获取消息统计
    public Map<String, Integer> getMessageStats(String username) {
        Map<String, Integer> stats = new HashMap<>();
        List<Message> userMessages = getMessagesByUser(username);
        
        stats.put("total", userMessages.size());
        stats.put("unread", (int) userMessages.stream().filter(msg -> !msg.isRead() && msg.getRecipient().equals(username)).count());
        stats.put("sent", (int) userMessages.stream().filter(msg -> msg.getSender().equals(username)).count());
        stats.put("received", (int) userMessages.stream().filter(msg -> msg.getRecipient().equals(username)).count());
        
        return stats;
    }
    
    // 保存消息到文件
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
    
    // 从文件加载消息
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
    
    // 解析JSON消息
    private void parseMessages(String jsonContent) {
        messages.clear();
        
        // 简单的JSON解析（类似于RentalSystem中的方法）
        String[] messageBlocks = jsonContent.split("\\},\\s*\\{");
        
        for (String block : messageBlocks) {
            block = block.trim();
            if (block.isEmpty() || block.equals("[") || block.equals("]")) continue;
            
            // 清理JSON块
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
    
    // 解析单个消息
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
            
            // 安全地解析MessageType
            try {
                message.setType(MessageType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown MessageType: " + typeStr + ", using SYSTEM_MESSAGE as default");
                message.setType(MessageType.SYSTEM_MESSAGE);
            }
            
            // 安全地解析时间戳
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
    
    // 提取JSON值
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\\s*\"([^\"]*)\"|\"" + key + "\":\\s*([^,}]+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1) != null ? m.group(1) : m.group(2).trim();
        }
        return "";
    }
    
    // 转义JSON字符串
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    // 获取所有消息（管理员用）
    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
    
    // 清理旧消息（可选功能）
    public void cleanupOldMessages(int daysOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        messages.removeIf(msg -> msg.getTimestamp().isBefore(cutoff));
        saveMessages();
    }
} 