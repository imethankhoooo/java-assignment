package services;

import enums.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import models.*;
import static services.UtilityService.*;

public class AdminService extends AccountService {

    public static List<Account> getAdminAccounts() {
        List<Account> admins = new ArrayList<>();
        for (Account account : getAccounts()) {
            if (account.getRole() == AccountRole.ADMIN) {
                admins.add(account);
            }
        }
        return admins;
    }

    public static void manageUsers(Scanner scanner) {
        adminUserManagement(scanner);
    }

    public static void manageVehicles(Scanner scanner) {
        while (true) {
            clearScreen();
            System.out.println("╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                      VEHICLE MANAGEMENT                          ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. View All Vehicles                                             ║");
            System.out.println("║ 2. Add New Vehicle                                               ║");
            System.out.println("║ 3. Update Vehicle Information                                    ║");
            System.out.println("║ 4. Manage Vehicle Status                                         ║");
            System.out.println("║ 5. Archive Vehicle                                               ║");
            System.out.println("║ 6. Restore Archived Vehicle                                      ║");
            System.out.println("║ 7. Vehicle Search                                                ║");
            System.out.println("║ 0. Back to Admin Menu                                            ║");
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
            System.out.printf("\nFound %d vehicle(s):\n", results.size());
            vehicleService.displaySearchResults(results);
        }

        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    public static void reportsAndAnalytics(RentalSystem system, Scanner scanner) {
        while (true) {
            System.out.println("\n");
            System.out.println("╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                      REPORTS & ANALYTICS                         ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. Monthly Report                                                ║");
            System.out.println("║ 2. Popular Vehicle Report                                        ║");
            System.out.println("║ 3. Customer Report                                               ║");
            System.out.println("║ 4. System Report                                                 ║");
            System.out.println("║ 0. Back to Admin Menu                                            ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            
            // Use system's rental data directly instead of creating new RentalHistoryManager
            List<Rental> rentals = system.getRentals();
            
            switch (choice) {
                case "1":
                    ReportService.generateMonthlyReport(rentals, scanner);
                    break;
                case "2":
                    ReportService.generatePopularVehicleReport(rentals, scanner);
                    break;
                case "3":
                    ReportService.generateCustomerReport(rentals);
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
                    break;
                case "4":
                    List<Customer> customers = CustomerService.getCustomerAccounts().stream()
                        .filter(account -> account instanceof Customer)
                        .map(account -> (Customer) account)
                        .collect(java.util.stream.Collectors.toList());
                    ReportService.generateSystemReport(rentals, vehicleService.getVehicles(), customers);
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
    System.out.println("╔══════════════════════════════════════════════════════════════════╗");
    System.out.println("║                        ADMIN PROFILE                             ║");
    System.out.println("╚══════════════════════════════════════════════════════════════════╝");

    System.out.println("╔════════════════════════════════╦════════════════════════════════╗");
    System.out.printf("║ %-30s ║ %-30s ║\n", "Username", adminAccount.getUsername());

    if (adminAccount instanceof Admin) {
        Admin admin = (Admin) adminAccount;
        System.out.printf("║ %-30s ║ %-30s ║\n", "Admin ID", admin.getAdminId());
    }

    System.out.printf("║ %-30s ║ %-30s ║\n", "Full Name", adminAccount.getFullName());
    System.out.printf("║ %-30s ║ %-30s ║\n", "Email", adminAccount.getEmail());
    System.out.printf("║ %-30s ║ %-30s ║\n", "Contact Number", adminAccount.getContactNumber());
    System.out.printf("║ %-30s ║ %-30s ║\n", "Role", adminAccount.getRole());
    System.out.println("╚════════════════════════════════╩════════════════════════════════╝");
}
}
