package models;
import enums.AccountRole;
import interfaces.User;

/**
 * Abstract Account base class supporting users and administrators
 * Common fields only - customer-specific fields moved to Customer class
 * Implements User interface and provides common functionality
 */
public abstract class Account implements User {
    private String username;
    private String password;
    private AccountRole role;
    private String email;
    private String fullName;
    private String contactNumber;

    public Account(String username, String password, AccountRole role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = "";
        this.fullName = "";
        this.contactNumber = "";
    }
    
    public Account(String username, String password, AccountRole role, String email) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email != null ? email : "";
        this.fullName = "";
        this.contactNumber = "";
    }
    
    public Account(String username, String password, AccountRole role, String email, String fullName, String contactNumber) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email != null ? email : "";
        this.fullName = fullName != null ? fullName : "";
        this.contactNumber = contactNumber != null ? contactNumber : "";
    }
    


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AccountRole getRole() {
        return role;
    }

    public void setRole(AccountRole role) {
        this.role = role;
    }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email != null ? email : ""; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName != null ? fullName : ""; }
    
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber != null ? contactNumber : ""; }
    
    // Implementation of User interface methods
    
    @Override
    public boolean validateCredentials(String password) {
        return this.password.equals(password);
    }
    
    @Override
    public String getAccountInfo() {
        return String.format("Username: %s, Role: %s, Name: %s, Email: %s, Contact: %s",
                           username, role, fullName, email, contactNumber);
    }
    
    @Override
    public boolean isAccountValid() {
        return username != null && !username.trim().isEmpty() &&
               password != null && !password.trim().isEmpty() &&
               role != null;
    }
    
    // Abstract methods to be implemented by subclasses
    
    /**
     * Get the specific account type identifier
     * @return account type string (e.g., "Customer", "Admin")
     */
    @Override
    public abstract String getAccountType();
    
    /**
     * Get detailed account information specific to the account type
     * @return detailed account information string
     */
    public abstract String getDetailedAccountInfo();
    
    /**
     * Validate account-specific information
     * @return true if account-specific data is valid
     */
    public abstract boolean validateAccountSpecificData();

} 