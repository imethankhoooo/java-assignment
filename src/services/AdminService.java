package services;

import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

import enums.*;
import models.*;

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
            System.out.println("║ 3. Maintenance Management                                       ║");
            System.out.println("║ 4. Vehicle Search                                               ║");
            System.out.println("║ 0. Back to Admin Menu                                           ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    vehicleService.viewAllVehicles(vehicleService.getVehicles());
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
                    break;
                case "2":
                    addNewVehicle(scanner);
                    break;
                case "3":
                    vehicleService.maintenanceManagement(null, scanner);
                    break;
                case "4":
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
     * Add new vehicle (admin function)
     */
    private static void addNewVehicle(Scanner scanner) {
        System.out.println("\n=== ADD NEW VEHICLE ===");
        
        System.out.print("Enter vehicle ID: ");
        int id;
        try {
            id = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format. Please enter a number.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        // Check if ID already exists
        if (vehicleService.findVehicleById(id) != null) {
            System.out.println("Vehicle ID already exists. Please choose another ID.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        System.out.print("Enter vehicle brand: ");
        String brand = scanner.nextLine().trim();
        if (brand.isEmpty()) {
            System.out.println("Brand cannot be empty.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        System.out.print("Enter vehicle model: ");
        String model = scanner.nextLine().trim();
        if (model.isEmpty()) {
            System.out.println("Model cannot be empty.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        System.out.print("Enter car plate: ");
        String carPlate = scanner.nextLine().trim();
        if (carPlate.isEmpty()) {
            System.out.println("Car plate cannot be empty.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        System.out.println("Select vehicle type:");
        System.out.println("1. CAR");
        System.out.println("2. MOTORCYCLE");
        System.out.println("3. TRUCK");
        System.out.println("4. VAN");
        System.out.print("Select option: ");
        
        VehicleType vehicleType = VehicleType.CAR;
        String typeChoice = scanner.nextLine();
        switch (typeChoice) {
            case "1": vehicleType = VehicleType.CAR; break;
            case "2": vehicleType = VehicleType.MOTORCYCLE; break;
            case "3": vehicleType = VehicleType.TRUCK; break;
            case "4": vehicleType = VehicleType.VAN; break;
            default:
                System.out.println("Invalid choice. Defaulting to CAR.");
                break;
        }
        
        System.out.println("Select fuel type:");
        System.out.println("1. PETROL");
        System.out.println("2. DIESEL");
        System.out.println("3. ELECTRIC");
        System.out.println("4. HYBRID");
        System.out.print("Select option: ");
        
        FuelType fuelType = FuelType.PETROL;
        String fuelChoice = scanner.nextLine();
        switch (fuelChoice) {
            case "1": fuelType = FuelType.PETROL; break;
            case "2": fuelType = FuelType.DIESEL; break;
            case "3": fuelType = FuelType.ELECTRIC; break;
            case "4": fuelType = FuelType.HYBRID; break;
            default:
                System.out.println("Invalid choice. Defaulting to PETROL.");
                break;
        }
        
        System.out.print("Enter base price per day: ");
        double basePrice;
        try {
            basePrice = Double.parseDouble(scanner.nextLine());
            if (basePrice <= 0) {
                System.out.println("Price must be positive.");
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid price format.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        System.out.print("Enter insurance rate (0.0 - 1.0): ");
        double insuranceRate;
        try {
            insuranceRate = Double.parseDouble(scanner.nextLine());
            if (insuranceRate < 0 || insuranceRate > 1) {
                System.out.println("Insurance rate must be between 0.0 and 1.0.");
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid insurance rate format.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        // Create new vehicle
        Vehicle newVehicle = new Vehicle(id, brand, model, carPlate, vehicleType, fuelType, 
                                       VehicleStatus.AVAILABLE, insuranceRate, basePrice, null);
        vehicleService.getVehicles().add(newVehicle);
        vehicleService.saveVehicles("vehicles.json");
        
        System.out.println("\n=== VEHICLE ADDED SUCCESSFULLY ===");
        System.out.printf("ID: %d%n", id);
        System.out.printf("Brand: %s%n", brand);
        System.out.printf("Model: %s%n", model);
        System.out.printf("Car Plate: %s%n", carPlate);
        System.out.printf("Type: %s%n", vehicleType);
        System.out.printf("Fuel: %s%n", fuelType);
        System.out.printf("Price: RM%.2f/day%n", basePrice);
        System.out.printf("Insurance Rate: %.1f%%%n", insuranceRate * 100);
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
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
            vehicleService.listAvailableVehicles(results);
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
            .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
            .count();
        long rentedVehicles = allVehicles.stream()
            .filter(v -> v.getStatus() == VehicleStatus.RENTED)
            .count();
        long maintenanceVehicles = allVehicles.stream()
            .filter(v -> v.getStatus() == VehicleStatus.UNDER_MAINTENANCE)
            .count();
        
        System.out.println("Vehicle Statistics:");
        System.out.printf("  Total Vehicles: %d%n", totalVehicles);
        System.out.printf("  Available: %d%n", availableVehicles);
        System.out.printf("  Rented: %d%n", rentedVehicles);
        System.out.printf("  Under Maintenance: %d%n", maintenanceVehicles);
        
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
}
