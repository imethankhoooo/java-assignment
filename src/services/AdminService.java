package services;

import enums.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import models.*;
import static services.UtilityService.*;

public class AdminService extends AccountService {

    /**
     * Get all admin accounts
     */
    public static List<Account> getAdminAccounts() {
        List<Account> admins = new ArrayList<>();
        for (Account account : getAccounts()) {
            if (account.getRole() == AccountRole.ADMIN) {
                admins.add(account);
            }
        }
        return admins;
    }

    /**
     * Admin user management interface
     */
    public static void manageUsers(Scanner scanner) {
        adminUserManagement(scanner);
    }

    /**
     * Admin vehicle management interface
     */
    public static void manageVehicles(Scanner scanner) {
        while (true) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                      VEHICLE MANAGEMENT                          ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. View All Vehicles                                            ║");
            System.out.println("║ 2. Add New Vehicle                                              ║");
            System.out.println("║ 3. Update Vehicle Information                                   ║");
            System.out.println("║ 4. Manage Vehicle Status                                        ║");
            System.out.println("║ 5. Archive Vehicle                                              ║");
            System.out.println("║ 6. Restore Archived Vehicle                                     ║");
            System.out.println("║ 7. Vehicle Search                                               ║");
            System.out.println("║ 0. Back to Admin Menu                                           ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    vehicleService.displayAllVehicles();
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
                    break;
                case "2":
                    vehicleService.addNewVehicle(null, scanner);
                    break;
                case "3":
                    vehicleService.updateVehicle(null, scanner);
                    break;
                case "4":
                    vehicleService.manageVehicleStatus(null, scanner);
                    break;
                case "5":
                    vehicleService.archiveVehicle(null, scanner);
                    break;
                case "6":
                    vehicleService.restoreArchived(null, scanner);
                    break;
                case "7":
                    searchVehicles(scanner);
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
     * Search vehicles (admin function)
     */
    private static void searchVehicles(Scanner scanner) {
        System.out.println("\n=== VEHICLE SEARCH ===");
        System.out.print("Enter search term (brand, model, plate, or leave empty for all): ");
        String searchTerm = scanner.nextLine().trim();

        List<Vehicle> results;
        if (searchTerm.isEmpty()) {
            results = vehicleService.getVehicles();
        } else {
            results = vehicleService.performSearch(vehicleService.getVehicles(), searchTerm);
        }

        if (results.isEmpty()) {
            System.out.println("No vehicles found matching your search.");
        } else {
            System.out.printf("\nFound %d vehicle(s):%n", results.size());
            vehicleService.displaySearchResults(results);
        }

        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Admin business operations interface
     */
    public static void businessOperations(Scanner scanner) {
        while (true) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                    BUSINESS OPERATIONS                           ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. View Business Reports                                        ║");
            System.out.println("║ 2. Export Reports                                               ║");
            System.out.println("║ 3. System Statistics                                            ║");
            System.out.println("║ 4. Revenue Analysis                                             ║");
            System.out.println("║ 0. Back to Admin Menu                                           ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    viewBusinessReports();
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
                    break;
                case "2":
                    exportReports(scanner);
                    break;
                case "3":
                    showSystemStatistics();
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
                    break;
                case "4":
                    analyzeRevenue();
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
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
     * View business reports
     */
    private static void viewBusinessReports() {
        System.out.println("\n=== BUSINESS REPORTS ===");

        // Vehicle statistics
        List<Vehicle> allVehicles = vehicleService.getVehicles();
        long totalVehicles = allVehicles.size();
        long availableVehicles = allVehicles.stream()
                .filter(v -> "available".equalsIgnoreCase(v.getStatus()))
                .count();
        long rentedVehicles = allVehicles.stream()
                .filter(v -> "rented".equalsIgnoreCase(v.getStatus()))
                .count();
        long outOfServiceVehicles = allVehicles.stream()
                .filter(v -> "out_of_service".equalsIgnoreCase(v.getStatus()))
                .count();

        System.out.println("Vehicle Statistics:");
        System.out.printf("  Total Vehicles: %d%n", totalVehicles);
        System.out.printf("  Available: %d%n", availableVehicles);
        System.out.printf("  Rented: %d%n", rentedVehicles);
        System.out.printf("  Out of Service: %d%n", outOfServiceVehicles);

        // Account statistics
        List<Account> allAccounts = getAccounts();
        long totalAccounts = allAccounts.size();
        long customerAccounts = allAccounts.stream()
                .filter(a -> a.getRole() == AccountRole.CUSTOMER)
                .count();
        long adminAccounts = allAccounts.stream()
                .filter(a -> a.getRole() == AccountRole.ADMIN)
                .count();

        System.out.println("\nAccount Statistics:");
        System.out.printf("  Total Accounts: %d%n", totalAccounts);
        System.out.printf("  Customers: %d%n", customerAccounts);
        System.out.printf("  Administrators: %d%n", adminAccounts);
    }

    /**
     * Export reports
     */
    private static void exportReports(Scanner scanner) {
        System.out.println("\n=== EXPORT REPORTS ===");
        System.out.println("1. Export Vehicle Report");
        System.out.println("2. Export Account Report");
        System.out.println("3. Export Rental Report");
        System.out.println("0. Cancel");
        System.out.print("Select report type: ");

        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                System.out.println("Vehicle report export functionality coming soon...");
                break;
            case "2":
                System.out.println("Account report export functionality coming soon...");
                break;
            case "3":
                System.out.println("Rental report export functionality coming soon...");
                break;
            case "0":
                return;
            default:
                System.out.println("Invalid option.");
        }

        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Show system statistics
     */
    private static void showSystemStatistics() {
        System.out.println("\n=== SYSTEM STATISTICS ===");
        System.out.println("Detailed system statistics functionality coming soon...");
    }

    /**
     * Analyze revenue
     */
    private static void analyzeRevenue() {
        System.out.println("\n=== REVENUE ANALYSIS ===");
        System.out.println("Revenue analysis functionality coming soon...");
    }

    public static void manageAdminProfile(Scanner scanner, Account adminAccount) {
        if (adminAccount.getRole() != AccountRole.ADMIN) {
            System.out.println("This function is only for admin accounts.");
            return;
        }

        while (true) {
            clearScreen();
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                           ADMIN PROFILE                          ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. View Profile Information                                      ║");
            System.out.println("║ 2. Update Profile Information                                    ║");
            System.out.println("║ 3. Change Password                                               ║");
            System.out.println("║ 4. Update Email Address                                          ║");
            System.out.println("║ 0. Back                                                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    displayAdminProfile(adminAccount);
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
                    clearScreen();
                    break;
                case "2":
                    viewAndModifyAccountInfo(scanner, adminAccount);
                    break;
                case "3":
                    changePassword(scanner, adminAccount);
                    break;
                case "4":
                    updateEmailAddress(scanner, adminAccount);
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

    private static void displayAdminProfile(Account adminAccount) {
        System.out.println("\n");
        System.out.println("==========================================");
        System.out.println("              ADMIN PROFILE           ");
        System.out.println("==========================================");

        System.out.printf("%-20s : %s\n", "Username", adminAccount.getUsername());

        if (adminAccount instanceof Admin) {
            Admin admin = (Admin) adminAccount;
            System.out.printf("%-20s : %s\n", "Admin ID", admin.getAdminId());
        }

        System.out.printf("%-20s : %s\n", "Full Name", adminAccount.getFullName());
        System.out.printf("%-20s : %s\n", "Email", adminAccount.getEmail());
        System.out.printf("%-20s : %s\n", "Contact Number", adminAccount.getContactNumber());
        System.out.printf("%-20s : %s\n", "Role", adminAccount.getRole());
    }
}
