package services;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Map;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDate; 
import java.util.HashMap;

import models.*;
import enums.*;
import main.*;


public class vehicleService {
    private static List<Vehicle> vehicles = new ArrayList<Vehicle>();
    private static List<Rental> rentals = new ArrayList<>();
    
    
    public static List<Vehicle> getVehicles() {
        return vehicles;
    }
    /**
     * Load vehicle data from JSON file
     */
    public static void loadVehicles(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            vehicles = new ArrayList<>();
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            vehicles = parseVehiclesFromJson(jsonContent.toString());
            System.out.println("Loaded vehicles: " + vehicles.size());
            if (vehicles == null)
                vehicles = new ArrayList<>();
        } catch (IOException e) {
            System.out.println("Failed to load vehicle data: " + e.getMessage());
            vehicles = new ArrayList<>();
        }
    }
     /**
     * Parse vehicle JSON data
     */
    public static List<Vehicle> parseVehiclesFromJson(String json) {
        List<Vehicle> vehicleList = new ArrayList<>();
        try {
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);

                String[] vehicleObjects = splitJsonObjects(json);

                for (String vehicleJson : vehicleObjects) {
                    Vehicle vehicle = parseVehicleFromJson(vehicleJson.trim());
                    if (vehicle != null) {
                        vehicleList.add(vehicle);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to parse vehicle JSON: " + e.getMessage());
        }
        return vehicleList;
    }
     /**
     * Parse single vehicle object
     */
    public static Vehicle parseVehicleFromJson(String json) {
        try {
            int id = Integer.parseInt(RentalSystem.extractJsonValue(json, "id"));
            String brand = RentalSystem.extractJsonValue(json, "brand");
            String model = RentalSystem.extractJsonValue(json, "model");
            String statusStr = RentalSystem.extractJsonValue(json, "status");
            double insuranceRate = Double.parseDouble(RentalSystem.extractJsonValue(json, "insuranceRate"));

            // Parse price, if not specified use default value
            String priceStr = RentalSystem.extractJsonValue(json, "basePrice");
            double basePrice = (priceStr != null) ? Double.parseDouble(priceStr) : 50.0;

            // Parse new vehicle attributes
            String carPlate = RentalSystem.extractJsonValue(json, "carPlate");
            if (carPlate == null)
                carPlate = "UNKNOWN";

            String vehicleTypeStr = RentalSystem.extractJsonValue(json, "vehicleType");
            VehicleType vehicleType = VehicleType.CAR; // Default value
            if (vehicleTypeStr != null) {
                try {
                    vehicleType = VehicleType.valueOf(vehicleTypeStr);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid vehicle type: " + vehicleTypeStr + ", using default CAR");
                }
            }

            String fuelTypeStr = RentalSystem.extractJsonValue(json, "fuelType");
            FuelType fuelType = FuelType.PETROL;
            if (fuelTypeStr != null) {
                try {
                    fuelType = FuelType.valueOf(fuelTypeStr);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid fuel type: " + fuelTypeStr + ", using default PETROL");
                }
            }

            VehicleStatus status = VehicleStatus.valueOf(statusStr);

            // Parse long term discounts
            Map<Integer, Double> discounts = new HashMap<>();
            String discountsJson = RentalSystem.extractJsonObject(json, "longTermDiscounts");
            if (discountsJson != null && !discountsJson.equals("null")) {
                // Simple parse discount object
                discountsJson = discountsJson.replace("{", "").replace("}", "");
                String[] pairs = discountsJson.split(",");
                for (String pair : pairs) {
                    if (pair.contains(":")) {
                        String[] keyValue = pair.split(":");
                        int days = Integer.parseInt(keyValue[0].trim().replace("\"", ""));
                        double discount = Double.parseDouble(keyValue[1].trim());
                        discounts.put(days, discount);
                    }
                }
            }

            // Use new constructor to create Vehicle object
            return new Vehicle(id, brand, model, carPlate, vehicleType, fuelType, status, insuranceRate, basePrice,
                    discounts);
        } catch (Exception e) {
            System.out.println("Failed to parse vehicle object: " + e.getMessage());
        }
        return null;
    }
    // Display all available vehicles with enhanced information
    public static void listAvailableVehicles(List<Vehicle> vehicles) {
        System.out.println("\n=== Vehicle List (All Vehicles) ===");

        if (vehicles.isEmpty()) {
            System.out.println("No vehicles found.");
            return;
        }

        System.out.println(
                "┌─────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐");
        System.out.println(
                "│ ID  │ Brand       │ Model       │ Car Plate   │ Type        │ Fuel        │ Price/Day   │ Status      │");
        System.out.println(
                "├─────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤");

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
                    v.getVehicleType().toString().length() > 11 ? v.getVehicleType().toString().substring(0, 11)
                            : v.getVehicleType().toString(),
                    v.getFuelType().toString().length() > 11 ? v.getFuelType().toString().substring(0, 11)
                            : v.getFuelType().toString(),
                    v.getBasePrice(),
                    statusDisplay);
        }
        System.out.println(
                "└─────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘");
        
    }
    
    public static void setVehicles(List<Vehicle> vehicleList) {
        vehicles = vehicleList;
    }
    
    public static void setRentals(List<Rental> rentalList) {
        rentals = rentalList;
    }
    public static List<Vehicle> performSearch(List<Vehicle> vehicles, String query) {
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
                    if (RentalSystem.matchesVehicle(vehicle, term.trim())) {
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
                    if (!RentalSystem.matchesVehicle(vehicle, term.trim())) {
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
                if (RentalSystem.matchesVehicle(vehicle, query)) {
                    results.add(vehicle);
                }
            }
        }
        
        return results;
    }
     /**
     * Display severity level guidance
     */
    public static void displaySeverityGuidance() {
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
    
    // Admin functions
    public static void viewAllVehicles(List<Vehicle> vehicles) {
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
            
            // Add icon hint based on status, but ensure length does not exceed 11 characters
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
    public static void addNewVehicle(RentalSystem system, Scanner scanner) {
        System.out.println("\n=== Add New Vehicle ===");
        System.out.print("Enter vehicle ID: ");
        int id = Integer.parseInt(scanner.nextLine());
        
        System.out.print("Enter vehicle model: ");
        String model = scanner.nextLine();
        
        System.out.print("Enter base price per day: ");
        double basePrice = Double.parseDouble(scanner.nextLine());
        
        // Create new vehicle and add to system
        Vehicle newVehicle = new Vehicle(id, "Default", model, VehicleStatus.AVAILABLE, 0.1, basePrice, null);
        getVehicles().add(newVehicle);
        
        System.out.println("Vehicle added successfully!");
        System.out.println("ID: " + id + ", Model: " + model + ", Price: RM" + basePrice + "/day");
    }
     // Maintenance management
     public static void maintenanceManagement(RentalSystem system, Scanner scanner) {
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
    // Maintenance status overview (alternative to viewVehiclesNeedingMaintenance)
    public static void maintenanceStatusOverview(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                     MAINTENANCE OVERVIEW                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        List<Vehicle> allVehicles = getVehicles();
        List<Vehicle> needMaintenance = getVehiclesNeedingMaintenance();
        List<Vehicle> underMaintenance = new ArrayList<>();
        List<Vehicle> available = new ArrayList<>();
        
        // Classify vehicle status
        for (Vehicle v : allVehicles) {
            if (v.getStatus() == VehicleStatus.UNDER_MAINTENANCE) {
                underMaintenance.add(v);
            } else if (v.getStatus() == VehicleStatus.AVAILABLE && !v.hasCriticalMaintenanceIssues()) {
                available.add(v);
            }
        }
        
        // Display statistics
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


    
    // View vehicle maintenance details
    public static void viewVehicleMaintenanceDetails(RentalSystem system, Scanner scanner, List<Vehicle> vehicles) {
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
    


    // Add maintenance record
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
                availableVehicles = getVehicles();
                System.out.println("\n=== ALL VEHICLES ===");
                break;
            case "2":
                availableVehicles = getVehiclesNeedingMaintenance();
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
        
        // Display vehicles in selected category
        displayVehiclesWithMaintenanceInfo(availableVehicles);
        
        System.out.print("\nEnter vehicle ID: ");
        try {
            int vehicleId = Integer.parseInt(scanner.nextLine());
            
            Vehicle vehicle = findVehicleById(vehicleId);
            if (vehicle == null) {
                System.out.println("Vehicle not found.");
                return;
            }
            
            // Check if vehicle is in selected category
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
            
            if (addMaintenanceLog(vehicleId, logType, description, reportedBy, severity, system)) {
                System.out.println(" Maintenance record added successfully!");
                
                // If vehicle is not in maintenance mode and issue is severe, automatically set to maintenance mode
                if (vehicle.getStatus() != VehicleStatus.UNDER_MAINTENANCE && severity >= 3) {
                    vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
                    System.out.printf(" Vehicle %d automatically set to UNDER_MAINTENANCE due to severity level %d.\n", 
                                     vehicleId, severity);
                }
                
                // High severity notifications sent by RentalSystem automatically
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
     * Display vehicle list and maintenance information
     */
    public static void displayVehiclesWithMaintenanceInfo(List<Vehicle> vehicles) {
        System.out.println("┌─────┬──────────────────┬─────────────┬─────────────────┐");
        System.out.println("│ ID  │     Vehicle      │   Status    │ Open Issues     │");
        System.out.println("├─────┼──────────────────┼─────────────┼─────────────────┤");
        
        for (Vehicle vehicle : vehicles) {
            String vehicleName = String.format("%s %s", vehicle.getBrand(), vehicle.getModel());
            if (vehicleName.length() > 16) {
                vehicleName = vehicleName.substring(0, 13) + "...";
            }
            
            // Shorten status display to fit column width
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

    // Resolve maintenance issues
    public static void resolveMaintenanceIssue(RentalSystem system, Scanner scanner, String resolvedBy) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                   FIX COMPLETED ISSUES                           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        List<MaintenanceLog> unresolvedLogs = getAllUnresolvedMaintenanceLogs();
        if (unresolvedLogs.isEmpty()) {
            System.out.println("No unresolved maintenance issues found.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        // Display unresolved maintenance issues
        System.out.println("\nUnresolved Maintenance Issues:");
        System.out.println("┌─────┬─────┬──────────────────┬─────────────┬──────────┬─────────────────┐");
        System.out.println("│ ID  │ VID │     Vehicle      │    Type     │ Severity │   Description   │");
        System.out.println("├─────┼─────┼──────────────────┼─────────────┼──────────┼─────────────────┤");
        
        for (MaintenanceLog log : unresolvedLogs) {
            Vehicle vehicle = findVehicleById(log.getVehicleId());
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
            
            MaintenanceLog log = findMaintenanceLogById(logId);
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
            
            // Display issue details
            Vehicle vehicle = findVehicleById(log.getVehicleId());
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
            
            if (resolveMaintenanceLog(log.getVehicleId(), logId, cost, resolvedBy)) {
                // Check status change
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
                
                // Send resolution notification to relevant users
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
                    
                    // Notify all administrators
                    for (Account account : AccountService.getAccounts()) {
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
        Main.clearScreen();
    }

    // View vehicle maintenance history
    public static void viewVehicleMaintenanceHistory(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                   VEHICLE MAINTENANCE HISTORY                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        // Display all vehicles, not just available
        RentalSystem.listAllVehicles(getVehicles());
        System.out.print("\nEnter vehicle ID: ");
        try {
            int vehicleId = Integer.parseInt(scanner.nextLine());
            
            Vehicle vehicle = findVehicleById(vehicleId);
            if (vehicle == null) {
                System.out.println("Vehicle not found.");
                return;
            }
            
            List<MaintenanceLog> history = getMaintenanceLogsByVehicleId(vehicleId);
            if (history.isEmpty()) {
                System.out.println("\nNo maintenance history found for this vehicle.");
            } else {
                System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
                System.out.printf("║             MAINTENANCE HISTORY FOR VEHICLE %-3d                  ║%n", vehicleId);
                System.out.println("╚══════════════════════════════════════════════════════════════════╝");
                System.out.println("┌─────┬──────────┬─────────────┬─────────────┬──────────┬─────────────┬─────────────┐");
                System.out.println("│ ID  │ Type     │ Status      │ Reported By │ Severity │ Cost        │ Resolved By │");
                System.out.println("├─────┼──────────┼─────────────┼─────────────┼──────────┼─────────────┼─────────────┤");
                for (MaintenanceLog log : history) {
                    String resolvedBy = log.getResolvedBy() != null ? log.getResolvedBy() : "-";
                    System.out.printf("│ %-3d │ %-8s │ %-11s │ %-11s │ %-8d │ RM%-9.2f │ %-11s │%n",
                        log.getId(),
                        log.getLogType().toString().length() > 8 ? log.getLogType().toString().substring(0, 8) : log.getLogType().toString(),
                        log.getStatus().toString().length() > 11 ? log.getStatus().toString().substring(0, 11) : log.getStatus().toString(),
                        log.getReportedBy().length() > 11 ? log.getReportedBy().substring(0, 11) : log.getReportedBy(),
                        log.getSeverityLevel(),
                        log.getCost(),
                        resolvedBy.length() > 11 ? resolvedBy.substring(0, 11) : resolvedBy
                    );
                }
                System.out.println("└─────┴──────────┴─────────────┴─────────────┴──────────┴─────────────┴─────────────┘");
                
                // Display total maintenance cost
                double totalCost = vehicle.getTotalMaintenanceCost();
                System.out.printf("Total maintenance cost for this vehicle: RM%.2f%n", totalCost);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input format.");
        }
    }
    
    // Manage vehicle status
    public static void manageVehicleStatus(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    MANAGE VEHICLE STATUS                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        RentalSystem.listAllVehicles(getVehicles());
        
        System.out.print("\nEnter vehicle ID to manage: ");
        try {
            int vehicleId = Integer.parseInt(scanner.nextLine());
            Vehicle vehicle = findVehicleById(vehicleId);
            
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
                    saveVehicles("vehicles.json");
                    System.out.println("Vehicle status changed to AVAILABLE.");
                    break;
                case "2":
                    setVehicleToMaintenanceWithIssues(system, vehicle, scanner);
                    break;
                case "3":
                    vehicle.setStatus(VehicleStatus.OUT_OF_SERVICE);
                    saveVehicles("vehicles.json");
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

    

    // Set vehicle maintenance mode
    public static void setVehicleMaintenanceMode(RentalSystem system, Scanner scanner) {
        System.out.println("\n═══ SET VEHICLE TO MAINTENANCE MODE ═══");
        RentalSystem.listAllVehicles(getVehicles());
        
        System.out.print("Enter vehicle ID: ");
        try {
            int vehicleId = Integer.parseInt(scanner.nextLine());
            Vehicle vehicle = findVehicleById(vehicleId);
            
            if (vehicle == null) {
                System.out.println("Vehicle not found.");
            } else {
                vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
                saveVehicles("vehicles.json"); // Save vehicle status change
                System.out.printf("Vehicle %d set to maintenance mode.\n", vehicleId);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid vehicle ID.");
        }
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    // Mark vehicle as available
    public static void markVehicleAvailable(RentalSystem system, Scanner scanner) {
        System.out.println("\n═══ MARK VEHICLE AS AVAILABLE ═══");
        
        List<Vehicle> maintenanceVehicles = new ArrayList<>();
        for (Vehicle v : getVehicles()) {
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
                saveVehicles("vehicles.json"); // Save vehicle status change
                System.out.println("Vehicle " + vehicleId + " marked as AVAILABLE.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid vehicle ID.");
        }
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }
     /**
     * Convert maintenance records to JSON format
     */
    public static String convertMaintenanceLogsToJson() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");

        List<MaintenanceLog> allLogs = getAllMaintenanceLogs();

        for (int i = 0; i < allLogs.size(); i++) {
            MaintenanceLog log = allLogs.get(i);
            json.append("  {\n");
            json.append("    \"id\": ").append(log.getId()).append(",\n");
            json.append("    \"vehicleId\": ").append(log.getVehicleId()).append(",\n");
            json.append("    \"logType\": \"").append(log.getLogType()).append("\",\n");
            json.append("    \"description\": \"").append(Main.escapeJson(log.getDescription())).append("\",\n");
            json.append("    \"reportDate\": \"").append(log.getReportDate()).append("\",\n");
            json.append("    \"completedDate\": ")
                    .append(log.getCompletedDate() != null ? "\"" + log.getCompletedDate() + "\"" : "null")
                    .append(",\n");
            json.append("    \"cost\": ").append(log.getCost()).append(",\n");
            json.append("    \"status\": \"").append(log.getStatus()).append("\",\n");
            json.append("    \"reportedBy\": \"").append(Main.escapeJson(log.getReportedBy())).append("\",\n");
            json.append("    \"assignedTo\": \"")
                    .append(Main.escapeJson(log.getAssignedTo() != null ? log.getAssignedTo() : "")).append("\",\n");
            json.append("    \"severityLevel\": ").append(log.getSeverityLevel()).append("\n");
            json.append("  }");

            if (i < allLogs.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("]");
        return json.toString();
    }



    /**
     * Save vehicle data to JSON file
     */
    public static void saveVehicles(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            String jsonContent = convertVehiclesToJson();
            writer.println(jsonContent);
        } catch (IOException e) {
            System.out.println("Failed to save vehicle data: " + e.getMessage());
        }
    }

    /**
     * Convert vehicle data to JSON format
     */
    public static String convertVehiclesToJson() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");

        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicle = vehicles.get(i);
            json.append("  {\n");
            json.append("    \"id\": ").append(vehicle.getId()).append(",\n");
            json.append("    \"brand\": \"").append(vehicle.getBrand()).append("\",\n");
            json.append("    \"model\": \"").append(vehicle.getModel()).append("\",\n");
            json.append("    \"carPlate\": \"").append(vehicle.getCarPlate()).append("\",\n");
            json.append("    \"vehicleType\": \"").append(vehicle.getVehicleType()).append("\",\n");
            json.append("    \"fuelType\": \"").append(vehicle.getFuelType()).append("\",\n");
            json.append("    \"status\": \"").append(vehicle.getStatus()).append("\",\n");
            json.append("    \"insuranceRate\": ").append(vehicle.getInsuranceRate()).append(",\n");
            json.append("    \"basePrice\": ").append(vehicle.getBasePrice()).append(",\n");
            json.append("    \"longTermDiscounts\": {");

            if (vehicle.getLongTermDiscounts() != null && !vehicle.getLongTermDiscounts().isEmpty()) {
                java.util.Iterator<java.util.Map.Entry<Integer, Double>> iterator = vehicle.getLongTermDiscounts()
                        .entrySet().iterator();
                while (iterator.hasNext()) {
                    java.util.Map.Entry<Integer, Double> entry = iterator.next();
                    json.append("\"").append(entry.getKey()).append("\": ").append(entry.getValue());
                    if (iterator.hasNext()) {
                        json.append(", ");
                    }
                }
            }

            json.append("}\n");
            json.append("  }");

            if (i < vehicles.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("]");
        return json.toString();
    }
    /**
     * Add vehicle maintenance record
     */
    public static boolean addMaintenanceLog(int vehicleId, MaintenanceLogType logType, String description,
            String reportedBy, int severityLevel) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle != null) {
            MaintenanceLog log = new MaintenanceLog(vehicleId, logType, description, reportedBy, severityLevel);
            vehicle.addMaintenanceLog(log);

            // Automatically save maintenance records to JSON file
            saveMaintenanceLogs("maintenance_logs.json");

            // Save vehicle status change to JSON file
            saveVehicles("vehicles.json");

            return true;
        }
        return false;
    }
    
    /**
     * Add vehicle maintenance record with notification support
     */
    public static boolean addMaintenanceLog(int vehicleId, MaintenanceLogType logType, String description,
            String reportedBy, int severityLevel, RentalSystem system) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle != null) {
            MaintenanceLog log = new MaintenanceLog(vehicleId, logType, description, reportedBy, severityLevel);
            vehicle.addMaintenanceLog(log);

            // If it is a high severity issue, automatically send notification to all admins
            if (severityLevel >= 4 && system != null) {
                sendCriticalMaintenanceNotificationToAllAdmins(vehicle, description, severityLevel, reportedBy, system);
            }

            // Automatically save maintenance records to JSON file
            saveMaintenanceLogs("maintenance_logs.json");

            // Save vehicle status change to JSON file
            saveVehicles("vehicles.json");

            return true;
        }
        return false;
    }



    
    /**
     * Send critical maintenance notification to all administrators
     */
    private static void sendCriticalMaintenanceNotificationToAllAdmins(Vehicle vehicle, String description,
            int severity, String reportedBy, RentalSystem system) {
        String subject = String.format("CRITICAL MAINTENANCE ALERT - Vehicle %d", vehicle.getId());
        String content = String.format(
                "🚨 HIGH PRIORITY MAINTENANCE ISSUE 🚨\n\n" +
                        "Vehicle: %s %s (ID: %d)\n" +
                        "Car Plate: %s\n" +
                        "Severity Level: %d/5\n" +
                        "Reported By: %s\n" +
                        "Issue Description: %s\n\n" +
                        "This issue requires immediate attention due to its high severity level.\n" +
                        "Please address this maintenance issue as soon as possible.\n\n" +
                        "System automatically generated alert.",
                vehicle.getBrand(), vehicle.getModel(), vehicle.getId(),
                vehicle.getCarPlate(), severity, reportedBy, description);

        // Send notification to all administrators
        for (Account account : AccountService.getAccounts()) {
            if (account.getRole() == AccountRole.ADMIN) {
                system.getNotificationService().sendUserMessage(
                    "SYSTEM", account.getUsername(), subject, content
                );
            }
        }
    }

    /**
     * Resolve maintenance issue
     */
    public static boolean resolveMaintenanceLog(int vehicleId, int logId, double cost, String resolvedBy) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle != null) {
            for (MaintenanceLog log : vehicle.getMaintenanceLogs()) {
                if (log.getId() == logId) {
                    log.setStatus(MaintenanceStatus.RESOLVED);
                    log.setCost(cost);
                    log.setResolvedBy(resolvedBy);
                    vehicle.resolveMaintenanceLog(logId, cost);

                    // Automatically save maintenance records to JSON file
                    saveMaintenanceLogs("maintenance_logs.json");

                    // Save vehicle status change to JSON file
                    saveVehicles("vehicles.json");

                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get all unresolved maintenance issues
     */
    public static List<MaintenanceLog> getAllUnresolvedMaintenanceLogs() {
        List<MaintenanceLog> allLogs = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            allLogs.addAll(vehicle.getUnresolvedMaintenanceLogs());
        }
        return allLogs;
    }

    /**
     * Get maintenance history for a specific vehicle
     */
    public static List<MaintenanceLog> getVehicleMaintenanceHistory(int vehicleId) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle != null) {
            return vehicle.getMaintenanceLogs();
        }
        return new ArrayList<>();
    }

    /**
     * Check vehicle availability for a specific period (including buffer period)
     */
    public static boolean isVehicleAvailable(int vehicleId, LocalDate startDate, LocalDate endDate) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle != null) {
            return vehicle.isAvailable(startDate, endDate);
        }
        return false;
    }

    /**
     * Get unavailable periods for a vehicle (including rental information)
     */
    public static List<String> getVehicleUnavailablePeriods(int vehicleId) {
        List<String> periods = new ArrayList<>();

        // Get all active and pending rentals
        for (Rental rental : rentals) {
            if (rental.getVehicle().getId() == vehicleId &&
                    (rental.getStatus() == RentalStatus.ACTIVE || rental.getStatus() == RentalStatus.PENDING)) {

                LocalDate bufferStart = rental.getStartDate().minusDays(2);
                LocalDate bufferEnd = rental.getEndDate().plusDays(2);

                String statusText = rental.getStatus() == RentalStatus.ACTIVE ? "ACTIVE" : "PENDING";
                String customerInfo = rental.getCustomer().getName();

                periods.add(String.format("%s to %s (includes 2-day buffer) - %s by %s",
                        bufferStart, bufferEnd, statusText, customerInfo));
            }
        }

        return periods;
    }
    /**
     * Load maintenance logs data from JSON file.
     */
    public static void loadMaintenanceLogs(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            parseMaintenanceLogsFromJson(jsonContent.toString());
        } catch (IOException e) {
            System.out.println("Failed to load maintenance logs: " + e.getMessage());
        }
    }

    /**
     * Save maintenance logs data to JSON file
     */
    public static void saveMaintenanceLogs(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            String jsonContent = convertMaintenanceLogsToJson();
            writer.println(jsonContent);
        } catch (IOException e) {
            System.out.println("Failed to save maintenance logs: " + e.getMessage());
        }
    }

    /**
     * Get severity level input with guidance
     */
    public static int getSeverityLevelWithGuidance(Scanner scanner) {
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
    /**
     * Set vehicle to maintenance mode and add multiple issue descriptions
     */
    public static void setVehicleToMaintenanceWithIssues(RentalSystem system, Vehicle vehicle, Scanner scanner) {
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
        
        // Add maintenance records for each issue
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
            
            // Add maintenance record
            addMaintenanceLog(vehicle.getId(), logType, issue, "ADMIN", severity, null);
            
            // High severity notifications sent by RentalSystem automatically
        }
        
        // Set vehicle status to maintenance
        vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
        
        // Save vehicle status change
        saveVehicles("vehicles.json");
        
        System.out.printf("\n[SUCCESS] Vehicle %d has been set to UNDER_MAINTENANCE with %d issue(s) recorded.\n", 
                         vehicle.getId(), issues.size());
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }
     /**
     * Internal class: Issue report
     */
    public static class IssueReport {
        final String description;
        final MaintenanceLogType logType;
        final int severity;
        
        IssueReport(String description, MaintenanceLogType logType, int severity) {
            this.description = description;
            this.logType = logType;
            this.severity = severity;
        }
    }
     /**
     * Get all maintenance logs
     */
    public static List<MaintenanceLog> getAllMaintenanceLogs() {
        List<MaintenanceLog> allLogs = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            allLogs.addAll(vehicle.getMaintenanceLogs());
        }
        return allLogs;
    }

    /**
     * Find maintenance log by ID
     */
    public static MaintenanceLog findMaintenanceLogById(int logId) {
        for (Vehicle vehicle : vehicles) {
            for (MaintenanceLog log : vehicle.getMaintenanceLogs()) {
                if (log.getId() == logId) {
                    return log;
                }
            }
        }
        return null;
    }

    /**
     * Get maintenance logs for a specific vehicle
     */
    public static List<MaintenanceLog> getMaintenanceLogsByVehicleId(int vehicleId) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle != null) {
            return vehicle.getMaintenanceLogs();
        }
        return new ArrayList<>();
    }
     /**
     * Parse maintenance records JSON data
     */
    private static void parseMaintenanceLogsFromJson(String json) {
        try {
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1); // Remove outer []

                String[] logObjects = splitJsonObjects(json);

                for (String logJson : logObjects) {
                    MaintenanceLog log = parseMaintenanceLogFromJson(logJson.trim());
                    if (log != null) {
                        // Find corresponding vehicle and add maintenance record
                        Vehicle vehicle = findVehicleById(log.getVehicleId());
                        if (vehicle != null) {
                            vehicle.addMaintenanceLogDirect(log);
                        }
                    }
                }
            }

            // After loading maintenance records, update the status of all vehicles
            for (Vehicle vehicle : vehicles) {
                if (vehicle.hasCriticalMaintenanceIssues() && vehicle.getStatus() == VehicleStatus.AVAILABLE) {
                    vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
                }
            }

            // Update the next ID for maintenance records
            List<MaintenanceLog> allLogs = getAllMaintenanceLogs();
            int maxId = 0;
            for (MaintenanceLog log : allLogs) {
                if (log.getId() > maxId) {
                    maxId = log.getId();
                }
            }
            if (maxId > 0) {
                MaintenanceLog.setNextId(maxId + 1);
            }
        } catch (Exception e) {
            System.out.println("Failed to parse maintenance logs JSON: " + e.getMessage());
        }
    }

    /**
     * Parse single maintenance record object
     */
    public static MaintenanceLog parseMaintenanceLogFromJson(String json) {
        try {
            int id = Integer.parseInt(RentalSystem.extractJsonValue(json, "id"));
            int vehicleId = Integer.parseInt(RentalSystem.extractJsonValue(json, "vehicleId"));
            String logTypeStr = RentalSystem.extractJsonValue(json, "logType");
            String description = RentalSystem.extractJsonValue(json, "description");
            String reportDateStr = RentalSystem.extractJsonValue(json, "reportDate");
            String completedDateStr = RentalSystem.extractJsonValue(json, "completedDate");
            double cost = Double.parseDouble(RentalSystem.extractJsonValue(json, "cost"));
            String statusStr = RentalSystem.extractJsonValue(json, "status");
            String reportedBy = RentalSystem.extractJsonValue(json, "reportedBy");
            String assignedTo = RentalSystem.extractJsonValue(json, "assignedTo");
            int severityLevel = Integer.parseInt(RentalSystem.extractJsonValue(json, "severityLevel"));

            // Create maintenance record object
            MaintenanceLog log = new MaintenanceLog(vehicleId,
                    MaintenanceLogType.valueOf(logTypeStr),
                    description, reportedBy, severityLevel);

            // Set other properties
            log.setId(id);
            log.setCost(cost);
            log.setStatus(MaintenanceStatus.valueOf(statusStr));
            log.setAssignedTo(assignedTo != null ? assignedTo : "");

            // Parse dates
            if (reportDateStr != null && !reportDateStr.isEmpty()) {
                log.setReportDate(java.time.LocalDateTime.parse(reportDateStr));
            }
            if (completedDateStr != null && !completedDateStr.isEmpty() && !completedDateStr.equals("null")) {
                log.setCompletedDate(java.time.LocalDateTime.parse(completedDateStr));
            }

            return log;
        } catch (Exception e) {
            System.out.println("Failed to parse maintenance log object: " + e.getMessage());
        }
        return null;
    }
    /**
     * Get list of vehicles needing maintenance
     */
    public static List<Vehicle> getVehiclesNeedingMaintenance() {
        List<Vehicle> needMaintenance = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.hasCriticalMaintenanceIssues() ||
                    vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE) {
                needMaintenance.add(vehicle);
            }
        }
        return needMaintenance;
    }
    /**
     * Find vehicle by ID
     */
    public static Vehicle findVehicleById(int id) {
        for (Vehicle v : vehicles) {
            if (v.getId() == id) {
                return v;
            }
        }
        return null;
    }

    /**
     * Split JSON objects (copied from RentalSystem)
     */
    public static String[] splitJsonObjects(String json) {
        List<String> objects = new ArrayList<>();
        int braceCount = 0;
        int start = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (braceCount == 0)
                    start = i;
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    objects.add(json.substring(start, i + 1));
                }
            }
        }

        return objects.toArray(new String[0]);
    }
}
