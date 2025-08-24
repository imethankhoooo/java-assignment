package enums;
/**
 * Message type enumeration
 * Defines different types of messages in the system
 */
public enum MessageType {
    RENTAL_CONFIRMATION,
    RENTAL_APPROVAL,
    RENTAL_REJECTION,
    RENTAL_REMINDER,
    OVERDUE_NOTIFICATION,
    ADMIN_NOTIFICATION,
    USER_MESSAGE,
    SYSTEM_MESSAGE,
    REMINDER,      // System auto reminder
    MANUAL,        // Admin manual send
    CONFIRMATION,  // Confirm message
    WARNING,       // Warning message
    MAINTENANCE_ALERT  // Maintenance alert
} 