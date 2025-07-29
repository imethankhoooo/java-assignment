import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


/**
 * Main program entry point, handles data loading, login, and menu navigation
 */
public class Main {
    
    /**
     * Method to clear terminal screen
     */
    private static void clearScreen() {
        try {
            // Detect operating system
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                // Windows systems use cls command
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                // Unix/Linux/Mac systems use clear command
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            // If clearing fails, print empty lines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
    

    public static void main(String[] args) {
        clearScreen();
        RentalSystem system = new RentalSystem();
        system.loadAccounts("accounts.json");
        system.loadVehicles("vehicles.json");
        system.loadRentals("rentals.json");
        system.loadMaintenanceLogs("maintenance_logs.json");
        
        // 同步车辆状态与租赁状态，确保一致性
        system.syncVehicleStatusWithRentals();
        system.saveVehicles("vehicles.json");
        
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Vehicle Rental Service System");
        
        // Check and send automatic reminders (but don't display summary yet)
        system.checkAndSendReminders();
        
        Account currentAccount = null;
        while (currentAccount == null) {
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();
            currentAccount = system.login(username, password);
            if (currentAccount == null) {
                System.out.println("Invalid username or password. Please try again.");
            }
        }
        
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
            adminMenu(system, scanner);
        } else {
            customerMenu(system, scanner, currentAccount);
        }
        
        // Save data before exit
        System.out.println("Saving all data...");
        system.saveRentals("rentals.json");
        system.saveMaintenanceLogs("maintenance_logs.json");
        system.saveVehicles("vehicles.json");
        System.out.println("All data saved successfully.");
        scanner.close();
    }

    // Admin main menu
    private static void adminMenu(RentalSystem system, Scanner scanner) {
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
            System.out.println("║  1. View All Vehicles                                            ║");
            System.out.println("║  2. View All Rentals                                             ║");
            System.out.println("║  3. Rental Management (Pending/Approve/Reject)                   ║");
            System.out.println("║  4. Confirm Vehicle Return                                       ║");
            System.out.println("║  5. Add New Vehicle                                              ║");
            System.out.println("║  6. Reports & Analytics                                          ║");
            System.out.println("║  7. Maintenance Management                                       ║");
            System.out.println("║  8. View Reminders                                               ║");
            System.out.println("║  9. Ticket Management                                            ║");
            System.out.println("║ 10. Message Center                                               ║");
            System.out.println("║ 11. Offline Booking (Walk-in Customers)                          ║");
            System.out.println("║  0. Logout                                                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    viewAllVehicles(system.getVehicles());
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "2":
                    viewAllRentals(system.getRentals());
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "3":
                    rentalManagement(system, scanner);
                    break;
                case "4":
                    confirmReturn(system, scanner);
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "5":
                    addNewVehicle(system, scanner);
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "6":
                    reportsAndAnalytics(system, scanner);
                    break;
                case "7":
                    maintenanceManagement(system, scanner);
                    break;
                case "8":
                    ReminderService reminderService = new ReminderService(system);
                    reminderService.displayReminderSummary();
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "9":
                    ticketManagement(system, scanner);
                    break;
                case "10":
                    messageCenter(system, scanner, "admin");
                    break;
                case "11":
                    offlineBooking(system, scanner);
                    break;
                case "0":
                    System.out.println("Logged out.");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
            }
        }
    }

    // Customer main menu
    private static void customerMenu(RentalSystem system, Scanner scanner, Account account) {
        boolean firstTime = true;
        while (true) {
            // Clear screen after first time
            if (!firstTime) {
                clearScreen();
            }
            firstTime = false;
            
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                          CUSTOMER PANEL                          ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. View Available Vehicles                                       ║");
            System.out.println("║ 2. Make Rental/Booking                                           ║");
            System.out.println("║ 3. Cancel Booking                                                ║");
            System.out.println("║ 4. Request Vehicle Return                                        ║");
            System.out.println("║ 5. View My Rental History                                        ║");
            System.out.println("║ 6. View My Rental Tickets                                        ║");
            System.out.println("║ 7. Message Center                                                ║");
            System.out.println("║ 0. Logout                                                        ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    listAvailableVehicles(system.getAvailableVehicles());
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "2":
                    initiateRental(system, scanner, account);
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "3":
                    cancelBooking(system, scanner, account.getUsername());
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "4":
                    requestReturn(system, scanner, account.getUsername());
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "5":
                    viewMyRentalHistoryEnhanced(system, account.getUsername());
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "6":
                    viewMyTickets(system, scanner, account.getUsername());
                    break;
                case "7":
                    messageCenter(system, scanner, account.getUsername());
                    break;
                case "0":
                    System.out.println("Logged out.");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
            }
        }
    }
    

    
   
    
    // 检查车辆是否匹配搜索词
    private static boolean matchesVehicle(Vehicle vehicle, String term) {
        term = term.toUpperCase();
        
        return vehicle.getBrand().toUpperCase().contains(term) ||
               vehicle.getModel().toUpperCase().contains(term) ||
               vehicle.getCarPlate().toUpperCase().contains(term) ||
               vehicle.getVehicleType().toString().toUpperCase().contains(term) ||
               vehicle.getFuelType().toString().toUpperCase().contains(term);
    }
    

    


    // Display all vehicles (for maintenance history)
    private static void listAllVehicles(List<Vehicle> vehicles) {
        System.out.println("\n┌─────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐");
        System.out.println("│ ID  │    Brand    │    Model    │  Car Plate  │    Type     │   Status    │");
        System.out.println("├─────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤");
        
        for (Vehicle v : vehicles) {
            String brand = v.getBrand().length() > 11 ? v.getBrand().substring(0, 8) + "..." : v.getBrand();
            String model = v.getModel().length() > 11 ? v.getModel().substring(0, 8) + "..." : v.getModel();
            String carPlate = v.getCarPlate().length() > 11 ? v.getCarPlate().substring(0, 8) + "..." : v.getCarPlate();
            String type = v.getVehicleType().toString().length() > 11 ? v.getVehicleType().toString().substring(0, 8) + "..." : v.getVehicleType().toString();
            String status = v.getStatus().toString();
            if (status.equals("UNDER_MAINTENANCE")) status = "MAINTENANCE";
            else if (status.length() > 11) status = status.substring(0, 8) + "...";
            
            System.out.printf("│ %-3d │ %-11s │ %-11s │ %-11s │ %-11s │ %-11s │%n",
                v.getId(), brand, model, carPlate, type, status
            );
        }
        
        System.out.println("└─────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘");
        System.out.println("Total vehicles: " + vehicles.size());
    }

    // Display all available vehicles with enhanced information
    private static void listAvailableVehicles(List<Vehicle> vehicles) {
        System.out.println("\n=== Vehicle List (All Vehicles) ===");
        
        if (vehicles.isEmpty()) {
            System.out.println("No vehicles found.");
            return;
        }
        
        System.out.println("┌─────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐");
        System.out.println("│ ID  │ Brand       │ Model       │ Car Plate   │ Type        │ Fuel        │ Price/Day   │ Status      │");
        System.out.println("├─────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤");
        
        for (Vehicle v : vehicles) {
            String statusDisplay = "";
            if (v.getStatus() == VehicleStatus.AVAILABLE) {
                statusDisplay = "AVAILABLE";
            } else if (v.getStatus() == VehicleStatus.RENTED) {
                statusDisplay = "RENTED";
            } else if (v.getStatus() == VehicleStatus.RESERVED) {
                statusDisplay = "RESERVED";
            } else if (v.getStatus() == VehicleStatus.UNDER_MAINTENANCE) {
                statusDisplay = "MAINTENANCE";
            } else if (v.getStatus() == VehicleStatus.OUT_OF_SERVICE) {
                statusDisplay = "OUT_SERVICE";
            }
            
                System.out.printf("│ %-3d │ %-11s │ %-11s │ %-11s │ %-11s │ %-11s │ RM%-9.2f │ %-11s │%n",
                v.getId(),
                v.getBrand().length() > 11 ? v.getBrand().substring(0, 11) : v.getBrand(),
                v.getModel().length() > 11 ? v.getModel().substring(0, 11) : v.getModel(),
                    v.getCarPlate().length() > 11 ? v.getCarPlate().substring(0, 11) : v.getCarPlate(),
                v.getVehicleType().toString().length() > 11 ? v.getVehicleType().toString().substring(0, 11) : v.getVehicleType().toString(),
                v.getFuelType().toString().length() > 11 ? v.getFuelType().toString().substring(0, 11) : v.getFuelType().toString(),
                v.getBasePrice(),
                statusDisplay
                );
        }
        System.out.println("└─────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘");
        System.out.println("\nNote: Enter vehicle ID to see detailed availability and view unavailable period for reserved vehicle");
    }

    // Initiate rental process
    private static void initiateRental(RentalSystem system, Scanner scanner, Account account) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                          VEHICLE BOOKING                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        // Display all vehicles with their status
        listAvailableVehicles(system.getVehicles()); // Show all vehicles, not just available ones
        
        int vehicleId;
        while (true) {
            System.out.print("\nEnter vehicle ID to rent (or type 'search' to search, 'exit' to return): ");
            String input = scanner.nextLine();
            
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Returning to main menu...");
                return;
            }
            
            if (input.equalsIgnoreCase("search")) {
                searchInBookingProcess(system, scanner, account);
                continue;
            }
            
