/**
 * 消息类型枚举
 * 定义系统中不同类型的消息
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
    REMINDER,      // 系统自动提醒
    MANUAL,        // 管理员手动发送
    CONFIRMATION,  // 确认消息
    WARNING,       // 警告消息
    MAINTENANCE_ALERT  // 维护警报
} 