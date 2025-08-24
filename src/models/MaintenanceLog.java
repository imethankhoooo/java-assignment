package models;
import java.time.LocalDateTime;

import enums.MaintenanceLogType;
import enums.MaintenanceStatus;

public class MaintenanceLog {
    private static int nextId = 1;
    
    private int id;
    private int vehicleId;
    private MaintenanceLogType logType;
    private String description;
    private LocalDateTime reportDate;
    private LocalDateTime completedDate;
    private double cost;
    private MaintenanceStatus status;
    private String reportedBy;  // Reported by (customer or admin)
    private String assignedTo;  // Assigned to who
    private String resolvedBy;  // Resolved by who
    private int severityLevel;  // Severity (1-5, 5 most severe)
    
    public MaintenanceLog(int vehicleId, MaintenanceLogType logType, String description, 
                         String reportedBy, int severityLevel) {
        this.id = nextId++;
        this.vehicleId = vehicleId;
        this.logType = logType;
        this.description = description;
        this.reportDate = LocalDateTime.now();
        this.reportedBy = reportedBy;
        this.severityLevel = severityLevel;
        this.status = MaintenanceStatus.REPORTED;
        this.cost = 0.0;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    /**
     * Set next ID (for synchronization when loading from JSON)
     */
    public static void setNextId(int nextId) {
        MaintenanceLog.nextId = nextId;
    }
    
    public int getVehicleId() { return vehicleId; }
    
    public MaintenanceLogType getLogType() { return logType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getReportDate() { return reportDate; }
    public void setReportDate(LocalDateTime reportDate) { this.reportDate = reportDate; }
    
    public LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }
    
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    
    public MaintenanceStatus getStatus() { return status; }
    public void setStatus(MaintenanceStatus status) { 
        this.status = status; 
        if (status == MaintenanceStatus.RESOLVED && completedDate == null) {
            this.completedDate = LocalDateTime.now();
        }
    }
    
    public String getReportedBy() { return reportedBy; }
    
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    
    public int getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(int severityLevel) { 
        this.severityLevel = Math.max(1, Math.min(5, severityLevel)); 
    }
    
    public boolean isUnresolved() {
        return status != MaintenanceStatus.RESOLVED;
    }
    
    public boolean isCritical() {
        return severityLevel >= 4 && isUnresolved();
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - %s (Severity: %d, Status: %s)", 
                           logType, description, reportedBy, severityLevel, status);
    }
} 