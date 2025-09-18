package services;

import java.util.*;
import models.*;

/**
 * Central manager for generating reports,
 * integrates RentalHistoryManager with ReportService.
 */
public class ReportManager {

    private final RentalHistoryManager rentalHistoryManager;

    // === Constructor ===
    public ReportManager(RentalHistoryManager rentalHistoryManager) {
        this.rentalHistoryManager = rentalHistoryManager;
    }

    // === Getters / Setters ===
    public List<Rental> getAllRentals() {
        return rentalHistoryManager.getRentalHistory();
    }

    public void addRental(Rental rental) {
        rentalHistoryManager.addRental(rental);
    }

    /** Quick run of monthly report */
    public void runMonthlyReport(Scanner scanner) {
        ReportService.generateMonthlyReport(rentalHistoryManager.getRentalHistory(), scanner);
    }

    /** Quick run of popular vehicle report */
    public void runPopularVehicleReport(Scanner scanner) {
        ReportService.generatePopularVehicleReport(rentalHistoryManager.getRentalHistory(), scanner);
    }

    /** Quick run of customer report */
    public void runCustomerReport() {
        ReportService.generateCustomerReport(rentalHistoryManager.getRentalHistory());
    }

    /** Quick run of system report */
    public void runSystemReport(List<Vehicle> vehicles, List<Customer> customers) {
        ReportService.generateSystemReport(
            rentalHistoryManager.getRentalHistory(),
            vehicles,
            customers
        );
    }
}
