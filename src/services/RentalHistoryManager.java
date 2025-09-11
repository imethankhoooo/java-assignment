package services;

import java.util.*;
import models.*;

/**
 * Manages rental history storage and retrieval.
 * Acts as the backbone for reporting.
 */
public class RentalHistoryManager {

    private final List<Rental> rentalHistory;

    // === Constructor ===
    public RentalHistoryManager() {
        this.rentalHistory = new ArrayList<>();
    }

    // === Getter ===
    public List<Rental> getRentalHistory() {
        return Collections.unmodifiableList(rentalHistory);
    }

    // === Setter (add rental) ===
    public void addRental(Rental rental) {
        rentalHistory.add(rental);
    }

    // === Functions ===

    /** Clear all history */
    public void clearHistory() {
        rentalHistory.clear();
    }

    /** Find rentals by a specific customer */
    public List<Rental> findByCustomer(Customer customer) {
        List<Rental> results = new ArrayList<>();
        for (Rental rental : rentalHistory) {
            if (rental.getCustomer().equals(customer)) {
                results.add(rental);
            }
        }
        return results;
    }

    /** Find rentals by a specific vehicle */
    public List<Rental> findByVehicle(Vehicle vehicle) {
        List<Rental> results = new ArrayList<>();
        for (Rental rental : rentalHistory) {
            if (rental.getVehicle().equals(vehicle)) {
                results.add(rental);
            }
        }
        return results;
    }

    /** Find rentals within a given date range */
    public List<Rental> findByDateRange(Date startDate, Date endDate) {
        List<Rental> results = new ArrayList<>();
        for (Rental rental : rentalHistory) {
            Date rentalStart = java.sql.Date.valueOf(rental.getStartDate());
            Date rentalEnd = java.sql.Date.valueOf(rental.getEndDate());

            if (!rentalEnd.before(startDate) && !rentalStart.after(endDate)) {
                results.add(rental);
            }
        }
        return results;
    }
}
