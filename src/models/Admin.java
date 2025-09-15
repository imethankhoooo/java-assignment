package models;

import enums.AccountRole;

/**
 * Admin class representing administrator accounts in the system
 * Inherits from Account class - maintains existing functionality structure
 */
public class Admin extends Account {
    private String adminId;
    private static int nextAdminNumber = 1;

    /**
     * Constructor with basic admin information
     */
    public Admin(String username, String password) {
        super(username, password, AccountRole.ADMIN);
        this.adminId = generateAdminId();
    }

    /**
     * Constructor with email
     */
    public Admin(String username, String password, String email) {
        super(username, password, AccountRole.ADMIN, email);
        this.adminId = generateAdminId();
    }

    /**
     * Constructor with full information
     */
    public Admin(String username, String password, String email, String fullName,
                String contactNumber) {
        super(username, password, AccountRole.ADMIN, email, fullName, contactNumber);
        this.adminId = generateAdminId();
    }
    
    /**
     * Constructor for JSON loading with existing ID
     */
    public Admin(String username, String password, String email, String fullName,
                String contactNumber, String adminId) {
        super(username, password, AccountRole.ADMIN, email, fullName, contactNumber);
        this.adminId = adminId;
        // Update next admin number if this ID is higher
        if (adminId.startsWith("A") && adminId.length() == 4) {
            try {
                int idNumber = Integer.parseInt(adminId.substring(1));
                if (idNumber >= nextAdminNumber) {
                    nextAdminNumber = idNumber + 1;
                }
            } catch (NumberFormatException e) {
                // Invalid ID format, ignore
            }
        }
    }
    
    /**
     * Generate a unique admin ID (A001, A002, etc.)
     */
    private static synchronized String generateAdminId() {
        return String.format("A%03d", nextAdminNumber++);
    }
    
    /**
     * Get admin ID
     */
    public String getAdminId() {
        return adminId;
    }
    
    /**
     * Set admin ID (for JSON loading)
     */
    public void setAdminId(String adminId) {
        if (adminId == null || !adminId.matches("A\\d{3}")) {
            throw new IllegalArgumentException("Invalid admin ID format (expected Axxx)");
        }
        this.adminId = adminId;
    }
    
    /**
     * Get next admin number for external use
     */
    public static int getNextAdminNumber() {
        return nextAdminNumber;
    }
    
    /**
     * Set next admin number for external use (e.g., when loading from JSON)
     */
    public static void setNextAdminNumber(int nextNumber) {
        nextAdminNumber = nextNumber;
    }
    
    // Implementation of abstract methods from Account class
    
    @Override
    public String getAccountType() {
        return "Admin";
    }
    
    @Override
    public String getDetailedAccountInfo() {
        return String.format("Admin Account - ID: %s, Username: %s, Name: %s, Email: %s, Contact: %s",
                           adminId, getUsername(), getFullName(), getEmail(), getContactNumber());
    }
    
    @Override
    public boolean validateAccountSpecificData() {
        return adminId != null && !adminId.trim().isEmpty() &&
               adminId.matches("A\\d{3}"); // Validate admin ID format (A001, A002, etc.)
    }
    
    /**
     * Override toString for better representation
     */
    @Override
    public String toString() {
        return String.format("Admin[id=%s, username=%s, name=%s]",
                           adminId, getUsername(), getFullName());
    }
}
