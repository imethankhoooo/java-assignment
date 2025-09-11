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

import static services.UtilityService.*;


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
            // 解析新格式的JSON字段
            String vehicleID = extractJsonValue(json, "vehicleID");
            String plateNo = extractJsonValue(json, "plateNo");
            String brand = extractJsonValue(json, "brand");
            String model = extractJsonValue(json, "model");
            String type = extractJsonValue(json, "type");
            String fuelType = extractJsonValue(json, "fuelType");
            String color = extractJsonValue(json, "color");
            
            String yearStr = extractJsonValue(json, "year");
            int year = Integer.parseInt(yearStr);
            
            String capacityStr = extractJsonValue(json, "capacity");
            double capacity = Double.parseDouble(capacityStr);
            
            String condition = extractJsonValue(json, "condition");
            
            String insuranceRateStr = extractJsonValue(json, "insuranceRate");
            double insuranceRate = Double.parseDouble(insuranceRateStr);
            
            String availability = extractJsonValue(json, "availability");
            
            String archivedStr = extractJsonValue(json, "archived");
            boolean archived = Boolean.parseBoolean(archivedStr);

            String priceStr = extractJsonValue(json, "basePrice");
            double basePrice = Double.parseDouble(priceStr);

            // Parse long term discounts
            Map<Integer, Double> discounts = new HashMap<>();
            String discountsJson = extractJsonObject(json, "longTermDiscounts");
            if (discountsJson != null && !discountsJson.equals("null")) {
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

            Vehicle vehicle = new Vehicle(vehicleID, plateNo, brand, model, type, fuelType, color, year, 
                             capacity, condition, insuranceRate, availability, basePrice, discounts);
            vehicle.setArchived(archived);
            return vehicle;
        } catch (Exception e) {
            System.out.println("Failed to parse vehicle object: " + e.getMessage());
        }
        return null;
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

    
    // Admin functions - Display all vehicles (Active and Archived)
    public static void displayAllVehicles() {
        List<Vehicle> vehicles = getVehicles();
        
        //Active Vehicles: Available / Rented
        System.out.println("\n╔═════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                        Active Vehicles                                                                      ║");
        System.out.println("╠═════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ %-13s %-12s %-10s %-10s %-13s %-10s %-10s %-15s %-12s %-12s %-15s %-11s ║%n",
                "VehicleID", "PlateNo", "Brand", "Model", "Type", "Fuel", "Color", "PurchaseYear", "Capacity", "Condition", "InsuranceRate", "Availability");
        System.out.println("╠═════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        
        for (Vehicle v : vehicles) {
            if (!v.isArchived()) {
                System.out.printf("║ %-13s %-12s %-10s %-10s %-13s %-10s %-10s %-15d %-12.1f %-12s %-15.2f %-12s ║%n",
                        v.getVehicleID(), v.getPlateNo(), v.getBrand(), v.getModel(), v.getType(), v.getFuelType(), v.getColor(), 
                        v.getYear(), v.getCapacity(), v.getCondition(), v.getInsuranceRate(), v.getAvailable());
            }
        }
        System.out.println("╚═════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╝\n");

        //Archived Vehicles
        System.out.println("╔═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                               Archived Vehicles                                                               ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ %-13s %-12s %-10s %-10s %-13s %-10s %-10s %-15s %-12s %-12s %-14s ║%n",
                "VehicleID", "PlateNo", "Brand", "Model", "Type", "Fuel", "Color", "PurchaseYear", "Capacity", "Condition", "InsuranceRate");
        System.out.println("╠═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╣");

        for (Vehicle v : vehicles) {
            if (v.isArchived()) {
                System.out.printf("║ %-13s %-12s %-10s %-10s %-13s %-10s %-10s %-15d %-12.1f %-12s %-14.2f ║%n",
                        v.getVehicleID(), v.getPlateNo(), v.getBrand(), v.getModel(), v.getType(), v.getFuelType(),
                        v.getColor(), v.getYear(), v.getCapacity(), v.getCondition(), v.getInsuranceRate());
            }
        }
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╝\n");
    }

    // Customer functionality: Display available vehicles
    public static void displayAvailableVehicles() {
        List<Vehicle> vehicles = getVehicles();
        
        // Sort vehicles by brand, then model, then type
        vehicles.sort(java.util.Comparator.comparing(Vehicle::getBrand, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Vehicle::getModel, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Vehicle::getType, String.CASE_INSENSITIVE_ORDER));
        
        System.out.println("\n╔═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                 Available Vehicles                                                ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ %-14s %-10s %-10s %-13s %-10s %-10s %-12s %-12s %-14s ║%n",
                "PlateNo", "Brand", "Model", "Type", "Fuel", "Color", "Capacity", "Condition", "InsuranceRate");
        System.out.println("╠═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════╣");

            for (Vehicle v : vehicles) {
            if (!v.isArchived() && v.getAvailable().equalsIgnoreCase("available")) {
                System.out.printf("║ %-14s %-10s %-10s %-13s %-10s %-10s %-12.1f %-12s %-14.2f ║%n",
                        v.getPlateNo(), v.getBrand(), v.getModel(), v.getType(), v.getFuelType(),
                        v.getColor(), v.getCapacity(), v.getCondition(), v.getInsuranceRate());
            }
        }
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════╝\n");
    }

    // Display search results in new format
    public static void displaySearchResults(List<Vehicle> vehicles) {
        if (vehicles.isEmpty()) {
            System.out.println("No vehicles found matching your search criteria.");
                return;
        }
        
        System.out.println("\n╔═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                        Search Results                                                                       ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ %-13s %-12s %-10s %-10s %-13s %-10s %-10s %-15s %-12s %-12s %-15s %-11s ║%n",
                "VehicleID", "PlateNo", "Brand", "Model", "Type", "Fuel", "Color", "PurchaseYear", "Capacity", "Condition", "InsuranceRate", "Availability");
        System.out.println("╠═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╣");

        for (Vehicle v : vehicles) {
            if (!v.isArchived()) { // Only show non-archived vehicles in search results
                System.out.printf("║ %-13s %-12s %-10s %-10s %-13s %-10s %-10s %-15d %-12.1f %-12s %-15.2f %-12s ║%n",
                        v.getVehicleID(), v.getPlateNo(), v.getBrand(), v.getModel(), v.getType(), v.getFuelType(), v.getColor(), 
                        v.getYear(), v.getCapacity(), v.getCondition(), v.getInsuranceRate(), v.getAvailable());
            }
        }
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╝\n");
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
        Vehicle newVehicle = new Vehicle(String.valueOf(id), "UNKNOWN", "Default", model, "CAR", "PETROL", "UNKNOWN", 2020, 1.5, "GOOD", 0.1, "available", basePrice, null);
        getVehicles().add(newVehicle);
        
        System.out.println("Vehicle added successfully!");
        System.out.println("ID: " + id + ", Model: " + model + ", Price: RM" + basePrice + "/day");
    }
    
    // Manage vehicle status
    public static void manageVehicleStatus(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    MANAGE VEHICLE STATUS                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        displayAllVehicles();
        
        System.out.print("\nEnter vehicle plate number to manage: ");
        String plateNo = scanner.nextLine().trim();
        Vehicle vehicle = findVehicleByPlateNo(plateNo);
        
        if (vehicle == null) {
            System.out.println("Vehicle not found.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
            
            System.out.printf("\nCurrent Status: %s\n", vehicle.getStatus());
            System.out.println("\nAvailable Actions:");
            System.out.println("1. Set to AVAILABLE");
            System.out.println("2. Set to OUT_OF_SERVICE");
            System.out.println("0. Cancel");
            System.out.print("Select action: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    vehicle.setStatus("available");
                    saveVehicles("vehicles.json");
                    System.out.println("Vehicle status changed to AVAILABLE.");
                    break;
                case "2":
                    vehicle.setStatus("out_of_service");
                    saveVehicles("vehicles.json");
                    System.out.println("Vehicle status changed to OUT_OF_SERVICE.");
                    break;
                case "0":
                    System.out.println("Operation cancelled.");
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Update vehicle information
     * @param system RentalSystem instance
     * @param scanner Scanner for user input
     */
    public static void updateVehicle(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      UPDATE VEHICLE                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        displayAllVehicles();
        
        System.out.print("\nEnter vehicle plate number to update: ");
        String plateNo = scanner.nextLine().trim();
        Vehicle vehicle = findVehicleByPlateNo(plateNo);
        
        if (vehicle == null) {
            System.out.println("Vehicle not found.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        System.out.println("\nCurrent Vehicle Information:");
        System.out.println(vehicle.toString());
        
        System.out.println("\nWhat would you like to update?");
        System.out.println("1. Base Price");
        System.out.println("2. Color");
        System.out.println("3. Condition");
        System.out.println("4. Insurance Rate");
        System.out.println("0. Cancel");
        System.out.print("Select option: ");
        
        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                System.out.print("Enter new base price per day: ");
                try {
                    double newPrice = Double.parseDouble(scanner.nextLine());
                    if (newPrice > 0) {
                        vehicle.setBasePrice(newPrice);
                        System.out.println("Base price updated successfully.");
                    } else {
                        System.out.println("Price must be positive.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid price format.");
                }
                break;
            case "2":
                System.out.print("Enter new color: ");
                String newColor = scanner.nextLine().trim();
                if (!newColor.isEmpty()) {
                    vehicle.setColor(newColor);
                    System.out.println("Color updated successfully.");
                }
                break;
            case "3":
                System.out.print("Enter new condition: ");
                String newCondition = scanner.nextLine().trim();
                if (!newCondition.isEmpty()) {
                    vehicle.setCondition(newCondition);
                    System.out.println("Condition updated successfully.");
                }
                break;
            case "4":
                System.out.print("Enter new insurance rate (0.0 - 1.0): ");
                try {
                    double newRate = Double.parseDouble(scanner.nextLine());
                    if (newRate >= 0 && newRate <= 1) {
                        vehicle.setInsuranceRate(newRate);
                        System.out.println("Insurance rate updated successfully.");
                    } else {
                        System.out.println("Insurance rate must be between 0.0 and 1.0.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid insurance rate format.");
                }
                break;
            case "0":
                System.out.println("Update cancelled.");
                return;
            default:
                System.out.println("Invalid option.");
                return;
        }
        
        saveVehicles("vehicles.json");
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Archive a vehicle
     * @param system RentalSystem instance
     * @param scanner Scanner for user input
     */
    public static void archiveVehicle(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      ARCHIVE VEHICLE                             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        // Show only active vehicles
        List<Vehicle> activeVehicles = vehicles.stream()
                .filter(v -> !v.isArchived())
                .collect(java.util.stream.Collectors.toList());
        
        if (activeVehicles.isEmpty()) {
            System.out.println("No active vehicles to archive.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        System.out.println("Active Vehicles:");
        for (Vehicle v : activeVehicles) {
            System.out.println(v.toString());
        }
        
        System.out.print("\nEnter vehicle plate number to archive: ");
        String plateNo = scanner.nextLine().trim();
        Vehicle vehicle = findVehicleByPlateNo(plateNo);
        
        if (vehicle == null || vehicle.isArchived()) {
            System.out.println("Vehicle not found or already archived.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        // Check if vehicle has future bookings
        if (vehicle.hasFutureBookings()) {
            System.out.println("Warning: This vehicle has future bookings!");
            System.out.println("Archiving will cancel all future bookings.");
            System.out.print("Are you sure you want to continue? (yes/no): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (!confirm.equals("yes") && !confirm.equals("y")) {
                System.out.println("Archive cancelled.");
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
                return;
            }
            
            // Clear all bookings
            vehicle.clearAllBookings();
        }
        
        vehicle.setArchived(true);
        vehicle.setStatus("archived");
        saveVehicles("vehicles.json");
        
        System.out.println("Vehicle archived successfully.");
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Restore an archived vehicle
     * @param system RentalSystem instance
     * @param scanner Scanner for user input
     */
    public static void restoreArchived(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    RESTORE ARCHIVED VEHICLE                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        // Show only archived vehicles
        List<Vehicle> archivedVehicles = vehicles.stream()
                .filter(Vehicle::isArchived)
                .collect(java.util.stream.Collectors.toList());
        
        if (archivedVehicles.isEmpty()) {
            System.out.println("No archived vehicles to restore.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        System.out.println("Archived Vehicles:");
        for (Vehicle v : archivedVehicles) {
            System.out.printf("ID: %s, Plate: %s, %s %s [ARCHIVED]\n", 
                    v.getVehicleID(), v.getPlateNo(), v.getBrand(), v.getModel());
        }
        
        System.out.print("\nEnter vehicle plate number to restore: ");
        String plateNo = scanner.nextLine().trim();
        Vehicle vehicle = findVehicleByPlateNo(plateNo);
        
        if (vehicle == null || !vehicle.isArchived()) {
            System.out.println("Vehicle not found or not archived.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        
        vehicle.setArchived(false);
        vehicle.setStatus("available");
        saveVehicles("vehicles.json");
        
        System.out.println("Vehicle restored successfully and set to AVAILABLE status.");
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
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
            json.append("    \"vehicleID\": \"").append(vehicle.getVehicleID()).append("\",\n");
            json.append("    \"plateNo\": \"").append(vehicle.getPlateNo()).append("\",\n");
            json.append("    \"brand\": \"").append(vehicle.getBrand()).append("\",\n");
            json.append("    \"model\": \"").append(vehicle.getModel()).append("\",\n");
            json.append("    \"type\": \"").append(vehicle.getType()).append("\",\n");
            json.append("    \"fuelType\": \"").append(vehicle.getFuelType()).append("\",\n");
            json.append("    \"color\": \"").append(vehicle.getColor()).append("\",\n");
            json.append("    \"year\": ").append(vehicle.getYear()).append(",\n");
            json.append("    \"capacity\": ").append(vehicle.getCapacity()).append(",\n");
            json.append("    \"condition\": \"").append(vehicle.getCondition()).append("\",\n");
            json.append("    \"insuranceRate\": ").append(vehicle.getInsuranceRate()).append(",\n");
            json.append("    \"availability\": \"").append(vehicle.getAvailable()).append("\",\n");
            json.append("    \"archived\": ").append(vehicle.isArchived()).append(",\n");
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
     * Find vehicle by ID (legacy method for backward compatibility)
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
     * Find vehicle by plate number (primary method)
     */
    public static Vehicle findVehicleByPlateNo(String plateNo) {
        if (plateNo == null || plateNo.trim().isEmpty()) {
            return null;
        }
        for (Vehicle v : vehicles) {
            if (v.getPlateNo().equalsIgnoreCase(plateNo.trim())) {
                return v;
            }
        }
        return null;
    }


    /**
     * Search vehicles by car plate number
     * @param carPlate Car plate number to search
     * @return List of vehicles matching the car plate
     */
    public static List<Vehicle> searchVehiclesByCarPlate(String carPlate) {
        List<Vehicle> results = new ArrayList<>();
        if (carPlate == null || carPlate.trim().isEmpty()) {
            return results;
        }

        String searchTerm = carPlate.toLowerCase().trim();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getCarPlate().toLowerCase().contains(searchTerm)) {
                results.add(vehicle);
            }
        }
        return results;
    }

    /**
     * Search vehicles by brand
     * @param brand Brand name to search
     * @return List of vehicles matching the brand
     */
    public static List<Vehicle> searchVehiclesByBrand(String brand) {
        List<Vehicle> results = new ArrayList<>();
        if (brand == null || brand.trim().isEmpty()) {
            return results;
        }

        String searchTerm = brand.toLowerCase().trim();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getBrand().toLowerCase().contains(searchTerm)) {
                results.add(vehicle);
            }
        }
        return results;
    }

    /**
     * Search vehicles by model
     * @param model Model name to search
     * @return List of vehicles matching the model
     */
    public static List<Vehicle> searchVehiclesByModel(String model) {
        List<Vehicle> results = new ArrayList<>();
        if (model == null || model.trim().isEmpty()) {
            return results;
        }

        String searchTerm = model.toLowerCase().trim();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getModel().toLowerCase().contains(searchTerm)) {
                results.add(vehicle);
            }
        }
        return results;
    }

    /**
     * Search vehicles by vehicle type
     * @param vehicleType Type of vehicle to search
     * @return List of vehicles matching the type
     */
    public static List<Vehicle> searchVehiclesByType(VehicleType vehicleType) {
        List<Vehicle> results = new ArrayList<>();
        if (vehicleType == null) {
            return results;
        }

        for (Vehicle vehicle : vehicles) {
            if (vehicleType != null && vehicleType.toString().equalsIgnoreCase(vehicle.getVehicleType())) {
                results.add(vehicle);
            }
        }
        return results;
    }

    /**
     * Search vehicles by fuel type
     * @param fuelType Fuel type to search
     * @return List of vehicles matching the fuel type
     */
    public static List<Vehicle> searchVehiclesByFuelType(FuelType fuelType) {
        List<Vehicle> results = new ArrayList<>();
        if (fuelType == null) {
            return results;
        }

        for (Vehicle vehicle : vehicles) {
            if (fuelType != null && fuelType.toString().equalsIgnoreCase(vehicle.getFuelType())) {
                results.add(vehicle);
            }
        }
        return results;
    }

    /**
     * Comprehensive search - can search by multiple conditions
     * @param carPlate Car plate filter
     * @param brand Brand filter
     * @param model Model filter
     * @param vehicleType Vehicle type filter
     * @param fuelType Fuel type filter
     * @param onlyAvailable Only show available vehicles
     * @return List of vehicles matching all specified criteria
     */
    public static List<Vehicle> searchVehicles(String carPlate, String brand, String model,
            VehicleType vehicleType, FuelType fuelType, boolean onlyAvailable) {
        List<Vehicle> results = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            boolean matches = true;

            // If specified to only show available vehicles
            if (onlyAvailable && !"available".equalsIgnoreCase(vehicle.getStatus())) {
                continue;
            }

            // Check car plate
            if (carPlate != null && !carPlate.trim().isEmpty()) {
                if (!vehicle.getCarPlate().toLowerCase().contains(carPlate.toLowerCase().trim())) {
                    matches = false;
                }
            }

            // Check brand
            if (brand != null && !brand.trim().isEmpty()) {
                if (!vehicle.getBrand().toLowerCase().contains(brand.toLowerCase().trim())) {
                    matches = false;
                }
            }

            // Check model
            if (model != null && !model.trim().isEmpty()) {
                if (!vehicle.getModel().toLowerCase().contains(model.toLowerCase().trim())) {
                    matches = false;
                }
            }

            // Check vehicle type
            if (vehicleType != null && !vehicleType.toString().equalsIgnoreCase(vehicle.getVehicleType())) {
                matches = false;
            }

            // Check fuel type
            if (fuelType != null && !fuelType.toString().equalsIgnoreCase(vehicle.getFuelType())) {
                matches = false;
            }

            if (matches) {
                results.add(vehicle);
            }
        }
        return results;
    }

    /**
     * Quick search for vehicles by keyword
     * @param keyword Search keyword
     * @param onlyAvailable Only show available vehicles
     * @return List of vehicles matching the keyword
     */
    public static List<Vehicle> quickSearchVehicles(String keyword, boolean onlyAvailable) {
        List<Vehicle> results = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }

        String searchTerm = keyword.toLowerCase().trim();

        for (Vehicle vehicle : vehicles) {
            // Check if only available vehicles should be shown
            if (onlyAvailable && !"available".equalsIgnoreCase(vehicle.getStatus())) {
                continue;
            }

            // Search in brand, model, car plate, and type
            boolean matches = vehicle.getBrand().toLowerCase().contains(searchTerm) ||
                             vehicle.getModel().toLowerCase().contains(searchTerm) ||
                             vehicle.getCarPlate().toLowerCase().contains(searchTerm) ||
                             vehicle.getVehicleType().toString().toLowerCase().contains(searchTerm) ||
                             vehicle.getFuelType().toString().toLowerCase().contains(searchTerm);

            if (matches) {
                results.add(vehicle);
            }
        }
        return results;
    }

    /**
     * Get all available vehicles
     * @return List of available vehicles
     */
    public static List<Vehicle> getAvailableVehicles() {
        List<Vehicle> availableVehicles = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            if ("available".equalsIgnoreCase(vehicle.getStatus())) {
                availableVehicles.add(vehicle);
            }
        }
        return availableVehicles;
    }

    /**
     * Find vehicle by ID (search version)
     * @param id Vehicle ID to find
     * @return Vehicle object if found, null otherwise
     */
    public static Vehicle findVehicleByIdForSearch(int id) {
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getId() == id) {
                return vehicle;
            }
        }
        return null;
    }

    /**
     * Get conflict details for a vehicle booking
     * @param vehicleId Vehicle ID
     * @param startDate Start date
     * @param endDate End date
     * @return Conflict details string
     */
    public static String getConflictDetails(int vehicleId, LocalDate startDate, LocalDate endDate) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle == null) {
            return "Vehicle not found.";
        }

        List<String> conflicts = vehicle.getUnavailablePeriods();
        if (conflicts.isEmpty()) {
            return "No conflicts found.";
        }

        StringBuilder details = new StringBuilder();
        details.append("Vehicle ").append(vehicle.getBrand()).append(" ").append(vehicle.getModel())
               .append(" (").append(vehicle.getCarPlate()).append(") has the following bookings:\n");

        for (String conflict : conflicts) {
            details.append("- ").append(conflict).append("\n");
        }

        return details.toString();
    }

    /**
     * Check if a vehicle has booking conflicts
     * @param vehicleId Vehicle ID
     * @param startDate Start date
     * @param endDate End date
     * @return true if there are conflicts, false otherwise
     */
    public static boolean hasConflict(int vehicleId, LocalDate startDate, LocalDate endDate) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle == null) {
            return true; // Consider vehicle not found as a conflict
        }

        return !vehicle.isAvailable(startDate, endDate);
    }

    /**
     * Check if a vehicle matches search terms
     * @param vehicle Vehicle to check
     * @param term Search term
     * @return true if vehicle matches the search term
     */
    public static boolean matchesVehicle(Vehicle vehicle, String term) {
        if (term == null || term.trim().isEmpty()) {
            return true;
        }

        term = term.toUpperCase().trim();

        return vehicle.getBrand().toUpperCase().contains(term) ||
                vehicle.getModel().toUpperCase().contains(term) ||
                vehicle.getCarPlate().toUpperCase().contains(term) ||
                vehicle.getVehicleType().toString().toUpperCase().contains(term) ||
                vehicle.getFuelType().toString().toUpperCase().contains(term);
    }



}
