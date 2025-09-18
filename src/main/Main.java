package main;
import enums.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import models.*;
import services.*;
import static services.UtilityService.*;

/**
 * Main program entry point, handles data loading, login, and menu navigation
 */
public class Main {
    
    public static void main(String[] args) {
        clearScreen();
        RentalSystem system = new RentalSystem();
        AccountService.loadAccounts("accounts.json");
        vehicleService.loadVehicles("vehicles.json");
        system.loadRentals("rentals.json");

        
        // Load user emails for notification service
        system.getNotificationService().loadUserEmailsFromAccounts(AccountService.getAccounts());
        
        // No need to sync vehicles to system anymore
        vehicleService.setRentals(system.getRentals());
        
        // Only sync status if there are inconsistencies, don't force override
        // The status from saved files should be respected initially
        // system.syncVehicleStatusWithRentals();
        // vehicleService.saveVehicles("vehicles.json");
        
        Scanner scanner = new Scanner(System.in);
        clearScreen();
        // Display ASCII art banner
        System.out.println("\n\n\n\n");
        System.out.println("                     ▄████████    ▄████████    ▄████████    ▄████████    ▄████████    ▄████████    ▄█   ▄█▄ ");
        System.out.println("                    ███    ███   ███    ███   ███    ███   ███    ███   ███    ███   ███    ███   ███ ▄███▀ ");
        System.out.println("                    ███    █▀    ███    ███   ███    ███   ███    █▀    ███    █▀    ███    █▀    ███▐██▀   ");
        System.out.println("                    ███          ███    ███  ▄███▄▄▄▄██▀   ███         ▄███▄▄▄      ▄███▄▄▄      ▄█████▀    ");
        System.out.println("                    ███        ▀███████████ ▀▀███▀▀▀▀▀   ▀███████████ ▀▀███▀▀▀     ▀▀███▀▀▀     ▀▀█████▄    ");
        System.out.println("                    ███    █▄    ███    ███ ▀███████████          ███   ███    █▄    ███    █▄    ███▐██▄   ");
        System.out.println("                    ███    ███   ███    ███   ███    ███    ▄█    ███   ███    ███   ███    ███   ███ ▀███▄ ");
        System.out.println("                    ████████▀    ███    █▀    ███    ███  ▄████████▀    ██████████   ██████████   ███   ▀█▀ ");
        System.out.println("                                              ███    ███                                          ▀         ");
        
        // Check and send automatic reminders (but don't display summary yet)
        system.checkAndSendReminders();
        
        Account currentAccount = null;
        while (true) {
            if (currentAccount == null) {
            // Show login/register choice
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                    WELCOME TO CARSEEK                            ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. Login                                                         ║");
            System.out.println("║ 2. Register                                                      ║");
            System.out.println("║ 0. Exit                                                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    currentAccount = AccountService.loginProcess(scanner);
                    break;
                case "2":
                    AccountService.registerProcess(scanner);
                    break;
                case "0":
                    System.out.println("Thank you for using CARSEEK. Goodbye!");
                    system.shouldExit = true;
                    break;
                default:
                    System.out.println("INVALID OPTION. PRESS ENTER TO CONTINUE...");
                    scanner.nextLine();
                    clearScreen();
                    break;
            }
            } else {
                clearScreen();
                
                System.out.println("Login successful. Welcome " + currentAccount.getUsername() + "!");
                
                // Display reminders summary after login (for admin users)
                if (currentAccount.getRole() == AccountRole.ADMIN) {
                    ReminderService reminderService = new ReminderService(system);
                    reminderService.displayReminderSummary();
                }
                
                // Show unread messages and mark them as read
                List<Message> unreadMessages = system.getNotificationService().getUnreadMessages(currentAccount.getUsername());
                if (!unreadMessages.isEmpty()) {
                    System.out.println("\n=== You have " + unreadMessages.size() + " unread messages ===");
                    for (Message message : unreadMessages) {
                        System.out.println("From: " + message.getSender());
                        System.out.println("Subject: " + message.getSubject());
                        System.out.println("Content: " + message.getContent());
                        System.out.println("Time: " + message.getTimestamp());
                        System.out.println("---");
                        
                        // Mark as read
                        system.markMessageAsRead(message.getId());
                    }
                    System.out.println("All messages marked as read.");
                }
                
                // Role-based menu routing
                if (currentAccount.getRole() == AccountRole.ADMIN) {
                    currentAccount = adminMenu(system, scanner, currentAccount);
                } else {
                    currentAccount = customerMenu(system, scanner, currentAccount);
                }
            }
            
            // Check if we should exit
            if (currentAccount == null && system.shouldExit) {
                break;
            }
        }
        
        // Save data before exit
        System.out.println("Saving all data...");
        AccountService.saveAccounts("accounts.json");
        system.saveRentals("rentals.json");

        // Sync system vehicle data back to vehicle service before saving
        // Vehicles are already managed by vehicleService
        vehicleService.saveVehicles("vehicles.json");
        System.out.println("All data saved successfully.");
        scanner.close();
    }

