import java.time.LocalDateTime;

public class Message {
    private String id;
    private String sender;
    private String recipient;
    private String subject;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private boolean isRead;
    private String relatedRentalId;
    private boolean emailSent;
    
    // Constructors
    public Message() {
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
        this.emailSent = false;
    }
    
    public Message(String sender, String recipient, String subject, String content, MessageType type) {
        this();
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
        this.type = type;
        this.id = generateId();
    }
    
    public Message(String sender, String recipient, String subject, String content, MessageType type, String relatedRentalId) {
        this(sender, recipient, subject, content, type);
        this.relatedRentalId = relatedRentalId;
    }
    
    // Generate unique ID
    private String generateId() {
        return "MSG" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    public String getRelatedRentalId() { return relatedRentalId; }
    public void setRelatedRentalId(String relatedRentalId) { this.relatedRentalId = relatedRentalId; }
    
    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }
    
    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", sender='" + sender + '\'' +
                ", recipient='" + recipient + '\'' +
                ", subject='" + subject + '\'' +
                ", type=" + type +
                ", timestamp=" + timestamp +
                ", isRead=" + isRead +
                ", emailSent=" + emailSent +
                '}';
    }
} 