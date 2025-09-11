package services;

import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

import enums.*;
import models.*;

import static services.UtilityService.*;

public class AccountService {
    private static List<Account> accounts = new ArrayList<>();
    
    public static List<Account> getAccounts() {
        return accounts;
    }
    
    public static void setAccounts(List<Account> accountList) {
        accounts = accountList;
    }
    
    public static Account getAccountByUsername(String username) {
        for (Account account : accounts) {
            if (account.getUsername().equals(username)) {
                return account;
            }
        }
        return null;
    }
    
    public static boolean addAccount(Account account) {
        if (getAccountByUsername(account.getUsername()) == null) {
            accounts.add(account);
            return true;
        }
        return false;
    }
    
    public static boolean updateAccount(String username, Account updatedAccount) {
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).getUsername().equals(username)) {
                accounts.set(i, updatedAccount);
                return true;
            }
        }
        return false;
    }
    
    public static boolean deleteAccount(String username) {
        return accounts.removeIf(account -> account.getUsername().equals(username));
    }
    
    public static Account login(String username, String password) {
        for (Account account : accounts) {
            if (account.getUsername().equals(username) && 
                verifyPassword(password, account.getPassword())) {
                return account;
            }
        }
        return null;
    }
    
    public static boolean updatePassword(String username, String hashedPassword) {
        Account account = getAccountByUsername(username);
        if (account != null) {
            account.setPassword(hashedPassword);
            return true;
        }
        return false;
    }
    
    public static List<Account> searchAccounts(String searchTerm, AccountRole filterRole) {
        List<Account> results = new ArrayList<>();
        String lowerSearchTerm = searchTerm != null ? searchTerm.toLowerCase() : "";
        
        for (Account account : accounts) {
            if (filterRole != null && account.getRole() != filterRole) {
                continue;
            }
            
            boolean matches = false;
            
            if (searchTerm == null || searchTerm.isEmpty()) {
                matches = true;
            } else {
                // Standard field searches
                matches = account.getUsername().toLowerCase().contains(lowerSearchTerm) ||
                         account.getFullName().toLowerCase().contains(lowerSearchTerm) ||
                         account.getEmail().toLowerCase().contains(lowerSearchTerm) ||
                         account.getContactNumber().contains(searchTerm);
                
                // Admin ID search
                if (!matches && account instanceof Admin) {
                    Admin admin = (Admin) account;
                    matches = admin.getAdminId().toLowerCase().contains(lowerSearchTerm);
                }
            }
            
            if (matches) {
                results.add(account);
            }
        }
        return results;
    }
    
    // Load account data from JSON file
    public static void loadAccounts(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            
            accounts = parseAccountsFromJson(jsonContent.toString());
            if (accounts == null) {
                accounts = new ArrayList<>();
            }
            System.out.println("Loaded accounts: " + accounts.size());
        } catch (IOException e) {
            System.out.println("Failed to load account data: " + e.getMessage());
            accounts = new ArrayList<>();
        }
    }
    
    // Save account data to JSON file
    public static void saveAccounts(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            String jsonContent = convertAccountsToJson();
            writer.println(jsonContent);
        } catch (IOException e) {
            System.out.println("Failed to save account data: " + e.getMessage());
        }
    }
    
    // Parse account JSON data
    private static List<Account> parseAccountsFromJson(String json) {
        List<Account> accountList = new ArrayList<>();
        try {
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);
                
                String[] accountObjects = splitJsonObjects(json);
                
                for (String accountJson : accountObjects) {
                    Account account = parseAccountFromJson(accountJson.trim());
                    if (account != null) {
                        accountList.add(account);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to parse account JSON: " + e.getMessage());
        }
        return accountList;
    }
    
    // Parse single account object
    // Note: 为了向后兼容性，继续提取所有字段，但根据账户类型只使用相关字段
    private static Account parseAccountFromJson(String json) {
        try {
            String username = extractJsonValue(json, "username");
            String password = extractJsonValue(json, "password");
            String roleStr = extractJsonValue(json, "role");
            String email = extractJsonValue(json, "email");
            String fullName = extractJsonValue(json, "fullName");
            String contactNumber = extractJsonValue(json, "contactNumber");
            // Customer专属字段 - Admin账户将忽略这些字段
            String address = extractJsonValue(json, "address");
            String dateOfBirth = extractJsonValue(json, "dateOfBirth");
            String licenseNumber = extractJsonValue(json, "licenseNumber");
            String emergencyContact = extractJsonValue(json, "emergencyContact");
            // Admin专属字段 - Customer账户将忽略此字段
            String adminId = extractJsonValue(json, "adminId");

            if (username != null && password != null && roleStr != null) {
                AccountRole role = AccountRole.valueOf(roleStr);
                if (role == AccountRole.ADMIN) {
                    if (adminId != null && !adminId.isEmpty()) {
                        // Load with existing ID
                        return new Admin(username, password,
                                email != null ? email : "",
                                fullName != null ? fullName : "",
                                contactNumber != null ? contactNumber : "",
                                adminId);
                    } else {
                        // Generate new ID
                        return new Admin(username, password,
                                email != null ? email : "",
                                fullName != null ? fullName : "",
                                contactNumber != null ? contactNumber : "");
                    }
                } else {
                    return new Customer(username, password,
                            email != null ? email : "",
                            fullName != null ? fullName : "",
                            contactNumber != null ? contactNumber : "",
                            address != null ? address : "",
                            dateOfBirth != null ? dateOfBirth : "",
                            licenseNumber != null ? licenseNumber : "",
                            emergencyContact != null ? emergencyContact : "");
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to parse account object: " + e.getMessage());
        }
        return null;
    }
    
    // Convert account data to JSON format
    private static String convertAccountsToJson() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");

        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            json.append("  {\n");
            json.append("    \"username\": \"").append(escapeJson(account.getUsername())).append("\",\n");
            json.append("    \"password\": \"").append(escapeJson(account.getPassword())).append("\",\n");
            json.append("    \"role\": \"").append(account.getRole()).append("\",\n");
            json.append("    \"email\": \"").append(escapeJson(account.getEmail())).append("\",\n");
            json.append("    \"fullName\": \"").append(escapeJson(account.getFullName())).append("\",\n");
            json.append("    \"contactNumber\": \"").append(escapeJson(account.getContactNumber())).append("\"");
            
            // Add role-specific fields
            if (account instanceof Customer) {
                Customer customer = (Customer) account;
                json.append(",\n");
                json.append("    \"address\": \"").append(escapeJson(customer.getAddress())).append("\",\n");
                json.append("    \"dateOfBirth\": \"").append(escapeJson(customer.getDateOfBirth())).append("\",\n");
                json.append("    \"licenseNumber\": \"").append(escapeJson(customer.getLicenseNumber())).append("\",\n");
                json.append("    \"emergencyContact\": \"").append(escapeJson(customer.getEmergencyContact())).append("\"\n");
            } else if (account instanceof Admin) {
                Admin admin = (Admin) account;
                json.append(",\n");
                json.append("    \"adminId\": \"").append(escapeJson(admin.getAdminId())).append("\"\n");
            }
            
            json.append("  }");

            if (i < accounts.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("]");
        return json.toString();
    }
    // ===== 账户管理功能 =====

    /**
     * Add new account with notification service update
     * @param account Account to add
     * @return true if added successfully, false if username already exists
     */
    public static boolean addAccountWithNotification(Account account) {
        if (!addAccount(account)) {
            return false;
        }

        // Update notification service if available
        try {
            NotificationService notificationService = new NotificationService();
            notificationService.loadUserEmailsFromAccounts(getAccounts());
        } catch (Exception e) {
            // Ignore notification service errors during account creation
        }

        return true;
    }

    /**
     * Update existing account with notification service update
     * @param username Username of account to update
     * @param updatedAccount Updated account information
     * @return true if updated successfully, false if account not found
     */
    public static boolean updateAccountWithNotification(String username, Account updatedAccount) {
        Account existingAccount = getAccountByUsername(username);
        if (existingAccount == null) {
            return false;
        }

        // Update fields
        existingAccount.setEmail(updatedAccount.getEmail());
        existingAccount.setFullName(updatedAccount.getFullName());
        existingAccount.setContactNumber(updatedAccount.getContactNumber());
        
        // Update customer-specific fields only if both are Customer instances
        if (existingAccount instanceof Customer && updatedAccount instanceof Customer) {
            Customer existingCustomer = (Customer) existingAccount;
            Customer updatedCustomer = (Customer) updatedAccount;
            existingCustomer.setAddress(updatedCustomer.getAddress());
            existingCustomer.setDateOfBirth(updatedCustomer.getDateOfBirth());
            existingCustomer.setLicenseNumber(updatedCustomer.getLicenseNumber());
            existingCustomer.setEmergencyContact(updatedCustomer.getEmergencyContact());
        }

        // Update notification service if available
        try {
            NotificationService notificationService = new NotificationService();
            notificationService.loadUserEmailsFromAccounts(getAccounts());
        } catch (Exception e) {
            // Ignore notification service errors during account update
        }

        return true;
    }

    /**
     * Delete account with notification service update
     * @param username Username of account to delete
     * @return true if deleted successfully, false if account not found
     */
    public static boolean deleteAccountWithNotification(String username) {
        Account account = getAccountByUsername(username);
        if (account == null) {
            return false;
        }

        boolean deleted = deleteAccount(username);

        if (deleted) {
            // Update notification service if available
            try {
                NotificationService notificationService = new NotificationService();
                notificationService.loadUserEmailsFromAccounts(getAccounts());
            } catch (Exception e) {
                // Ignore notification service errors during account deletion
            }
        }

        return deleted;
    }

    /**
     * Search accounts by role and search term
     * @param searchTerm Search term (can be null for all accounts)
     * @param role Account role filter (can be null for all roles)
     * @return List of accounts matching the search criteria
     */
    public static List<Account> searchAccountsAdvanced(String searchTerm, AccountRole role) {
        List<Account> results = new ArrayList<>();
        String searchLower = searchTerm == null ? "" : searchTerm.toLowerCase().trim();

        for (Account account : accounts) {
            if (role != null && account.getRole() != role) {
                continue;
            }

            boolean matches = false;
            
            if (searchLower.isEmpty()) {
                matches = true;
            } else {
                matches = account.getUsername().toLowerCase().contains(searchLower) ||
                         (account.getFullName() != null && account.getFullName().toLowerCase().contains(searchLower)) ||
                         (account.getContactNumber() != null && account.getContactNumber().contains(searchTerm)) ||
                         (account.getEmail() != null && account.getEmail().toLowerCase().contains(searchLower));
                
                // Admin ID search
                if (!matches && account instanceof Admin) {
                    Admin admin = (Admin) account;
                    matches = admin.getAdminId().toLowerCase().contains(searchLower);
                }
            }
            
            if (matches) {
                results.add(account);
            }
        }
        return results;
    }

    /**
     * Get all accounts by role
     * @param role Account role to filter by
     * @return List of accounts with the specified role
     */
    public static List<Account> getAccountsByRole(AccountRole role) {
        List<Account> results = new ArrayList<>();
        for (Account account : accounts) {
            if (account.getRole() == role) {
                results.add(account);
            }
        }
        return results;
    }

    /**
     * Search customer accounts by search term
     * @param searchTerm Search term (can be null for all customers)
     * @return List of customer accounts matching the search criteria
     */
    public static List<Account> searchCustomerAccounts(String searchTerm) {
        List<Account> results = new ArrayList<>();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // Return all customer accounts if no search term
            for (Account account : accounts) {
                if (account.getRole() == AccountRole.CUSTOMER) {
                    results.add(account);
                }
            }
            return results;
        }

        String searchLower = searchTerm.toLowerCase().trim();
        for (Account account : accounts) {
            if (account.getRole() == AccountRole.CUSTOMER) {
                if (account.getUsername().toLowerCase().contains(searchLower) ||
                        (account.getFullName() != null && account.getFullName().toLowerCase().contains(searchLower)) ||
                        (account.getContactNumber() != null && account.getContactNumber().contains(searchTerm))) {
                    results.add(account);
                }
            }
        }
        return results;
    }

     /**
     * Handle login process
     */
    public static Account loginProcess(Scanner scanner) {
        System.out.println("\n=== LOGIN ===");
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        
        Account account = login(username, password);
        if (account == null) {
            System.out.println("Invalid username or password. Please try again.");
        }
        return account;
    }

    /**
     * Handle registration process
     */
    public static void registerProcess(Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        USER REGISTRATION                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        // Step 1: Basic account information
        System.out.println("\n=== Step 1: Basic Account Information ===");
        
        String username;
        while (true) {
            System.out.print("Username: ");
            username = scanner.nextLine().trim();
            if (username.isEmpty()) {
                System.out.println("Username cannot be empty.");
                continue;
            }
            if (getAccountByUsername(username) != null) {
                System.out.println("Username already exists. Please choose another.");
                continue;
            }
            break;
        }
        
        String password;
        while (true) {
            System.out.print("Password: ");
            password = scanner.nextLine();
            if (password.length() < 4) {
                System.out.println("Password must be at least 4 characters long.");
                continue;
            }
            break;
        }
        
        // Hash the password
        password = hashPassword(password);
        
        String email;
        while (true) {
            System.out.print("Email: ");
            email = scanner.nextLine().trim();
            if (!isValidEmail(email)) {
                System.out.println("Please enter a valid email address.");
                continue;
            }
            break;
        }
        
        // Step 2: Personal Information
        System.out.println("\n=== Step 2: Personal Information ===");
        
        System.out.print("Full Name: ");
        String fullName = scanner.nextLine().trim();
        while (fullName.isEmpty()) {
            System.out.println("Full name is required.");
            System.out.print("Full Name: ");
            fullName = scanner.nextLine().trim();
        }
        
        String contactNumber;
        while (true) {
            System.out.print("Contact Number (60xxxxxxxxx): ");
            contactNumber = scanner.nextLine().trim();
            if (!isValidContactNumber(contactNumber)) {
                System.out.println("Contact number must start with 60 and be 11-13 digits long.");
                continue;
            }
            break;
        }
        
        System.out.print("Address: ");
        String address = scanner.nextLine().trim();
        while (address.isEmpty()) {
            System.out.println("Address is required.");
            System.out.print("Address: ");
            address = scanner.nextLine().trim();
        }
        
        String licenseNumber;
        String dateOfBirth;
        while (true) {
            System.out.print("IC/License Number (xxxxxx-xx-xxxx): ");
            licenseNumber = scanner.nextLine().trim();
            if (!isValidLicenseNumber(licenseNumber)) {
                System.out.println("Please enter a valid IC number in format: xxxxxx-xx-xxxx");
                continue;
            }
            
            // Extract date of birth from IC
            dateOfBirth = extractDateOfBirthFromIC(licenseNumber);
            if (dateOfBirth == null) {
                System.out.println("Invalid IC number format. Cannot extract date of birth.");
                continue;
            }
            
            System.out.println("Date of birth extracted from IC: " + dateOfBirth);
            if (getYesNoInput(scanner, "Is this correct?")) {
                break;
            }
        }
        
        System.out.print("Emergency Contact Number (optional): ");
        String emergencyContact = scanner.nextLine().trim();
        
        // Step 3: Email Verification
        System.out.println("\n=== Step 3: Email Verification ===");
        String verificationCode = generateVerificationCode();
        
        if (!sendEmailVerification(email, verificationCode)) {
            System.out.println("Failed to send verification email. Registration cancelled.");
            return;
        }
        
        if (!verifyEmailCode(scanner, verificationCode, email)) {
            System.out.println("Email verification failed. Registration cancelled.");
            return;
        }
        
        // Create and save account
        Customer newAccount = new Customer(username, password, email, fullName, 
                                         contactNumber, address, dateOfBirth, 
                                         licenseNumber, emergencyContact);
        
        if (addAccount(newAccount)) {
            saveAccounts("accounts.json");
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║              REGISTRATION SUCCESSFUL!                            ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.println("Welcome " + fullName + "! You can now login with your credentials.");
        } else {
            System.out.println("Registration failed. Please try again.");
        }
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".") && 
               email.indexOf("@") < email.lastIndexOf(".");
    }
    
    /**
     * Validate full name format (only letters and spaces)
     */
    public static boolean isValidFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return false;
        }
        
        // Only allow letters, spaces, dots, apostrophes, and hyphens
        String namePattern = "^[a-zA-Z\\s.'\\-]+$";
        return fullName.matches(namePattern);
    }
    
    /**
     * Clean and validate full name input
     */
    public static String cleanAndValidateFullName(String input) {
        if (input == null) return null;
        
        // Trim whitespace
        String cleaned = input.trim();
        
        // Remove extra spaces between words
        cleaned = cleaned.replaceAll("\\s+", " ");
        
        return cleaned;
    }
    
    /**
     * Get validated Y/N input from user
     */
    public static boolean getYesNoInput(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt + " (Y/N): ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("Y") || input.equalsIgnoreCase("Yes")) {
                return true;
            } else if (input.equalsIgnoreCase("N") || input.equalsIgnoreCase("No")) {
                return false;
            } else {
                System.out.println("Invalid input. Please enter Y, y, N, or n only.");
            }
        }
    }

    /**
     * Validate contact number (60xxxxxxxxx, 11-13 digits)
     */
    public static boolean isValidContactNumber(String contactNumber) {
        if (contactNumber == null || !contactNumber.startsWith("60")) {
            return false;
        }
        
        // Remove any non-digit characters for length check
        String digitsOnly = contactNumber.replaceAll("\\D", "");
        return digitsOnly.length() >= 11 && digitsOnly.length() <= 13;
    }

    /**
     * Validate IC/License number format (xxxxxx-xx-xxxx)
     */
    public static boolean isValidLicenseNumber(String licenseNumber) {
        if (licenseNumber == null) return false;
        
        // Check format: 6 digits - 2 digits - 4 digits
        String pattern = "\\d{6}-\\d{2}-\\d{4}";
        return licenseNumber.matches(pattern);
    }

    /**
     * Extract date of birth from Malaysian IC number
     */
    public static String extractDateOfBirthFromIC(String licenseNumber) {
        try {
            if (!isValidLicenseNumber(licenseNumber)) {
                return null;
            }
            
            String[] parts = licenseNumber.split("-");
            String datePart = parts[0]; // yymmdd
            
            String yearStr = datePart.substring(0, 2);
            String monthStr = datePart.substring(2, 4);
            String dayStr = datePart.substring(4, 6);
            
            int year = Integer.parseInt(yearStr);
            int month = Integer.parseInt(monthStr);
            int day = Integer.parseInt(dayStr);
            
            // Validate month and day
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                return null;
            }
            
            // Determine century (if > 25, assume 19xx, else 20xx)
            int fullYear = year > 25 ? 1900 + year : 2000 + year;
            
            return String.format("%04d-%02d-%02d", fullYear, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generate 6-digit verification code
     */
    public static String generateVerificationCode() {
        return String.format("%06d", (int)(Math.random() * 1000000));
    }
    /**
     * Hash password using simple hash function
     */
    public static String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            // Fallback to simple hash if SHA-256 is not available
            return Integer.toString(password.hashCode());
        }
    }

    /**
     * Verify password against hash
     */
    public static boolean verifyPassword(String password, String hash) {
        return hashPassword(password).equals(hash);
    }

    /**
     * Send email verification code
     */
    public static boolean sendEmailVerification(String email, String code) {
        EmailService emailService = new EmailService();
        String subject = "Vehicle Rental System - Email Verification";
        String content = String.format(
            "Welcome to Vehicle Rental System!\n\n" +
            "Your verification code is: %s\n\n" +
            "Please enter this code to complete your registration.\n\n" +
            "This code will expire in 10 minutes.\n\n" +
            "Best regards,\n" +
            "Vehicle Rental System Team", code);
        
        return emailService.sendEmailWithAttachment(email, subject, content, null, null);
    }

    /**
     * Verify email code with options to resend or change email
     */
    public static boolean verifyEmailCode(Scanner scanner, String expectedCode, String originalEmail) {
        String currentEmail = originalEmail;
        String currentCode = expectedCode;
        long lastSendTime = System.currentTimeMillis();
        int maxAttempts = 3;
        int attempts = 0;
        
        while (attempts < maxAttempts) {
            System.out.println("Verification email sent to: " + currentEmail);
            System.out.print("Enter verification code (or type 'resend' to resend, 'change' to change email): ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("resend")) {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastSend = (currentTime - lastSendTime) / 1000;
                
                if (timeSinceLastSend < 30) {
                    long waitTime = 30 - timeSinceLastSend;
                    System.out.println("Please wait " + waitTime + " seconds before requesting another code.");
                    continue;
                }
                
                currentCode = generateVerificationCode();
                if (sendEmailVerification(currentEmail, currentCode)) {
                    lastSendTime = currentTime;
                    System.out.println("New verification code sent.");
                } else {
                    System.out.println("Failed to send verification code. Please try again.");
                }
                continue;
            }
            
            if (input.equalsIgnoreCase("change")) {
                System.out.println("Current email: " + currentEmail);
                System.out.print("Enter new email address: ");
                String newEmail = scanner.nextLine().trim();
                
                if (!isValidEmail(newEmail)) {
                    System.out.println("Invalid email format. Please try again.");
                    continue;
                }
                
                currentEmail = newEmail;
                currentCode = generateVerificationCode();
                if (sendEmailVerification(currentEmail, currentCode)) {
                    lastSendTime = System.currentTimeMillis();
                    System.out.println("Verification code sent to new email: " + currentEmail);
                } else {
                    System.out.println("Failed to send verification code. Please try again.");
                }
                continue;
            }
            
            if (input.equals(currentCode)) {
                System.out.println("Email verification successful!");
                return true;
            } else {
                attempts++;
                System.out.println("Invalid verification code. " + (maxAttempts - attempts) + " attempts remaining.");
            }
        }
        
        System.out.println("Maximum verification attempts exceeded.");
        return false;
    }

    /**
     * View and modify account information
     */
    public static void viewAndModifyAccountInfo(Scanner scanner, Account account) {
        while (true) {
            clearScreen();
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                    ACCOUNT INFORMATION                           ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            
            // Display current information
            System.out.println("Current Account Information:");
            System.out.println("Username: " + account.getUsername());
            
            // Display role-specific ID
            if (account instanceof Admin) {
                Admin admin = (Admin) account;
                System.out.println("Admin ID: " + admin.getAdminId());
            }
            
            System.out.println("Full Name: " + account.getFullName());
            System.out.println("Email: " + account.getEmail());
            System.out.println("Contact Number: " + account.getContactNumber());
            
            // Display customer-specific fields only for Customer accounts
            if (account instanceof Customer) {
                Customer customer = (Customer) account;
                System.out.println("Address: " + customer.getAddress());
                System.out.println("Date of Birth: " + customer.getDateOfBirth());
                System.out.println("License Number: " + customer.getLicenseNumber());
                System.out.println("Emergency Contact: " + customer.getEmergencyContact());
                
                System.out.println("\n=== Which information would you like to modify? ===");
                System.out.println("1. Full Name");
                System.out.println("2. Contact Number");
                System.out.println("3. Address");
                System.out.println("4. License Number");
                System.out.println("5. Emergency Contact");
            } else {
                // For Admin accounts, show simpler options
                System.out.println("\n=== Which information would you like to modify? ===");
                System.out.println("1. Full Name");
                System.out.println("2. Contact Number");
            }
            System.out.println("0. Back");
            System.out.print("Select option: ");
            
            String choice = scanner.nextLine();
            
            boolean updated = false;
            switch (choice) {
                case "1":
                    while (true) {
                        System.out.print("Enter new full name: ");
                        String newFullName = cleanAndValidateFullName(scanner.nextLine());
                        if (newFullName == null || newFullName.isEmpty()) {
                            System.out.println("Full name cannot be empty. Please try again.");
                            continue;
                        }
                        if (!isValidFullName(newFullName)) {
                            System.out.println("Full name can only contain letters, spaces, dots, apostrophes, and hyphens. Please try again.");
                            continue;
                        }
                        account.setFullName(newFullName);
                        updated = true;
                        break;
                    }
                    break;
                case "2":
                    while (true) {
                        System.out.print("Enter new contact number (60xxxxxxxxx): ");
                        String newContact = scanner.nextLine().trim();
                        if (isValidContactNumber(newContact)) {
                            account.setContactNumber(newContact);
                            updated = true;
                            break;
                        } else {
                            System.out.println("Invalid contact number format. Please try again.");
                        }
                    }
                    break;
                case "3":
                    if (account instanceof Customer) {
                        Customer customer = (Customer) account;
                        System.out.print("Enter new address: ");
                        String newAddress = scanner.nextLine().trim();
                        if (!newAddress.isEmpty()) {
                            customer.setAddress(newAddress);
                            updated = true;
                        }
                    } else {
                        System.out.println("Invalid option for admin account.");
                    }
                    break;
                case "4":
                    if (account instanceof Customer) {
                        Customer customer = (Customer) account;
                        while (true) {
                            System.out.print("Enter new license number (xxxxxx-xx-xxxx): ");
                            String newLicense = scanner.nextLine().trim();
                            if (isValidLicenseNumber(newLicense)) {
                                customer.setLicenseNumber(newLicense);
                                // Update date of birth based on new license
                                String newDateOfBirth = extractDateOfBirthFromIC(newLicense);
                                if (newDateOfBirth != null) {
                                    customer.setDateOfBirth(newDateOfBirth);
                                    System.out.println("Date of birth updated to: " + newDateOfBirth);
                                }
                                updated = true;
                                break;
                            } else {
                                System.out.println("Invalid license number format. Please try again.");
                            }
                        }
                    } else {
                        System.out.println("Invalid option for admin account.");
                    }
                    break;
                case "5":
                    if (account instanceof Customer) {
                        Customer customer = (Customer) account;
                        System.out.print("Enter new emergency contact (leave blank to clear): ");
                        String newEmergency = scanner.nextLine().trim();
                        customer.setEmergencyContact(newEmergency);
                        updated = true;
                    } else {
                        System.out.println("Invalid option for admin account.");
                    }
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
                    continue;
            }
            
            if (updated) {
                if (updateAccount(account.getUsername(), account)) {
                    saveAccounts("accounts.json");
                    System.out.println("Account information updated successfully!");
                } else {
                    System.out.println("Failed to update account information.");
                }
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    /**
     * Update email address with verification
     */
    public static void updateEmailAddress(Scanner scanner, Account account) {
        clearScreen();
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        UPDATE EMAIL                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        System.out.println("Current email: " + account.getEmail());
        
        String newEmail;
        while (true) {
            System.out.print("Enter new email address: ");
            newEmail = scanner.nextLine().trim();
            if (!isValidEmail(newEmail)) {
                System.out.println("Please enter a valid email address.");
                continue;
            }
            break;
        }
        
        // Send verification code
        String verificationCode = generateVerificationCode();
        if (!sendEmailVerification(newEmail, verificationCode)) {
            System.out.println("Failed to send verification email. Email update cancelled.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        if (verifyEmailCode(scanner, verificationCode, newEmail)) {
            account.setEmail(newEmail);
            if (updateAccount(account.getUsername(), account)) {
                saveAccounts("accounts.json");
                System.out.println("Email address updated successfully!");
            } else {
                System.out.println("Failed to update email address.");
            }
        } else {
            System.out.println("Email verification failed. Email update cancelled.");
        }
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Change password
     */
    public static void changePassword(Scanner scanner, Account account) {
        clearScreen();
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                       CHANGE PASSWORD                            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        // Verify current password
        System.out.print("Enter current password: ");
        String currentPassword = scanner.nextLine();
        
        // Verify current password
        if (!verifyPassword(currentPassword, account.getPassword())) {
            System.out.println("Current password is incorrect.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            clearScreen();
            return;
        }
        
        // Get new password
        String newPassword;
        while (true) {
            System.out.print("Enter new password: ");
            newPassword = scanner.nextLine();
            if (newPassword.length() < 4) {
                System.out.println("Password must be at least 4 characters long.");
                continue;
            }
            break;
        }
        
        // Confirm new password
        System.out.print("Confirm new password: ");
        String confirmPassword = scanner.nextLine();
        if (!newPassword.equals(confirmPassword)) {
            System.out.println("Passwords do not match. Password change cancelled.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        // Hash and update password
        String hashedPassword = hashPassword(newPassword);
        if (updatePassword(account.getUsername(), hashedPassword)) {
            account.setPassword(hashedPassword);
            saveAccounts("accounts.json");
            System.out.println("Password changed successfully!");
        } else {
            System.out.println("Failed to change password.");
        }
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
        clearScreen();
    }

    /**
     * View all user accounts
     */
    public static void viewAllUserAccounts(Scanner scanner) {
        clearScreen();
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        ALL USER ACCOUNTS                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        List<Account> allAccounts = getAccounts();
        if (allAccounts.isEmpty()) {
            System.out.println("No user accounts found.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        System.out.println("\n┌─────┬─────────────┬─────────────────────┬──────────┬─────────────────────┬─────────────────────┐");
        System.out.println("│ No. │ Username    │ Full Name           │ Role     │ Email               │ Contact             │");
        System.out.println("├─────┼─────────────┼─────────────────────┼──────────┼─────────────────────┼─────────────────────┤");
        
        for (int i = 0; i < allAccounts.size(); i++) {
            Account acc = allAccounts.get(i);
            System.out.printf("│ %-3d │ %-11s │ %-19s │ %-8s │ %-19s │ %-19s │%n",
                (i + 1),
                acc.getUsername().length() > 11 ? acc.getUsername().substring(0, 11) : acc.getUsername(),
                acc.getFullName().length() > 19 ? acc.getFullName().substring(0, 19) : acc.getFullName(),
                acc.getRole().toString(),
                acc.getEmail().length() > 19 ? acc.getEmail().substring(0, 19) : acc.getEmail(),
                acc.getContactNumber().length() > 19 ? acc.getContactNumber().substring(0, 19) : acc.getContactNumber());
        }
        
        System.out.println("└─────┴─────────────┴─────────────────────┴──────────┴─────────────────────┴─────────────────────┘");
        System.out.println("\nTotal accounts: " + allAccounts.size());
        
        // Allow searching from this view
        System.out.println("\nOptions:");
        System.out.println("1. Search accounts");
        System.out.println("0. Back");
        System.out.print("Select option: ");
        
        String choice = scanner.nextLine();
        if (choice.equals("1")) {
            searchAndManageUserAccounts(scanner);
        }
    }

    /**
     * Search and manage user accounts (integrated functionality)
     */
    public static void searchAndManageUserAccounts(Scanner scanner) {
        while (true) {
            clearScreen();
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                   SEARCH & MANAGE ACCOUNTS                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            
            System.out.print("Enter search term (username/name/email/phone, or press Enter for all): ");
            String searchTerm = scanner.nextLine().trim();
            
            System.out.println("Filter by role:");
            System.out.println("1. All");
            System.out.println("2. Customer only");
            System.out.println("3. Admin only");
            System.out.print("Select option: ");
            
            String roleChoice = scanner.nextLine();
            AccountRole filterRole = null;
            switch (roleChoice) {
                case "2":
                    filterRole = AccountRole.CUSTOMER;
                    break;
                case "3":
                    filterRole = AccountRole.ADMIN;
                    break;
                default:
                    filterRole = null; // All
                    break;
            }
            
            List<Account> results = searchAccounts(searchTerm, filterRole);
            
            clearScreen();
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                      SEARCH RESULTS                              ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            
            if (results.isEmpty()) {
                System.out.println("No accounts found matching your criteria.");
                System.out.println("\nPress Enter to search again, or type 'exit' to return to main menu: ");
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("exit")) {
                    return;
                }
                continue;
            }
            
            // Display results
            System.out.println("\n┌─────┬─────────────┬─────────────────────┬──────────┬─────────────────────┬─────────────────────┐");
            System.out.println("│ No. │ Username    │ Full Name           │ Role     │ Email               │ Contact             │");
            System.out.println("├─────┼─────────────┼─────────────────────┼──────────┼─────────────────────┼─────────────────────┤");
            
            for (int i = 0; i < results.size(); i++) {
                Account acc = results.get(i);
                System.out.printf("│ %-3d │ %-11s │ %-19s │ %-8s │ %-19s │ %-19s │%n",
                    (i + 1),
                    acc.getUsername().length() > 11 ? acc.getUsername().substring(0, 11) : acc.getUsername(),
                    acc.getFullName().length() > 19 ? acc.getFullName().substring(0, 19) : acc.getFullName(),
                    acc.getRole().toString(),
                    acc.getEmail().length() > 19 ? acc.getEmail().substring(0, 19) : acc.getEmail(),
                    acc.getContactNumber().length() > 19 ? acc.getContactNumber().substring(0, 19) : acc.getContactNumber());
            }
            
            System.out.println("└─────┴─────────────┴─────────────────────┴──────────┴─────────────────────┴─────────────────────┘");
            System.out.println("\nFound " + results.size() + " accounts.");
            
            // Management options
            System.out.println("\nSelect an account to manage (enter number), or:");
            System.out.println("'search' - Search again");
            System.out.println("'exit' - Return to main menu");
            System.out.print("Your choice: ");
            
            String choice = scanner.nextLine().trim();
            
            if (choice.equalsIgnoreCase("exit")) {
                return;
            }
            
            if (choice.equalsIgnoreCase("search")) {
                continue;
            }
            
            try {
                int accountIndex = Integer.parseInt(choice) - 1;
                if (accountIndex >= 0 && accountIndex < results.size()) {
                    Account selectedAccount = results.get(accountIndex);
                    manageSelectedAccount(scanner, selectedAccount);
                } else {
                    System.out.println("Invalid account number.");
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number, 'search', or 'exit'.");
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    /**
     * Manage selected account
     */
    public static void manageSelectedAccount(Scanner scanner, Account account) {
        while (true) {
            clearScreen();
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                      MANAGE ACCOUNT                              ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            
            // Display account information
            System.out.println("Account Information:");
            System.out.println("Username: " + account.getUsername());
            System.out.println("Role: " + account.getRole());
            
            // Display role-specific ID
            if (account instanceof Admin) {
                Admin admin = (Admin) account;
                System.out.println("Admin ID: " + admin.getAdminId());
            }
            
            System.out.println("Full Name: " + account.getFullName());
            System.out.println("Email: " + account.getEmail());
            System.out.println("Contact Number: " + account.getContactNumber());
            
            // Display customer-specific fields only for Customer accounts
            if (account instanceof Customer) {
                Customer customer = (Customer) account;
                System.out.println("Address: " + customer.getAddress());
                System.out.println("Date of Birth: " + customer.getDateOfBirth());
                System.out.println("License Number: " + customer.getLicenseNumber());
                System.out.println("Emergency Contact: " + customer.getEmergencyContact());
            }
            
            System.out.println("\n=== What would you like to do? ===");
            System.out.println("1. Modify Account Information");
            System.out.println("2. Delete Account");
            System.out.println("0. Back to Search Results");
            System.out.print("Select option: ");
            
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1":
                    modifySelectedAccount(scanner, account);
                    break;
                case "2":
                    if (deleteSelectedAccount(scanner, account)) {
                        return; // Account deleted, return to search
                    }
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
            }
        }
    }

    /**
     * Add new user account (admin function)
     */
    public static void addNewUserAccount(Scanner scanner) {
        clearScreen();
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                       ADD NEW USER ACCOUNT                       ║"); 
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        // Basic account information
        String username;
        while (true) {
            System.out.print("Username: ");
            username = scanner.nextLine().trim();
            if (username.isEmpty()) {
                System.out.println("Username cannot be empty.");
                continue;
            }
            if (getAccountByUsername(username) != null) {
                System.out.println("Username already exists. Please choose another.");
                continue;
            }
            break;
        }
        
        String password;
        while (true) {
            System.out.print("Password: ");
            password = scanner.nextLine();
            if (password.length() < 4) {
                System.out.println("Password must be at least 4 characters long.");
                continue;
            }
            break;
        }
        
        // Hash the password
        password = hashPassword(password);
        
        // Role selection
        System.out.println("Select role:");
        System.out.println("1. Customer");
        System.out.println("2. Admin");
        System.out.print("Select option: ");
        String roleChoice = scanner.nextLine();
        
        AccountRole role;
        switch (roleChoice) {
            case "1":
                role = AccountRole.CUSTOMER;
                break;
            case "2":
                role = AccountRole.ADMIN;
                break;
            default:
                System.out.println("Invalid role selection. Defaulting to Customer.");
                role = AccountRole.CUSTOMER;
                break;
        }
        
        String email;
        while (true) {
            System.out.print("Email: ");
            email = scanner.nextLine().trim();
            if (!isValidEmail(email)) {
                System.out.println("Please enter a valid email address.");
                continue;
            }
            break;
        }
        
        System.out.print("Full Name: ");
        String fullName = scanner.nextLine().trim();
        while (fullName.isEmpty()) {
            System.out.println("Full name is required.");
            System.out.print("Full Name: ");
            fullName = scanner.nextLine().trim();
        }
        
        String contactNumber;
        while (true) {
            System.out.print("Contact Number (60xxxxxxxxx): ");
            contactNumber = scanner.nextLine().trim();
            if (!isValidContactNumber(contactNumber)) {
                System.out.println("Contact number must start with 60 and be 11-13 digits long.");
                continue;
            }
            break;
        }
        
        System.out.print("Address: ");
        String address = scanner.nextLine().trim();
        
        String licenseNumber = "";
        String dateOfBirth = "";
        if (role == AccountRole.CUSTOMER) {
            while (true) {
                System.out.print("IC/License Number (xxxxxx-xx-xxxx): ");
                licenseNumber = scanner.nextLine().trim();
                if (!licenseNumber.isEmpty() && !isValidLicenseNumber(licenseNumber)) {
                    System.out.println("Please enter a valid IC number in format: xxxxxx-xx-xxxx");
                    continue;
                }
                
                if (!licenseNumber.isEmpty()) {
                    dateOfBirth = extractDateOfBirthFromIC(licenseNumber);
                    if (dateOfBirth != null) {
                        System.out.println("Date of birth extracted from IC: " + dateOfBirth);
                    }
                }
                break;
            }
        }
        
        System.out.print("Emergency Contact Number (optional): ");
        String emergencyContact = scanner.nextLine().trim();
        
        // Create and save account
        Account newAccount;
        if (role == AccountRole.ADMIN) {
            newAccount = new Admin(username, password, email, fullName, contactNumber);
        } else {
            newAccount = new Customer(username, password, email, fullName, 
                                    contactNumber, address, dateOfBirth, 
                                    licenseNumber, emergencyContact);
        }
        
        if (addAccount(newAccount)) {
            saveAccounts("accounts.json");
            System.out.println("\nAccount created successfully!");
            System.out.println("Username: " + username);
            System.out.println("Role: " + role);
        } else {
            System.out.println("Failed to create account.");
        }
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }



    /**
     * Modify selected account (integrated version)
     */
    public static void modifySelectedAccount(Scanner scanner, Account account) {
        System.out.println("\n=== Which information would you like to modify? ===");
        System.out.println("1. Full Name");
        System.out.println("2. Email");
        System.out.println("3. Contact Number");
        
        // Customer-specific options
        if (account instanceof Customer) {
            System.out.println("4. Address");
            System.out.println("5. License Number");
            System.out.println("6. Emergency Contact");
        }
        
        System.out.println("7. Role");
        System.out.println("8. Reset Password");
        System.out.println("0. Back");
        System.out.print("Select option: ");
        
        String choice = scanner.nextLine();
        
        boolean updated = false;
        switch (choice) {
            case "1":
                while (true) {
                    System.out.print("Enter new full name: ");
                    String newFullName = cleanAndValidateFullName(scanner.nextLine());
                    if (newFullName == null || newFullName.isEmpty()) {
                        System.out.println("Full name cannot be empty. Please try again.");
                        continue;
                    }
                    if (!isValidFullName(newFullName)) {
                        System.out.println("Full name can only contain letters, spaces, dots, apostrophes, and hyphens. Please try again.");
                        continue;
                    }
                    account.setFullName(newFullName);
                    updated = true;
                    break;
                }
                break;
            case "2":
                while (true) {
                    System.out.print("Enter new email: ");
                    String newEmail = scanner.nextLine().trim();
                    if (isValidEmail(newEmail)) {
                        account.setEmail(newEmail);
                        updated = true;
                        break;
                    } else {
                        System.out.println("Invalid email format. Please try again.");
                    }
                }
                break;
            case "3":
                while (true) {
                    System.out.print("Enter new contact number (60xxxxxxxxx): ");
                    String newContact = scanner.nextLine().trim();
                    if (isValidContactNumber(newContact)) {
                        account.setContactNumber(newContact);
                        updated = true;
                        break;
                    } else {
                        System.out.println("Invalid contact number format. Please try again.");
                    }
                }
                break;
            case "4":
                if (account instanceof Customer) {
                    Customer customer = (Customer) account;
                    System.out.print("Enter new address: ");
                    String newAddress = scanner.nextLine().trim();
                    customer.setAddress(newAddress);
                    updated = true;
                } else {
                    System.out.println("Invalid option for admin account.");
                }
                break;
            case "5":
                if (account instanceof Customer) {
                    Customer customer = (Customer) account;
                    while (true) {
                        System.out.print("Enter new license number (xxxxxx-xx-xxxx): ");
                        String newLicense = scanner.nextLine().trim();
                        if (newLicense.isEmpty() || isValidLicenseNumber(newLicense)) {
                            customer.setLicenseNumber(newLicense);
                            if (!newLicense.isEmpty()) {
                                String newDateOfBirth = extractDateOfBirthFromIC(newLicense);
                                if (newDateOfBirth != null) {
                                    customer.setDateOfBirth(newDateOfBirth);
                                    System.out.println("Date of birth updated to: " + newDateOfBirth);
                                }
                            }
                            updated = true;
                            break;
                        } else {
                            System.out.println("Invalid license number format. Please try again.");
                        }
                    }
                } else {
                    System.out.println("Invalid option for admin account.");
                }
                break;
            case "6":
                if (account instanceof Customer) {
                    Customer customer = (Customer) account;
                    System.out.print("Enter new emergency contact (leave blank to clear): ");
                    String newEmergency = scanner.nextLine().trim();
                    customer.setEmergencyContact(newEmergency);
                    updated = true;
                } else {
                    System.out.println("Invalid option for admin account.");
                }
                break;
            case "7":
                System.out.println("Current role: " + account.getRole());
                System.out.println("Select new role:");
                System.out.println("1. Customer");
                System.out.println("2. Admin");
                System.out.print("Select option: ");
                String roleChoice = scanner.nextLine();
                
                AccountRole newRole = null;
                switch (roleChoice) {
                    case "1":
                        newRole = AccountRole.CUSTOMER;
                        break;
                    case "2":
                        newRole = AccountRole.ADMIN;
                        break;
                    default:
                        System.out.println("Invalid role selection.");
                        break;
                }
                
                if (newRole != null && newRole != account.getRole()) {
                    account.setRole(newRole);
                    updated = true;
                    System.out.println("Role updated to: " + newRole);
                }
                break;
            case "8":
                String newPassword;
                while (true) {
                    System.out.print("Enter new password: ");
                    newPassword = scanner.nextLine();
                    if (newPassword.length() < 4) {
                        System.out.println("Password must be at least 4 characters long.");
                        continue;
                    }
                    break;
                }
                
                // Hash the password
                String hashedPassword = hashPassword(newPassword);
                account.setPassword(hashedPassword);
                updated = true;
                System.out.println("Password reset successfully.");
                break;
            case "0":
                return;
            default:
                System.out.println("Invalid option.");
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
                return;
        }
        
        if (updated) {
            if (updateAccount(account.getUsername(), account)) {
                saveAccounts("accounts.json");
                System.out.println("Account information updated successfully!");
            } else {
                System.out.println("Failed to update account information.");
            }
        }
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Delete selected account (integrated version)
     */
    public static boolean deleteSelectedAccount(Scanner scanner, Account account) {
        // Prevent admin from deleting themselves
        System.out.print("Enter your admin username for confirmation: ");
        String adminUsername = scanner.nextLine().trim();
        
        if (adminUsername.equals(account.getUsername())) {
            System.out.println("Error: You cannot delete your own account!");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return false;
        }
        
        System.out.print("\nAre you sure you want to delete this account? (type 'DELETE' to confirm): ");
        String confirmation = scanner.nextLine();
        
        if (confirmation.equals("DELETE")) {
            if (deleteAccount(account.getUsername())) {
                saveAccounts("accounts.json");
                System.out.println("Account deleted successfully!");
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
                return true;
            } else {
                System.out.println("Failed to delete account.");
            }
        } else {
            System.out.println("Account deletion cancelled.");
        }
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
        return false;
    }
    // Admin user management menu
    public static void adminUserManagement(Scanner scanner) {
        boolean firstTime = true;
        while (true) {
            // Clear screen after first time
            if (!firstTime) {
                clearScreen();
            }
            firstTime = false;
            
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                       USER MANAGEMENT                            ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║  1. View All User Accounts                                       ║");
            System.out.println("║  2. Search & Manage User Accounts                                ║");
            System.out.println("║  3. Add New User Account                                         ║");
            System.out.println("║  0. Back to Main Menu                                            ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    viewAllUserAccounts(scanner);
                    break;
                case "2":
                    searchAndManageUserAccounts(scanner);
                    break;
                case "3":
                    addNewUserAccount(scanner);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
            }
        }
    }
}