    // Admin main menu - new two-level structure
    private static Account adminMenu(RentalSystem system, Scanner scanner, Account currentAccount) {
        boolean firstTime = true;
        while (true) {
            // Clear screen after first time
            if (!firstTime) {
                clearScreen();
            }
            firstTime = false;
            
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                           ADMIN PANEL                            ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║  1. User Account Management                                      ║");
            System.out.println("║  2. Vehicle Management                                           ║");
            System.out.println("║  3. Booking Management                                           ║");
            System.out.println("║  4. Payment Management                                           ║");
            System.out.println("║  5. Reports & Analytics                                          ║");
            System.out.println("║  6. Admin Profile Management                                     ║");
            System.out.println("║  0. Logout                                                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    AccountService.adminUserManagement(scanner, system);
                    break;
                case "2":
                    AdminService.manageVehicles(scanner);
                    break;
                case "3":
                    RentalSystem.adminBusinessOperations(system, scanner);
                    break;
                case "4":
                    paymentManagementMenu(system, scanner, currentAccount);
                    break;
                case "5":
                    AdminService.reportsAndAnalytics(system, scanner);
                    break;
                case "6":
                    AdminService.manageAdminProfile(scanner, currentAccount);
                    break;
                case "0":
                    System.out.println("Logged out.");
                    clearScreen();
                    return null;
                default:
                    System.out.println("Invalid option. Please try again.");
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
            }
        }
    }
    // Customer main menu - new two-level structure  
    private static Account customerMenu(RentalSystem system, Scanner scanner, Account account) {
        boolean firstTime = true;
        while (true) {
            // Clear screen after first time
            if (!firstTime) {
                clearScreen();
            }
            firstTime = false;
            
            System.out.println("╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                          CUSTOMER PANEL                          ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. Account Management                                            ║");
            System.out.println("║ 2. Vehicle Rental & Booking                                      ║");
            System.out.println("║ 3. Payment Management                                            ║");
            System.out.println("║ 0. Logout                                                        ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    CustomerService.manageCustomerProfile(scanner, account);
                    break;
                case "2":
                    RentalSystem.customerBookingModule(system, scanner, account);
                    break;
                case "3":
                    paymentManagementMenu(system, scanner, account);
                    break;
                case "0":
                    System.out.println("Logged out.");
                    clearScreen();
                    return null;
                default:
                    System.out.println("Invalid option. Please try again.");
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
            }
        }
    }

    public static void viewStatistics(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      SYSTEM STATISTICS                           ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        
        List<Rental> rentals = system.getRentals();
        List<Vehicle> vehicles = vehicleService.getVehicles();
        
        int totalRentals = rentals.size();
        int activeRentals = 0;
        int completedRentals = 0;
        int pendingRentals = 0;
        double totalRevenue = 0.0;
        
        for (Rental rental : rentals) {
            switch (rental.getStatus()) {
                case ACTIVE:
                    activeRentals++;
                    break;
                case RETURNED:
                    completedRentals++;
                    totalRevenue += rental.getTotalFee();
                    break;
                case PENDING:
                    pendingRentals++;
                    break;
                case CANCELLED:
                    // Cancelled rentals are not counted in statistics
                    break;
            }
        }
        
        int availableVehicles = 0;
        int rentedVehicles = 0;
        
        for (Vehicle vehicle : vehicles) {
            String status = vehicle.getStatus().toUpperCase();
            switch (status) {
                case "AVAILABLE":
                    availableVehicles++;
                    break;
                case "RENTED":
                    rentedVehicles++;
                    break;
                case "RESERVED":
                    // Reserved vehicles are counted as available
                    availableVehicles++;
                    break;
                case "OUT_OF_SERVICE":
                    // Disabled vehicles are not counted in any statistics
                    break;
            }
        }
        
        System.out.printf("║ Total Vehicles:        │ %-39d ║%n", vehicles.size());
        System.out.printf("║ Available Vehicles:    │ %-39d ║%n", availableVehicles);
        System.out.printf("║ Rented Vehicles:       │ %-39d ║%n", rentedVehicles);

        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Total Rentals:         │ %-39d ║%n", totalRentals);
        System.out.printf("║ Active Rentals:        │ %-39d ║%n", activeRentals);
        System.out.printf("║ Completed Rentals:     │ %-39d ║%n", completedRentals);
        System.out.printf("║ Pending Rentals:       │ %-39d ║%n", pendingRentals);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Total Revenue:         │ RM%-37.2f ║%n", totalRevenue);
        System.out.printf("║ Average Revenue/Rental:│ RM%-37.2f ║%n", 
                         completedRentals > 0 ? totalRevenue / completedRentals : 0.0);
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        // Prepare data for export
        List<String> headers = Arrays.asList("Metric", "Value");
        List<List<String>> data = Arrays.asList(
            Arrays.asList("Total Vehicles", String.valueOf(vehicles.size())),
            Arrays.asList("Available Vehicles", String.valueOf(availableVehicles)),
            Arrays.asList("Rented Vehicles", String.valueOf(rentedVehicles)),

            Arrays.asList("Total Rentals", String.valueOf(totalRentals)),
            Arrays.asList("Active Rentals", String.valueOf(activeRentals)),
            Arrays.asList("Completed Rentals", String.valueOf(completedRentals)),
            Arrays.asList("Pending Rentals", String.valueOf(pendingRentals)),
            Arrays.asList("Total Revenue", String.format("%.2f", totalRevenue)),
            Arrays.asList("Average Revenue per Rental", 
                         String.format("%.2f", completedRentals > 0 ? totalRevenue / completedRentals : 0.0))
        );
        
        ReportExportService exportService = new ReportExportService();
        exportService.promptForExport(scanner, "System Statistics", headers, data, "system_statistics");
    }

    // Payment management menu
    public static void paymentManagementMenu(RentalSystem system, Scanner scanner, Account account) {
        PaymentService paymentService = new PaymentService();
        paymentService.paymentManagementMenu(scanner, account);
    }

} 