package services;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.*;

/**
 * Ticket Service - manages rental confirmation tickets
 */
public class TicketService {
    private Map<String, Ticket> tickets; // ticketId -> Ticket
    private Map<String, List<Ticket>> customerTickets; // customerName -> List of tickets
    private Map<Integer, Ticket> rentalTickets; // rentalId -> Ticket
    
    public TicketService() {
        this.tickets = new HashMap<>();
        this.customerTickets = new HashMap<>();
        this.rentalTickets = new HashMap<>();
    }
    
    /**
     * Generate a new ticket for approved rental
     */
    public Ticket generateTicket(Rental rental) {
        // Check if ticket already exists for this rental
        if (rentalTickets.containsKey(rental.getId())) {
            System.out.println("Warning: Ticket already exists for rental ID " + rental.getId());
            // Update existing ticket with latest rental information
            Ticket existingTicket = rentalTickets.get(rental.getId());
            existingTicket.updateFromRental(rental);
            System.out.println("Ticket updated with new rental information: " + existingTicket.getTicketId());
            return existingTicket;
        }
        
        // Create new ticket
        Ticket ticket = new Ticket(rental);
        
        // Store in maps
        tickets.put(ticket.getTicketId(), ticket);
        rentalTickets.put(rental.getId(), ticket);
        
        // Store in customer tickets
        String customerName = ticket.getCustomerName();
        customerTickets.computeIfAbsent(customerName, k -> new ArrayList<>()).add(ticket);
        
        System.out.println("Ticket generated: " + ticket.getTicketId());
        return ticket;
    }
    
    /**
     * Get ticket by ticket ID
     */
    public Ticket getTicketById(String ticketId) {
        return tickets.get(ticketId);
    }
    
    /**
     * Get ticket by rental ID
     */
    public Ticket getTicketByRentalId(int rentalId) {
        return rentalTickets.get(rentalId);
    }
    
    /**
     * Get all tickets for a customer
     */
    public List<Ticket> getCustomerTickets(String customerName) {
        return customerTickets.getOrDefault(customerName, new ArrayList<>());
    }

    /**
     * Load an existing ticket (used when loading from JSON)
     */
    public void loadTicket(Ticket ticket) {
        // Store in maps
        tickets.put(ticket.getTicketId(), ticket);
        rentalTickets.put(ticket.getRentalId(), ticket);
        
        // Store in customer tickets
        String customerName = ticket.getCustomerName();
        customerTickets.computeIfAbsent(customerName, k -> new ArrayList<>()).add(ticket);
    }
    
    /**
     * Get all valid (unused) tickets for a customer
     */
    public List<Ticket> getValidCustomerTickets(String customerName) {
        List<Ticket> allTickets = getCustomerTickets(customerName);
        List<Ticket> validTickets = new ArrayList<>();
        
        for (Ticket ticket : allTickets) {
            if (!ticket.isUsed()) {
                validTickets.add(ticket);
            }
        }
        
        return validTickets;
    }
    
    /**
     * Validate and use a ticket
     */
    public boolean validateAndUseTicket(String ticketId, String customerName) {
        Ticket ticket = tickets.get(ticketId);
        
        if (ticket == null) {
            System.out.println("Error: Ticket not found.");
            return false;
        }
        
        if (ticket.isUsed()) {
            System.out.println("Error: Ticket has already been used.");
            return false;
        }
        
        if (!ticket.getCustomerName().equals(customerName)) {
            System.out.println("Error: Ticket does not belong to this customer.");
            return false;
        }
        
        ticket.markAsUsed();
        System.out.println(" Ticket validated and marked as used.");
        return true;
    }
    
    /**
     * Display all tickets for a customer
     */
    public void displayCustomerTickets(String customerName) {
        List<Ticket> customerTicketList = getCustomerTickets(customerName);
        
        if (customerTicketList.isEmpty()) {
            System.out.println("No tickets found for customer: " + customerName);
            return;
        }
        
        System.out.println("\n=== Your Rental Tickets ===");
        System.out.println("Total tickets: " + customerTicketList.size());
        
        for (int i = 0; i < customerTicketList.size(); i++) {
            Ticket ticket = customerTicketList.get(i);
            System.out.println("\n" + (i + 1) + ". " + (ticket.isUsed() ? "[USED]" : "[VALID]"));
            ticket.displayCompact();
        }
    }
    
    /**
     * Display detailed ticket information
     */
    public void displayTicketDetails(String ticketId) {
        Ticket ticket = tickets.get(ticketId);
        
        if (ticket == null) {
            System.out.println("Ticket not found: " + ticketId);
            return;
        }
        
        ticket.displayTicket();
    }
    
    /**
     * Get ticket statistics
     */
    public Map<String, Integer> getTicketStats() {
        Map<String, Integer> stats = new HashMap<>();
        
        int totalTickets = tickets.size();
        int usedTickets = 0;
        int validTickets = 0;
        
        for (Ticket ticket : tickets.values()) {
            if (ticket.isUsed()) {
                usedTickets++;
            } else {
                validTickets++;
            }
        }
        
        stats.put("total", totalTickets);
        stats.put("used", usedTickets);
        stats.put("valid", validTickets);
        stats.put("customers", customerTickets.size());
        
        return stats;
    }
    
    /**
     * Admin function: Display all tickets
     */
    public void displayAllTickets() {
        if (tickets.isEmpty()) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                     NO TICKETS FOUND                             ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ No tickets have been generated in the system yet.                ║");
            System.out.println("║ Tickets are created when admin approves rental requests.         ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            return;
        }
        
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                       ALL SYSTEM TICKETS                         ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Total Tickets: %-49d ║%n", tickets.size());
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        int index = 1;
        for (Ticket ticket : tickets.values()) {
            System.out.printf("\n%d. %s\n", index++, (ticket.isUsed() ? "🔴 [USED]" : "🟢 [VALID]"));
            System.out.println("┌──────────────────────────────────────────────────────────────────┐");
            System.out.printf("│ Ticket ID: %-53s │%n", ticket.getTicketId());
            System.out.printf("│ Rental ID: %-53d │%n", ticket.getRentalId());
            System.out.printf("│ Customer: %-54s │%n", ticket.getCustomerName());
            System.out.printf("│ Vehicle: %-55s │%n", ticket.getVehicleInfo() + " (" + ticket.getCarPlate() + ")");
            System.out.printf("│ Period: %-56s │%n", ticket.getStartDate() + " to " + ticket.getEndDate());
            System.out.printf("│ Total Fee: RM%-51.2f │%n", ticket.getTotalFee());
            System.out.println("└──────────────────────────────────────────────────────────────────┘");
        }
    }
    
    /**
     * Check if customer has valid tickets
     */
    public boolean hasValidTickets(String customerName) {
        return !getValidCustomerTickets(customerName).isEmpty();
    }
} 