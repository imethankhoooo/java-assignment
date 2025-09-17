package services;

import enums.*;
import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import models.*;
import static services.UtilityService.*;

/**
 * Main system class: manages accounts, vehicles, rentals and core operations
 */
public class RentalSystem {
    private List<Rental> rentals;
    private NotificationService notificationService;
    private TicketService ticketService;
    private PaymentService paymentService;
    private int nextRentalId = 1;
    public boolean shouldExit = false;

    public RentalSystem() {
        rentals = new ArrayList<>();
        notificationService = new NotificationService();
        ticketService = new TicketService();
        paymentService = new PaymentService();
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
            String name = extractJsonValue(json, "name");
            String contact = extractJsonValue(json, "contact");
            return new models.Customer(name, contact);
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
            json.append("      \"name\": \"").append(customer.getName()).append("\",\n");
            json.append("      \"contact\": \"").append(customer.getContact()).append("\"\n");
            json.append("    },\n");

            // Vehicle information - using field names expected by vehicleService.parseVehicleFromJson
            Vehicle vehicle = rental.getVehicle();
            json.append("    \"vehicle\": {\n");
            json.append("      \"vehicleID\": \"").append(vehicle.getVehicleID()).append("\",\n");
            json.append("      \"plateNo\": \"").append(vehicle.getPlateNo()).append("\",\n");
            json.append("      \"brand\": \"").append(vehicle.getBrand()).append("\",\n");
            json.append("      \"model\": \"").append(vehicle.getModel()).append("\",\n");
            json.append("      \"type\": \"").append(vehicle.getType()).append("\",\n");
            json.append("      \"fuelType\": \"").append(vehicle.getFuelType()).append("\",\n");
            json.append("      \"color\": \"").append(vehicle.getColor()).append("\",\n");
            json.append("      \"year\": ").append(vehicle.getYear()).append(",\n");
            json.append("      \"capacity\": ").append(vehicle.getCapacity()).append(",\n");
            json.append("      \"condition\": \"").append(vehicle.getCondition()).append("\",\n");
            json.append("      \"insuranceRate\": ").append(vehicle.getInsuranceRate()).append(",\n");
            json.append("      \"availability\": \"").append(vehicle.getStatus()).append("\",\n");
            json.append("      \"archived\": ").append(vehicle.isArchived()).append(",\n");
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
                    .append(escapeJson(rental.getUsername() != null ? rental.getUsername() : "")).append("\",\n");
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
    public double calculateRentalFee(Vehicle vehicle, LocalDate startDate, LocalDate endDate,
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
        vehicle.setStatus("reserved");

        // Also update the vehicle in vehicleService to keep data in sync
        Vehicle vehicleInService = vehicleService.findVehicleById(vehicle.getId());
        if (vehicleInService != null) {
            vehicleInService.setStatus("reserved");
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
            // Keep vehicle reserved until actual pickup
            rental.getVehicle().setStatus("reserved");

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
                vehicle.setStatus("reserved");
            } else {
                vehicle.setStatus("available");
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
                vehicle.setStatus("reserved");
            } else {
                vehicle.setStatus("available");
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
    public double calculateActualRentalFee(Rental rental) {
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
        vehicle.setStatus("reserved");

        // Also update the vehicle in vehicleService to keep data in sync
        Vehicle vehicleInService = vehicleService.findVehicleById(vehicle.getId());
        if (vehicleInService != null) {
            vehicleInService.setStatus("reserved");
        }

        saveRentals("rentals.json");
        vehicleService.saveVehicles("vehicles.json"); // Save vehicle status change
        return rental;
    }

    /**
     * Process vehicle return (simplified without damage checking)
     */
    public boolean returnVehicle(int rentalId, Scanner scanner) {
        Rental rental = findRentalById(rentalId);
        if (rental != null && rental.getStatus() == RentalStatus.ACTIVE) {
            // Calculate actual fee
            double actualFee = calculateActualRentalFee(rental);
            rental.setActualFee(actualFee);

            // Set rental status to returned
            rental.setStatus(RentalStatus.RETURNED);

            // Remove this booking from vehicle booking list
            Vehicle vehicle = rental.getVehicle();
            vehicle.removeBooking(rental.getStartDate(), rental.getEndDate());

            // Set vehicle status based on future bookings
            if (vehicle.hasFutureBookings()) {
                vehicle.setStatus("reserved");
            } else {
                vehicle.setStatus("available");
            }

            saveRentals("rentals.json");

            // Sync vehicle status to ensure consistency
            syncVehicleStatusWithRentals();
            vehicleService.saveVehicles("vehicles.json"); // Save vehicle status change

            // Process payment after successful return
            paymentService.processPayment(rental, scanner);

            return true;
        }
        return false;
    }

    /**
     * Get all available vehicles
     */


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
        vehicle.setStatus("rented");

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
     * Find pending rental by user and vehicle
     */
    public Rental findPendingRentalByUserAndVehicle(String username, int vehicleId) {
        // Try to match by username first
        for (Rental rental : rentals) {
            if (rental.getVehicle().getId() == vehicleId && rental.getStatus() == RentalStatus.PENDING) {
                if (rental.getUsername() != null && rental.getUsername().equals(username)) {
                    return rental;
                }
            }
        }

        // Fallback: try match by customer name equals username
        for (Rental rental : rentals) {
            if (rental.getVehicle().getId() == vehicleId && rental.getStatus() == RentalStatus.PENDING) {
                if (rental.getCustomer() != null && rental.getCustomer().getName().equals(username)) {
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

        // Update vehicle booking schedule (extension-safe, no global buffer check)
        vehicle.removeBooking(existingRental.getStartDate(), originalEndDate);
        vehicle.addBookingForExtension(existingRental.getStartDate(), newEndDate);

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
     * Extend pending rental by same user and vehicle
     */
    public boolean extendPendingRental(String username, int vehicleId, LocalDate newEndDate, boolean insurance) {
        Rental pending = findPendingRentalByUserAndVehicle(username, vehicleId);
        if (pending == null) {
            return false;
        }

        Vehicle vehicle = pending.getVehicle();
        LocalDate originalEndDate = pending.getEndDate();

        // Update schedule for pending extension
        vehicle.removeBooking(pending.getStartDate(), originalEndDate);
        vehicle.addBookingForExtension(pending.getStartDate(), newEndDate);

        // Recompute fee and update rental fields
        double newTotalFee = calculateRentalFee(vehicle, pending.getStartDate(), newEndDate, insurance);
        pending.setEndDate(newEndDate);
        pending.setTotalFee(newTotalFee);
        pending.setInsuranceSelected(insurance);

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
            if ("out_of_service".equalsIgnoreCase(vehicle.getStatus())) {
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
                vehicle.setStatus("rented");
            } else if (hasActiveRental || hasPendingRental) {
                // Vehicle has approved/pending rental but not picked up - should be RESERVED
                vehicle.setStatus("reserved");
            } else {
                // No active or pending rentals
                if (vehicle.hasFutureBookings()) {
                    vehicle.setStatus("reserved");
                } else {
                    vehicle.setStatus("available");
                }
            }
        }
    }

    /**
     * Add new account to the system
     */
    // Initiate rental process
    public static void initiateRental(RentalSystem system, Scanner scanner, Account account) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                          VEHICLE BOOKING                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");

        // Display all vehicles with their status (include available & reserved)
        vehicleService.displayAvailableVehicles();

        String plateNo;
        Vehicle selected = null;
        while (selected == null) {
            System.out.println(
                    "\nNote: Enter vehicle plate number to see detailed availability and view unavailable period for reserved vehicle");
            System.out.print("\nEnter vehicle plate number to rent (or type 'search' to search, 'exit' to return): ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Returning to main menu...");
                return;
            }

            if (input.equalsIgnoreCase("search")) {
                searchInBookingProcess(system, scanner, account);
                continue;
            }

            // Check if input is a valid plate number
            if (!input.isEmpty()) {
                plateNo = input;
                selected = vehicleService.findVehicleByPlateNo(plateNo);
                if (selected != null) {
                    break;
                }else{
                    System.out.println("Vehicle not found.");
                }
            } else {
                System.out.println("Invalid input. Please enter a valid vehicle plate number, 'search', or 'exit'.");

            }
        }

        
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
        int vehicleId = selected.getId(); // Get ID from selected vehicle
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

        // Also check for existing pending rental by the same user for this vehicle
        Rental pendingRental = system.findPendingRentalByUserAndVehicle(account.getUsername(), vehicleId);
        if (pendingRental != null) {
            System.out.println("\n=== EXISTING RENTAL DETECTED ===\n");
            System.out.printf("You already have a pending rental for this vehicle:\n");
            System.out.printf("  Current rental period: %s to %s\n", pendingRental.getStartDate(), pendingRental.getEndDate());
            System.out.printf("  Total fee: RM%.2f\n", pendingRental.getTotalFee());
            System.out.println("This booking is awaiting approval. You can choose another period if needed.");

            if (AccountService.getYesNoInput(scanner, "\nDo you want to extend this pending rental instead?")) {
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

                LocalDate newEndDate = pendingRental.getEndDate().plusDays(additionalDays);

                // Show extension details
                System.out.println("\n=== Extension Details ===\n");
                System.out.printf("Original end date: %s\n", pendingRental.getEndDate());
                System.out.printf("New proposed end date: %s\n", newEndDate);
                System.out.printf("Additional days: %d\n", additionalDays);

                int totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(pendingRental.getStartDate(),
                        newEndDate) + 1;
                double discount = selected.getDiscountForDays(totalDays);
                if (discount > 0) {
                    System.out.printf("New total period: %d days\n", totalDays);
                    System.out.printf("Long-term discount: %.1f%% off\n", discount * 100);
                }

                double insuranceRate2 = selected.getInsuranceRate();
                System.out.printf("\nInsurance rate: %.1f%%\n", insuranceRate2 * 100);
                boolean insurance2 = AccountService.getYesNoInput(scanner, "Include insurance for the extended period?");

                if (system.extendPendingRental(account.getUsername(), vehicleId, newEndDate, insurance2)) {
                    System.out.println("\n=== PENDING RENTAL EXTENSION SUCCESSFUL ===\n");
                    System.out.printf("Extended until: %s\n", newEndDate);
                } else {
                    System.out.println("Failed to extend pending rental. Please try again or contact support.");
                }

                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
                return;
            }
        }

        // If not extending, or no existing rental, proceed with new booking process
        // Check if vehicle is available for booking
        if ("out_of_service".equalsIgnoreCase(selected.getStatus())) {
            System.out.println("\n*** VEHICLE NOT AVAILABLE FOR BOOKING ***\n");
            System.out.println("Reason: Vehicle status is " + selected.getStatus());

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
            System.out.println("This period conflicts with existing bookings.");
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
        models.Customer customer = new models.Customer(customerName, contact);

        // Final confirmation and booking
        double totalFee = system.calculateRentalFee(selected, startDate, endDate, insurance);
        System.out.println("\n=== Rental Summary ===\n");
        System.out.println("Vehicle: " + selected.getBrand() + " " + selected.getModel());
        System.out.println("Type: " + selected.getVehicleType() + " | Fuel: " + selected.getFuelType());
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);
        System.out.println("Insurance: " + (insurance ? "Included" : "Not included"));
        System.out.printf("Total Fee: RM%.2f\n", totalFee);

        if (AccountService.getYesNoInput(scanner, "\nConfirm booking?")) {
            try {
                clearScreen();
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
                System.out.println("ID: " + r.getId() + " | Vehicle: " + r.getVehicle().getPlateNo() + " - " + r.getVehicle().getBrand() +
                        " " + r.getVehicle().getModel() + " | Period: " + r.getStartDate() +
                        " to " + r.getEndDate() + " | Est. Fee: RM" + String.format("%.2f", r.getTotalFee()));
            }
        }

        // Display active rentals
        if (!activeRentals.isEmpty()) {
            System.out.println("\n[ACTIVE RENTALS] - Currently in Use");
            System.out.println("================================================================");
            for (Rental r : activeRentals) {
                System.out.println("ID: " + r.getId() + " | Vehicle: " + r.getVehicle().getPlateNo() + " - " + r.getVehicle().getBrand() +
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
        vehicleService.displaySearchResults(searchResults);
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

            // Process vehicle return
            System.out.println("\n=== Processing Vehicle Return ===");
            
            Vehicle vehicle = rental.getVehicle();
            System.out.printf("Returning Vehicle: %s %s (Car Plate: %s)\n",
                    vehicle.getBrand(), vehicle.getModel(), vehicle.getCarPlate());

            if (system.returnVehicle(rentalId, scanner)) {
                System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
                System.out.println("║                      RETURN SUMMARY                             ║");
                System.out.println("╚══════════════════════════════════════════════════════════════════╝");
                System.out.println(" Vehicle returned successfully.");
                System.out.printf("Vehicle: %s %s\n", vehicle.getBrand(), vehicle.getModel());
                System.out.printf("Final Fee: RM%.2f\n", rental.getActualFee());
                System.out.println("Vehicle is ready for next rental.");
                
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

            String vehicleInfo = String.format("%s - %s %s", 
                r.getVehicle().getPlateNo(),
                r.getVehicle().getBrand(), 
                r.getVehicle().getModel());
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

            // Process vehicle return
            System.out.println("\n=== Processing Vehicle Return ===");
            
            Vehicle vehicle = rental.getVehicle();
            System.out.printf("Confirming return for Vehicle: %s %s (Car Plate: %s)\n",
                    vehicle.getBrand(), vehicle.getModel(), vehicle.getCarPlate());
            System.out.printf("Customer: %s\n", rental.getCustomer().getName());

            // Execute return (simplified)
            if (system.returnVehicle(rentalId, scanner)) {
                System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
                System.out.println("║                      RETURN SUMMARY                              ║");
                System.out.println("╚══════════════════════════════════════════════════════════════════╝");
                System.out.println(" Vehicle return confirmed successfully.");
                System.out.printf("Vehicle: %s %s\n", vehicle.getBrand(), vehicle.getModel());
                System.out.printf("Customer: %s\n", rental.getCustomer().getName());
                System.out.printf("Final Fee: RM%.2f\n", rental.getActualFee());

                System.out.println("Vehicle is ready for next rental.");

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
            System.out.println("\n╔═══════════════════════════════════════════════════════════════════════════════════╗");
            System.out.println("║                                 RENTAL MANAGEMENT                                 ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════════════════════╝");

            List<Rental> pendingRentals = system.getPendingRentals();
            if (pendingRentals.isEmpty()) {
                System.out.println("\nNo pending rentals.");
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
                return;
            }

            System.out.println("\nPending Rentals:");
            System.out.println("┌─────┬─────────────┬──────────────────────────────────┬─────────────┬─────────────┬─────────────┐");
            System.out.println("│ ID  │ User        │ Vehicle                          │ Start Date  │ End Date    │ Fee         │");
            System.out.println("├─────┼─────────────┼──────────────────────────────────┼─────────────┼─────────────┼─────────────┤");

            for (Rental rental : pendingRentals) {
                // Create vehicle info with plate number and brand/model
                String vehicleInfo = String.format("%s - %s %s", 
                    rental.getVehicle().getPlateNo(),
                    rental.getVehicle().getBrand(), 
                    rental.getVehicle().getModel());
                if (vehicleInfo.length() > 25) {
                    vehicleInfo = vehicleInfo.substring(0, 24) + "...";
                }
                
                System.out.printf("│ %-3d │ %-11s │ %-24s │ %-11s │ %-11s │ RM%-9.2f │%n",
                        rental.getId(),
                        rental.getUsername() != null
                                ? (rental.getUsername().length() > 11 ? rental.getUsername().substring(0, 11)
                                        : rental.getUsername())
                                : "N/A",
                        vehicleInfo,
                        rental.getStartDate().toString(),
                        rental.getEndDate().toString(),
                        rental.getTotalFee());
            }
            System.out.println("└─────┴─────────────┴──────────────────────────────────┴─────────────┴─────────────┴─────────────┘");

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
        int rentalId = 0;
        while (rentalId <= 0) {
            System.out.print("Enter rental ID to reject: ");
            String idStr = scanner.nextLine().trim();
            if (idStr.isEmpty()) {
                System.out.println("Rental ID cannot be empty. Please try again.");
                continue;
            }
            try {
                rentalId = Integer.parseInt(idStr);
                if (rentalId <= 0) {
                    System.out.println("Rental ID must be a positive number. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid rental ID format. Please enter a valid number.");
            }
        }

        System.out.print("Enter rejection reason: ");
        String reason = scanner.nextLine().trim();
        if (reason.isEmpty()) {
            reason = "No reason provided";
        }

        if (system.cancelRental(rentalId, reason)) {
            System.out.println("Rental rejected successfully with reason: " + reason);
        } else {
            System.out.println("Failed to reject rental. Please check the rental ID.");
        }
    }

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
                    rental.getVehicle().setStatus("rented");

                    // Also update the vehicle in vehicleService to keep data in sync
                    Vehicle vehicleInService = vehicleService.findVehicleById(rental.getVehicle().getId());
                    if (vehicleInService != null) {
                        vehicleInService.setStatus("rented");
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

        List<Account> customerAccounts = AccountService.searchCustomerAccounts(searchTerm);
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
        vehicleService.displayAvailableVehicles();

        System.out.print("\nEnter vehicle plate number: ");
        String plateNo = scanner.nextLine().trim();
        
        if (plateNo.isEmpty()) {
            System.out.println("Invalid plate number format.");
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
            return;
        }

        Vehicle selectedVehicle = vehicleService.findVehicleByPlateNo(plateNo);
        if (selectedVehicle == null) {
            System.out.println("Vehicle not found.");
            return;
        }

        if ("out_of_service".equalsIgnoreCase(selectedVehicle.getStatus())) {
            System.out.println("Vehicle not available for booking. Status: " + selectedVehicle.getStatus());
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
            return;
        }

        // Check for existing active rental by the same customer for this vehicle
        int vehicleId = selectedVehicle.getId(); // Get ID from selected vehicle
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
        double totalFee = system.calculateRentalFee(selectedVehicle, startDate, endDate, insurance);
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
            models.Customer customer = new models.Customer(customerName, contact);
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
                clearScreen();
            }
            firstTime = false;

            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                      Booking Management                          ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║  1. View All Vehicles                                            ║");
            System.out.println("║  2. View All Rentals                                             ║");
            System.out.println("║  3. Rental Management (Pending/Approve/Reject)                   ║");
            System.out.println("║  4. Confirm Vehicle Return                                       ║");
            System.out.println("║  5. View Reminders                                               ║");
            System.out.println("║  6. Ticket Management                                            ║");
            System.out.println("║  7. Message Center                                               ║");
            System.out.println("║  8. Offline Booking (Walk-in Customers)                          ║");
            System.out.println("║  0. Back to Main Menu                                            ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    vehicleService.displayAllVehicles();
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
                    ReminderService reminderService = new ReminderService(system);
                    reminderService.displayReminderSummary();
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
                case "6":
                    ticketManagement(system, scanner);
                    break;
                case "7":
                    MessageService.messageCenter(system, scanner, "admin");
                    break;
                case "8":
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
                clearScreen();
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
                clearScreen();
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
                    vehicleService.displayAvailableVehicles();
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