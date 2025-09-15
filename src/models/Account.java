package models;
import enums.AccountRole;
import interfaces.User;

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
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        this.username = username.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        this.password = password;
    }

    public AccountRole getRole() {
        return role;
    }

    public void setRole(AccountRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        this.role = role;
    }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { 
        if (email == null || email.trim().isEmpty()) {
            this.email = "";
            return;
        }
        String e = email.trim();
        if (!(e.contains("@") && e.contains(".") && e.indexOf("@") < e.lastIndexOf("."))) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.email = e; 
    }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName != null ? fullName.trim() : ""; }
    
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { 
        if (contactNumber == null || contactNumber.trim().isEmpty()) {
            this.contactNumber = "";
            return;
        }
        String digitsOnly = contactNumber.replaceAll("\\D", "");
        if (!(contactNumber.startsWith("60") && digitsOnly.length() >= 11 && digitsOnly.length() <= 13)) {
            throw new IllegalArgumentException("Invalid contact number format");
        }
        this.contactNumber = contactNumber.trim(); 
    }
    
    // Implementation of User interface methods
    
    @Override
    public boolean validateCredentials(String password) {
        if (password == null) return false;
        String hashed = hashPassword(password);
        return this.password.equals(hashed) || this.password.equals(password);
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

    // Local hashing utility to avoid cross-package dependency
    private static String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return Integer.toString(password.hashCode());
        }
    }

} 