            // Try to parse vehicle ID
            try {
                vehicleId = Integer.parseInt(input);
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid vehicle ID, 'search', or 'exit'.");
                continue;
            }
        }
        
        Vehicle selected = system.findVehicleById(vehicleId);
        if (selected == null) {
            System.out.println("Vehicle not found.");
            return;
        }
        
        // Show vehicle details regardless of status
        System.out.println("\n=== Vehicle Details ===\n");
        System.out.println("Vehicle: " + selected.getBrand() + " " + selected.getModel());
        System.out.println("Car Plate: " + selected.getCarPlate());
        System.out.println("Type: " + selected.getVehicleType());
        System.out.println("Fuel Type: " + selected.getFuelType());
        System.out.printf("Base Price: RM%.2f/day\n", selected.getBasePrice());
        System.out.println("Current Status: " + selected.getStatus());

        // Check for existing active rental by the same user for this vehicle
        Rental existingRental = system.findActiveRentalByUserAndVehicle(account.getUsername(), vehicleId);

        if (existingRental != null) {
            System.out.println("\n=== EXISTING RENTAL DETECTED ===\n");
            System.out.printf("You already have an active rental for this vehicle:\n");
            System.out.printf("  Current rental period: %s to %s\n",
                              existingRental.getStartDate(), existingRental.getEndDate());
            System.out.printf("  Total fee: RM%.2f\n", existingRental.getTotalFee());

            System.out.print("\nDo you want to extend this existing rental instead? (y/n): ");
            String extendChoice = scanner.nextLine();

            if (extendChoice.equalsIgnoreCase("y")) {
                System.out.print("How many additional days do you want to extend? ");
                try {
                    int additionalDays = Integer.parseInt(scanner.nextLine());
                    if (additionalDays <= 0) {
                        System.out.println("Extension days must be positive.");
                        System.out.println("\nPress Enter to continue...");
                        scanner.nextLine();
                        return;
                    }

                    LocalDate newEndDate = existingRental.getEndDate().plusDays(additionalDays);

                    // Show extension details
                    System.out.println("\n=== Extension Details ===\n");
                    System.out.printf("Original end date: %s\n", existingRental.getEndDate());
                    System.out.printf("New proposed end date: %s\n", newEndDate);
                    System.out.printf("Additional days: %d\n", additionalDays);

                    // Calculate new total rental days for discount display
                    int totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(existingRental.getStartDate(), newEndDate) + 1;
                    double discount = selected.getDiscountForDays(totalDays);
                    if (discount > 0) {
                        System.out.printf("New total period: %d days\n", totalDays);
                        System.out.printf("Long-term discount: %.1f%% off\n", discount * 100);
                    }

                    // Insurance selection
                    double insuranceRate = selected.getInsuranceRate();
                    System.out.printf("\nInsurance rate: %.1f%%\n", insuranceRate * 100);
                    System.out.print("Include insurance for the extended period? (y/n): ");
                    boolean insurance = scanner.nextLine().equalsIgnoreCase("y");

                    // Extend the rental
                    if (system.extendRental(account.getUsername(), vehicleId, newEndDate, insurance)) {
                        Rental updatedRental = system.findActiveRentalByUserAndVehicle(account.getUsername(), vehicleId);
                        System.out.println("\n=== RENTAL EXTENSION SUCCESSFUL ===\n");
                        System.out.printf("Updated Rental ID: %d\n", updatedRental.getId());
                        System.out.printf("Extended until: %s\n", updatedRental.getEndDate());
                        System.out.printf("Updated total fee: RM%.2f\n", updatedRental.getTotalFee());
                        System.out.println("New ticket generated.");

                        // Display new ticket
                        Ticket newTicket = system.getTicketService().getTicketByRentalId(updatedRental.getId());
                        if (newTicket != null) {
                            System.out.println("\n=== Updated Ticket ===\n");
                            newTicket.displayTicket();
                        }
                    } else {
                        System.out.println("Failed to extend rental. Please try again or contact support.");
                    }

                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    return;

                } catch (NumberFormatException e) {
                    System.out.println("Invalid number format. Please enter a valid number of days.");
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
            return;
                }
            } else {
                System.out.println("You chose not to extend. Proceeding with new booking for this vehicle (if available).");
            }
        }

        // If not extending, or no existing rental, proceed with new booking process
        // Check if vehicle is available for booking
        if ((selected.getStatus() == VehicleStatus.UNDER_MAINTENANCE || selected.getStatus() == VehicleStatus.OUT_OF_SERVICE) || selected.hasCriticalMaintenanceIssues()) {
            System.out.println("\n*** VEHICLE NOT AVAILABLE FOR BOOKING ***\n");
            System.out.println("Reason: Vehicle status is " + selected.getStatus());
            if (selected.hasCriticalMaintenanceIssues()) {
                System.out.println("Additional reason: Vehicle has unresolved maintenance issues.");
            }

            // Still show booking schedule for informational purposes
        List<String> unavailablePeriods = system.getVehicleUnavailablePeriods(vehicleId);
        if (!unavailablePeriods.isEmpty()) {
                System.out.println("\n=== Current Booking Schedule ===\n");
            for (String period : unavailablePeriods) {
                System.out.println("- " + period);
            }
        } else {
                System.out.println("\nNo current bookings found for this vehicle.");
            }

            System.out.println("\nPlease select a different vehicle with AVAILABLE status.");
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
            return;
        }

        // Show booking schedule for available vehicles
        List<String> unavailablePeriods = system.getVehicleUnavailablePeriods(vehicleId);
        if (!unavailablePeriods.isEmpty()) {
            System.out.println("\n=== Vehicle Schedule (Unavailable Periods) ===\n");
            for (String period : unavailablePeriods) {
                System.out.println("- " + period);
            }
        } else {
            System.out.println("\nNo existing bookings found for this vehicle.");
        }

        System.out.print("\nEnter rental start date (yyyy-MM-dd, 'today', or 'exit' to return): ");
        String startStr = scanner.nextLine();

        if (startStr.equalsIgnoreCase("exit")) {
            System.out.println("Returning to main menu...");
            return;
        }
        
        LocalDate startDate;
            if (startStr.equalsIgnoreCase("today")) {
                startDate = LocalDate.now();
            } else {
            try {
                startDate = LocalDate.parse(startStr);
            } catch (Exception e) {
                System.out.println("Invalid date format. Please use yyyy-MM-dd.");
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            return;
            }
        }

        // Show available discounts before asking for rental days
        if (selected.getLongTermDiscounts() != null && !selected.getLongTermDiscounts().isEmpty()) {
            System.out.println("\n=== Available Discounts ===\n");
            System.out.println("Long-term rental discounts:");
            for (Map.Entry<Integer, Double> entry : selected.getLongTermDiscounts().entrySet()) {
                System.out.printf("- %d+ days: %.1f%% off\n", entry.getKey(), entry.getValue() * 100);
            }
            System.out.println("\nChoose your rental duration to see applicable discounts!");
        }
        
        System.out.print("\nEnter number of rental days (or 'exit' to return): ");
        String daysStr = scanner.nextLine();

        if (daysStr.equalsIgnoreCase("exit")) {
            System.out.println("Returning to main menu...");
            return;
        }

        int rentalDays;
        try {
            rentalDays = Integer.parseInt(daysStr);
            if (rentalDays <= 0) {
                System.out.println("Rental days must be positive.");
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format. Please enter a valid number.");
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        LocalDate endDate = startDate.plusDays(rentalDays - 1);
        
        // Check for conflicts
        String conflictDetails = system.getConflictDetails(vehicleId, startDate, endDate);
        if (conflictDetails != null) {
            System.out.println("\n*** BOOKING CONFLICT DETECTED ***\n");
            System.out.println("Conflict Details: " + conflictDetails);
            System.out.println("Vehicle is not available for the selected dates.");
            System.out.println("This period conflicts with existing bookings or maintenance schedules.");
            System.out.println("\nPlease choose different dates or another vehicle.");
            System.out.println("Tip: Check the 'Vehicle Schedule' above to see unavailable periods.");
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        // Show applied discount
        double discount = selected.getDiscountForDays(rentalDays);
        double baseRentalCost = selected.getBasePrice() * rentalDays;
        if (discount > 0) {
            System.out.println("\n=== Discount Applied ===\n");
            System.out.printf("[DISCOUNT] Long-term discount applied: %.1f%% off\n", discount * 100);
            double discountedPrice = baseRentalCost * (1 - discount);
            System.out.printf("Estimated price after discount: RM%.2f (was RM%.2f)\n", discountedPrice, baseRentalCost);
        } else {
            System.out.println("\n=== Pricing ===\n");
            System.out.printf("Base rental cost: RM%.2f\n", baseRentalCost);
        }
        
        // Calculate base rental cost for insurance display
        double insuranceRate = selected.getInsuranceRate();
        double insuranceCost = baseRentalCost * insuranceRate;
        
        System.out.printf("\n=== Insurance Information ===\n");
        System.out.printf("Insurance rate: %.1f%%\n", insuranceRate * 100);
        System.out.printf("Estimated insurance cost: RM%.2f\n", insuranceCost);
        System.out.print("Purchase insurance? (y/n): ");
        String ins = scanner.nextLine();
        boolean insurance = ins.equalsIgnoreCase("y");
        
        // Get customer details with profile confirmation
        System.out.println("\n=== Customer Information ===\n");
        String customerName = account.getFullName();
        String contact = account.getContactNumber();
        
        if (customerName.isEmpty() || contact.isEmpty()) {
            System.out.println("Please complete your profile information:");
            if (customerName.isEmpty()) {
                System.out.print("Enter your full name: ");
                customerName = scanner.nextLine();
                account.setFullName(customerName);
            }
            if (contact.isEmpty()) {
                System.out.print("Enter your contact number: ");
                contact = scanner.nextLine();
                account.setContactNumber(contact);
            }
        } else {
            System.out.println("Current profile information:");
            System.out.println("Full name: " + customerName);
            System.out.println("Contact: " + contact);
            System.out.print("Is this information correct? (y/n): ");
            String confirm = scanner.nextLine();
            
            if (!confirm.equalsIgnoreCase("y")) {
                System.out.print("Enter full name for this booking: ");
                customerName = scanner.nextLine();
                System.out.print("Enter contact number for this booking: ");
                contact = scanner.nextLine();
                System.out.println("Note: This will not update your profile permanently.");
            }
        }
        
        Customer customer = system.findOrCreateCustomer(customerName, contact);
        
        // Final confirmation and booking
        double totalFee = system.calculateRentalFee(selected, startDate, endDate, insurance);
        System.out.println("\n=== Rental Summary ===\n");
        System.out.println("Vehicle: " + selected.getBrand() + " " + selected.getModel());
        System.out.println("Type: " + selected.getVehicleType() + " | Fuel: " + selected.getFuelType());
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);
        System.out.println("Insurance: " + (insurance ? "Included" : "Not included"));
        System.out.printf("Total Fee: RM%.2f\n", totalFee);
        
        System.out.print("\nConfirm booking? (y/n): ");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            try {
                Rental rental = system.createRentalWithSchedule(customer, selected, startDate, endDate, insurance, account.getUsername());
                System.out.println("\n=== BOOKING SUCCESSFUL ===\n");
                System.out.println("Rental ID: " + rental.getId());
                System.out.println("Status: PENDING APPROVAL (Admin will review)");
                System.out.println("You will receive a notification once approved.");
            } catch (IllegalArgumentException e) {
                System.out.println("Booking failed: " + e.getMessage());
            }
        } else {
            System.out.println("Booking cancelled.");
        }
    }

    // View my rental history (enhanced with status grouping)
    private static void viewMyRentalHistoryEnhanced(RentalSystem system, String username) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        MY RENTAL HISTORY                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        List<Rental> myRentals = system.getRentalsByUsername(username);
        if (myRentals.isEmpty()) {
            System.out.println("\nNo rental history found.");
            return;
        }
        
        // Group rentals by status
        List<Rental> pendingRentals = new ArrayList<>();
        List<Rental> activeRentals = new ArrayList<>();
        List<Rental> returnedRentals = new ArrayList<>();
        List<Rental> cancelledRentals = new ArrayList<>();
        
        for (Rental r : myRentals) {
            switch (r.getStatus()) {
                case PENDING:
                    pendingRentals.add(r);
                    break;
                case ACTIVE:
                activeRentals.add(r);
                    break;
                case RETURNED:
                    returnedRentals.add(r);
                    break;
                case CANCELLED:
                    cancelledRentals.add(r);
                    break;
            }
        }
        
        // Display pending rentals
        if (!pendingRentals.isEmpty()) {
            System.out.println("\n[PENDING RENTALS] - Awaiting Admin Approval");
            System.out.println("================================================================");
            for (Rental r : pendingRentals) {
                System.out.println("ID: " + r.getId() + " | Vehicle: " + r.getVehicle().getBrand() + 
                                 " " + r.getVehicle().getModel() + " | Period: " + r.getStartDate() + 
                                 " to " + r.getEndDate() + " | Est. Fee: RM" + String.format("%.2f", r.getTotalFee()));
            }
        }
        
        // Display active rentals
        if (!activeRentals.isEmpty()) {
            System.out.println("\n[ACTIVE RENTALS] - Currently in Use");
            System.out.println("================================================================");
            for (Rental r : activeRentals) {
                System.out.println("ID: " + r.getId() + " | Vehicle: " + r.getVehicle().getBrand() + 
                                 " " + r.getVehicle().getModel() + " | Period: " + r.getStartDate() + 
                                 " to " + r.getEndDate() + " | Est. Fee: RM" + String.format("%.2f", r.getTotalFee()));
            }
        }
        
        // Display returned rentals
        if (!returnedRentals.isEmpty()) {
            System.out.println("\n[COMPLETED RENTALS] - Successfully Returned");
            System.out.println("================================================================");
            for (Rental r : returnedRentals) {
                String feeDisplay = r.getActualFee() > 0 ? 
                    "Final Fee: RM" + String.format("%.2f", r.getActualFee()) : 
                    "Est. Fee: RM" + String.format("%.2f", r.getTotalFee());
                System.out.println("ID: " + r.getId() + " | Vehicle: " + r.getVehicle().getBrand() + 
                                 " " + r.getVehicle().getModel() + " | Period: " + r.getStartDate() + 
                                 " to " + r.getEndDate() + " | " + feeDisplay);
            }
        }
        
        // Display cancelled rentals
        if (!cancelledRentals.isEmpty()) {
            System.out.println("\n[CANCELLED RENTALS]");
            System.out.println("================================================================");
            for (Rental r : cancelledRentals) {
                System.out.println("ID: " + r.getId() + " | Vehicle: " + r.getVehicle().getBrand() + 
                                 " " + r.getVehicle().getModel() + " | Period: " + r.getStartDate() + 
                                 " to " + r.getEndDate() + " | Status: CANCELLED");
            }
        }
        
        System.out.println("\n================================================================");
        System.out.println("Total Rentals: " + myRentals.size() + " | Pending: " + pendingRentals.size() + 
                         " | Active: " + activeRentals.size() + " | Completed: " + returnedRentals.size() + 
                         " | Cancelled: " + cancelledRentals.size());
    }


    // View my rental tickets
    private static void viewMyTickets(RentalSystem system, Scanner scanner, String username) {
        // Get customer name from username
        Account account = system.findAccountByUsername(username);
        if (account == null) {
            System.out.println("Account not found.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        String customerName = account.getFullName();
        if (customerName == null || customerName.isEmpty()) {
            System.out.println("Please complete your profile first to view tickets.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        TicketService ticketService = system.getTicketService();
        List<Ticket> customerTickets = ticketService.getCustomerTickets(customerName);
        
        if (customerTickets.isEmpty()) {
            System.out.println("\n=== No Tickets Found ===");
            System.out.println("You don't have any rental tickets yet.");
            System.out.println("Tickets are generated when admin approves your rental requests.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        System.out.println("\n=== My Rental Tickets ===");
        System.out.println("Total tickets: " + customerTickets.size());
        
        // Display tickets summary
        for (int i = 0; i < customerTickets.size(); i++) {
            Ticket ticket = customerTickets.get(i);
            System.out.println("\n" + (i + 1) + ". " + (ticket.isUsed() ? "[USED]" : "[VALID]"));
            ticket.displayCompact();
        }
        
        // Ask if user wants to view detailed ticket
        System.out.print("\nEnter ticket number to view details (or 0 to return): ");
        try {
            int choice = Integer.parseInt(scanner.nextLine());
            if (choice > 0 && choice <= customerTickets.size()) {
                Ticket selectedTicket = customerTickets.get(choice - 1);
                selectedTicket.displayTicket();
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
        }
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    // Cancel booking
    private static void cancelBooking(RentalSystem system, Scanner scanner, String username) {
        System.out.println("\n=== Cancel Booking ===");
        List<Rental> myRentals = system.getRentalsByUsername(username);
        List<Rental> pendingRentals = new ArrayList<>();
        
        for (Rental r : myRentals) {
            if (r.getStatus() == RentalStatus.PENDING) {
                pendingRentals.add(r);
            }
        }
        
        if (pendingRentals.isEmpty()) {
            System.out.println("No pending bookings to cancel.");
            return;
        }
        
        System.out.println("Pending bookings:");
        for (Rental r : pendingRentals) {
            System.out.println("ID: " + r.getId() + ", Vehicle: " + r.getVehicle().getBrand() + 
                             " " + r.getVehicle().getModel() + ", Dates: " + r.getStartDate() + 
                             " to " + r.getEndDate());
        }
        
        System.out.print("Enter rental ID to cancel: ");
        String idStr = scanner.nextLine();
        try {
            int rentalId = Integer.parseInt(idStr);
            if (system.cancelRental(rentalId)) {
                System.out.println("Booking cancelled successfully.");
            } else {
                System.out.println("Unable to cancel booking.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid rental ID format.");
        }
    }

    // Request vehicle return
    private static void requestReturn(RentalSystem system, Scanner scanner, String username) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                     REQUEST VEHICLE RETURN                       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        List<Rental> myRentals = system.getRentalsByUsername(username);
        List<Rental> activeRentals = new ArrayList<>();
        
        for (Rental r : myRentals) {
            if (r.getStatus() == RentalStatus.ACTIVE) {
                activeRentals.add(r);
            }
        }
        
        if (activeRentals.isEmpty()) {
            System.out.println("No active rentals to return.");
            return;
        }
        
        System.out.println("\nActive rentals:");
        System.out.println("┌─────┬──────────────────┬─────────────┬─────────────┐");
        System.out.println("│ ID  │     Vehicle      │ Car Plate   │  End Date   │");
        System.out.println("├─────┼──────────────────┼─────────────┼─────────────┤");
        for (Rental r : activeRentals) {
            String vehicleName = String.format("%s %s", r.getVehicle().getBrand(), r.getVehicle().getModel());
            if (vehicleName.length() > 16) {
                vehicleName = vehicleName.substring(0, 13) + "...";
            }
            System.out.printf("│ %-3d │ %-16s │ %-11s │ %-11s │\n",
                             r.getId(), vehicleName, r.getVehicle().getCarPlate(), r.getEndDate());
        }
        System.out.println("└─────┴──────────────────┴─────────────┴─────────────┘");
        
        System.out.print("\nEnter rental ID to return: ");
        String idStr = scanner.nextLine();
        try {
            int rentalId = Integer.parseInt(idStr);
            Rental rental = system.findRentalById(rentalId);
            
            if (rental == null || rental.getStatus() != RentalStatus.ACTIVE) {
                System.out.println("Invalid rental ID or rental not active.");
                return;
            }
            
            // Check if vehicle has been picked up (ticket used)
            TicketService ticketService = system.getTicketService();
            Ticket ticket = ticketService.getTicketByRentalId(rentalId);
            
            if (ticket == null || !ticket.isUsed()) {
                System.out.println("\n ERROR: Vehicle has not been picked up yet!");
                System.out.println("You must pick up the vehicle before you can return it.");
                if (ticket != null) {
                    System.out.println("Please use your ticket: " + ticket.getTicketId());
                    System.out.println("Go to admin to validate your ticket for pickup first.");
                }
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
                return;
            }
            
            // 增强的车辆状况报告流程
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                   VEHICLE CONDITION REPORT                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            
            Vehicle vehicle = rental.getVehicle();
            System.out.printf("Vehicle: %s %s (Car Plate: %s)\n", 
                             vehicle.getBrand(), vehicle.getModel(), vehicle.getCarPlate());
            
            System.out.println("\nPlease report any issues or damage with the vehicle:");
            System.out.print("Any issues to report? (y/n): ");
            String hasIssues = scanner.nextLine();
            
            List<IssueReport> issueReports = new ArrayList<>();
            if (hasIssues.equalsIgnoreCase("y")) {
                System.out.println("\nPlease describe each issue (type 'done' when finished):");
                int issueNum = 1;
                
                while (true) {
                    System.out.printf("\nIssue #%d description: ", issueNum);
                    String description = scanner.nextLine().trim();
                    
                    if (description.equalsIgnoreCase("done")) {
                        if (issueReports.isEmpty()) {
                            System.out.println("No issues reported. Vehicle will be returned without maintenance records.");
                        }
                        break;
                    }
                    
                    if (description.isEmpty()) {
                        System.out.println("Issue description cannot be empty. Please try again.");
                        continue;
                    }
                    
                    System.out.println("\nIssue Type:");
                    System.out.println("1. DAMAGE_REPORT (physical damage)");
                    System.out.println("2. REPAIR (mechanical/functional issues)");
                    System.out.println("3. CLEANING (cleanliness issues)");
                    System.out.print("Choose type (1-3, default: 1): ");
                    
                    MaintenanceLogType logType = MaintenanceLogType.DAMAGE_REPORT;
                    String typeChoice = scanner.nextLine().trim();
                    switch (typeChoice) {
                        case "2": logType = MaintenanceLogType.REPAIR; break;
                        case "3": logType = MaintenanceLogType.CLEANING; break;
                        default: logType = MaintenanceLogType.DAMAGE_REPORT; break;
                    }
                    
                    System.out.println("\nSeverity Assessment:");
                    System.out.println("Please assess the severity of this issue.");
                    int severity = getSeverityLevelWithGuidance(scanner);
                    
                    issueReports.add(new IssueReport(description, logType, severity));
                    System.out.printf(" Issue #%d recorded: %s (Severity: %d)\n", issueNum, description, severity);
                    issueNum++;
                }
            }
            
            // 处理还车
            System.out.println("\n=== Processing Vehicle Return ===");
            
            // 为每个问题创建维护记录
            for (IssueReport issue : issueReports) {
                system.addMaintenanceLog(vehicle.getId(), issue.logType, issue.description, 
                                       rental.getCustomer().getName(), issue.severity);
                
                // 高严重程度通知由RentalSystem自动发送
            }
            
            // 执行返还（包含损坏检查）
            List<String> damageDescriptions = new ArrayList<>();
            for (IssueReport issue : issueReports) {
                damageDescriptions.add(issue.description);
            }
            
            if (system.returnVehicleWithDamageCheck(rentalId, rental.getCustomer().getName(), damageDescriptions)) {
                System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
                System.out.println("║                      RETURN SUMMARY                             ║");
                System.out.println("╚══════════════════════════════════════════════════════════════════╝");
                System.out.println(" Vehicle returned successfully.");
                System.out.printf("Vehicle: %s %s\n", vehicle.getBrand(), vehicle.getModel());
                System.out.printf("Final Fee: RM%.2f\n", rental.getActualFee());
                
                if (!issueReports.isEmpty()) {
                    System.out.printf("Issues reported: %d\n", issueReports.size());
                    
                    // 检查是否有严重问题导致车辆进入维修模式
                    boolean hasCriticalIssues = issueReports.stream().anyMatch(issue -> issue.severity >= 3);
                    if (hasCriticalIssues) {
                        System.out.println(" Vehicle automatically set to UNDER_MAINTENANCE due to reported issues.");
                        System.out.println(" Maintenance notifications sent to administrators.");
                    }
                    
                    // 显示问题摘要
                    System.out.println("\nReported Issues:");
                    for (int i = 0; i < issueReports.size(); i++) {
                        IssueReport issue = issueReports.get(i);
                        System.out.printf("  %d. %s (Severity: %d)\n", i+1, issue.description, issue.severity);
                }
            } else {
                    System.out.println("No issues reported - vehicle is ready for next rental.");
                }
                
                System.out.println("\nThank you for using our service!");
            } else {
                System.out.println(" Unable to process vehicle return.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid rental ID format.");
        }

    }

    /**
     * 内部类：问题报告
     */
    private static class IssueReport {
        final String description;
        final MaintenanceLogType logType;
        final int severity;
        
        IssueReport(String description, MaintenanceLogType logType, int severity) {
            this.description = description;
            this.logType = logType;
            this.severity = severity;
        }
    }

    // Admin functions
    private static void viewAllVehicles(List<Vehicle> vehicles) {
        System.out.println("\n=== All Vehicles ===");
        if (vehicles.isEmpty()) {
            System.out.println("No vehicles found.");
            return;
        }
        
        System.out.println("┌─────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────────────────┐");
        System.out.println("│ ID  │ Brand       │ Model       │ Car Plate   │ Type        │ Fuel        │ Status      │ Price/Day   │ Discounts               │");
        System.out.println("├─────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────────────────┤");
        
        for (Vehicle v : vehicles) {
            String discountInfo = "";
            if (v.getLongTermDiscounts() != null && !v.getLongTermDiscounts().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<Integer, Double> entry : v.getLongTermDiscounts().entrySet()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(String.format("%d+:%.0f%%", entry.getKey(), entry.getValue() * 100));
                }
                discountInfo = sb.toString();
                if (discountInfo.length() > 23) {
                    discountInfo = discountInfo.substring(0, 20) + "...";
                }
            } else {
                discountInfo = "None";
            }
            
            // 根据状态添加图标提示，但确保长度不超过11字符
            String statusDisplay = v.getStatus().toString();
            if (v.getStatus() == VehicleStatus.AVAILABLE) {
                statusDisplay = "[OK] AVAIL";
            } else if (v.getStatus() == VehicleStatus.UNDER_MAINTENANCE) {
                statusDisplay = "[!] MAINT";
            } else if (v.getStatus() == VehicleStatus.RENTED) {
                statusDisplay = "RENTED";
            } else if (v.getStatus() == VehicleStatus.RESERVED) {
                statusDisplay = "RESERVED";
            } else if (v.getStatus() == VehicleStatus.OUT_OF_SERVICE) {
                statusDisplay = "OUT_SERVICE";
            }
            
            System.out.printf("│ %-3d │ %-11s │ %-11s │ %-11s │ %-11s │ %-11s │ %-11s │ RM%-9.2f │ %-23s │%n",
                v.getId(),
                v.getBrand().length() > 11 ? v.getBrand().substring(0, 11) : v.getBrand(),
                v.getModel().length() > 11 ? v.getModel().substring(0, 11) : v.getModel(),
                v.getCarPlate().length() > 11 ? v.getCarPlate().substring(0, 11) : v.getCarPlate(),
                v.getVehicleType().toString().length() > 11 ? v.getVehicleType().toString().substring(0, 11) : v.getVehicleType().toString(),
                v.getFuelType().toString().length() > 11 ? v.getFuelType().toString().substring(0, 11) : v.getFuelType().toString(),
                statusDisplay,
                v.getBasePrice(),
                discountInfo
            );
        }
        System.out.println("└─────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────────────────┘");
        
        System.out.println("\nStatus Legend:");
        System.out.println("   [OK] AVAIL = Available for booking");
        System.out.println("   [!] MAINT = Under maintenance");
        System.out.println("   RENTED = Currently rented");
        System.out.println("   RESERVED = Reserved for booking");
        System.out.println("   OUT_SERVICE = Out of service");
    }

    private static void viewAllRentals(List<Rental> rentals) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                           ALL RENTALS                           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        if (rentals.isEmpty()) {
            System.out.println("\nNo rentals found.");
            return;
        }
        
        System.out.println("\n┌──────┬─────────────────────┬──────────────────┬────────────┬──────────────┬──────────────┬─────────────┐");
        System.out.println("│  ID  │      Customer       │     Vehicle      │   Status   │ Start Date   │  End Date    │    Fee      │");
        System.out.println("├──────┼─────────────────────┼──────────────────┼────────────┼──────────────┼──────────────┼─────────────┤");
        
        for (Rental r : rentals) {
            String customerName = r.getCustomer().getName();
            if (customerName.length() > 19) customerName = customerName.substring(0, 16) + "...";
            
            String vehicleInfo = r.getVehicle().getBrand() + " " + r.getVehicle().getModel();
            if (vehicleInfo.length() > 16) vehicleInfo = vehicleInfo.substring(0, 13) + "...";
            
            String statusText = r.getStatus().toString();
            if (statusText.length() > 10) statusText = statusText.substring(0, 7) + "...";
            
            String feeDisplay;
            if (r.getStatus() == RentalStatus.RETURNED && r.getActualFee() > 0) {
                feeDisplay = String.format("RM%.2f", r.getActualFee());
            } else {
                feeDisplay = String.format("RM%.2f", r.getTotalFee());
            }
            
            System.out.printf("│ %-4d │ %-19s │ %-16s │ %-10s │ %-12s │ %-12s │ %-11s │%n",
                r.getId(),
                customerName,
                vehicleInfo,
                statusText,
                r.getStartDate().toString(),
                r.getEndDate().toString(),
                feeDisplay
            );
        }
        
        System.out.println("└──────┴─────────────────────┴──────────────────┴────────────┴──────────────┴──────────────┴─────────────┘");
        System.out.println("\nTotal Rentals: " + rentals.size());
    }

    private static void viewPendingRentals(List<Rental> pendingRentals) {
        System.out.println("\n=== Pending Rentals ===");
        if (pendingRentals.isEmpty()) {
            System.out.println("No pending rentals.");
        } else {
            for (Rental r : pendingRentals) {
                System.out.println("ID: " + r.getId() + ", Customer: " + r.getCustomer().getName() + 
                                 ", Vehicle: " + r.getVehicle().getBrand() + " " + r.getVehicle().getModel() + 
                                 ", Dates: " + r.getStartDate() + " to " + r.getEndDate() + 
                                 ", Estimated Fee: RM" + String.format("%.2f", r.getTotalFee()));
            }
        }
    }

    private static void approveRental(RentalSystem system, Scanner scanner) {
        System.out.println("\n=== Approve Rental ===");
        viewPendingRentals(system.getPendingRentals());
        
        System.out.print("Enter rental ID to approve: ");
        String idStr = scanner.nextLine();
        try {
            int rentalId = Integer.parseInt(idStr);
            if (system.approveRental(rentalId)) {
                System.out.println("Rental approved successfully.");
            } else {
                System.out.println("Unable to approve rental.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid rental ID format.");
        }
    }



    private static void confirmReturn(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                  ADMIN: CONFIRM VEHICLE RETURN                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        List<Rental> activeRentals = new ArrayList<>();
        for (Rental r : system.getRentals()) {
            if (r.getStatus() == RentalStatus.ACTIVE) {
                activeRentals.add(r);
            }
        }
        
        if (activeRentals.isEmpty()) {
            System.out.println("No active rentals to return.");
            return;
        }
        
        System.out.println("\nActive rentals:");
        System.out.println("┌─────┬──────────────────┬─────────────┬─────────────┬─────────────┐");
        System.out.println("│ ID  │    Customer      │   Vehicle   │ Car Plate   │  End Date   │");
        System.out.println("├─────┼──────────────────┼─────────────┼─────────────┼─────────────┤");
        for (Rental r : activeRentals) {
            String customerName = r.getCustomer().getName();
            if (customerName.length() > 16) {
                customerName = customerName.substring(0, 13) + "...";
            }
            String vehicleName = String.format("%s %s", r.getVehicle().getBrand(), r.getVehicle().getModel());
            if (vehicleName.length() > 11) {
                vehicleName = vehicleName.substring(0, 8) + "...";
            }
            System.out.printf("│ %-3d │ %-16s │ %-11s │ %-11s │ %-11s │\n",
                             r.getId(), customerName, vehicleName, r.getVehicle().getCarPlate(), r.getEndDate());
        }
        System.out.println("└─────┴──────────────────┴─────────────┴─────────────┴─────────────┘");
        
        System.out.print("\nEnter rental ID to confirm return: ");
        String idStr = scanner.nextLine();
        try {
            int rentalId = Integer.parseInt(idStr);
            Rental rental = system.findRentalById(rentalId);
            
            if (rental == null || rental.getStatus() != RentalStatus.ACTIVE) {
                System.out.println("Invalid rental ID or rental not active.");
                return;
            }
            
            // Check if vehicle has been picked up (ticket used)
            TicketService ticketService = system.getTicketService();
            Ticket ticket = ticketService.getTicketByRentalId(rentalId);
            
            if (ticket == null || !ticket.isUsed()) {
                System.out.println("\n ERROR: Vehicle has not been picked up yet!");
                System.out.println("Customer must pick up the vehicle before it can be returned.");
                if (ticket != null) {
                    System.out.println("Customer ticket: " + ticket.getTicketId());
                    System.out.println("Please validate the ticket for pickup first.");
                }
                return;
            }
            
            // Enhanced vehicle condition report process
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                   VEHICLE CONDITION REPORT                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            
            Vehicle vehicle = rental.getVehicle();
            System.out.printf("Vehicle: %s %s (Car Plate: %s)\n", 
                             vehicle.getBrand(), vehicle.getModel(), vehicle.getCarPlate());
            System.out.printf("Customer: %s\n", rental.getCustomer().getName());
            
            System.out.println("\nInspect the vehicle and report any issues or damage:");
            System.out.print("Any issues to report? (y/n): ");
            String hasIssues = scanner.nextLine();
            
            List<IssueReport> issueReports = new ArrayList<>();
            if (hasIssues.equalsIgnoreCase("y")) {
                System.out.println("\nPlease describe each issue (type 'done' when finished):");
                
                int issueCounter = 1;
                while (true) {
                    System.out.printf("\nIssue #%d description: ", issueCounter);
                    String description = scanner.nextLine().trim();
                    
                    if (description.equalsIgnoreCase("done")) {
                        break;
                    }
                    
                    if (description.isEmpty()) {
                        System.out.println("Description cannot be empty. Please try again.");
                        continue;
                    }
                    
                    // Issue type selection
                    System.out.println("\nIssue Type:");
                    System.out.println("1. DAMAGE_REPORT (physical damage)");
                    System.out.println("2. REPAIR (mechanical/functional issues)");
                    System.out.println("3. CLEANING (cleanliness issues)");
                    System.out.print("Choose type (1-3, default: 1): ");
                    String typeChoice = scanner.nextLine();
                    
                    MaintenanceLogType logType = MaintenanceLogType.DAMAGE_REPORT;
                    switch (typeChoice) {
                        case "2":
                            logType = MaintenanceLogType.REPAIR;
                            break;
                        case "3":
                            logType = MaintenanceLogType.CLEANING;
                            break;
                        default:
                            logType = MaintenanceLogType.DAMAGE_REPORT;
                    }
                    
                    // Severity assessment
                    System.out.println("\nSeverity Assessment:");
                    System.out.println("Please assess the severity of this issue.");
                    System.out.println("\nSeverity Level Help:");
                    System.out.println("1. Type 'help' to see detailed guidance");
                    System.out.println("2. Enter level directly (1-5)");
                    
                    int severity = 3; // default
                    System.out.print("Enter severity level (1-5) or 'help': ");
                    String severityInput = scanner.nextLine();
                    
                    if (severityInput.equalsIgnoreCase("help")) {
                        displaySeverityGuidance();
                        System.out.print("Enter severity level (1-5): ");
                        severityInput = scanner.nextLine();
                    }
                    
                    try {
                        severity = Integer.parseInt(severityInput);
                        if (severity < 1 || severity > 5) {
                            System.out.println("Invalid severity. Using default level 3.");
                            severity = 3;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Using default severity level 3.");
                        severity = 3;
                    }
                    
                    System.out.printf(" Issue #%d recorded: %s (Severity: %d)\n", issueCounter, description, severity);
                    
                    issueReports.add(new IssueReport(description, logType, severity));
                    issueCounter++;
                }
            }
            
            // Process return with damage reports
            System.out.println("\n=== Processing Vehicle Return ===");
            
            // Submit maintenance logs
            for (IssueReport issue : issueReports) {
                system.addMaintenanceLog(vehicle.getId(), issue.logType, issue.description, 
                                       rental.getCustomer().getName(), issue.severity);
            }
            
            // Execute return (including damage check)
            List<String> damageDescriptions = new ArrayList<>();
            for (IssueReport issue : issueReports) {
                damageDescriptions.add(issue.description);
            }
            
            if (system.returnVehicleWithDamageCheck(rentalId, rental.getCustomer().getName(), damageDescriptions)) {
                System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
                System.out.println("║                      RETURN SUMMARY                              ║");
                System.out.println("╚══════════════════════════════════════════════════════════════════╝");
                System.out.println(" Vehicle return confirmed successfully.");
                System.out.printf("Vehicle: %s %s\n", vehicle.getBrand(), vehicle.getModel());
                System.out.printf("Customer: %s\n", rental.getCustomer().getName());
                System.out.printf("Final Fee: RM%.2f\n", rental.getActualFee());
                
                if (!issueReports.isEmpty()) {
                    System.out.printf("Issues reported: %d\n", issueReports.size());
                    
                    // Check if vehicle was set to maintenance due to critical issues
                    boolean hasCriticalIssues = issueReports.stream().anyMatch(issue -> issue.severity >= 3);
                    if (hasCriticalIssues) {
                        System.out.println(" Vehicle automatically set to UNDER_MAINTENANCE due to reported issues.");
                        System.out.println(" Maintenance notifications sent to administrators.");
                    }
                    
                    // Display issue summary
                    System.out.println("\nReported Issues:");
                    for (int i = 0; i < issueReports.size(); i++) {
                        IssueReport issue = issueReports.get(i);
                        System.out.printf("  %d. %s (Severity: %d)\n", i+1, issue.description, issue.severity);
                    }
            } else {
                    System.out.println("No issues reported - vehicle is ready for next rental.");
                }
                
                System.out.println("\nReturn process completed.");
            } else {
                System.out.println(" Unable to process vehicle return.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid rental ID format.");
        }
    }

    private static void addNewVehicle(RentalSystem system, Scanner scanner) {
        System.out.println("\n=== Add New Vehicle ===");
        System.out.print("Enter vehicle ID: ");
        int id = Integer.parseInt(scanner.nextLine());
        
        System.out.print("Enter vehicle model: ");
        String model = scanner.nextLine();
        
        System.out.print("Enter base price per day: ");
        double basePrice = Double.parseDouble(scanner.nextLine());
        
        // 创建新车辆并添加到系统
        Vehicle newVehicle = new Vehicle(id, "Default", model, VehicleStatus.AVAILABLE, 0.1, basePrice, null);
        system.getVehicles().add(newVehicle);
        
        System.out.println("Vehicle added successfully!");
        System.out.println("ID: " + id + ", Model: " + model + ", Price: RM" + basePrice + "/day");
    }

    private static void viewStatistics(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      SYSTEM STATISTICS                           ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        
        List<Rental> rentals = system.getRentals();
        List<Vehicle> vehicles = system.getVehicles();
        
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
                    // 取消的租赁不计入统计
                    break;
            }
        }
        
        int availableVehicles = 0;
        int maintenanceVehicles = 0;
        int rentedVehicles = 0;
        
        for (Vehicle vehicle : vehicles) {
            switch (vehicle.getStatus()) {
                case AVAILABLE:
                    availableVehicles++;
                    break;
                case UNDER_MAINTENANCE:
                    maintenanceVehicles++;
                    break;
                case RENTED:
                    rentedVehicles++;
                    break;
                case RESERVED:
                    // 预留车辆计入可用车辆
                    availableVehicles++;
                    break;
                case OUT_OF_SERVICE:
                    // 停用车辆不计入任何统计
                    break;
            }
        }
        
        System.out.printf("║ Total Vehicles:        │ %-38d ║%n", vehicles.size());
        System.out.printf("║ Available Vehicles:    │ %-38d ║%n", availableVehicles);
        System.out.printf("║ Rented Vehicles:       │ %-38d ║%n", rentedVehicles);
        System.out.printf("║ Maintenance Vehicles:  │ %-38d ║%n", maintenanceVehicles);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Total Rentals:         │ %-38d ║%n", totalRentals);
        System.out.printf("║ Active Rentals:        │ %-38d ║%n", activeRentals);
        System.out.printf("║ Completed Rentals:     │ %-38d ║%n", completedRentals);
        System.out.printf("║ Pending Rentals:       │ %-38d ║%n", pendingRentals);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Total Revenue:         │ RM%-38.2f ║%n", totalRevenue);
        System.out.printf("║ Average Revenue/Rental:│ RM%-38.2f ║%n", 
                         completedRentals > 0 ? totalRevenue / completedRentals : 0.0);
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        // Prepare data for export
        List<String> headers = Arrays.asList("Metric", "Value");
        List<List<String>> data = Arrays.asList(
            Arrays.asList("Total Vehicles", String.valueOf(vehicles.size())),
            Arrays.asList("Available Vehicles", String.valueOf(availableVehicles)),
            Arrays.asList("Rented Vehicles", String.valueOf(rentedVehicles)),
            Arrays.asList("Maintenance Vehicles", String.valueOf(maintenanceVehicles)),
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




    
    // 消息中心
    private static void messageCenter(RentalSystem system, Scanner scanner, String username) {
        while (true) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                         MESSAGE CENTER                           ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. View All Messages                                             ║");
            System.out.println("║ 2. View Unread Messages                                          ║");
            System.out.println("║ 3. Send Message                                                  ║");
            System.out.println("║ 4. Delete Message                                                ║");
            System.out.println("║ 5. Message Statistics                                            ║");
            System.out.println("║ 0. Back to Main Menu                                             ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    viewAllMessages(system, username);
                    break;
                case "2":
                    viewUnreadMessages(system, username);
                    break;
                case "3":
                    sendMessage(system, scanner, username);
                    break;
                case "4":
                    deleteMessage(system, scanner, username);
                    break;
                case "5":
                    viewMessageStatistics(system, username);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    // 查看所有消息
    private static void viewAllMessages(RentalSystem system, String username) {
        List<Message> messages = system.getUserMessages(username);
        if (messages.isEmpty()) {
            System.out.println("No messages found.");
            return;
        }
        
        System.out.println("\n=== All Messages ===");
        for (Message message : messages) {
            System.out.println("ID: " + message.getId());
            System.out.println("From: " + message.getSender());
            System.out.println("To: " + message.getRecipient());
            System.out.println("Subject: " + message.getSubject());
            System.out.println("Content: " + message.getContent());
            System.out.println("Type: " + message.getType());
            System.out.println("Time: " + message.getTimestamp());
            System.out.println("Status: " + (message.isRead() ? "Read" : "Unread"));
            System.out.println("---");
        }
    }
    
    // 查看未读消息
    private static void viewUnreadMessages(RentalSystem system, String username) {
        List<Message> unreadMessages = system.getNotificationService().getUnreadMessages(username);
        if (unreadMessages.isEmpty()) {
            System.out.println("No unread messages.");
            return;
        }
        
        System.out.println("\n=== Unread Messages ===");
        for (Message message : unreadMessages) {
            System.out.println("ID: " + message.getId());
            System.out.println("From: " + message.getSender());
            System.out.println("Subject: " + message.getSubject());
            System.out.println("Content: " + message.getContent());
            System.out.println("Time: " + message.getTimestamp());
            System.out.println("---");
        }
    }
    
    // 发送消息
    private static void sendMessage(RentalSystem system, Scanner scanner, String fromUser) {
        System.out.print("Enter recipient username: ");
        String toUser = scanner.nextLine();
        
        System.out.print("Enter subject: ");
        String subject = scanner.nextLine();
        
        System.out.print("Enter message content: ");
        String content = scanner.nextLine();
        
        if (system.sendUserMessage(fromUser, toUser, subject, content)) {
            System.out.println("Message sent successfully!");
        } else {
            System.out.println("Failed to send message.");
        }
    }
    
    // 标记消息为已读
    // 删除消息
    private static void deleteMessage(RentalSystem system, Scanner scanner, String username) {
        viewAllMessages(system, username);
        System.out.print("Enter message ID to delete: ");
        String messageId = scanner.nextLine();
        
        if (system.getNotificationService().deleteMessage(messageId)) {
            System.out.println("Message deleted successfully.");
        } else {
            System.out.println("Failed to delete message.");
        }
    }
    
    // 查看消息统计
    private static void viewMessageStatistics(RentalSystem system, String username) {
        Map<String, Integer> stats = system.getNotificationService().getMessageStats(username);
        
        System.out.println("\n=== Message Statistics ===");
        System.out.println("Total messages: " + stats.get("total"));
        System.out.println("Unread messages: " + stats.get("unread"));
        System.out.println("Sent messages: " + stats.get("sent"));
        System.out.println("Received messages: " + stats.get("received"));
    }
    
    // 租赁管理（合并审批和拒绝功能）
    private static void rentalManagement(RentalSystem system, Scanner scanner) {
        while (true) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                       RENTAL MANAGEMENT                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            
            List<Rental> pendingRentals = system.getPendingRentals();
            if (pendingRentals.isEmpty()) {
                System.out.println("\nNo pending rentals.");
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
                return;
            }
            
            System.out.println("\nPending Rentals:");
            System.out.println("┌─────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐");
            System.out.println("│ ID  │ User        │ Vehicle     │ Start Date  │ End Date    │ Fee         │");
            System.out.println("├─────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤");
            
            for (Rental rental : pendingRentals) {
                System.out.printf("│ %-3d │ %-11s │ %-11s │ %-11s │ %-11s │ RM%-10.2f │%n",
                    rental.getId(),
                    rental.getUsername() != null ? 
                        (rental.getUsername().length() > 11 ? rental.getUsername().substring(0, 11) : rental.getUsername()) : "N/A",
                    rental.getVehicle().getModel().length() > 11 ? 
                        rental.getVehicle().getModel().substring(0, 11) : rental.getVehicle().getModel(),
                    rental.getStartDate().toString(),
                    rental.getEndDate().toString(),
                    rental.getTotalFee()
                );
            }
            System.out.println("└─────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘");
            
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                           OPTIONS                                ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. Approve Rental                                                ║");
            System.out.println("║ 2. Reject Rental                                                 ║");
            System.out.println("║ 0. Back to Main Menu                                             ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    approveRental(system, scanner);
                    break;
                case "2":
                    rejectRentalWithReason(system, scanner);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    // 拒绝租赁（带原因）
    private static void rejectRentalWithReason(RentalSystem system, Scanner scanner) {
        System.out.print("Enter rental ID to reject: ");
        int rentalId = Integer.parseInt(scanner.nextLine());
        
        System.out.print("Enter rejection reason: ");
        String reason = scanner.nextLine();
        
        if (system.cancelRental(rentalId, reason)) {
            System.out.println("Rental rejected successfully with reason: " + reason);
        } else {
            System.out.println("Failed to reject rental. Please check the rental ID.");
        }
    }
    
    // 报告和分析（合并功能）
    private static void reportsAndAnalytics(RentalSystem system, Scanner scanner) {
        while (true) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                      REPORTS & ANALYTICS                         ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. View Statistics                                               ║");
            System.out.println("║ 2. Generate Detailed Report                                      ║");
            System.out.println("║ 0. Back to Main Menu                                             ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    viewStatistics(system, scanner);
                    break;
                case "2":
                    generateDetailedReport(system, scanner);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    // 生成详细报告
    private static void generateDetailedReport(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    DETAILED REPORT OPTIONS                       ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 1. Monthly Rental Statistics                                     ║");
        System.out.println("║ 2. Popular Vehicle Report                                        ║");
        System.out.println("║ 3. Customer Activity Report                                      ║");
        System.out.println("║ 4. Rental History Export                                         ║");
        System.out.println("║ 0. Back to Reports Menu                                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.print("Select report type: ");
        
        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                ReportService.generateMonthlyReport(system.getRentals(), scanner);
                break;
            case "2":
                ReportService.generatePopularVehicleReport(system.getRentals(), scanner);
                break;
            case "3":
                generateCustomerActivityReport(system, scanner);
                break;
            case "4":
                exportRentalHistory(system, scanner);
                break;
            case "0":
                return;
            default:
                System.out.println("Invalid option. Please try again.");
        }
    }
    
    // 导出租赁历史
    private static void exportRentalHistory(RentalSystem system, Scanner scanner) {
        List<Rental> rentals = system.getRentals();
        
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      RENTAL HISTORY EXPORT                       ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        
        // Prepare data for export
        List<String> headers = Arrays.asList("Rental ID", "Customer", "Vehicle", "Start Date", 
                                           "End Date", "Status", "Total Fee", "Insurance");
        List<List<String>> data = new ArrayList<>();
        
        for (Rental rental : rentals) {
            List<String> row = Arrays.asList(
                String.valueOf(rental.getId()),
                rental.getCustomer().getName(),
                rental.getVehicle().getBrand() + " " + rental.getVehicle().getModel(),
                rental.getStartDate().toString(),
                rental.getEndDate().toString(),
                rental.getStatus().toString(),
                String.format("%.2f", rental.getTotalFee()),
                rental.isInsuranceSelected() ? "Yes" : "No"
            );
            data.add(row);
        }
        
        // Display summary
        System.out.printf("║ Total Rentals: %-48d ║%n", rentals.size());
        System.out.printf("║ Data prepared for export with %d records                     ║%n", data.size());
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        // Export options
        ReportExportService exportService = new ReportExportService();
        exportService.promptForExport(scanner, "Rental History Export", headers, data, "rental_history");
    }
    
    private static void generateCustomerActivityReport(RentalSystem system, Scanner scanner) {
        List<Rental> rentals = system.getRentals();
        Map<String, Integer> customerRentals = new HashMap<>();
        Map<String, Double> customerRevenue = new HashMap<>();
        
        for (Rental rental : rentals) {
            if (rental.getStatus() == RentalStatus.RETURNED) {
                String customerKey = rental.getCustomer().getName();
                customerRentals.put(customerKey, customerRentals.getOrDefault(customerKey, 0) + 1);
                customerRevenue.put(customerKey, customerRevenue.getOrDefault(customerKey, 0.0) + rental.getTotalFee());
            }
        }
        
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                   CUSTOMER ACTIVITY REPORT                       ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Customer Name          │ Rentals │ Revenue      │ Avg Revenue    ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        
        List<String> headers = Arrays.asList("Customer Name", "Rentals", "Revenue", "Avg Revenue");
        List<List<String>> data = new ArrayList<>();
        
        // Sort by rental count (descending)
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(customerRentals.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            String customer = entry.getKey();
            int count = entry.getValue();
            double revenue = customerRevenue.getOrDefault(customer, 0.0);
            double avgRevenue = count > 0 ? revenue / count : 0.0;
            
            String displayCustomer = customer.length() > 22 ? customer.substring(0, 19) + "..." : customer;
            System.out.printf("║ %-22s │ %-7d │ RM%-10.2f │ RM%-12.2f  ║%n", 
                            displayCustomer, count, revenue, avgRevenue);
            
            data.add(Arrays.asList(customer, String.valueOf(count), 
                                 String.format("%.2f", revenue), 
                                 String.format("%.2f", avgRevenue)));
        }
        
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        // Offer export option
        ReportExportService exportService = new ReportExportService();
        exportService.promptForExport(scanner, "Customer Activity Report", headers, data, "customer_activity_report");
    }
    


    // 维护管理
    private static void maintenanceManagement(RentalSystem system, Scanner scanner) {
        while (true) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                    MAINTENANCE MANAGEMENT                        ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. Quick Status Overview                                         ║");
            System.out.println("║ 2. Report New Issue                                              ║");
            System.out.println("║ 3. Fix Completed Issues                                          ║");
            System.out.println("║ 4. View Vehicle History                                          ║");
            System.out.println("║ 5. Manage Vehicle Status                                         ║");
            System.out.println("║ 0. Return to Main Menu                                           ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Choose option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    maintenanceStatusOverview(system, scanner);
                    break;
                case "2":
                    addMaintenanceRecord(system, scanner, "admin");
                    break;
                case "3":
                    resolveMaintenanceIssue(system, scanner, "admin");
                    break;
                case "4":
                    viewVehicleMaintenanceHistory(system, scanner);
                    break;
                case "5":
                    manageVehicleStatus(system, scanner);
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

    // 票据管理
    private static void ticketManagement(RentalSystem system, Scanner scanner) {
        TicketService ticketService = system.getTicketService();
        
        while (true) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                       TICKET MANAGEMENT                          ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. View All Tickets                                              ║");
            System.out.println("║ 2. View Ticket Details                                           ║");
            System.out.println("║ 3. Validate Ticket                                               ║");
            System.out.println("║ 4. Ticket Statistics                                             ║");
            System.out.println("║ 0. Return to Main Menu                                           ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Choose option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    ticketService.displayAllTickets();
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "2":
                    System.out.print("Enter ticket ID: ");
                    String ticketId = scanner.nextLine();
                    ticketService.displayTicketDetails(ticketId);
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "3":
                    validateTicketForPickup(system, scanner);
                    break;
                case "4":
                    displayTicketStatistics(ticketService);
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "0":
            return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    // 验证票据用于取车
    private static void validateTicketForPickup(RentalSystem system, Scanner scanner) {
        System.out.println("\n=== Ticket Validation for Vehicle Pickup ===");
        System.out.print("Enter ticket ID: ");
        String ticketId = scanner.nextLine();
        
        System.out.print("Enter customer name: ");
        String customerName = scanner.nextLine();
        
        TicketService ticketService = system.getTicketService();
        Ticket ticket = ticketService.getTicketById(ticketId);
        
        if (ticket == null) {
            System.out.println(" Ticket not found.");
            return;
        }
        
        // Display ticket details for verification
        ticket.displayTicket();
        
        // Validate ticket before confirming pickup
        if (ticket.isUsed()) {
            System.out.println("\n ERROR: This ticket has already been used!");
            System.out.println("Pickup cancelled - ticket is no longer valid.");
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        // Validate customer name (exact match, case-insensitive)
        if (!ticket.getCustomerName().equalsIgnoreCase(customerName.trim())) {
            System.out.println("\n ERROR: Customer name does not match!");
            System.out.printf("Expected: %s\n", ticket.getCustomerName());
            System.out.printf("Provided: %s\n", customerName.trim());
            System.out.println("Pickup cancelled - name verification failed.");
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        // Validate pickup date (check if it's the rental start date or later)
        try {
            LocalDate startDate = LocalDate.parse(ticket.getStartDate());
            LocalDate today = LocalDate.now();
            
            if (today.isBefore(startDate)) {
                System.out.println("\n ERROR: It's too early for pickup!");
                System.out.printf("Rental start date: %s\n", startDate);
                System.out.printf("Today's date: %s\n", today);
                System.out.println("Please return on or after the rental start date.");
                System.out.println("Pickup cancelled - date verification failed.");
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
                return;
            }
        } catch (Exception e) {
            System.out.println("\n ERROR: Invalid date format in ticket.");
            System.out.println("Please contact support.");
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        System.out.print("\nConfirm vehicle pickup? (y/n): ");
        String confirm = scanner.nextLine();
        
        if (confirm.equalsIgnoreCase("y")) {
            if (ticketService.validateAndUseTicket(ticketId, customerName)) {
                System.out.println(" Ticket validated successfully!");
                System.out.println("Vehicle can be handed over to customer.");
                
                // Update rental status to active if needed
                Rental rental = system.findRentalById(ticket.getRentalId());
                if (rental != null && rental.getStatus() == RentalStatus.ACTIVE) {
                    System.out.println(" Rental is active and ready for pickup.");
                }
            } else {
                System.out.println(" Ticket validation failed.");
            }
        } else {
            System.out.println("Pickup cancelled.");
        }
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    // 显示票据统计
    private static void displayTicketStatistics(TicketService ticketService) {
        Map<String, Integer> stats = ticketService.getTicketStats();

        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        TICKET STATISTICS                         ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Total Tickets: %-49d ║%n", stats.get("total"));
        System.out.printf("║ Valid Tickets: %-50d ║%n", stats.get("valid"));
        System.out.printf("║ Used Tickets: %-51d ║%n", stats.get("used"));
        System.out.printf("║ Customers with Tickets: %-41d ║%n", stats.get("customers"));
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }

    // 维护状态总览（替代原来的viewVehiclesNeedingMaintenance）
    private static void maintenanceStatusOverview(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                     MAINTENANCE OVERVIEW                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        List<Vehicle> allVehicles = system.getVehicles();
        List<Vehicle> needMaintenance = system.getVehiclesNeedingMaintenance();
        List<Vehicle> underMaintenance = new ArrayList<>();
        List<Vehicle> available = new ArrayList<>();
        
        // 分类车辆状态
        for (Vehicle v : allVehicles) {
            if (v.getStatus() == VehicleStatus.UNDER_MAINTENANCE) {
                underMaintenance.add(v);
            } else if (v.getStatus() == VehicleStatus.AVAILABLE && !v.hasCriticalMaintenanceIssues()) {
                available.add(v);
            }
        }
        
        // 显示统计信息
        System.out.println("\n═══ STATUS SUMMARY ═══");
        System.out.printf("● Total Vehicles: %d\n", allVehicles.size());
        System.out.printf("● Available: %d\n", available.size());
        System.out.printf("● Under Maintenance: %d\n", underMaintenance.size());
        System.out.printf("● Need Attention: %d\n", needMaintenance.size() - underMaintenance.size());
        
        if (!needMaintenance.isEmpty()) {
            System.out.println("\n═══ VEHICLES UNDER MAINTENANCE ═══");
            System.out.println("┌─────┬──────────────────┬─────────────┬─────────┐");
            System.out.println("│ ID  │      Vehicle     │   Status    │ Issues  │");
            System.out.println("├─────┼──────────────────┼─────────────┼─────────┤");
            
            for (Vehicle v : needMaintenance) {
                List<MaintenanceLog> issues = v.getUnresolvedMaintenanceLogs();
                String vehicleInfo = v.getBrand() + " " + v.getModel();
                if (vehicleInfo.length() > 16) vehicleInfo = vehicleInfo.substring(0, 13) + "...";
                
                String status = v.getStatus().toString();
                if (status.equals("UNDER_MAINTENANCE")) status = "MAINTENANCE";
                else if (status.length() > 11) status = status.substring(0, 8) + "...";
                
                System.out.printf("│ %-3d │ %-16s │ %-11s │ %-7d │%n",
                    v.getId(), vehicleInfo, status, issues.size());
            }
            
            System.out.println("└─────┴──────────────────┴─────────────┴─────────┘");
        } else {
            System.out.println("\n All vehicles are in good condition!");
        }
        
        System.out.println("\n═══ QUICK ACTIONS ═══");
        System.out.println("1. View details of specific vehicle");
        System.out.println("2. Set vehicle to maintenance mode");
        System.out.println("3. Mark vehicle as available");
        System.out.println("0. Back to maintenance menu");
        System.out.print("Select action: ");
        
        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                if (!needMaintenance.isEmpty()) {
                    viewVehicleMaintenanceDetails(system, scanner, needMaintenance);
                } else {
                    System.out.println("No vehicles need maintenance attention.");
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
                }
                break;
            case "2":
                setVehicleMaintenanceMode(system, scanner);
                break;
            case "3":
                markVehicleAvailable(system, scanner);
                break;
            case "0":
                return;
            default:
                System.out.println("Invalid option.");
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
        }
    }


    
    // 查看车辆维护详情
    private static void viewVehicleMaintenanceDetails(RentalSystem system, Scanner scanner, List<Vehicle> vehicles) {
        System.out.print("Enter vehicle ID to view details: ");
        try {
            int vehicleId = Integer.parseInt(scanner.nextLine());
            Vehicle vehicle = null;
            for (Vehicle v : vehicles) {
                if (v.getId() == vehicleId) {
                    vehicle = v;
                    break;
                }
            }
            
            if (vehicle == null) {
                System.out.println("Vehicle not found in maintenance list.");
                return;
            }
            
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.printf("║                   VEHICLE %d MAINTENANCE DETAILS                  ║%n", vehicleId);
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.printf("║ Vehicle: %-54s  ║%n", vehicle.getBrand() + " " + vehicle.getModel());
            System.out.printf("║ Car Plate: %-54s  ║%n", vehicle.getCarPlate());
            System.out.printf("║ Status:  %-54s  ║%n", vehicle.getStatus());
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║                      UNRESOLVED ISSUES                           ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            
            List<MaintenanceLog> unresolvedLogs = vehicle.getUnresolvedMaintenanceLogs();
            if (unresolvedLogs.isEmpty()) {
                System.out.println("║                      No unresolved issues                       ║");
            } else {
                for (MaintenanceLog log : unresolvedLogs) {
                    System.out.printf("║   %-62s ║%n", log.getDescription());
                    System.out.printf("║   Severity: %-51d  ║%n", log.getSeverityLevel());
                    System.out.printf("║   Reported: %-51s  ║%n", log.getReportDate());
                    System.out.println("║                                                                  ║");
                }
            }
            
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid vehicle ID format.");
        }
    }
    
    // 取消车辆维修状态
    private static void cancelMaintenanceStatus(RentalSystem system, Scanner scanner, List<Vehicle> vehicles) {
        System.out.print("Enter vehicle ID to cancel maintenance status: ");
        try {
            int vehicleId = Integer.parseInt(scanner.nextLine());
            Vehicle vehicle = null;
            for (Vehicle v : vehicles) {
                if (v.getId() == vehicleId) {
                    vehicle = v;
                    break;
                }
            }
            
            if (vehicle == null) {
                System.out.println("Vehicle not found in maintenance list.");
                return;
            }
            
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.printf("║                 CANCEL MAINTENANCE STATUS                       ║%n");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.printf("║ Vehicle: %-54s ║%n", vehicle.getBrand() + " " + vehicle.getModel());
            System.out.printf("║ Car Plate: %-54s ║%n", vehicle.getCarPlate());
            System.out.printf("║ Current Status: %-45s ║%n", vehicle.getStatus());
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            
            List<MaintenanceLog> unresolvedLogs = vehicle.getUnresolvedMaintenanceLogs();
            if (!unresolvedLogs.isEmpty()) {
                System.out.println("║ WARNING: This vehicle has unresolved maintenance issues:        ║");
                for (MaintenanceLog log : unresolvedLogs) {
                    System.out.printf("║ • %-62s ║%n", log.getDescription());
                }
                System.out.println("║                                                                  ║");
            }
            
            System.out.println("║ Are you sure you want to set this vehicle as AVAILABLE?          ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Confirm (y/n): ");
            
            String confirm = scanner.nextLine();
            if (confirm.equalsIgnoreCase("y")) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                system.saveVehicles("vehicles.json");
                System.out.println(" Vehicle status changed to AVAILABLE.");
                
                // Optionally mark all maintenance logs as resolved
                System.out.print("Mark all maintenance issues as resolved? (y/n): ");
                String resolveAll = scanner.nextLine();
                if (resolveAll.equalsIgnoreCase("y")) {
                    for (MaintenanceLog log : unresolvedLogs) {
                        log.setStatus(MaintenanceStatus.RESOLVED);
                    }
                    System.out.println(" All maintenance issues marked as resolved.");
                }
            } else {
                System.out.println("Operation cancelled.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid vehicle ID format.");
        }
    }

    // 查看所有维护日志
    private static void viewAllMaintenanceLogs(RentalSystem system) {
        List<MaintenanceLog> logs = system.getAllMaintenanceLogs();
        if (logs.isEmpty()) {
            System.out.println("No maintenance logs found.");
        } else {
            System.out.println("\n=== All Maintenance Logs ===");
            System.out.println("┌─────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐");
            System.out.println("│ ID  │ Vehicle ID  │ Type        │ Status      │ Reported By │ Severity    │");
            System.out.println("├─────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤");
            for (MaintenanceLog log : logs) {
                System.out.printf("│ %-3d │ %-11d │ %-11s │ %-11s │ %-11s │ %-11d │%n",
                    log.getId(),
                    log.getVehicleId(),
                    log.getLogType().toString().length() > 11 ? log.getLogType().toString().substring(0, 11) : log.getLogType().toString(),
                    log.getStatus().toString().length() > 11 ? log.getStatus().toString().substring(0, 11) : log.getStatus().toString(),
                    log.getReportedBy().length() > 11 ? log.getReportedBy().substring(0, 11) : log.getReportedBy(),
                    log.getSeverityLevel()
                );
            }
            System.out.println("└─────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘");
        }
    }

    // 添加维护记录
    private static void addMaintenanceRecord(RentalSystem system, Scanner scanner, String reportedBy) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                     REPORT NEW ISSUE                             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        System.out.println("\nSelect vehicle category to report issue:");
        System.out.println("1. All vehicles");
        System.out.println("2. Vehicles already under maintenance (add additional issues)");
        System.out.println("0. Cancel");
        System.out.print("Your choice: ");
        
        String choice = scanner.nextLine().trim();
        List<Vehicle> availableVehicles;
        
        switch (choice) {
            case "1":
                availableVehicles = system.getVehicles();
                System.out.println("\n=== ALL VEHICLES ===");
                break;
            case "2":
                availableVehicles = system.getVehiclesNeedingMaintenance();
                if (availableVehicles.isEmpty()) {
                    System.out.println("\nNo vehicles are currently under maintenance.");
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
                    return;
                }
                System.out.println("\n=== VEHICLES UNDER MAINTENANCE ===");
                break;
            case "0":
                System.out.println("Operation cancelled.");
                return;
            default:
                System.out.println("Invalid choice.");
                return;
        }
        
        // 显示选定类别的车辆
        displayVehiclesWithMaintenanceInfo(availableVehicles);
        
        System.out.print("\nEnter vehicle ID: ");
        try {
            int vehicleId = Integer.parseInt(scanner.nextLine());
            
            Vehicle vehicle = system.findVehicleById(vehicleId);
            if (vehicle == null) {
                System.out.println("Vehicle not found.");
                return;
            }
            
            // 检查是否是选定类别中的车辆
            if (!availableVehicles.contains(vehicle)) {
                System.out.println("This vehicle is not in the selected category.");
                return;
            }
            
            System.out.println("Maintenance Types:");
            System.out.println("1. ROUTINE_MAINTENANCE");
            System.out.println("2. REPAIR");
            System.out.println("3. DAMAGE_REPORT");
            System.out.println("4. CLEANING");
            System.out.println("5. INSPECTION");
            System.out.print("Choose type (1-5): ");
            int typeChoice = Integer.parseInt(scanner.nextLine());
            
            MaintenanceLogType logType;
            switch (typeChoice) {
                case 1: logType = MaintenanceLogType.ROUTINE_MAINTENANCE; break;
                case 2: logType = MaintenanceLogType.REPAIR; break;
                case 3: logType = MaintenanceLogType.DAMAGE_REPORT; break;
                case 4: logType = MaintenanceLogType.CLEANING; break;
                case 5: logType = MaintenanceLogType.INSPECTION; break;
                default:
                    System.out.println("Invalid choice.");
                    return;
            }
            
            System.out.print("Enter issue description: ");
            String description = scanner.nextLine();
            
            int severity = getSeverityLevelWithGuidance(scanner);
            
            if (system.addMaintenanceLog(vehicleId, logType, description, reportedBy, severity)) {
                System.out.println(" Maintenance record added successfully!");
                
                // 如果车辆还不在维修模式，且问题严重，自动设置为维修模式
                if (vehicle.getStatus() != VehicleStatus.UNDER_MAINTENANCE && severity >= 3) {
                    vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
                    System.out.printf(" Vehicle %d automatically set to UNDER_MAINTENANCE due to severity level %d.\n", 
                                     vehicleId, severity);
                }
                
                // 高严重程度通知由RentalSystem自动发送
                if (severity >= 4) {
                    System.out.println(" Critical maintenance alert automatically sent to all administrators.");
                }
                
            } else {
                System.out.println("Failed to add maintenance record.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input format.");
        }
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * 显示车辆列表和维护信息
     */
    private static void displayVehiclesWithMaintenanceInfo(List<Vehicle> vehicles) {
        System.out.println("┌─────┬──────────────────┬─────────────┬─────────────────┐");
        System.out.println("│ ID  │     Vehicle      │   Status    │ Open Issues     │");
        System.out.println("├─────┼──────────────────┼─────────────┼─────────────────┤");
        
        for (Vehicle vehicle : vehicles) {
            String vehicleName = String.format("%s %s", vehicle.getBrand(), vehicle.getModel());
            if (vehicleName.length() > 16) {
                vehicleName = vehicleName.substring(0, 13) + "...";
            }
            
            // 缩短状态显示以适应列宽
            String status = vehicle.getStatus().toString();
            if (status.equals("UNDER_MAINTENANCE")) {
                status = "MAINTENANCE";
            } else if (status.length() > 11) {
                status = status.substring(0, 8) + "...";
            }
            
            int openIssues = vehicle.getUnresolvedMaintenanceLogs().size();
            String issueCount = openIssues > 0 ? String.valueOf(openIssues) : "None";
            
            System.out.printf("│ %-3d │ %-16s │ %-11s │ %-15s │\n",
                             vehicle.getId(), vehicleName, status, issueCount);
        }
        
        System.out.println("└─────┴──────────────────┴─────────────┴─────────────────┘");
    }

    // 解决维护问题
    private static void resolveMaintenanceIssue(RentalSystem system, Scanner scanner, String resolvedBy) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                   FIX COMPLETED ISSUES                           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        List<MaintenanceLog> unresolvedLogs = system.getAllUnresolvedMaintenanceLogs();
        if (unresolvedLogs.isEmpty()) {
            System.out.println("No unresolved maintenance issues found.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        // 显示未解决的维护问题
        System.out.println("\nUnresolved Maintenance Issues:");
        System.out.println("┌─────┬─────┬──────────────────┬─────────────┬──────────┬─────────────────┐");
        System.out.println("│ ID  │ VID │     Vehicle      │    Type     │ Severity │   Description   │");
        System.out.println("├─────┼─────┼──────────────────┼─────────────┼──────────┼─────────────────┤");
        
        for (MaintenanceLog log : unresolvedLogs) {
            Vehicle vehicle = system.findVehicleById(log.getVehicleId());
            String vehicleName = vehicle != null ? 
                String.format("%s %s", vehicle.getBrand(), vehicle.getModel()) : "Unknown";
            if (vehicleName.length() > 16) {
                vehicleName = vehicleName.substring(0, 13) + "...";
            }
            
            String description = log.getDescription();
            if (description.length() > 15) {
                description = description.substring(0, 12) + "...";
            }
            
            System.out.printf("│ %-3d │ %-3d │ %-16s │ %-11s │ %-8d │ %-15s │\n",
                             log.getId(), log.getVehicleId(), vehicleName, 
                             log.getLogType().name().substring(0, Math.min(11, log.getLogType().name().length())),
                             log.getSeverityLevel(), description);
        }
        System.out.println("└─────┴─────┴──────────────────┴─────────────┴──────────┴─────────────────┘");
        
        System.out.print("\nEnter maintenance log ID to resolve: ");
        try {
            int logId = Integer.parseInt(scanner.nextLine());
            
            MaintenanceLog log = system.findMaintenanceLogById(logId);
            if (log == null) {
                System.out.println("Maintenance log not found.");
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
                return;
            }
            
            if (log.getStatus() == MaintenanceStatus.RESOLVED) {
                System.out.println("This maintenance issue is already resolved.");
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
                return;
            }
            
            // 显示问题详细信息
            Vehicle vehicle = system.findVehicleById(log.getVehicleId());
            System.out.println("\n=== Issue Details ===");
            System.out.printf("Vehicle: %s %s (ID: %d)\n", 
                             vehicle.getBrand(), vehicle.getModel(), vehicle.getId());
            System.out.printf("Issue Type: %s\n", log.getLogType());
            System.out.printf("Severity Level: %d/5\n", log.getSeverityLevel());
            System.out.printf("Description: %s\n", log.getDescription());
            System.out.printf("Reported By: %s\n", log.getReportedBy());
            System.out.printf("Report Date: %s\n", log.getReportDate());
            
            System.out.print("\nEnter resolution cost: RM");
            double cost = Double.parseDouble(scanner.nextLine());
            
            VehicleStatus oldStatus = vehicle.getStatus();
            int unresolvedCountBefore = vehicle.getUnresolvedMaintenanceLogs().size();
            
            if (system.resolveMaintenanceLog(log.getVehicleId(), logId, cost, resolvedBy)) {
                // 检查状态变化
                VehicleStatus newStatus = vehicle.getStatus();
                int unresolvedCountAfter = vehicle.getUnresolvedMaintenanceLogs().size();
                
                System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
                System.out.println("║                    RESOLUTION COMPLETE                           ║");
                System.out.println("╚══════════════════════════════════════════════════════════════════╝");
                
                System.out.printf(" Maintenance issue resolved successfully!\n");
                System.out.printf(" Resolution cost: RM%.2f\n", cost);
                System.out.printf(" Remaining issues for Vehicle %d: %d\n", 
                                 vehicle.getId(), unresolvedCountAfter);
                
                if (oldStatus == VehicleStatus.UNDER_MAINTENANCE && newStatus == VehicleStatus.AVAILABLE) {
                    System.out.println("\n ALL MAINTENANCE COMPLETED!");
                    System.out.printf(" Vehicle %d → Status changed to AVAILABLE\n", vehicle.getId());
                    System.out.println(" This vehicle is now ready for new rentals.");
                } else if (unresolvedCountAfter > 0) {
                    System.out.printf("\n Vehicle %d → Still UNDER_MAINTENANCE\n", vehicle.getId());
                    System.out.printf(" Pending issues: %d remaining\n", unresolvedCountAfter);
                }
                
                // 发送解决通知给相关用户
                if (log.getSeverityLevel() >= 4) {
                    String notificationSubject = String.format("Maintenance Issue Resolved - Vehicle %d", vehicle.getId());
                    String notificationContent = String.format(
                        "Critical maintenance issue resolved:\n\n" +
                        "Vehicle: %s %s (ID: %d)\n" +
                        "Issue: %s\n" +
                        "Resolution Cost: RM%.2f\n" +
                        "Resolved By: %s\n\n" +
                        "Vehicle Status: %s",
                        vehicle.getBrand(), vehicle.getModel(), vehicle.getId(),
                        log.getDescription(), cost, resolvedBy, newStatus
                    );
                    
                    // 通知所有管理员
                    for (Account account : system.getAccounts()) {
                        if (account.getRole() == AccountRole.ADMIN) {
                            system.getNotificationService().sendUserMessage(
                                "SYSTEM", account.getUsername(), notificationSubject, notificationContent
                            );
                        }
                    }
                    System.out.println(" Resolution notification sent to administrators.");
                }
                
            } else {
                System.out.println(" Failed to resolve maintenance issue.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input format.");
        }
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    // 查看车辆维护历史
    private static void viewVehicleMaintenanceHistory(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                   VEHICLE MAINTENANCE HISTORY                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        // 显示所有车辆，不只是可用的
        listAllVehicles(system.getVehicles());
        System.out.print("\nEnter vehicle ID: ");
        try {
            int vehicleId = Integer.parseInt(scanner.nextLine());
            
            Vehicle vehicle = system.findVehicleById(vehicleId);
            if (vehicle == null) {
                System.out.println("Vehicle not found.");
                return;
            }
            
            List<MaintenanceLog> history = system.getMaintenanceLogsByVehicleId(vehicleId);
            if (history.isEmpty()) {
                System.out.println("\nNo maintenance history found for this vehicle.");
            } else {
                System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
                System.out.printf("║             MAINTENANCE HISTORY FOR VEHICLE %-3d                  ║%n", vehicleId);
                System.out.println("╚══════════════════════════════════════════════════════════════════╝");
                System.out.println("┌─────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐");
                System.out.println("│ ID  │ Type        │ Status      │ Reported By │ Severity    │ Cost        │");
                System.out.println("├─────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤");
                for (MaintenanceLog log : history) {
                    System.out.printf("│ %-3d │ %-11s │ %-11s │ %-11s │ %-11d │ RM%-10.2f │%n",
                        log.getId(),
                        log.getLogType().toString().length() > 11 ? log.getLogType().toString().substring(0, 11) : log.getLogType().toString(),
                        log.getStatus().toString().length() > 11 ? log.getStatus().toString().substring(0, 11) : log.getStatus().toString(),
                        log.getReportedBy().length() > 11 ? log.getReportedBy().substring(0, 11) : log.getReportedBy(),
                        log.getSeverityLevel(),
                        log.getCost()
                    );
                }
                System.out.println("└─────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘");
                
                // 显示总维护成本
                double totalCost = vehicle.getTotalMaintenanceCost();
                System.out.printf("Total maintenance cost for this vehicle: RM%.2f%n", totalCost);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input format.");
        }
    }
    
    // 管理车辆状态
    private static void manageVehicleStatus(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    MANAGE VEHICLE STATUS                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        listAllVehicles(system.getVehicles());
        
        System.out.print("\nEnter vehicle ID to manage: ");
        try {
            int vehicleId = Integer.parseInt(scanner.nextLine());
            Vehicle vehicle = system.findVehicleById(vehicleId);
            
            if (vehicle == null) {
                System.out.println("Vehicle not found.");
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
                return;
            }
            
            System.out.printf("\nCurrent Status: %s\n", vehicle.getStatus());
            System.out.println("\nAvailable Actions:");
            System.out.println("1. Set to AVAILABLE");
            System.out.println("2. Set to UNDER_MAINTENANCE");
            System.out.println("3. Set to OUT_OF_SERVICE");
            System.out.println("0. Cancel");
            System.out.print("Select action: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    vehicle.setStatus(VehicleStatus.AVAILABLE);
                    system.saveVehicles("vehicles.json");
                    System.out.println("Vehicle status changed to AVAILABLE.");
                    break;
                case "2":
                    setVehicleToMaintenanceWithIssues(system, vehicle, scanner);
                    break;
                case "3":
                    vehicle.setStatus(VehicleStatus.OUT_OF_SERVICE);
                    system.saveVehicles("vehicles.json");
                    System.out.println("Vehicle status changed to OUT_OF_SERVICE.");
                    break;
                case "0":
                    System.out.println("Operation cancelled.");
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid vehicle ID format.");
        }
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * 设置车辆进入维修模式并添加多个问题描述
     */
    private static void setVehicleToMaintenanceWithIssues(RentalSystem system, Vehicle vehicle, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 SET VEHICLE TO MAINTENANCE MODE                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        System.out.printf("Vehicle: %s %s (ID: %d)\n", vehicle.getBrand(), vehicle.getModel(), vehicle.getId());
        System.out.println("\nYou can add multiple maintenance issues for this vehicle.");
        System.out.println("Type 'done' when finished adding issues, or 'exit' to cancel.\n");
        
        List<String> issues = new ArrayList<>();
        int issueCount = 1;
        
        while (true) {
            System.out.printf("Issue #%d description: ", issueCount);
            String description = scanner.nextLine().trim();
            
            if ("exit".equalsIgnoreCase(description)) {
                System.out.println("Operation cancelled.");
                    return;
            }
            
            if ("done".equalsIgnoreCase(description)) {
                if (issues.isEmpty()) {
                    System.out.println("Please add at least one issue before finishing.");
                    continue;
                }
                break;
            }
            
            if (description.isEmpty()) {
                System.out.println("Issue description cannot be empty. Please try again.");
                continue;
            }
            
            issues.add(description);
            System.out.printf(" Issue #%d added: %s\n", issueCount, description);
            issueCount++;
        }
        
        // 为每个问题添加维护记录
        System.out.println("\nAdding maintenance records for each issue...");
        for (String issue : issues) {
            System.out.printf("\nFor issue: %s\n", issue);
            
            System.out.println("Maintenance Types:");
            System.out.println("1. ROUTINE_MAINTENANCE");
            System.out.println("2. REPAIR");
            System.out.println("3. DAMAGE_REPORT");
            System.out.println("4. CLEANING");
            System.out.println("5. INSPECTION");
            System.out.print("Choose type (1-5): ");
            
            int typeChoice;
            try {
                typeChoice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice, using REPAIR as default.");
                typeChoice = 2;
            }
            
            MaintenanceLogType logType;
            switch (typeChoice) {
                case 1: logType = MaintenanceLogType.ROUTINE_MAINTENANCE; break;
                case 2: logType = MaintenanceLogType.REPAIR; break;
                case 3: logType = MaintenanceLogType.DAMAGE_REPORT; break;
                case 4: logType = MaintenanceLogType.CLEANING; break;
                case 5: logType = MaintenanceLogType.INSPECTION; break;
                default: logType = MaintenanceLogType.REPAIR; break;
            }
            
            int severity = getSeverityLevelWithGuidance(scanner);
            
            // 添加维护记录
            system.addMaintenanceLog(vehicle.getId(), logType, issue, "ADMIN", severity);
            
            // 高严重程度通知由RentalSystem自动发送
        }
        
        // 设置车辆状态为维修中
        vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
        
        // 保存车辆状态变化
        system.saveVehicles("vehicles.json");
        
        System.out.printf("\n[SUCCESS] Vehicle %d has been set to UNDER_MAINTENANCE with %d issue(s) recorded.\n", 
                         vehicle.getId(), issues.size());
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    // 设置车辆维护模式
    private static void setVehicleMaintenanceMode(RentalSystem system, Scanner scanner) {
        System.out.println("\n═══ SET VEHICLE TO MAINTENANCE MODE ═══");
        listAllVehicles(system.getVehicles());
        
        System.out.print("Enter vehicle ID: ");
        try {
            int vehicleId = Integer.parseInt(scanner.nextLine());
            Vehicle vehicle = system.findVehicleById(vehicleId);
            
            if (vehicle == null) {
                System.out.println("Vehicle not found.");
            } else {
                vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
                system.saveVehicles("vehicles.json"); // 保存车辆状态变化
                System.out.printf("Vehicle %d set to maintenance mode.\n", vehicleId);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid vehicle ID.");
        }
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    // 标记车辆为可用
    private static void markVehicleAvailable(RentalSystem system, Scanner scanner) {
        System.out.println("\n═══ MARK VEHICLE AS AVAILABLE ═══");
        
        List<Vehicle> maintenanceVehicles = new ArrayList<>();
        for (Vehicle v : system.getVehicles()) {
            if (v.getStatus() == VehicleStatus.UNDER_MAINTENANCE) {
                maintenanceVehicles.add(v);
            }
        }
        
        if (maintenanceVehicles.isEmpty()) {
            System.out.println("No vehicles currently under maintenance.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
                    return;
        }
        
        System.out.println("Vehicles under maintenance:");
        System.out.println("┌─────┬──────────────────┬─────────────┐");
        System.out.println("│ ID  │      Vehicle     │   Issues    │");
        System.out.println("├─────┼──────────────────┼─────────────┤");
        
        for (Vehicle v : maintenanceVehicles) {
            String vehicleInfo = v.getBrand() + " " + v.getModel();
            if (vehicleInfo.length() > 16) vehicleInfo = vehicleInfo.substring(0, 13) + "...";
            int issues = v.getUnresolvedMaintenanceLogs().size();
            
            System.out.printf("│ %-3d │ %-16s │ %-11d │%n", v.getId(), vehicleInfo, issues);
        }
        System.out.println("└─────┴──────────────────┴─────────────┘");
        
        System.out.print("Enter vehicle ID to mark as available: ");
        try {
            int vehicleId = Integer.parseInt(scanner.nextLine());
            Vehicle vehicle = null;
            for (Vehicle v : maintenanceVehicles) {
                if (v.getId() == vehicleId) {
                    vehicle = v;
                    break;
                }
            }
            
            if (vehicle == null) {
                System.out.println("Vehicle not found in maintenance list.");
            } else {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                system.saveVehicles("vehicles.json"); // 保存车辆状态变化
                System.out.println("Vehicle " + vehicleId + " marked as AVAILABLE.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid vehicle ID.");
        }
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }
    
    // Quick search
    private static void searchInBookingProcess(RentalSystem system, Scanner scanner, Account account) {
        System.out.println("\n=== Quick Vehicle Search ===");
        System.out.print("Enter search query (brand/model/type/fuel) or use Boolean operators (AND/OR): ");
        String query = scanner.nextLine().trim().toLowerCase();
        
        if (query.isEmpty()) {
            System.out.println("Search query cannot be empty.");
                    return;
        }
        
        // Display search help
        System.out.println("\nSearch Tips:");
        System.out.println("- Single term: 'toyota' or 'suv'");
        System.out.println("- Multiple terms (AND): 'toyota AND suv'");
        System.out.println("- Alternative terms (OR): 'toyota OR honda'");
        System.out.println("- Complex: 'toyota OR honda AND car'");
        
        // Parse and execute search
        List<Vehicle> searchResults = performSearch(system.getAvailableVehicles(), query);
        
        if (searchResults.isEmpty()) {
            System.out.println("\nNo vehicles found matching your search criteria.");
            System.out.println("Try using different keywords or Boolean operators.");
                    return;
        }
        
        // Execute Boolean logic search
        System.out.println("\nSearch Results (" + searchResults.size() + " vehicles found):");
        listAvailableVehicles(searchResults);
    }

    private static List<Vehicle> performSearch(List<Vehicle> vehicles, String query) {
        List<Vehicle> results = new ArrayList<>();
        
        // Remove parentheses and process the inner content
        query = query.trim();
        if (query.startsWith("(") && query.endsWith(")")) {
            query = query.substring(1, query.length() - 1).trim();
        }
        
        // If query contains OR operator
        if (query.contains(" or ")) {
            String[] orTerms = query.split(" or ");
            for (Vehicle vehicle : vehicles) {
                for (String term : orTerms) {
                    if (matchesVehicle(vehicle, term.trim())) {
                        if (!results.contains(vehicle)) {
                            results.add(vehicle);
                        }
                        break;
                    }
                }
            }
        }
        // Handle AND operator
        else if (query.contains(" and ")) {
            String[] andTerms = query.split(" and ");
            for (Vehicle vehicle : vehicles) {
                boolean matchesAll = true;
                for (String term : andTerms) {
                    if (!matchesVehicle(vehicle, term.trim())) {
                        matchesAll = false;
                        break;
                    }
                }
                if (matchesAll) {
                    results.add(vehicle);
                }
            }
        }
        // Single search term
        else {
            for (Vehicle vehicle : vehicles) {
                if (matchesVehicle(vehicle, query)) {
                    results.add(vehicle);
                }
            }
        }
        
        return results;
    }

    /**
     * Display severity level guidance
     */
    private static void displaySeverityGuidance() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    SEVERITY LEVEL GUIDANCE                       ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Level 1: Minor cosmetic issues, no functional impact             ║");
        System.out.println("║          (Minor scratches, light dirt, no effect on function)    ║");
        System.out.println("║                                                                  ║");
        System.out.println("║ Level 2: Minor functional issues, slightly affects experience    ║");
        System.out.println("║          (Poor AC performance, minor audio issues)               ║");
        System.out.println("║                                                                  ║");
        System.out.println("║ Level 3: Moderate issues, noticeable impact on usage             ║");
        System.out.println("║          (Lighting faults, window/door problems, seat issues)    ║");
        System.out.println("║                                                                  ║");
        System.out.println("║ Level 4: Serious issues, significantly affects safety/function   ║");
        System.out.println("║          (Soft brakes, engine noise, steering vibration)         ║");
        System.out.println("║          - AUTO NOTIFIES ADMIN                                   ║");
        System.out.println("║                                                                  ║");
        System.out.println("║ Level 5: Critical safety issues, vehicle should not be used      ║");
        System.out.println("║          (Brake failure, engine fault, safety hazards)           ║");
        System.out.println("║          - AUTO NOTIFIES ADMIN                                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println("Press Enter to continue...");
    }

    /**
     * Get severity level input with guidance
     */
    private static int getSeverityLevelWithGuidance(Scanner scanner) {
        while (true) {
            System.out.println("\nSeverity Level Help:");
            System.out.println("1. Type 'help' to see detailed guidance");
            System.out.println("2. Enter level directly (1-5)");
            System.out.print("Enter severity level (1-5) or 'help': ");
            
            String input = scanner.nextLine().trim();
            
            if ("help".equalsIgnoreCase(input)) {
                displaySeverityGuidance();
                scanner.nextLine(); // Wait for Enter
                continue;
            }
            
            try {
                int level = Integer.parseInt(input);
                if (level >= 1 && level <= 5) {
                    return level;
                } else {
                    System.out.println("Invalid level. Please enter 1-5.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number 1-5 or 'help'.");
            }
        }
    }

    // Admin offline booking for walk-in customers
    private static void offlineBooking(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    OFFLINE BOOKING                               ║");
        System.out.println("║                  (Walk-in Customers)                             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        // Step 1: Search and select customer account
        System.out.println("\n=== Step 1: Select Customer Account ===");
        System.out.print("Search customer (username/name/phone, or press Enter to see all): ");
        String searchTerm = scanner.nextLine();
        
        List<Account> customerAccounts = system.searchUserAccounts(searchTerm);
        if (customerAccounts.isEmpty()) {
            System.out.println("No customer accounts found.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
                    return;
        }
        
        // Display customer accounts
        System.out.println("\n=== Customer Accounts ===");
        System.out.println("┌─────┬─────────────┬─────────────────────┬─────────────────────┐");
        System.out.println("│ No. │ Username    │ Full Name           │ Contact             │");
        System.out.println("├─────┼─────────────┼─────────────────────┼─────────────────────┤");
        
        for (int i = 0; i < customerAccounts.size(); i++) {
            Account acc = customerAccounts.get(i);
            String fullName = (acc.getFullName() != null && !acc.getFullName().isEmpty()) ? acc.getFullName() : "Not set";
            String contact = (acc.getContactNumber() != null && !acc.getContactNumber().isEmpty()) ? acc.getContactNumber() : "Not set";
            
            System.out.printf("│ %-3d │ %-11s │ %-19s │ %-19s │%n",
                (i + 1),
                acc.getUsername().length() > 11 ? acc.getUsername().substring(0, 11) : acc.getUsername(),
                fullName.length() > 19 ? fullName.substring(0, 19) : fullName,
                contact.length() > 19 ? contact.substring(0, 19) : contact
            );
        }
        System.out.println("└─────┴─────────────┴─────────────────────┴─────────────────────┘");
        
        // Select customer
        System.out.print("\nSelect customer number (1-" + customerAccounts.size() + ") or 0 to cancel: ");
        int customerChoice;
        try {
            customerChoice = Integer.parseInt(scanner.nextLine());
            if (customerChoice == 0) {
                System.out.println("Operation cancelled.");
                    return;
            }
            if (customerChoice < 1 || customerChoice > customerAccounts.size()) {
                System.out.println("Invalid selection.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input format.");
            return;
        }
        
        Account selectedAccount = customerAccounts.get(customerChoice - 1);
        System.out.println("Selected customer: " + selectedAccount.getUsername());
        
        // Step 2: Vehicle selection (reuse existing logic)
        System.out.println("\n=== Step 2: Select Vehicle ===");
        listAvailableVehicles(system.getVehicles());
        
        System.out.print("\nEnter vehicle ID: ");
        int vehicleId;
        try {
            vehicleId = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid vehicle ID format.");
            System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
            return;
        }
        
        Vehicle selectedVehicle = system.findVehicleById(vehicleId);
        if (selectedVehicle == null) {
            System.out.println("Vehicle not found.");
            return;
        }
        
        if ((selectedVehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE || selectedVehicle.getStatus() == VehicleStatus.OUT_OF_SERVICE) || selectedVehicle.hasCriticalMaintenanceIssues()) {
            System.out.println("Vehicle not available for booking. Status: " + selectedVehicle.getStatus());
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
            return;
        }

        // Check for existing active rental by the same customer for this vehicle
        Rental existingRental = system.findActiveRentalByUserAndVehicle(selectedAccount.getUsername(), vehicleId);

        if (existingRental != null) {
            System.out.println("\n=== EXISTING RENTAL DETECTED ===\n");
            System.out.printf("Customer already has an active rental for this vehicle:\n");
            System.out.printf("  Current rental period: %s to %s\n",
                              existingRental.getStartDate(), existingRental.getEndDate());
            System.out.printf("  Total fee: RM%.2f\n", existingRental.getTotalFee());

            System.out.print("\nDo you want to extend this existing rental instead? (y/n): ");
            String extendChoice = scanner.nextLine();

            if (extendChoice.equalsIgnoreCase("y")) {
                System.out.print("How many additional days to extend? ");
                try {
                    int additionalDays = Integer.parseInt(scanner.nextLine());
                    if (additionalDays <= 0) {
                        System.out.println("Extension days must be positive.");
                        System.out.println("\nPress Enter to continue...");
                        scanner.nextLine();
                        return;
                    }

                    LocalDate newEndDate = existingRental.getEndDate().plusDays(additionalDays);

                    // Show extension details
                    System.out.println("\n=== Extension Details ===\n");
                    System.out.printf("Original end date: %s\n", existingRental.getEndDate());
                    System.out.printf("New proposed end date: %s\n", newEndDate);
                    System.out.printf("Additional days: %d\n", additionalDays);

                    // Calculate new total rental days for discount display
                    int totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(existingRental.getStartDate(), newEndDate) + 1;
                    double discount = selectedVehicle.getDiscountForDays(totalDays);
                    if (discount > 0) {
                        System.out.printf("New total period: %d days\n", totalDays);
                        System.out.printf("Long-term discount: %.1f%% off\n", discount * 100);
                    }

                    // Insurance selection
                    double insuranceRate = selectedVehicle.getInsuranceRate();
                    System.out.printf("\nInsurance rate: %.1f%%\n", insuranceRate * 100);
                    System.out.print("Include insurance for the extended period? (y/n): ");
                    boolean extensionInsurance = scanner.nextLine().equalsIgnoreCase("y");

                    // Extend the rental
                    if (system.extendRental(selectedAccount.getUsername(), vehicleId, newEndDate, extensionInsurance)) {
                        Rental updatedRental = system.findActiveRentalByUserAndVehicle(selectedAccount.getUsername(), vehicleId);
                        System.out.println("\n=== OFFLINE RENTAL EXTENSION SUCCESSFUL ===\n");
                        System.out.printf("Updated Rental ID: %d\n", updatedRental.getId());
                        System.out.printf("Extended until: %s\n", updatedRental.getEndDate());
                        System.out.printf("Updated total fee: RM%.2f\n", updatedRental.getTotalFee());
                        System.out.println("New ticket generated.");

                        // Display new ticket
                        Ticket newTicket = system.getTicketService().getTicketByRentalId(updatedRental.getId());
                        if (newTicket != null) {
                            System.out.println("\n=== Updated Ticket ===\n");
                            newTicket.displayTicket();
                        }
                    } else {
                        System.out.println("Failed to extend rental. Please try again.");
                    }

                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    return;

                } catch (NumberFormatException e) {
                    System.out.println("Invalid number format. Please enter a valid number of days.");
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    return;
                }
            } else {
                System.out.println("Customer chose not to extend. Proceeding with new booking for this vehicle (if available).");
            }
        }

        // If not extending, or no existing rental, proceed with new booking process
        // Show vehicle details and schedule
        System.out.println("\n=== Vehicle Details ===\n");
        System.out.println("Vehicle: " + selectedVehicle.getBrand() + " " + selectedVehicle.getModel());
        System.out.println("Car Plate: " + selectedVehicle.getCarPlate());
        System.out.println("Type: " + selectedVehicle.getVehicleType());
        System.out.println("Fuel Type: " + selectedVehicle.getFuelType());
        System.out.printf("Base Price: RM%.2f/day\n", selectedVehicle.getBasePrice());

        List<String> unavailablePeriods = system.getVehicleUnavailablePeriods(vehicleId);
        if (!unavailablePeriods.isEmpty()) {
            System.out.println("\n=== Vehicle Schedule (Unavailable Periods) ===\n");
            for (String period : unavailablePeriods) {
                System.out.println("- " + period);
            }
        }

        // Step 3: Date selection
        System.out.println("\n=== Step 3: Rental Period ===\n");
        System.out.print("Enter rental start date (yyyy-MM-dd or 'today'): ");
        String startStr = scanner.nextLine();

        LocalDate startDate;
        if (startStr.equalsIgnoreCase("today")) {
            startDate = LocalDate.now();
        } else {
            try {
                startDate = LocalDate.parse(startStr);
            } catch (Exception e) {
                System.out.println("Invalid date format. Please use yyyy-MM-dd.");
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
                return;
            }
        }

        // Show available discounts before asking for rental days
        if (selectedVehicle.getLongTermDiscounts() != null && !selectedVehicle.getLongTermDiscounts().isEmpty()) {
            System.out.println("\n=== Available Discounts ===\n");
            System.out.println("Long-term rental discounts:");
            for (Map.Entry<Integer, Double> entry : selectedVehicle.getLongTermDiscounts().entrySet()) {
                System.out.printf("- %d+ days: %.1f%% off\n", entry.getKey(), entry.getValue() * 100);
            }
            System.out.println("\nChoose your rental duration to see applicable discounts!");
        }

        System.out.print("\nEnter rental duration (days): ");
        int rentalDays;
        try {
            rentalDays = Integer.parseInt(scanner.nextLine());
            if (rentalDays <= 0) {
                System.out.println("Rental days must be positive.");
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format. Please enter a valid number.");
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
            return;
        }

        LocalDate endDate = startDate.plusDays(rentalDays - 1);

        // Check for conflicts
        String conflictDetails = system.getConflictDetails(vehicleId, startDate, endDate);
        if (conflictDetails != null) {
            System.out.println("\n*** BOOKING CONFLICT DETECTED ***\n");
            System.out.println("Conflict Details: " + conflictDetails);
            System.out.println("Cannot proceed with booking.");
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
            return;
        }

        // Step 4: Insurance selection
        System.out.println("\n=== Step 4: Insurance ===\n");
        double insuranceRate = selectedVehicle.getInsuranceRate();
        double baseRentalCost = selectedVehicle.getBasePrice() * rentalDays;
        double insuranceCost = baseRentalCost * insuranceRate;

        System.out.printf("Insurance rate: %.1f%%\n", insuranceRate * 100);
        System.out.printf("Estimated insurance cost: RM%.2f\n", insuranceCost);
        System.out.print("Include insurance? (y/n): ");
        boolean insurance = scanner.nextLine().equalsIgnoreCase("y");

        // Step 5: Customer information verification
        System.out.println("\n=== Step 5: Customer Information ===\n");
        String customerName = selectedAccount.getFullName();
        String contact = selectedAccount.getContactNumber();

        if (customerName == null || customerName.isEmpty() || contact == null || contact.isEmpty()) {
            System.out.println("Customer profile incomplete. Please update:");
            if (customerName == null || customerName.isEmpty()) {
                System.out.print("Enter customer full name: ");
                customerName = scanner.nextLine();
                selectedAccount.setFullName(customerName);
            }
            if (contact == null || contact.isEmpty()) {
                System.out.print("Enter customer contact number: ");
                contact = scanner.nextLine();
                selectedAccount.setContactNumber(contact);
            }
        } else {
            System.out.println("Customer Name: " + customerName);
            System.out.println("Contact: " + contact);
        }

        // Step 6: Final confirmation and booking
        double totalFee = system.calculateRentalFee(selectedVehicle, startDate, endDate, insurance);
        System.out.println("\n=== Booking Summary ===\n");
        System.out.println("Customer: " + customerName + " (" + selectedAccount.getUsername() + ")");
        System.out.println("Vehicle: " + selectedVehicle.getBrand() + " " + selectedVehicle.getModel());
        System.out.println("Period: " + startDate + " to " + endDate + " (" + rentalDays + " days)");
        System.out.println("Insurance: " + (insurance ? "Yes" : "No"));
        System.out.printf("Total Fee: RM%.2f\n", totalFee);

        System.out.print("\nConfirm offline booking? (y/n): ");
        if (!scanner.nextLine().equalsIgnoreCase("y")) {
            System.out.println("Booking cancelled.");
            return;
        }

        try {
            Customer customer = system.findOrCreateCustomer(customerName, contact);
            Rental rental = system.createOfflineRental(customer, selectedVehicle, startDate, endDate, 
                                                     insurance, selectedAccount.getUsername());

            System.out.println("\n=== OFFLINE BOOKING SUCCESSFUL ===\n");
            System.out.println("Rental ID: " + rental.getId());
            System.out.println("Status: ACTIVE (No approval needed)");
            System.out.println("Ticket generated.");

            // Display ticket information
            Ticket ticket = system.getTicketService().getTicketByRentalId(rental.getId());
            if (ticket != null) {
                System.out.println("\n=== Generated Ticket ===\n");
                ticket.displayTicket();
            }

        } catch (Exception e) {
            System.out.println("Failed to create offline booking: " + e.getMessage());
        }

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

} 