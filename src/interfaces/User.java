package interfaces;

import enums.AccountRole;

/**
 * User interface defining common behaviors for all user accounts
 * This interface establishes the contract for user account operations
 */
public interface User {
    
    // Basic account information methods
    String getUsername();
    void setUsername(String username);
    
    String getPassword();
    void setPassword(String password);
    
    AccountRole getRole();
    void setRole(AccountRole role);
    
    String getEmail();
    void setEmail(String email);
    
    String getFullName();
    void setFullName(String fullName);
    
    String getContactNumber();
    void setContactNumber(String contactNumber);
    
    // Authentication and validation methods
    boolean validateCredentials(String password);
    
    // Account status and information display
    String getAccountInfo();
    boolean isAccountValid();
    
    // Account type identification
    String getAccountType();
}
