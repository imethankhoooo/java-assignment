package services;

import java.util.*;

import enums.*;
import models.*;
import main.*;

import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Main system class: manages accounts, vehicles, rentals and core operations
 */
public class RentalSystem {
    private List<Rental> rentals;
    private NotificationService notificationService;
    private TicketService ticketService;
    private int nextRentalId = 1;
    public boolean shouldExit = false;

    public RentalSystem() {
        rentals = new ArrayList<>();
        notificationService = new NotificationService();
        ticketService = new TicketService();
    }

    // Account management moved to AccountService

    // Account saving moved to AccountService

    /**
     * Load rental data from JSON file
     */
    public void loadRentals(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            rentals = parseRentalsFromJson(jsonContent.toString());
            if (rentals == null)
                rentals = new ArrayList<>();

            System.out.println("Loaded rentals: " + rentals.size());
            // Update next rental ID
            for (Rental r : rentals) {
                if (r.getId() >= nextRentalId) {
                    nextRentalId = r.getId() + 1;
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to load rental data: " + e.getMessage());
            rentals = new ArrayList<>();
        }
    }

    /**
     * Save rental data to JSON file
     */
    public void saveRentals(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            String jsonContent = convertRentalsToJson();
            writer.println(jsonContent);
        } catch (IOException e) {
            System.out.println("Failed to save rental data: " + e.getMessage());
        }
    }

    // Account parsing moved to AccountService

    /**
     * Parse rental JSON data
     */
    private List<Rental> parseRentalsFromJson(String json) {
        List<Rental> rentalList = new ArrayList<>();
        try {
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);

                String[] rentalObjects = splitJsonObjects(json);

                for (String rentalJson : rentalObjects) {
                    Rental rental = parseRentalFromJson(rentalJson.trim());
                    if (rental != null) {
                        rentalList.add(rental);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to parse rental JSON: " + e.getMessage());
        }
        return rentalList;
    }

    /**
     * Parse single rental object
     */
    private Rental parseRentalFromJson(String json) {
        try {
            int id = Integer.parseInt(extractJsonValue(json, "id"));
            double fee = Double.parseDouble(extractJsonValue(json, "fee"));
            String actualFeeStr = extractJsonValue(json, "actualFee");
            double actualFee = (actualFeeStr != null) ? Double.parseDouble(actualFeeStr) : 0.0;
            boolean insurance = Boolean.parseBoolean(extractJsonValue(json, "insurance"));
            String statusStr = extractJsonValue(json, "status");
            String startDateStr = extractJsonValue(json, "startDate");
            String endDateStr = extractJsonValue(json, "endDate");

            RentalStatus status = RentalStatus.valueOf(statusStr);
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);

            // Parse customer information
            String customerJson = extractJsonObject(json, "customer");
            Customer customer = parseCustomerFromJson(customerJson);

            // Parse vehicle information
            String vehicleJson = extractJsonObject(json, "vehicle");
            Vehicle vehicle = vehicleService.parseVehicleFromJson(vehicleJson);

            // Parse username, if not set to null
            String username = extractJsonValue(json, "username");

            if (customer != null && vehicle != null) {
                Rental rental = new Rental(id, customer, vehicle, startDate, endDate, status, fee, insurance, username);
                rental.setActualFee(actualFee);

                // Parse reminder record
                String dueSoonReminderStr = extractJsonValue(json, "dueSoonReminderSent");
                if (dueSoonReminderStr != null) {
                    rental.setDueSoonReminderSent(Boolean.parseBoolean(dueSoonReminderStr));
                }

                String overdueReminderStr = extractJsonValue(json, "overdueReminderSent");
                if (overdueReminderStr != null) {
                    rental.setOverdueReminderSent(Boolean.parseBoolean(overdueReminderStr));
                }

                // Parse ticket information
                String ticketJson = extractJsonObject(json, "ticket");
                if (ticketJson != null && !ticketJson.equals("null")) {
                    loadTicketFromJson(ticketJson, rental);
                }

                return rental;
            }
        } catch (Exception e) {
            System.out.println("Failed to parse rental object: " + e.getMessage());
        }
        return null;
    }

    /**
     * Parse customer object
     */
    private models.Customer parseCustomerFromJson(String json) {
        try {
            int id = Integer.parseInt(extractJsonValue(json, "id"));
            String name = extractJsonValue(json, "name");
            String contact = extractJsonValue(json, "contact");
            return new models.Customer(id, name, contact);
        } catch (Exception e) {
            System.out.println("Failed to parse client object: " + e.getMessage());
        }
        return null;
    }

    /**
     * Load ticket information from JSON to TicketService
     */
    private void loadTicketFromJson(String ticketJson, Rental rental) {
        try {
            String ticketId = extractJsonValue(ticketJson, "ticketId");
            String pickupLocation = extractJsonValue(ticketJson, "pickupLocation");
            String specialInstructions = extractJsonValue(ticketJson, "specialInstructions");
            String isUsedStr = extractJsonValue(ticketJson, "isUsed");

            if (ticketId != null) {
                // Create ticket object
                Ticket ticket = new Ticket(rental);

                // Set ticket properties (need to modify Ticket class to support these setters)
                ticket.setPickupLocation(
                        pickupLocation != null ? pickupLocation : "Main Office - Vehicle Rental Center");
                ticket.setSpecialInstructions(specialInstructions != null ? specialInstructions
                        : "Please bring valid ID and this ticket for vehicle pickup");

                if (isUsedStr != null && Boolean.parseBoolean(isUsedStr)) {
                    ticket.markAsUsed();
                }

                // Load ticket into TicketService
                ticketService.loadTicket(ticket);

            }
        } catch (Exception e) {
            System.out.println("Failure to parse ticket information: " + e.getMessage());
        }
    }

    /**
     * Convert rental data to JSON format
     */
    private String convertRentalsToJson() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");

        for (int i = 0; i < rentals.size(); i++) {
            Rental rental = rentals.get(i);
            json.append("  {\n");
            json.append("    \"id\": ").append(rental.getId()).append(",\n");
            json.append("    \"fee\": ").append(rental.getTotalFee()).append(",\n");
            json.append("    \"actualFee\": ").append(rental.getActualFee()).append(",\n");
            json.append("    \"insurance\": ").append(rental.isInsuranceSelected()).append(",\n");
            json.append("    \"status\": \"").append(rental.getStatus()).append("\",\n");
            json.append("    \"startDate\": \"").append(rental.getStartDate()).append("\",\n");
            json.append("    \"endDate\": \"").append(rental.getEndDate()).append("\",\n");

            // Customer information
            models.Customer customer = rental.getCustomer();
            json.append("    \"customer\": {\n");
            json.append("      \"id\": ").append(customer.getId()).append(",\n");
            json.append("      \"name\": \"").append(customer.getName()).append("\",\n");
            json.append("      \"contact\": \"").append(customer.getContact()).append("\"\n");
            json.append("    },\n");

            // Vehicle information
            Vehicle vehicle = rental.getVehicle();
            json.append("    \"vehicle\": {\n");
            json.append("      \"id\": ").append(vehicle.getId()).append(",\n");
            json.append("      \"brand\": \"").append(vehicle.getBrand()).append("\",\n");
            json.append("      \"model\": \"").append(vehicle.getModel()).append("\",\n");
            json.append("      \"carPlate\": \"").append(vehicle.getCarPlate()).append("\",\n");
            json.append("      \"vehicleType\": \"").append(vehicle.getVehicleType()).append("\",\n");
            json.append("      \"fuelType\": \"").append(vehicle.getFuelType()).append("\",\n");
            json.append("      \"status\": \"").append(vehicle.getStatus()).append("\",\n");
            json.append("      \"insuranceRate\": ").append(vehicle.getInsuranceRate()).append(",\n");
            json.append("      \"basePrice\": ").append(vehicle.getBasePrice()).append(",\n");
            json.append("      \"longTermDiscounts\": {");

            Map<Integer, Double> discounts = vehicle.getLongTermDiscounts();
            if (discounts != null && !discounts.isEmpty()) {
                int count = 0;
                for (Map.Entry<Integer, Double> entry : discounts.entrySet()) {
                    if (count > 0)
                        json.append(",");
                    json.append("\"").append(entry.getKey()).append("\": ").append(entry.getValue());
                    count++;
                }
            }
            json.append("}\n");
            json.append("    },\n");

            // Add username field
            json.append("    \"username\": \"")
                    .append(Main.escapeJson(rental.getUsername() != null ? rental.getUsername() : "")).append("\",\n");
            json.append("    \"dueSoonReminderSent\": ").append(rental.isDueSoonReminderSent()).append(",\n");
            json.append("    \"overdueReminderSent\": ").append(rental.isOverdueReminderSent()).append(",\n");

            // Add ticket information field
            json.append("    \"ticket\": ");
            Ticket ticket = ticketService.getTicketByRentalId(rental.getId());
            if (ticket != null) {
                json.append("{\n");
                json.append("      \"ticketId\": \"").append(ticket.getTicketId()).append("\",\n");
                json.append("      \"generatedTime\": \"").append(ticket.getGeneratedTime()).append("\",\n");
                json.append("      \"pickupLocation\": \"").append(ticket.getPickupLocation()).append("\",\n");
                json.append("      \"specialInstructions\": \"").append(ticket.getSpecialInstructions())
                        .append("\",\n");
                json.append("      \"isUsed\": ").append(ticket.isUsed()).append("\n");
                json.append("    }");
            } else {
                json.append("null");
            }
            json.append("\n");
            json.append("  }");

            if (i < rentals.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("]");
        return json.toString();
    }

    /**
     * Extract value from JSON string by specified key
     */
    public static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1)
            return null;

