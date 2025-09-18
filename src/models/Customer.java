package models;

import enums.AccountRole;

public class Customer extends Account {
    private String address;
    private String dateOfBirth;
    private String licenseNumber;
    private String emergencyContact;

    public Customer(String name, String contact) {
        super("", "", AccountRole.CUSTOMER, "", name, contact);
        this.address = "";
        this.dateOfBirth = "";
        this.licenseNumber = "";
        this.emergencyContact = "";
    }

    public Customer(String username, String password, String email, String fullName,
                   String contactNumber, String address, String dateOfBirth,
                   String licenseNumber, String emergencyContact) {
        super(username, password, AccountRole.CUSTOMER, email, fullName, contactNumber);
        this.address = address != null ? address : "";
        this.dateOfBirth = dateOfBirth != null ? dateOfBirth : "";
        this.licenseNumber = licenseNumber != null ? licenseNumber : "";
        this.emergencyContact = emergencyContact != null ? emergencyContact : "";
    }

    public String getName() {
        return getFullName();
    }

    public void setName(String name) {
        setFullName(name);
    }

    public String getContact() {
        return getContactNumber();
    }

    public void setContact(String contact) {
        setContactNumber(contact);
    }

    public String getAddress() { 
        return address; 
    }
    
    public void setAddress(String address) { 
        this.address = address != null ? address.trim() : ""; 
    }
    
    public String getDateOfBirth() { 
        return dateOfBirth; 
    }
    
    public void setDateOfBirth(String dateOfBirth) { 
        this.dateOfBirth = dateOfBirth != null ? dateOfBirth.trim() : ""; 
    }
    
    public String getLicenseNumber() { 
        return licenseNumber; 
    }
    
    public void setLicenseNumber(String licenseNumber) { 
        this.licenseNumber = licenseNumber != null ? licenseNumber.trim() : ""; 
    }
    
    public String getEmergencyContact() { 
        return emergencyContact; 
    }
    
    public void setEmergencyContact(String emergencyContact) { 
        this.emergencyContact = emergencyContact != null ? emergencyContact.trim() : ""; 
    }
    
    @Override
    public String getAccountType() {
        return "Customer";
    }
    
    @Override
    public String getDetailedAccountInfo() {
        return String.format("Customer Account - Username: %s, Name: %s, Email: %s, Contact: %s, " +
                           "Address: %s, DOB: %s, License: %s, Emergency Contact: %s",
                           getUsername(), getFullName(), getEmail(), getContactNumber(),
                           address, dateOfBirth, licenseNumber, emergencyContact);
    }
    
    @Override
    public boolean validateAccountSpecificData() {
        // Basic validation - customer must have name and contact
        boolean basicValid = getFullName() != null && !getFullName().trim().isEmpty() &&
                            getContactNumber() != null && !getContactNumber().trim().isEmpty();
        
        boolean licenseValid = licenseNumber == null || licenseNumber.isEmpty() ||
                              licenseNumber.matches("\\d{6}-\\d{2}-\\d{4}");
        
        return basicValid && licenseValid;
    }

    /**
     * Override toString for better representation
     */
    @Override
    public String toString() {
        return String.format("Customer[username=%s, name=%s, contact=%s]",
                           getUsername(), getFullName(), getContactNumber());
    }
} 