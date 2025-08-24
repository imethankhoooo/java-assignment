package services;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import enums.*;
import models.*;

/**
 * Service class for generating reports and exporting data
 */
public class ReportService {
    
    /**
     * Generate monthly rental statistics with export option
     */
    public static void generateMonthlyReport(List<Rental> rentals, Scanner scanner) {
        Map<String, Integer> monthlyRentals = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (Rental rental : rentals) {
            String month = rental.getStartDate().format(formatter);
            monthlyRentals.put(month, monthlyRentals.getOrDefault(month, 0) + 1);
        }

        System.out.println("\n--- Monthly Rental Report ---");
        List<String> headers = Arrays.asList("Month", "Total Rentals");
        List<List<String>> data = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : monthlyRentals.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + " rentals");
            data.add(Arrays.asList(entry.getKey(), String.valueOf(entry.getValue())));
        }
        
        ReportExportService exportService = new ReportExportService();
        exportService.promptForExport(scanner, "Monthly Rental Report", headers, data, "monthly_report");
    }
    
    /**
     * Generate popular vehicle report with export option
     */
    public static void generatePopularVehicleReport(List<Rental> rentals, Scanner scanner) {
        Map<String, Integer> vehicleRentals = new HashMap<>();

        for (Rental rental : rentals) {
            String vehicleModel = rental.getVehicle().getBrand() + " " + rental.getVehicle().getModel();
            vehicleRentals.put(vehicleModel, vehicleRentals.getOrDefault(vehicleModel, 0) + 1);
        }

        System.out.println("\n--- Popular Vehicle Report ---");
        List<String> headers = Arrays.asList("Vehicle", "Total Rentals");
        List<List<String>> data = new ArrayList<>();
        vehicleRentals.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    System.out.println(entry.getKey() + ": " + entry.getValue() + " rentals");
                    data.add(Arrays.asList(entry.getKey(), String.valueOf(entry.getValue())));
                });

        ReportExportService exportService = new ReportExportService();
        exportService.promptForExport(scanner, "Popular Vehicle Report", headers, data, "popular_vehicles_report");
    }
    
    /**
     * Generate customer report
     */
    public static void generateCustomerReport(List<Rental> rentals) {
        Map<String, Integer> customerStats = new HashMap<>();
        Map<String, Double> customerRevenue = new HashMap<>();
        
        for (Rental rental : rentals) {
            if (rental.getStatus() == RentalStatus.RETURNED) {
                String customerName = rental.getCustomer().getName();
                customerStats.put(customerName, customerStats.getOrDefault(customerName, 0) + 1);
                customerRevenue.put(customerName, customerRevenue.getOrDefault(customerName, 0.0) + rental.getTotalFee());
            }
        }
        
        System.out.println("\n=== Customer Report ===");
        for (Map.Entry<String, Integer> entry : customerStats.entrySet()) {
            String customer = entry.getKey();
            int count = entry.getValue();
            double revenue = customerRevenue.getOrDefault(customer, 0.0);
            System.out.println("Customer: " + customer + ", Rentals: " + count + 
                             ", Total Spent: RM" + String.format("%.2f", revenue));
        }
    }
    

    
    /**
     * Generate comprehensive system report
     */
    public static void generateSystemReport(List<Rental> rentals, List<Vehicle> vehicles, List<Customer> customers) {
        System.out.println("\n=== SYSTEM REPORT ===");
        System.out.println("Generated on: " + LocalDate.now());
        System.out.println();
        
        // Overall statistics
        int totalRentals = rentals.size();
        int activeRentals = 0;
        int completedRentals = 0;
        int cancelledRentals = 0;
        int pendingRentals = 0;
        double totalRevenue = 0;
        int insurancePurchases = 0;
        
        for (Rental rental : rentals) {
            switch (rental.getStatus()) {
                case ACTIVE:
                    activeRentals++;
                    break;
                case RETURNED:
                    completedRentals++;
                    totalRevenue += rental.getTotalFee();
                    break;
                case CANCELLED:
                    cancelledRentals++;
                    break;
                case PENDING:
                    pendingRentals++;
                    break;
            }
            
            if (rental.isInsuranceSelected()) {
                insurancePurchases++;
            }
        }
        
        System.out.println("RENTAL STATISTICS:");
        System.out.println("Total Rentals: " + totalRentals);
        System.out.println("Active Rentals: " + activeRentals);
        System.out.println("Completed Rentals: " + completedRentals);
        System.out.println("Pending Rentals: " + pendingRentals);
        System.out.println("Cancelled Rentals: " + cancelledRentals);
        System.out.println("Total Revenue: RM" + String.format("%.2f", totalRevenue));
        System.out.println("Insurance Purchases: " + insurancePurchases + " (" + 
                         String.format("%.1f", (double)insurancePurchases/totalRentals*100) + "%)");
        System.out.println();
        
        // Vehicle statistics
        int availableVehicles = 0;
        int rentedVehicles = 0;
        int maintenanceVehicles = 0;
        
        for (Vehicle vehicle : vehicles) {
            switch (vehicle.getStatus()) {
                case AVAILABLE:
                    availableVehicles++;
                    break;
                case RESERVED:
                    rentedVehicles++;
                    break;
                case RENTED:
                    rentedVehicles++;
                    break;
                case UNDER_MAINTENANCE:
                    maintenanceVehicles++;
                    break;
                case OUT_OF_SERVICE:
                    maintenanceVehicles++;
                    break;
            }
        }
        
        System.out.println("VEHICLE STATISTICS:");
        System.out.println("Total Vehicles: " + vehicles.size());
        System.out.println("Available: " + availableVehicles);
        System.out.println("Rented: " + rentedVehicles);
        System.out.println("In Maintenance: " + maintenanceVehicles);
        System.out.println();
        
        System.out.println("CUSTOMER STATISTICS:");
        System.out.println("Total Customers: " + customers.size());
        System.out.println();
        
        // Average rental duration
        if (completedRentals > 0) {
            long totalDays = 0;
            for (Rental rental : rentals) {
                if (rental.getStatus() == RentalStatus.RETURNED) {
                    totalDays += java.time.temporal.ChronoUnit.DAYS.between(rental.getStartDate(), rental.getEndDate()) + 1;
                }
            }
            double avgDuration = (double)totalDays / completedRentals;
            System.out.println("Average Rental Duration: " + String.format("%.1f", avgDuration) + " days");
        }
        
        if (completedRentals > 0) {
            double avgRevenue = totalRevenue / completedRentals;
            System.out.println("Average Revenue per Rental: RM" + String.format("%.2f", avgRevenue));
        }
    }
} 