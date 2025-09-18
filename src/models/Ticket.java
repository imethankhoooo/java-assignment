package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Rental Ticket class - represents a booking confirmation ticket Generated when
 * admin approves a rental request
 */
public class Ticket {

    private String ticketId;
    private int rentalId;
    private String customerName;
    private String customerContact;
    private String vehicleInfo;
    private String carPlate;
    private String startDate;
    private String endDate;
    private double totalFee;
    private boolean insuranceIncluded;
    private LocalDateTime generatedTime;
    private String pickupLocation;
    private String specialInstructions;
    private boolean isUsed;

    // Constructor
    public Ticket(Rental rental) {
        this.ticketId = generateTicketId();
        this.rentalId = rental.getId();
        this.customerName = rental.getCustomer().getName();
        this.customerContact = rental.getCustomer().getContact();
        this.vehicleInfo = rental.getVehicle().getBrand() + " " + rental.getVehicle().getModel();
        this.carPlate = rental.getVehicle().getCarPlate();
        this.startDate = rental.getStartDate().toString();
        this.endDate = rental.getEndDate().toString();
        this.totalFee = rental.getTotalFee();
        this.insuranceIncluded = rental.isInsuranceSelected();
        this.generatedTime = LocalDateTime.now();
        this.pickupLocation = "Main Office - CarSeek HQ";
        this.specialInstructions = "Please bring valid ID and this ticket for vehicle pickup";
        this.isUsed = false;
    }

    // Generate unique ticket ID
    private String generateTicketId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "TKT-" + timestamp + "-" + randomPart;
    }

    /**
     * Update ticket information from rental (for rental extensions)
     */
    public void updateFromRental(Rental rental) {
        // Update rental information but keep original ticket ID
        this.customerName = rental.getCustomer().getName();
        this.customerContact = rental.getCustomer().getContact();
        this.vehicleInfo = rental.getVehicle().getBrand() + " " + rental.getVehicle().getModel();
        this.carPlate = rental.getVehicle().getCarPlate();
        this.startDate = rental.getStartDate().toString();
        this.endDate = rental.getEndDate().toString();
        this.totalFee = rental.getTotalFee();
        this.insuranceIncluded = rental.isInsuranceSelected();
        this.generatedTime = LocalDateTime.now(); // Update generation time
        // Keep pickup location and special instructions unchanged
        // Reset used status for updated ticket
        this.isUsed = false;
    }

    // Display ticket information
    public void displayTicket() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    RENTAL CONFIRMATION TICKET                    ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Ticket ID: %-53s ║%n", ticketId);
        System.out.printf("║ Rental ID: %-53d ║%n", rentalId);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Customer: %-54s ║%n", customerName);
        System.out.printf("║ Contact: %-54s  ║%n", customerContact);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Vehicle: %-55s ║%n", vehicleInfo);
        System.out.printf("║ Car Plate: %-53s ║%n", carPlate);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Rental Period: %-49s ║%n", startDate + " to " + endDate);
        System.out.printf("║ Total Fee: RM%-51.2f ║%n", totalFee);
        System.out.printf("║ Insurance: %-53s ║%n", insuranceIncluded ? "Included" : "Not included");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Pickup Location: %-47s ║%n", pickupLocation);
        System.out.printf("║ Generated: %-53s ║%n", generatedTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.printf("║ Status: %-56s ║%n", isUsed ? "Used" : "Valid");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ IMPORTANT INSTRUCTIONS:                                          ║");
        System.out.println("║ - Bring valid government-issued ID                               ║");
        System.out.println("║ - Present this ticket at pickup location                         ║");
        System.out.println("║ - Arrive 15 minutes before rental start time                     ║");
        System.out.println("║ - Vehicle inspection will be conducted before handover           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }

    // Compact display for lists
    public void displayCompact() {
        System.out.printf("Ticket: %s | Rental: %d | Vehicle: %s (%s) | Period: %s to %s | Fee: RM%.2f%n",
                ticketId, rentalId, vehicleInfo, carPlate, startDate, endDate, totalFee);
    }

    // Mark ticket as used
    public void markAsUsed() {
        this.isUsed = true;
    }

    // Getters
    public String getTicketId() {
        return ticketId;
    }

    public int getRentalId() {
        return rentalId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerContact() {
        return customerContact;
    }

    public String getVehicleInfo() {
        return vehicleInfo;
    }

    public String getCarPlate() {
        return carPlate;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public double getTotalFee() {
        return totalFee;
    }

    public boolean isInsuranceIncluded() {
        return insuranceIncluded;
    }

    public LocalDateTime getGeneratedTime() {
        return generatedTime;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    public boolean isUsed() {
        return isUsed;
    }

    // Setters
    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }
}
