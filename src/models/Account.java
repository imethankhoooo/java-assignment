package models;
import enums.AccountRole;

/**
 * Account class supporting users and administrators
 */
public class Account {
    private String username;
    private String password;
    private AccountRole role;
    private String email;
    private String fullName;
    private String contactNumber;
    private String address;
    private String dateOfBirth;
    private String licenseNumber;
    private String emergencyContact;

    public Account(String username, String password, AccountRole role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = "";
        this.fullName = "";
        this.contactNumber = "";
        this.address = "";
        this.dateOfBirth = "";
        this.licenseNumber = "";
        this.emergencyContact = "";
    }
    
    public Account(String username, String password, AccountRole role, String email) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email != null ? email : "";
        this.fullName = "";
        this.contactNumber = "";
        this.address = "";
        this.dateOfBirth = "";
        this.licenseNumber = "";
        this.emergencyContact = "";
    }
    
    public Account(String username, String password, AccountRole role, String email, String fullName, String contactNumber) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email != null ? email : "";
        this.fullName = fullName != null ? fullName : "";
        this.contactNumber = contactNumber != null ? contactNumber : "";
        this.address = "";
        this.dateOfBirth = "";
        this.licenseNumber = "";
        this.emergencyContact = "";
    }
    
    // Complete constructor with all fields
    public Account(String username, String password, AccountRole role, String email, String fullName, 
                  String contactNumber, String address, String dateOfBirth, String licenseNumber, String emergencyContact) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email != null ? email : "";
        this.fullName = fullName != null ? fullName : "";
        this.contactNumber = contactNumber != null ? contactNumber : "";
        this.address = address != null ? address : "";
        this.dateOfBirth = dateOfBirth != null ? dateOfBirth : "";
        this.licenseNumber = licenseNumber != null ? licenseNumber : "";
        this.emergencyContact = emergencyContact != null ? emergencyContact : "";
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
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address != null ? address : ""; }
    
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth != null ? dateOfBirth : ""; }
    
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber != null ? licenseNumber : ""; }
    
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact != null ? emergencyContact : ""; }
} 