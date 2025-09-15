package services;

import enums.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import models.*;
import static services.UtilityService.*;

public class CustomerService extends AccountService {

    /**
     * Get all customer accounts
     */
    public static List<Account> getCustomerAccounts() {
        List<Account> customers = new ArrayList<>();
        for (Account account : getAccounts()) {
            if (account.getRole() == AccountRole.CUSTOMER) {
                customers.add(account);
            }
        }
        return customers;
    }

    /**
     * Customer-specific registration with IC validation
     */
    public static boolean registerCustomer(Scanner scanner) {
        System.out.println("\n=== CUSTOMER REGISTRATION ===");

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

        // Email verification
        System.out.println("\n=== EMAIL VERIFICATION ===");
        String verificationCode = generateVerificationCode();

        if (!sendEmailVerification(email, verificationCode)) {
            System.out.println("Failed to send verification email. Registration cancelled.");
            return false;
        }

        if (!verifyEmailCode(scanner, verificationCode, email)) {
            System.out.println("Email verification failed. Registration cancelled.");
            return false;
        }

        // Create and save account
        Customer newAccount = new Customer(username, password, email, fullName,
                contactNumber, address, dateOfBirth,
                licenseNumber, emergencyContact);

        if (addAccount(newAccount)) {
            saveAccounts("accounts.json");
            System.out.println("\n=== REGISTRATION SUCCESSFUL! ===");
            System.out.println("Welcome " + fullName + "! You can now login with your credentials.");
            return true;
        } else {
            System.out.println("Registration failed. Please try again.");
            return false;
        }
    }

    public static void manageCustomerProfile(Scanner scanner, Account customerAccount) {
        if (customerAccount.getRole() != AccountRole.CUSTOMER) {
            System.out.println("This function is only for customer accounts.");
            return;
        }

        while (true) {
            clearScreen();
            System.out.println("\n");
            System.out.println("╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                         CUSTOMER PROFILE                         ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. View Profile Information                                      ║");
            System.out.println("║ 2. Update Profile Information                                    ║");
            System.out.println("║ 3. Change Password                                               ║");
            System.out.println("║ 4. Update Email Address                                          ║");
            System.out.println("║ 0. Back to Panel                                                 ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    displayCustomerProfile(customerAccount);
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    clearScreen();
                    break;
                case "2":
                    viewAndModifyAccountInfo(scanner, customerAccount);
                    break;
                case "3":
                    changePassword(scanner, customerAccount);
                    break;
                case "4":
                    updateEmailAddress(scanner, customerAccount);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
            }
        }
    }

    private static void displayCustomerProfile(Account customerAccount) {
        System.out.println("\n");
        System.out.println("=====================================");
        System.out.println("            CUSTOMER PROFILE          ");
        System.out.println("=====================================");
        System.out.printf("%-20s : %s\n", "Username", customerAccount.getUsername());
        System.out.printf("%-20s : %s\n", "Full Name", customerAccount.getFullName());
        System.out.printf("%-20s : %s\n", "Email", customerAccount.getEmail());
        System.out.printf("%-20s : %s\n", "Contact Number", customerAccount.getContactNumber());

        if (customerAccount instanceof Customer) {
            Customer customer = (Customer) customerAccount;
            System.out.printf("%-20s : %s\n", "Address", customer.getAddress());
            System.out.printf("%-20s : %s\n", "Date of Birth", customer.getDateOfBirth());
            System.out.printf("%-20s : %s\n", "License Number", customer.getLicenseNumber());
            System.out.printf("%-20s : %s\n", "Emergency Contact", customer.getEmergencyContact());
        }
    }
}