        startIndex += searchKey.length();

        // Skip spaces
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }

        if (startIndex >= json.length())
            return null;

        // Check value type
        char firstChar = json.charAt(startIndex);
        if (firstChar == '"') {
            // String value
            startIndex++; // Skip start quote
            int endIndex = json.indexOf('"', startIndex);
            if (endIndex == -1)
                return null;
            return json.substring(startIndex, endIndex);
        } else {
            // Number or boolean value
            int endIndex = startIndex;
            while (endIndex < json.length() &&
                    json.charAt(endIndex) != ',' &&
                    json.charAt(endIndex) != '}' &&
                    json.charAt(endIndex) != ']' &&
                    !Character.isWhitespace(json.charAt(endIndex))) {
                endIndex++;
            }
            return json.substring(startIndex, endIndex);
        }
    }

    /**
     * Extracts the object value of a specified key from a JSON string
     */
    public static String extractJsonObject(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1)
            return null;

        startIndex += searchKey.length();

        // Skip Space
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }

        if (startIndex >= json.length() || json.charAt(startIndex) != '{')
            return null;

        // Finding matching closing brackets
        int braceCount = 0;
        int endIndex = startIndex;
        while (endIndex < json.length()) {
            char c = json.charAt(endIndex);
            if (c == '{')
                braceCount++;
            else if (c == '}')
                braceCount--;

            endIndex++;
            if (braceCount == 0)
                break;
        }

        return json.substring(startIndex, endIndex);
    }

    /**
     * Splitting objects in a JSON array
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

    /**
     * Login validation, returns Account object or null
     */
    // Login moved to AccountService

    // Password methods moved to AccountService

    // Account lookup moved to AccountService

    // Customer management moved to separate service

    /**
     * Check for rental conflicts (same vehicle, overlapping dates with buffer) and
     * return conflict details
     */
    public String getConflictDetails(int vehicleId, LocalDate startDate, LocalDate endDate) {
        for (Rental r : rentals) {
            if (r.getVehicle().getId() == vehicleId &&
                    (r.getStatus() == RentalStatus.ACTIVE || r.getStatus() == RentalStatus.PENDING)) {

                // Include 2-day buffer periods
                LocalDate bufferStart = r.getStartDate().minusDays(2);
                LocalDate bufferEnd = r.getEndDate().plusDays(2);

                // Check date overlap with buffer
                if (!(endDate.isBefore(bufferStart) || startDate.isAfter(bufferEnd))) {
                    String customerInfo = r.getCustomer().getName();
                    String statusText = r.getStatus() == RentalStatus.ACTIVE ? "ACTIVE" : "PENDING";
                    return String.format(
                            "Conflict with %s rental by %s (ID: %d) from %s to %s (with 2-day buffer: %s to %s)",
                            statusText, customerInfo, r.getId(),
                            r.getStartDate(), r.getEndDate(), bufferStart, bufferEnd);
                }
            }
        }
        return null; // No conflict
    }

    /**
     * Check for rental conflicts (same vehicle, overlapping dates with buffer)
     */
    public boolean hasConflict(int vehicleId, LocalDate startDate, LocalDate endDate) {
        return getConflictDetails(vehicleId, startDate, endDate) != null;
    }

    /**
     * Calculate rental fee with insurance and long-term discounts
     */
    public static double calculateRentalFee(Vehicle vehicle, LocalDate startDate, LocalDate endDate,
            boolean insurance) {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double baseRate = vehicle.getBasePrice(); // Use vehicle's base price
        double totalFee = baseRate * days;

        // Apply long-term discount
        double discount = 0.0;
        Map<Integer, Double> discounts = vehicle.getLongTermDiscounts();
        if (discounts != null) {
            for (Map.Entry<Integer, Double> entry : discounts.entrySet()) {
                if (days >= entry.getKey()) {
                    discount = Math.max(discount, entry.getValue());
                }
            }
        }
        totalFee = totalFee * (1 - discount);

        // Add insurance if selected
        if (insurance) {
            totalFee += totalFee * vehicle.getInsuranceRate();
        }

        return totalFee;
    }

    /**
     * Create a new rental
     */
    public Rental createRental(Customer customer, Vehicle vehicle, LocalDate startDate, LocalDate endDate,
            boolean insurance, String username) {
        double fee = calculateRentalFee(vehicle, startDate, endDate, insurance);
        Rental rental = new Rental(nextRentalId++, customer, vehicle, startDate, endDate, RentalStatus.PENDING, fee,
                insurance, username);
        rentals.add(rental);

        // Update vehicle status to reserved
        vehicle.setStatus(VehicleStatus.RESERVED);

        // Also update the vehicle in vehicleService to keep data in sync
        Vehicle vehicleInService = vehicleService.findVehicleById(vehicle.getId());
        if (vehicleInService != null) {
            vehicleInService.setStatus(VehicleStatus.RESERVED);
        }

        // Send lease confirmation notice
        notificationService.sendRentalConfirmation(username, vehicle.getModel(),
                startDate.toString(), endDate.toString(), fee);

        saveRentals("rentals.json"); // Save rental record immediately
        vehicleService.saveVehicles("vehicles.json"); // Save vehicle status change
        return rental;
    }

    /**
     * Approve a rental (admin function)
     */
    public boolean approveRental(int rentalId) {
        Rental rental = findRentalById(rentalId);
        if (rental != null && rental.getStatus() == RentalStatus.PENDING) {
            rental.setStatus(RentalStatus.ACTIVE);
            rental.getVehicle().setStatus(VehicleStatus.RENTED);

            // Generate ticket for approved rental
            Ticket ticket = ticketService.generateTicket(rental);

            // Generate PDF ticket
            PdfTicketService pdfTicketService = new PdfTicketService();
            byte[] pdfTicket = pdfTicketService.generatePdfTicket(ticket);

            if (pdfTicket != null) {
                // Send rental approval notification with PDF ticket
                notificationService.sendRentalApprovalWithPdfTicket(rental.getUsername(),
                        rental.getVehicle().getModel(),
                        String.valueOf(rental.getId()),
                        ticket.getTicketId(),
                        pdfTicket);

                System.out.println(" PDF ticket generated and sent to customer email!");
            } else {
                // Fallback to regular ticket notification
                notificationService.sendRentalApprovalWithTicket(rental.getUsername(),
                        rental.getVehicle().getModel(),
                        String.valueOf(rental.getId()),
                        ticket.getTicketId());

                System.out.println(" PDF generation failed, sent regular ticket notification");
            }

            // Display ticket information to admin
            System.out.println("\n=== Ticket Generated ===");
            ticket.displayTicket();

            // Save data to JSON file
            saveRentals("rentals.json");
            vehicleService.saveVehicles("vehicles.json"); // Save vehicle status change

            return true;
        }
        return false;
    }

    /**
     * Cancel a rental
     */
    public boolean cancelRental(int rentalId) {
        return cancelRental(rentalId, "No reason provided");
    }

    public boolean cancelRental(int rentalId, String reason) {
        Rental rental = findRentalById(rentalId);
        if (rental != null && rental.getStatus() == RentalStatus.PENDING) {
            rental.setStatus(RentalStatus.CANCELLED);

            // Remove this booking from the vehicle booking list
            Vehicle vehicle = rental.getVehicle();
            vehicle.removeBooking(rental.getStartDate(), rental.getEndDate());

            // Set vehicle status based on future bookings
            if (vehicle.hasFutureBookings()) {
                vehicle.setStatus(VehicleStatus.RESERVED);
            } else {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
            }

            // Save data to JSON file immediately
            saveRentals("rentals.json");
            vehicleService.saveVehicles("vehicles.json"); // Save vehicle status change

            return true;
        }
        return false;
    }

    /**
     * Return a vehicle
     */
    public boolean returnVehicle(int rentalId) {
        Rental rental = findRentalById(rentalId);
        if (rental != null && rental.getStatus() == RentalStatus.ACTIVE) {
            // Calculate actual fee based on actual return date
            double actualFee = calculateActualRentalFee(rental);
            rental.setActualFee(actualFee);

            rental.setStatus(RentalStatus.RETURNED);

            // Remove this booking from the vehicle booking list
            Vehicle vehicle = rental.getVehicle();
            vehicle.removeBooking(rental.getStartDate(), rental.getEndDate());

            // Set vehicle status based on future bookings
            if (vehicle.hasFutureBookings()) {
                vehicle.setStatus(VehicleStatus.RESERVED);
            } else {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
            }

            saveRentals("rentals.json");
            vehicleService.saveVehicles("vehicles.json"); // Save vehicle status change
            return true;
        }
        return false;
    }

    /**
     * Calculate actual rental fee based on actual return date
     */
    public static double calculateActualRentalFee(Rental rental) {
        LocalDate actualEndDate = LocalDate.now();
        LocalDate originalStartDate = rental.getStartDate();

        // Use the later of original end date or actual return date
        LocalDate effectiveEndDate = actualEndDate.isAfter(rental.getEndDate()) ? actualEndDate : rental.getEndDate();

        return calculateRentalFee(rental.getVehicle(), originalStartDate, effectiveEndDate,
                rental.isInsuranceSelected());
    }

    /**
     * Find rental by ID
     */
    public Rental findRentalById(int id) {
        for (Rental r : rentals) {
            if (r.getId() == id) {
                return r;
            }
        }
        return null;
    }

    /**
     * Find rental by ID (static version for backward compatibility)
     */
    public static Rental findRentalByIdStatic(List<Rental> rentalList, int id) {
        for (Rental r : rentalList) {
            if (r.getId() == id) {
                return r;
            }
        }
        return null;
    }

    /**
     * Get rentals by customer name
     */
    public List<Rental> getRentalsByCustomer(String customerName) {
        List<Rental> result = new ArrayList<>();
        for (Rental r : rentals) {
            if (r.getCustomer().getName().equals(customerName)) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Get rentals by username (for logged in user)
     */
    public List<Rental> getRentalsByUsername(String username) {
        List<Rental> result = new ArrayList<>();
        for (Rental r : rentals) {
            if (r.getUsername() != null && r.getUsername().equals(username)) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Get all pending rentals (for admin approval)
     */
    public List<Rental> getPendingRentals() {
        List<Rental> result = new ArrayList<>();
        for (Rental r : rentals) {
            if (r.getStatus() == RentalStatus.PENDING) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Get all active rentals
     */
    public List<Rental> getActiveRentals() {
        List<Rental> result = new ArrayList<>();
        for (Rental r : rentals) {
            if (r.getStatus() == RentalStatus.ACTIVE) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Search vehicles - by car plate
     */
    public List<Vehicle> searchVehiclesByCarPlate(String carPlate) {
        List<Vehicle> results = new ArrayList<>();
        String searchTerm = carPlate.toLowerCase().trim();

        for (Vehicle vehicle : vehicleService.getVehicles()) {
            if (vehicle.getCarPlate().toLowerCase().contains(searchTerm)) {
                results.add(vehicle);
            }
        }
        return results;
    }

    /**
     * Search vehicles - by brand
     */
    public List<Vehicle> searchVehiclesByBrand(String brand) {
        List<Vehicle> results = new ArrayList<>();
        String searchTerm = brand.toLowerCase().trim();

        for (Vehicle vehicle : vehicleService.getVehicles()) {
            if (vehicle.getBrand().toLowerCase().contains(searchTerm)) {
                results.add(vehicle);
            }
        }
        return results;
    }

    /**
     * Search vehicles - by model
     */
    public List<Vehicle> searchVehiclesByModel(String model) {
        List<Vehicle> results = new ArrayList<>();
        String searchTerm = model.toLowerCase().trim();

        for (Vehicle vehicle : vehicleService.getVehicles()) {
            if (vehicle.getModel().toLowerCase().contains(searchTerm)) {
                results.add(vehicle);
            }
        }
        return results;
    }

    /**
     * Search vehicles - by vehicle type
     */
    public List<Vehicle> searchVehiclesByType(VehicleType vehicleType) {
        List<Vehicle> results = new ArrayList<>();

        for (Vehicle vehicle : vehicleService.getVehicles()) {
            if (vehicle.getVehicleType() == vehicleType) {
                results.add(vehicle);
            }
        }
        return results;
    }

    /**
     * Search vehicles - by fuel type
     */
    public List<Vehicle> searchVehiclesByFuelType(FuelType fuelType) {
        List<Vehicle> results = new ArrayList<>();

        for (Vehicle vehicle : vehicleService.getVehicles()) {
            if (vehicle.getFuelType() == fuelType) {
                results.add(vehicle);
            }
        }
        return results;
    }

    /**
     * Comprehensive search - can search by multiple conditions
     */
    public List<Vehicle> searchVehicles(String carPlate, String brand, String model,
            VehicleType vehicleType, FuelType fuelType, boolean onlyAvailable) {
        List<Vehicle> results = new ArrayList<>();

        for (Vehicle vehicle : vehicleService.getVehicles()) {
            boolean matches = true;

            // If specified to only show available vehicles
            if (onlyAvailable && vehicle.getStatus() != VehicleStatus.AVAILABLE) {
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
            if (vehicleType != null && vehicle.getVehicleType() != vehicleType) {
                matches = false;
            }

            // Check fuel type
            if (fuelType != null && vehicle.getFuelType() != fuelType) {
                matches = false;
            }

            if (matches) {
                results.add(vehicle);
            }
        }

        return results;
    }

    /**
     * Quick search - search keywords in car plate, brand and model
     */
    public List<Vehicle> quickSearchVehicles(String keyword, boolean onlyAvailable) {
        List<Vehicle> results = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return onlyAvailable ? getAvailableVehicles() : vehicleService.getVehicles();
        }

        String searchTerm = keyword.toLowerCase().trim();

        for (Vehicle vehicle : vehicleService.getVehicles()) {
            // If specified to only show available vehicles
            if (onlyAvailable && vehicle.getStatus() != VehicleStatus.AVAILABLE) {
                continue;
            }

            // Search in car plate, brand and model
            if (vehicle.getCarPlate().toLowerCase().contains(searchTerm) ||
                    vehicle.getBrand().toLowerCase().contains(searchTerm) ||
                    vehicle.getModel().toLowerCase().contains(searchTerm)) {
                results.add(vehicle);
            }
        }

        return results;
    }

    // Account management moved to AccountService

    public List<Rental> getRentals() {
        return rentals;
    }

    // Customer management moved to AccountService

    // Get notification service
    public NotificationService getNotificationService() {
        return notificationService;
    }

    public TicketService getTicketService() {
        return ticketService;
    }

    // Check and send reminders
    public void checkAndSendReminders() {
        notificationService.checkAndSendReminders(rentals);
    }

    // Get user messages
    public List<Message> getUserMessages(String username) {
        return notificationService.getUserMessages(username);
    }

    // Get unread message count
    public int getUnreadMessageCount(String username) {
        return notificationService.getUnreadMessages(username).size();
    }

    // Mark message as read
    public boolean markMessageAsRead(String messageId) {
        return notificationService.markMessageAsRead(messageId);
    }

    // Send user message
    public boolean sendUserMessage(String fromUser, String toUser, String subject, String content) {
        return notificationService.sendUserMessage(fromUser, toUser, subject, content);
    }

    // Generate report
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Rental System Report ===\n");
        report.append("Generated Time: ").append(LocalDate.now()).append("\n\n");

        // Vehicle statistics
        report.append("Vehicle Statistics:\n");
        Map<VehicleStatus, Long> vehicleStats = vehicleService.getVehicles().stream()
                .collect(java.util.stream.Collectors.groupingBy(Vehicle::getStatus,
                        java.util.stream.Collectors.counting()));

        for (Map.Entry<VehicleStatus, Long> entry : vehicleStats.entrySet()) {
            report.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // Rental statistics
        report.append("\nRental Statistics:\n");
        Map<RentalStatus, Long> rentalStats = rentals.stream()
                .collect(java.util.stream.Collectors.groupingBy(Rental::getStatus,
                        java.util.stream.Collectors.counting()));

        for (Map.Entry<RentalStatus, Long> entry : rentalStats.entrySet()) {
            report.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // Income statistics
        report.append("\nIncome Statistics:\n");
        double totalRevenue = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.RETURNED)
                .mapToDouble(Rental::getActualFee)
                .sum();

        double pendingRevenue = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.PENDING || r.getStatus() == RentalStatus.ACTIVE)
                .mapToDouble(Rental::getTotalFee)
                .sum();

        report.append("  Lease income completed: RM").append(String.format("%.2f", totalRevenue)).append("\n");
        report.append("  Lease income to be completed: RM").append(String.format("%.2f", pendingRevenue)).append("\n");
        report.append("  Total: RM").append(String.format("%.2f", totalRevenue + pendingRevenue)).append("\n");

        // Overdue rentals
        report.append("\nOverdue rentals:\n");
        List<Rental> overdueRentals = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.ACTIVE && r.isOverdue())
                .collect(java.util.stream.Collectors.toList());

        if (overdueRentals.isEmpty()) {
            report.append("  No overdue leases\n");
        } else {
            for (Rental rental : overdueRentals) {
                report.append("  Rental ID: ").append(rental.getId())
                        .append(", Username: ").append(rental.getUsername())
                        .append(", Vehicle: ").append(rental.getVehicle().getModel())
                        .append(", End Date: ").append(rental.getEndDate())
                        .append("\n");
            }
        }

        return report.toString();
    }

    // Export data
    public boolean exportData(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("=== Leasing system data export ===");
            writer.println("Export time: " + LocalDate.now());
            writer.println();

            // Export vehicle data
            writer.println("=== Vehicle data ===");
            for (Vehicle vehicle : vehicleService.getVehicles()) {
                writer.println("ID: " + vehicle.getId() +
                        ",Modal: " + vehicle.getModel() +
                        ", Status: " + vehicle.getStatus() +
                        ", Price: RM" + vehicle.getBasePrice() + "/day");
            }
            writer.println();

            // Export rental data
            writer.println("=== Leasing data ===");
            for (Rental rental : rentals) {
                writer.println("ID: " + rental.getId() +
                        ", Username: " + rental.getUsername() +
                        ", Vehicle: " + rental.getVehicle().getModel() +
                        ", Status: " + rental.getStatus() +
                        ", Start Date: " + rental.getStartDate() +
                        ", End Date: " + rental.getEndDate() +
                        ", Fee: RM" + rental.getTotalFee());
            }
            writer.println();

            // Export user data
            writer.println("=== User data ===");
            for (Account account : AccountService.getAccounts()) {
                writer.println("Username: " + account.getUsername() +
                        ", Role: " + account.getRole());
            }

            return true;
        } catch (IOException e) {
            System.err.println("Export data failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create rental (updated version, supports new vehicle booking system)
     */
    public Rental createRentalWithSchedule(Customer customer, Vehicle vehicle, LocalDate startDate,
            LocalDate endDate, boolean insurance, String username) {
        // Check if vehicle is available (including buffer period check)
        if (!vehicle.isAvailable(startDate, endDate)) {
            throw new IllegalArgumentException("Vehicle is not available for the requested period");
        }

        double fee = calculateRentalFee(vehicle, startDate, endDate, insurance);
        Rental rental = new Rental(nextRentalId++, customer, vehicle, startDate, endDate,
                RentalStatus.PENDING, fee, insurance, username);
        rentals.add(rental);

        // Add to vehicle booking schedule
        vehicle.addBooking(startDate, endDate);

        // Update vehicle status to reserved
        vehicle.setStatus(VehicleStatus.RESERVED);

        // Also update the vehicle in vehicleService to keep data in sync
        Vehicle vehicleInService = vehicleService.findVehicleById(vehicle.getId());
        if (vehicleInService != null) {
            vehicleInService.setStatus(VehicleStatus.RESERVED);
        }

        saveRentals("rentals.json");
        vehicleService.saveVehicles("vehicles.json"); // Save vehicle status change
        return rental;
    }

    /**
     * Check for damages when returning a vehicle
     */
    public boolean returnVehicleWithDamageCheck(int rentalId, String customerName,
            List<String> damageReports) {
        Rental rental = findRentalById(rentalId);
        if (rental != null && rental.getStatus() == RentalStatus.ACTIVE) {
            // Calculate actual fee
            double actualFee = calculateActualRentalFee(rental);
            rental.setActualFee(actualFee);

            // Process damage reports
            if (damageReports != null && !damageReports.isEmpty()) {
                for (String damage : damageReports) {
                    vehicleService.addMaintenanceLog(rental.getVehicle().getId(), MaintenanceLogType.DAMAGE_REPORT,
                            damage, customerName, 3, this); // Default severity level is 3
                }
                System.out.println("Damage reports filed: " + damageReports.size() + " issues reported.");
            }

            // Set rental status to returned
            rental.setStatus(RentalStatus.RETURNED);

            // Remove this booking from vehicle booking list
            Vehicle vehicle = rental.getVehicle();
            vehicle.removeBooking(rental.getStartDate(), rental.getEndDate());

            // Check if there are critical maintenance issues
            if (vehicle.hasCriticalMaintenanceIssues()) {
                vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
                System.out.println(
                        "Vehicle " + vehicle.getId() + " has been placed under maintenance due to reported issues.");
            } else {
                // Set vehicle status based on future bookings
                if (vehicle.hasFutureBookings()) {
                    vehicle.setStatus(VehicleStatus.RESERVED);
                } else {
                    vehicle.setStatus(VehicleStatus.AVAILABLE);
                }
            }

            saveRentals("rentals.json");

            // Sync vehicle status to ensure consistency
            syncVehicleStatusWithRentals();
            vehicleService.saveVehicles("vehicles.json"); // Save vehicle status change
            return true;
        }
        return false;
    }

    /**
     * Get all available vehicles (considering maintenance status)
     */
    public List<Vehicle> getAvailableVehicles() {
        List<Vehicle> available = new ArrayList<>();
        for (Vehicle vehicle : vehicleService.getVehicles()) {
            if (vehicle.getStatus() == VehicleStatus.AVAILABLE || vehicle.getStatus() == VehicleStatus.RESERVED &&
                    !vehicle.hasCriticalMaintenanceIssues()) {
                available.add(vehicle);
            }
        }
        return available;
    }

    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();

        int totalRentals = rentals.size();
        long activeRentals = rentals.stream().filter(r -> r.getStatus() == RentalStatus.ACTIVE).count();
        long completedRentals = rentals.stream().filter(r -> r.getStatus() == RentalStatus.RETURNED).count();
        long pendingRentals = rentals.stream().filter(r -> r.getStatus() == RentalStatus.PENDING).count();
        double totalRevenue = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.RETURNED)
                .mapToDouble(Rental::getTotalFee)
                .sum();

        long totalVehicles = vehicleService.getVehicles().size();
        long availableVehicles = vehicleService.getVehicles().stream()
                .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE).count();
        long rentedVehicles = vehicleService.getVehicles().stream().filter(v -> v.getStatus() == VehicleStatus.RENTED)
                .count();
        long maintenanceVehicles = vehicleService.getVehicles().stream()
                .filter(v -> v.getStatus() == VehicleStatus.UNDER_MAINTENANCE)
                .count();

        stats.put("totalVehicles", totalVehicles);
        stats.put("availableVehicles", availableVehicles);
        stats.put("rentedVehicles", rentedVehicles);
        stats.put("maintenanceVehicles", maintenanceVehicles);
        stats.put("totalRentals", totalRentals);
        stats.put("activeRentals", activeRentals);
        stats.put("completedRentals", completedRentals);
        stats.put("pendingRentals", pendingRentals);
        stats.put("totalRevenue", totalRevenue);
        stats.put("averageRevenue", completedRentals > 0 ? totalRevenue / completedRentals : 0.0);

        List<String> headers = Arrays.asList("Metric", "Value");
        List<List<String>> data = Arrays.asList(
                Arrays.asList("Total Vehicles", String.valueOf(totalVehicles)),
                Arrays.asList("Available Vehicles", String.valueOf(availableVehicles)),
                Arrays.asList("Rented Vehicles", String.valueOf(rentedVehicles)),
                Arrays.asList("Maintenance Vehicles", String.valueOf(maintenanceVehicles)),
                Arrays.asList("Total Rentals", String.valueOf(totalRentals)),
                Arrays.asList("Active Rentals", String.valueOf(activeRentals)),
                Arrays.asList("Completed Rentals", String.valueOf(completedRentals)),
                Arrays.asList("Pending Rentals", String.valueOf(pendingRentals)),
                Arrays.asList("Total Revenue", String.format("RM%.2f", totalRevenue)),
                Arrays.asList("Average Revenue/Rental",
                        String.format("RM%.2f", (completedRentals > 0 ? totalRevenue / completedRentals : 0.0))));

        stats.put("exportHeaders", headers);
        stats.put("exportData", data);

        return stats;
    }

    /**
     * Check if this is a rental extension by the same user (no buffer needed)
     */
    public boolean isRentalExtension(int vehicleId, LocalDate startDate, LocalDate endDate, String username) {
        for (Rental r : rentals) {
            if (r.getVehicle().getId() == vehicleId &&
                    r.getUsername().equals(username) &&
                    (r.getStatus() == RentalStatus.ACTIVE || r.getStatus() == RentalStatus.PENDING)) {

                // Check if the new booking is adjacent or overlapping with existing booking
                if (startDate.equals(r.getEndDate().plusDays(1)) ||
                        endDate.equals(r.getStartDate().minusDays(1)) ||
                        (!startDate.isAfter(r.getEndDate()) && !endDate.isBefore(r.getStartDate()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Enhanced conflict check that considers rental extensions
     */
    public String getConflictDetailsWithExtension(int vehicleId, LocalDate startDate, LocalDate endDate,
            String username) {
        // If this is a rental extension, use less strict checking
        if (isRentalExtension(vehicleId, startDate, endDate, username)) {
            Vehicle vehicle = vehicleService.findVehicleById(vehicleId);
            if (vehicle != null && vehicle.isAvailableForExtension(startDate, endDate, username)) {
                return null; // No conflict for extension
            }
        }

        // Use normal conflict checking for new bookings
        return getConflictDetails(vehicleId, startDate, endDate);
    }

    /**
     * Search and display user accounts (for admin offline booking)
     */
    public List<Account> searchUserAccounts(String searchTerm) {
        List<Account> results = new ArrayList<>();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // Return all customer accounts if no search term
            for (Account account : AccountService.getAccounts()) {
                if (account.getRole() == AccountRole.CUSTOMER) {
                    results.add(account);
                }
            }
            return results;
        }

        String searchLower = searchTerm.toLowerCase().trim();
        for (Account account : AccountService.getAccounts()) {
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
     * Get account by username
     */
    public Account getAccountByUsername(String username) {
        for (Account account : AccountService.getAccounts()) {
            if (account.getUsername().equals(username)) {
                return account;
            }
        }
        return null;
    }

    /**
     * Create a rental directly as ACTIVE (for admin offline booking)
     */
    public Rental createOfflineRental(Customer customer, Vehicle vehicle, LocalDate startDate,
            LocalDate endDate, boolean insurance, String username) {
        // Check if vehicle is available
        String conflictDetails = getConflictDetailsWithExtension(vehicle.getId(), startDate, endDate, username);
        if (conflictDetails != null) {
            throw new IllegalArgumentException("Vehicle conflict: " + conflictDetails);
        }

        double fee = calculateRentalFee(vehicle, startDate, endDate, insurance);
        Rental rental = new Rental(nextRentalId++, customer, vehicle, startDate, endDate,
                RentalStatus.ACTIVE, fee, insurance, username);
        rentals.add(rental);

        // Add to vehicle schedule
        vehicle.addBooking(startDate, endDate);

        // Set vehicle status to RENTED
        vehicle.setStatus(VehicleStatus.RENTED);

        // Generate ticket immediately
        ticketService.generateTicket(rental);

        // Save data
        saveRentals("rentals.json");
        vehicleService.saveVehicles("vehicles.json");

        return rental;
    }

    /**
     * Find active rental by user and vehicle (enhanced matching)
     */
    public Rental findActiveRentalByUserAndVehicle(String username, int vehicleId) {
        // First, try to find the account to get the full name
        Account account = getAccountByUsername(username);
        String accountFullName = (account != null) ? account.getFullName() : null;

        for (Rental rental : rentals) {
            if (rental.getVehicle().getId() == vehicleId &&
                    rental.getStatus() == RentalStatus.ACTIVE) {

                // Check username match
                if (rental.getUsername() != null && rental.getUsername().equals(username)) {
                    return rental;
                }

                // Check customer name match with account full name
                if (accountFullName != null && !accountFullName.isEmpty() &&
                        rental.getCustomer() != null &&
                        rental.getCustomer().getName().equals(accountFullName)) {
                    return rental;
                }

                // Check if username matches customer name (fallback)
                if (rental.getCustomer() != null &&
                        rental.getCustomer().getName().equals(username)) {
                    return rental;
                }
            }
        }
        return null;
    }

    /**
     * Extend existing rental
     */
    public boolean extendRental(String username, int vehicleId, LocalDate newEndDate, boolean insurance) {
        Rental existingRental = findActiveRentalByUserAndVehicle(username, vehicleId);
        if (existingRental == null) {
            return false;
        }

        Vehicle vehicle = existingRental.getVehicle();
        LocalDate originalEndDate = existingRental.getEndDate();

        // Update vehicle booking schedule
        vehicle.removeBooking(existingRental.getStartDate(), originalEndDate);
        vehicle.addBooking(existingRental.getStartDate(), newEndDate);

        // Calculate new total fee
        double newTotalFee = calculateRentalFee(vehicle, existingRental.getStartDate(), newEndDate, insurance);

        // Update rental details
        existingRental.setEndDate(newEndDate);
        existingRental.setTotalFee(newTotalFee);
        existingRental.setInsuranceSelected(insurance);

        // Reset reminder flags for extended rental
        existingRental.setDueSoonReminderSent(false);
        existingRental.setOverdueReminderSent(false);

        // Generate new ticket for the extended rental
        ticketService.generateTicket(existingRental);

        // Save data
        saveRentals("rentals.json");
        vehicleService.saveVehicles("vehicles.json");

        return true;
    }

    /**
     * Sync vehicle status with rental status to ensure consistency
     */
    public void syncVehicleStatusWithRentals() {
        List<Rental> activeRentals = getActiveRentals();
        List<Rental> pendingRentals = getPendingRentals();

        for (Vehicle vehicle : vehicleService.getVehicles()) {
            // Protect special status: UNDER_MAINTENANCE and OUT_OF_SERVICE from being
            // overridden
            if (vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE ||
                    vehicle.getStatus() == VehicleStatus.OUT_OF_SERVICE) {
                continue; // Skip these vehicles, keep their status unchanged
            }

            boolean hasActiveRental = false;
            boolean hasPendingRental = false;
            boolean hasPickedUpRental = false;

            // Check if there is an active rental for this vehicle
            for (Rental rental : activeRentals) {
                if (rental.getVehicle().getId() == vehicle.getId()) {
                    hasActiveRental = true;
                    // Check if vehicle has been picked up (ticket is used)
                    Ticket ticket = ticketService.getTicketByRentalId(rental.getId());
                    if (ticket != null && ticket.isUsed()) {
                        hasPickedUpRental = true;
                    }
                    break;
                }
            }

            // Check if there is a pending rental for this vehicle
            for (Rental rental : pendingRentals) {
                if (rental.getVehicle().getId() == vehicle.getId()) {
                    hasPendingRental = true;
                    break;
                }
            }

            // Determine correct status based on rental state
            if (hasPickedUpRental) {
                // Vehicle has been picked up - should be RENTED
                vehicle.setStatus(VehicleStatus.RENTED);
            } else if (hasActiveRental || hasPendingRental) {
                // Vehicle has approved/pending rental but not picked up - should be RESERVED
                vehicle.setStatus(VehicleStatus.RESERVED);
            } else {
                // No active or pending rentals
                if (vehicle.hasFutureBookings()) {
                    vehicle.setStatus(VehicleStatus.RESERVED);
                } else {
                    vehicle.setStatus(VehicleStatus.AVAILABLE);
                }
            }
        }
    }

    /**
     * Add new account to the system
     */
    public boolean addAccount(Account account) {
        // Check if username already exists
        if (getAccountByUsername(account.getUsername()) != null) {
            return false;
        }
        AccountService.addAccount(account);
        // Update notification service
        notificationService.loadUserEmailsFromAccounts(AccountService.getAccounts());
        return true;
    }

    /**
     * Update existing account
     */
    public boolean updateAccount(String username, Account updatedAccount) {
        Account existingAccount = getAccountByUsername(username);
        if (existingAccount == null) {
            return false;
        }

        // Update fields
        existingAccount.setEmail(updatedAccount.getEmail());
        existingAccount.setFullName(updatedAccount.getFullName());
        existingAccount.setContactNumber(updatedAccount.getContactNumber());
        existingAccount.setAddress(updatedAccount.getAddress());
        existingAccount.setDateOfBirth(updatedAccount.getDateOfBirth());
        existingAccount.setLicenseNumber(updatedAccount.getLicenseNumber());
        existingAccount.setEmergencyContact(updatedAccount.getEmergencyContact());

        // Update notification service
        notificationService.loadUserEmailsFromAccounts(AccountService.getAccounts());
        return true;
    }

    /**
     * Update account password
     */
    public boolean updatePassword(String username, String newPassword) {
        Account account = getAccountByUsername(username);
        if (account == null) {
            return false;
        }
        account.setPassword(newPassword);
        return true;
    }

    /**
     * Delete account by username
     */
    public boolean deleteAccount(String username) {
        Account account = getAccountByUsername(username);
        if (account == null) {
            return false;
        }
        AccountService.deleteAccount(account.getUsername());
        // Update notification service
        notificationService.loadUserEmailsFromAccounts(AccountService.getAccounts());
        return true;
    }

    /**
     * Search accounts by role and search term
     */
    public List<Account> searchAccounts(String searchTerm, AccountRole role) {
        List<Account> results = new ArrayList<>();
        String searchLower = searchTerm == null ? "" : searchTerm.toLowerCase().trim();

        for (Account account : AccountService.getAccounts()) {
            if (role != null && account.getRole() != role) {
                continue;
            }

            if (searchLower.isEmpty() ||
                    account.getUsername().toLowerCase().contains(searchLower) ||
                    (account.getFullName() != null && account.getFullName().toLowerCase().contains(searchLower)) ||
                    (account.getContactNumber() != null && account.getContactNumber().contains(searchTerm)) ||
                    (account.getEmail() != null && account.getEmail().toLowerCase().contains(searchLower))) {
                results.add(account);
            }
        }
        return results;
    }

    /**
     * Get all accounts by role
     */
    public List<Account> getAccountsByRole(AccountRole role) {
        List<Account> results = new ArrayList<>();
        for (Account account : AccountService.getAccounts()) {
            if (account.getRole() == role) {
                results.add(account);
            }
        }
        return results;
    }

    // Check if vehicle matches search terms
    public static boolean matchesVehicle(Vehicle vehicle, String term) {
        term = term.toUpperCase();

        return vehicle.getBrand().toUpperCase().contains(term) ||
                vehicle.getModel().toUpperCase().contains(term) ||
                vehicle.getCarPlate().toUpperCase().contains(term) ||
                vehicle.getVehicleType().toString().toUpperCase().contains(term) ||
                vehicle.getFuelType().toString().toUpperCase().contains(term);
    }

    // Display all vehicles (for maintenance history)
    public static void listAllVehicles(List<Vehicle> vehicles) {
        System.out.println("\n┌─────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐");
        System.out.println("│ ID  │    Brand    │    Model    │  Car Plate  │    Type     │   Status    │");
        System.out.println("├─────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤");

        for (Vehicle v : vehicleService.getVehicles()) {
            String brand = v.getBrand().length() > 11 ? v.getBrand().substring(0, 8) + "..." : v.getBrand();
            String model = v.getModel().length() > 11 ? v.getModel().substring(0, 8) + "..." : v.getModel();
            String carPlate = v.getCarPlate().length() > 11 ? v.getCarPlate().substring(0, 8) + "..." : v.getCarPlate();
            String type = v.getVehicleType().toString().length() > 11
                    ? v.getVehicleType().toString().substring(0, 8) + "..."
                    : v.getVehicleType().toString();
            String status = v.getStatus().toString();
            if (status.equals("UNDER_MAINTENANCE"))
                status = "MAINTENANCE";
            else if (status.length() > 11)
                status = status.substring(0, 8) + "...";

            System.out.printf("│ %-3d │ %-11s │ %-11s │ %-11s │ %-11s │ %-11s │%n",
                    v.getId(), brand, model, carPlate, type, status);
        }

        System.out.println("└─────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘");
        System.out.println("Total vehicles: " + vehicles.size());
    }

    // Initiate rental process
    public static void initiateRental(RentalSystem system, Scanner scanner, Account account) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                          VEHICLE BOOKING                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");

        // Display all vehicles with their status
        vehicleService.listAvailableVehicles(vehicleService.getVehicles()); // Show all vehicles, not just available
                                                                            // ones

        int vehicleId;
        while (true) {
            System.out.println(
                    "\nNote: Enter vehicle ID to see detailed availability and view unavailable period for reserved vehicle");
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

        Vehicle selected = vehicleService.findVehicleById(vehicleId);
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
            

            if (AccountService.getYesNoInput(scanner, "\nDo you want to extend this existing rental instead?")) {
                int additionalDays = 0;
                while (additionalDays <= 0) {
                    System.out.print("How many additional days do you want to extend (or 'exit' to return)? ");
                    String daysStr = scanner.nextLine();

                    if (daysStr.equalsIgnoreCase("exit")) {
                        System.out.println("Returning to main menu...");
                        return;
                    }

                    try {
                        additionalDays = Integer.parseInt(daysStr);
                        if (additionalDays <= 0) {
                            System.out.println("Extension days must be positive.");
                            System.out.println("\nPress Enter to continue...");
                            scanner.nextLine();
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number format. Please enter a valid number.");
                        System.out.println("\nPress Enter to continue...");
                        scanner.nextLine();
                    }
                }

                LocalDate newEndDate = existingRental.getEndDate().plusDays(additionalDays);

                // Show extension details
                System.out.println("\n=== Extension Details ===\n");
                System.out.printf("Original end date: %s\n", existingRental.getEndDate());
                System.out.printf("New proposed end date: %s\n", newEndDate);
                System.out.printf("Additional days: %d\n", additionalDays);

                // Calculate new total rental days for discount display
                int totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(existingRental.getStartDate(),
                        newEndDate) + 1;
                double discount = selected.getDiscountForDays(totalDays);
                if (discount > 0) {
                    System.out.printf("New total period: %d days\n", totalDays);
                    System.out.printf("Long-term discount: %.1f%% off\n", discount * 100);
                }

                // Insurance selection
                double insuranceRate = selected.getInsuranceRate();
                System.out.printf("\nInsurance rate: %.1f%%\n", insuranceRate * 100);
                boolean insurance = AccountService.getYesNoInput(scanner, "Include insurance for the extended period?");

                // Extend the rental
                if (system.extendRental(account.getUsername(), vehicleId, newEndDate, insurance)) {
                    Rental updatedRental = system.findActiveRentalByUserAndVehicle(account.getUsername(),
                            vehicleId);
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
            } else {
                System.out.println("You chose not to extend. Proceeding with new booking for this vehicle (if available).");
            }

        }

        // If not extending, or no existing rental, proceed with new booking process
        // Check if vehicle is available for booking
        if ((selected.getStatus() == VehicleStatus.UNDER_MAINTENANCE
                || selected.getStatus() == VehicleStatus.OUT_OF_SERVICE) || selected.hasCriticalMaintenanceIssues()) {
            System.out.println("\n*** VEHICLE NOT AVAILABLE FOR BOOKING ***\n");
            System.out.println("Reason: Vehicle status is " + selected.getStatus());
            if (selected.hasCriticalMaintenanceIssues()) {
                System.out.println("Additional reason: Vehicle has unresolved maintenance issues.");
            }

            // Still show booking schedule for informational purposes
            List<String> unavailablePeriods = vehicleService.getVehicleUnavailablePeriods(vehicleId);
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
        List<String> unavailablePeriods = vehicleService.getVehicleUnavailablePeriods(vehicleId);
        if (!unavailablePeriods.isEmpty()) {
            System.out.println("\n=== Vehicle Schedule (Unavailable Periods) ===\n");
            for (String period : unavailablePeriods) {
                System.out.println("- " + period);
            }
        } else {
            System.out.println("\nNo existing bookings found for this vehicle.");
        }
        LocalDate startDate = null;
        do {

            System.out.print("\nEnter rental start date (yyyy-MM-dd, 'today', or 'exit' to return): ");
            String startStr = scanner.nextLine();

            if (startStr.equalsIgnoreCase("exit")) {
                System.out.println("Returning to main menu...");
                return;
            }

            if (startStr.equalsIgnoreCase("today")) {
                startDate = LocalDate.now();
                break;
            } else {
                try {
                    startDate = LocalDate.parse(startStr);
                    if (startDate.isBefore(LocalDate.now())) {
                        System.out.println("Start date cannot be in the past.");
                        System.out.println("\nPress Enter to continue...");
                        scanner.nextLine();
                        startDate = null; // Reset to continue loop
                        continue;
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Invalid date format. Please use yyyy-MM-dd.");
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    startDate = null; // Reset to continue loop
                }
            }
        } while (startDate == null);

        // Show available discounts before asking for rental days
        if (selected.getLongTermDiscounts() != null && !selected.getLongTermDiscounts().isEmpty()) {
            System.out.println("\n=== Available Discounts ===\n");
            System.out.println("Long-term rental discounts:");
            for (Map.Entry<Integer, Double> entry : selected.getLongTermDiscounts().entrySet()) {
                System.out.printf("- %d+ days: %.1f%% off\n", entry.getKey(), entry.getValue() * 100);
            }
            System.out.println("\nChoose your rental duration to see applicable discounts!");
        }

        int rentalDays = 0;
        while (rentalDays <= 0) {
            System.out.print("\nEnter number of rental days (or 'exit' to return): ");
            String daysStr = scanner.nextLine();

            if (daysStr.equalsIgnoreCase("exit")) {
                System.out.println("Returning to main menu...");
                return;
            }

            try {
                rentalDays = Integer.parseInt(daysStr);
                if (rentalDays <= 0) {
                    System.out.println("Rental days must be positive.");
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format. Please enter a valid number.");
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
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
        boolean insurance = AccountService.getYesNoInput(scanner, "Purchase insurance?");

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

            if (!AccountService.getYesNoInput(scanner, "Is this information correct?")) {
                System.out.print("Enter full name for this booking: ");
                customerName = scanner.nextLine();
                System.out.print("Enter contact number for this booking: ");
                contact = scanner.nextLine();
                System.out.println("Note: This will not update your profile permanently.");
            }
        }

        // Create temporary customer object for rental
        models.Customer customer = new models.Customer(9999, customerName, contact);

        // Final confirmation and booking
        double totalFee = calculateRentalFee(selected, startDate, endDate, insurance);
        System.out.println("\n=== Rental Summary ===\n");
        System.out.println("Vehicle: " + selected.getBrand() + " " + selected.getModel());
        System.out.println("Type: " + selected.getVehicleType() + " | Fuel: " + selected.getFuelType());
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);
        System.out.println("Insurance: " + (insurance ? "Included" : "Not included"));
        System.out.printf("Total Fee: RM%.2f\n", totalFee);

        if (AccountService.getYesNoInput(scanner, "\nConfirm booking?")) {
            try {
                Rental rental = system.createRentalWithSchedule(customer, selected, startDate, endDate, insurance,
                        account.getUsername());
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
    public static void viewMyRentalHistoryEnhanced(RentalSystem system, String username) {
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
                String feeDisplay = r.getActualFee() > 0 ? "Final Fee: RM" + String.format("%.2f", r.getActualFee())
                        : "Est. Fee: RM" + String.format("%.2f", r.getTotalFee());
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

    // Quick search
    public static void searchInBookingProcess(RentalSystem system, Scanner scanner, Account account) {
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
        List<Vehicle> searchResults = vehicleService.performSearch(vehicleService.getVehicles(), query);

        if (searchResults.isEmpty()) {
            System.out.println("\nNo vehicles found matching your search criteria.");
            System.out.println("Try using different keywords or Boolean operators.");
            return;
        }

        // Execute Boolean logic search
        System.out.println("\nSearch Results (" + searchResults.size() + " vehicles found):");
        vehicleService.listAvailableVehicles(searchResults);
    }

    // Request vehicle return
    public static void requestReturn(RentalSystem system, Scanner scanner, String username) {
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

            // Enhanced vehicle condition report process
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                   VEHICLE CONDITION REPORT                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");

            Vehicle vehicle = rental.getVehicle();
            System.out.printf("Vehicle: %s %s (Car Plate: %s)\n",
                    vehicle.getBrand(), vehicle.getModel(), vehicle.getCarPlate());

            System.out.println("\nPlease report any issues or damage with the vehicle:");

            List<vehicleService.IssueReport> issueReports = new ArrayList<>();
            if (AccountService.getYesNoInput(scanner, "Any issues to report?")) {
                System.out.println("\nPlease describe each issue (type 'done' when finished):");
                int issueNum = 1;

                while (true) {
                    System.out.printf("\nIssue #%d description: ", issueNum);
                    String description = scanner.nextLine().trim();

                    if (description.equalsIgnoreCase("done")) {
                        if (issueReports.isEmpty()) {
                            System.out.println(
                                    "No issues reported. Vehicle will be returned without maintenance records.");
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
                        case "2":
                            logType = MaintenanceLogType.REPAIR;
                            break;
                        case "3":
                            logType = MaintenanceLogType.CLEANING;
                            break;
                        default:
                            logType = MaintenanceLogType.DAMAGE_REPORT;
                            break;
                    }

                    System.out.println("\nSeverity Assessment:");
                    System.out.println("Please assess the severity of this issue.");
                    int severity = vehicleService.getSeverityLevelWithGuidance(scanner);

                    issueReports.add(new vehicleService.IssueReport(description, logType, severity));
                    System.out.printf(" Issue #%d recorded: %s (Severity: %d)\n", issueNum, description, severity);
                    issueNum++;
                }
            }

            // Process vehicle return
            System.out.println("\n=== Processing Vehicle Return ===");

            // Create maintenance record for each issue
            for (vehicleService.IssueReport issue : issueReports) {
                vehicleService.addMaintenanceLog(vehicle.getId(), issue.logType, issue.description,
                        rental.getCustomer().getName(), issue.severity);

                // High severity notifications sent by RentalSystem automatically
            }

            // Execute return (including damage check)
            List<String> damageDescriptions = new ArrayList<>();
            for (vehicleService.IssueReport issue : issueReports) {
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

                    // Check if there are critical issues causing vehicle to enter maintenance mode
                    boolean hasCriticalIssues = issueReports.stream().anyMatch(issue -> issue.severity >= 3);
                    if (hasCriticalIssues) {
                        System.out.println(" Vehicle automatically set to UNDER_MAINTENANCE due to reported issues.");
                        System.out.println(" Maintenance notifications sent to administrators.");
                    }

                    // Display issue summary
                    System.out.println("\nReported Issues:");
                    for (int i = 0; i < issueReports.size(); i++) {
                        vehicleService.IssueReport issue = issueReports.get(i);
                        System.out.printf("  %d. %s (Severity: %d)\n", i + 1, issue.description, issue.severity);
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

    public static void viewAllRentals(List<Rental> rentals) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                           ALL RENTALS                            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");

        if (rentals.isEmpty()) {
            System.out.println("\nNo rentals found.");
            return;
        }

        System.out.println(
                "\n┌──────┬─────────────────────┬──────────────────┬────────────┬──────────────┬──────────────┬─────────────┐");
        System.out.println(
                "│  ID  │      Customer       │     Vehicle      │   Status   │ Start Date   │  End Date    │    Fee      │");
        System.out.println(
                "├──────┼─────────────────────┼──────────────────┼────────────┼──────────────┼──────────────┼─────────────┤");

        for (Rental r : rentals) {
            String customerName = r.getCustomer().getName();
            if (customerName.length() > 19)
                customerName = customerName.substring(0, 16) + "...";

            String vehicleInfo = r.getVehicle().getBrand() + " " + r.getVehicle().getModel();
            if (vehicleInfo.length() > 16)
                vehicleInfo = vehicleInfo.substring(0, 13) + "...";

            String statusText = r.getStatus().toString();
            if (statusText.length() > 10)
                statusText = statusText.substring(0, 7) + "...";

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
                    feeDisplay);
        }

        System.out.println(
                "└──────┴─────────────────────┴──────────────────┴────────────┴──────────────┴──────────────┴─────────────┘");
        System.out.println("\nTotal Rentals: " + rentals.size());
    }

    public static void viewPendingRentals(List<Rental> pendingRentals) {
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

    public static void approveRental(RentalSystem system, Scanner scanner) {
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

    public static void confirmReturn(RentalSystem system, Scanner scanner) {
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

            List<vehicleService.IssueReport> issueReports = new ArrayList<>();
            if (AccountService.getYesNoInput(scanner, "Any issues to report?")) {
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
                        vehicleService.displaySeverityGuidance();
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

                    issueReports.add(new vehicleService.IssueReport(description, logType, severity));
                    issueCounter++;
                }
            }

            // Process return with damage reports
            System.out.println("\n=== Processing Vehicle Return ===");

            // Submit maintenance logs
            for (vehicleService.IssueReport issue : issueReports) {
                vehicleService.addMaintenanceLog(vehicle.getId(), issue.logType, issue.description,
                        rental.getCustomer().getName(), issue.severity);
            }

            // Execute return (including damage check)
            List<String> damageDescriptions = new ArrayList<>();
            for (vehicleService.IssueReport issue : issueReports) {
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
                        vehicleService.IssueReport issue = issueReports.get(i);
                        System.out.printf("  %d. %s (Severity: %d)\n", i + 1, issue.description, issue.severity);
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

    // Rental management (merge approval and rejection functionality)
    public static void rentalManagement(RentalSystem system, Scanner scanner) {
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
                System.out.printf("│ %-3d │ %-11s │ %-11s │ %-11s │ %-11s │ RM%-9.2f │%n",
                        rental.getId(),
                        rental.getUsername() != null
                                ? (rental.getUsername().length() > 11 ? rental.getUsername().substring(0, 11)
                                        : rental.getUsername())
                                : "N/A",
                        rental.getVehicle().getModel().length() > 11 ? rental.getVehicle().getModel().substring(0, 11)
                                : rental.getVehicle().getModel(),
                        rental.getStartDate().toString(),
                        rental.getEndDate().toString(),
                        rental.getTotalFee());
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

    // Reject rental (with reason)
    public static void rejectRentalWithReason(RentalSystem system, Scanner scanner) {
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

    // Reports and analytics (merge functionality)
    public static void reportsAndAnalytics(RentalSystem system, Scanner scanner) {
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
                    Main.viewStatistics(system, scanner);
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

    // Generate detailed report
    public static void generateDetailedReport(RentalSystem system, Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    DETAILED REPORT OPTIONS                       ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 1. Monthly Rental Statistics                                     ║");
        System.out.println("║ 2. Popular Vehicle Report                                        ║");
        System.out.println("║ 3. Rental History Export                                         ║");
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
                exportRentalHistory(system, scanner);
                break;
            case "0":
                return;
            default:
                System.out.println("Invalid option. Please try again.");
        }
    }

    // Export rental history
    public static void exportRentalHistory(RentalSystem system, Scanner scanner) {
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
                    rental.isInsuranceSelected() ? "Yes" : "No");
            data.add(row);
        }

        // Display summary
        System.out.printf("║ Total Rentals: %-49d ║%n", rentals.size());
        System.out.printf("║ Data prepared for export with %2d records                         ║%n", data.size());
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");

        // Export options
        ReportExportService exportService = new ReportExportService();
        exportService.promptForExport(scanner, "Rental History Export", headers, data, "rental_history");
    }

    // Ticket management
    public static void ticketManagement(RentalSystem system, Scanner scanner) {
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

    // Validate ticket for vehicle pickup
    public static void validateTicketForPickup(RentalSystem system, Scanner scanner) {
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

                // Update rental status to active if needed and set vehicle status to RENTED
                Rental rental = system.findRentalById(ticket.getRentalId());
                if (rental != null && rental.getStatus() == RentalStatus.ACTIVE) {
                    // Set vehicle status to RENTED when picked up
                    rental.getVehicle().setStatus(VehicleStatus.RENTED);

                    // Also update the vehicle in vehicleService to keep data in sync
                    Vehicle vehicleInService = vehicleService.findVehicleById(rental.getVehicle().getId());
                    if (vehicleInService != null) {
                        vehicleInService.setStatus(VehicleStatus.RENTED);
                    }

                    System.out.println(" Rental is active and ready for pickup.");
                    System.out.println(" Vehicle status updated to RENTED.");
                    vehicleService.saveVehicles("vehicles.json");
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

    // Display ticket statistics
    public static void displayTicketStatistics(TicketService ticketService) {
        Map<String, Integer> stats = ticketService.getTicketStats();

        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        TICKET STATISTICS                         ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Total Tickets: %-49d ║%n", stats.get("total"));
        System.out.printf("║ Valid Tickets: %-49d ║%n", stats.get("valid"));
        System.out.printf("║ Used Tickets: %-50d ║%n", stats.get("used"));
        System.out.printf("║ Customers with Tickets: %-40d ║%n", stats.get("customers"));
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }

    // Admin offline booking for walk-in customers
    public static void offlineBooking(RentalSystem system, Scanner scanner) {
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
            String fullName = (acc.getFullName() != null && !acc.getFullName().isEmpty()) ? acc.getFullName()
                    : "Not set";
            String contact = (acc.getContactNumber() != null && !acc.getContactNumber().isEmpty())
                    ? acc.getContactNumber()
                    : "Not set";

            System.out.printf("│ %-3d │ %-11s │ %-19s │ %-19s │%n",
                    (i + 1),
                    acc.getUsername().length() > 11 ? acc.getUsername().substring(0, 11) : acc.getUsername(),
                    fullName.length() > 19 ? fullName.substring(0, 19) : fullName,
                    contact.length() > 19 ? contact.substring(0, 19) : contact);
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
        vehicleService.listAvailableVehicles(vehicleService.getVehicles());

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

        Vehicle selectedVehicle = vehicleService.findVehicleById(vehicleId);
        if (selectedVehicle == null) {
            System.out.println("Vehicle not found.");
            return;
        }

        if ((selectedVehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE
                || selectedVehicle.getStatus() == VehicleStatus.OUT_OF_SERVICE)
                || selectedVehicle.hasCriticalMaintenanceIssues()) {
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

            
            if (AccountService.getYesNoInput(scanner, "\nDo you want to extend this existing rental instead?")) {
                int additionalDays = 0;
                while (additionalDays <= 0) {
                    System.out.print("How many additional days to extend (or 'exit' to return)? ");
                    String daysStr = scanner.nextLine();

                    if (daysStr.equalsIgnoreCase("exit")) {
                        System.out.println("Returning to main menu...");
                        return;
                    }

                    try {
                        additionalDays = Integer.parseInt(daysStr);
                        if (additionalDays <= 0) {
                            System.out.println("Extension days must be positive.");
                            System.out.println("\nPress Enter to continue...");
                            scanner.nextLine();
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number format. Please enter a valid number.");
                        System.out.println("\nPress Enter to continue...");
                        scanner.nextLine();
                    }
                }

                LocalDate newEndDate = existingRental.getEndDate().plusDays(additionalDays);

                // Show extension details
                System.out.println("\n=== Extension Details ===\n");
                System.out.printf("Original end date: %s\n", existingRental.getEndDate());
                System.out.printf("New proposed end date: %s\n", newEndDate);
                System.out.printf("Additional days: %d\n", additionalDays);

                // Calculate new total rental days for discount display
                int totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(existingRental.getStartDate(),
                        newEndDate) + 1;
                double discount = selectedVehicle.getDiscountForDays(totalDays);
                if (discount > 0) {
                    System.out.printf("New total period: %d days\n", totalDays);
                    System.out.printf("Long-term discount: %.1f%% off\n", discount * 100);
                }

                // Insurance selection
                double insuranceRate = selectedVehicle.getInsuranceRate();
                System.out.printf("\nInsurance rate: %.1f%%\n", insuranceRate * 100);
                boolean extensionInsurance = AccountService.getYesNoInput(scanner, "Include insurance for the extended period?");

                // Extend the rental
                if (system.extendRental(selectedAccount.getUsername(), vehicleId, newEndDate, extensionInsurance)) {
                    Rental updatedRental = system.findActiveRentalByUserAndVehicle(selectedAccount.getUsername(),
                            vehicleId);
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
            } else {
                System.out.println(
                        "Customer chose not to extend. Proceeding with new booking for this vehicle (if available).");
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

        List<String> unavailablePeriods = vehicleService.getVehicleUnavailablePeriods(vehicleId);
        if (!unavailablePeriods.isEmpty()) {
            System.out.println("\n=== Vehicle Schedule (Unavailable Periods) ===\n");
            for (String period : unavailablePeriods) {
                System.out.println("- " + period);
            }
        }

        // Step 3: Date selection
        System.out.println("\n=== Step 3: Rental Period ===\n");
        
        LocalDate startDate = null;
        do {
            System.out.print("Enter rental start date (yyyy-MM-dd, 'today', or 'exit' to return): ");
            String startStr = scanner.nextLine();

            if (startStr.equalsIgnoreCase("exit")) {
                System.out.println("Returning to main menu...");
                return;
            }

            if (startStr.equalsIgnoreCase("today")) {
                startDate = LocalDate.now();
                break;
            } else {
                try {
                    startDate = LocalDate.parse(startStr);
                    if (startDate.isBefore(LocalDate.now())) {
                        System.out.println("Start date cannot be in the past.");
                        System.out.println("\nPress Enter to continue...");
                        scanner.nextLine();
                        startDate = null; // Reset to continue loop
                        continue;
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Invalid date format. Please use yyyy-MM-dd.");
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    startDate = null; // Reset to continue loop
                }
            }
        } while (startDate == null);

        // Show available discounts before asking for rental days
        if (selectedVehicle.getLongTermDiscounts() != null && !selectedVehicle.getLongTermDiscounts().isEmpty()) {
            System.out.println("\n=== Available Discounts ===\n");
            System.out.println("Long-term rental discounts:");
            for (Map.Entry<Integer, Double> entry : selectedVehicle.getLongTermDiscounts().entrySet()) {
                System.out.printf("- %d+ days: %.1f%% off\n", entry.getKey(), entry.getValue() * 100);
            }
            System.out.println("\nChoose your rental duration to see applicable discounts!");
        }

        int rentalDays = 0;
        while (rentalDays <= 0) {
            System.out.print("\nEnter rental duration (days) or 'exit' to return: ");
            String daysStr = scanner.nextLine();

            if (daysStr.equalsIgnoreCase("exit")) {
                System.out.println("Returning to main menu...");
                return;
            }

            try {
                rentalDays = Integer.parseInt(daysStr);
                if (rentalDays <= 0) {
                    System.out.println("Rental days must be positive.");
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format. Please enter a valid number.");
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
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
        boolean insurance = AccountService.getYesNoInput(scanner, "Include insurance?");

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
        double totalFee = calculateRentalFee(selectedVehicle, startDate, endDate, insurance);
        System.out.println("\n=== Booking Summary ===\n");
        System.out.println("Customer: " + customerName + " (" + selectedAccount.getUsername() + ")");
        System.out.println("Vehicle: " + selectedVehicle.getBrand() + " " + selectedVehicle.getModel());
        System.out.println("Period: " + startDate + " to " + endDate + " (" + rentalDays + " days)");
        System.out.println("Insurance: " + (insurance ? "Yes" : "No"));
        System.out.printf("Total Fee: RM%.2f\n", totalFee);

        if (!AccountService.getYesNoInput(scanner, "\nConfirm offline booking?")) {
            System.out.println("Booking cancelled.");
            return;
        }

        try {
            // Create temporary customer object for rental
            models.Customer customer = new models.Customer(9999, customerName, contact);
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

    // Admin business operations menu (original admin panel)
    public static void adminBusinessOperations(RentalSystem system, Scanner scanner) {
        boolean firstTime = true;
        while (true) {
            // Clear screen after first time
            if (!firstTime) {
                Main.clearScreen();
            }
            firstTime = false;

            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                      Booking Management                          ║");
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
            System.out.println("║  0. Back to Main Menu                                            ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    vehicleService.viewAllVehicles(vehicleService.getVehicles());
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
                    vehicleService.addNewVehicle(system, scanner);
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "6":
                    reportsAndAnalytics(system, scanner);
                    break;
                case "7":
                    vehicleService.maintenanceManagement(system, scanner);
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
                    MessageService.messageCenter(system, scanner, "admin");
                    break;
                case "11":
                    RentalSystem.offlineBooking(system, scanner);
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

    // Customer account management menu
    public static void customerAccountManagement(RentalSystem system, Scanner scanner, Account account) {
        boolean firstTime = true;
        while (true) {
            // Clear screen after first time
            if (!firstTime) {
                Main.clearScreen();
            }
            firstTime = false;

            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                      ACCOUNT MANAGEMENT                          ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. View & Modify Account Information                             ║");
            System.out.println("║ 2. Update Email Address                                          ║");
            System.out.println("║ 3. Change Password                                               ║");
            System.out.println("║ 0. Back to Main Menu                                             ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    AccountService.viewAndModifyAccountInfo(scanner, account);
                    break;
                case "2":
                    AccountService.updateEmailAddress(scanner, account);
                    break;
                case "3":
                    AccountService.changePassword(scanner, account);
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

    // Customer booking module (original customer panel)
    public static void customerBookingModule(RentalSystem system, Scanner scanner, Account account) {
        boolean firstTime = true;
        while (true) {
            // Clear screen after first time
            if (!firstTime) {
                Main.clearScreen();
            }
            firstTime = false;

            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                    VEHICLE RENTAL & BOOKING                      ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. View Available Vehicles                                       ║");
            System.out.println("║ 2. Make Rental/Booking                                           ║");
            System.out.println("║ 3. Cancel Booking                                                ║");
            System.out.println("║ 4. Request Vehicle Return                                        ║");
            System.out.println("║ 5. View My Rental History                                        ║");
            System.out.println("║ 6. View My Rental Tickets                                        ║");
            System.out.println("║ 7. Message Center                                                ║");
            System.out.println("║ 0. Back to Main Menu                                             ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    vehicleService.listAvailableVehicles(vehicleService.getVehicles());
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "2":
                    RentalSystem.initiateRental(system, scanner, account);
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
                    RentalSystem.viewMyRentalHistoryEnhanced(system, account.getUsername());
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "6":
                    viewMyTickets(system, scanner, account.getUsername());
                    break;
                case "7":
                    MessageService.messageCenter(system, scanner, account.getUsername());
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

    // View my rental tickets
    public static void viewMyTickets(RentalSystem system, Scanner scanner, String username) {
        // Get customer name from username
        Account account = AccountService.getAccountByUsername(username);
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
    public static void cancelBooking(RentalSystem system, Scanner scanner, String username) {
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

    /**
     * Set vehicles list (for data synchronization)
     */
    public void setVehicles(List<Vehicle> vehicleList) {
        vehicleService.setVehicles(vehicleList != null ? vehicleList : new ArrayList<>());
    }

    /**
     * Get all vehicles
     */
    public List<Vehicle> getVehicles() {
        return vehicleService.getVehicles();
    }

    /**
     * Find vehicle by ID
     */
    public Vehicle findVehicleById(int id) {
        for (Vehicle v : vehicleService.getVehicles()) {
            if (v.getId() == id) {
                return v;
            }
        }
        return null;
    }
}