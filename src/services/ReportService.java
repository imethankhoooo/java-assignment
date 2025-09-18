package services;
import enums.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import models.Customer;
import models.Rental;
import models.Vehicle;

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
        
        // Vehicle statistics (excluding archived vehicles)
        int availableVehicles = 0;
        int rentedVehicles = 0;
        int outOfServiceVehicles = 0;
        int archivedVehicles = 0;
        
        for (Vehicle vehicle : vehicles) {
            if (vehicle.isArchived()) {
                archivedVehicles++;
                continue; // Skip archived vehicles from status counting
            }
            
            String status = vehicle.getStatus().toUpperCase();
            switch (status) {
                case "AVAILABLE":
                    availableVehicles++;
                    break;
                case "RESERVED":
                    rentedVehicles++;
                    break;
                case "RENTED":
                    rentedVehicles++;
                    break;
                case "OUT_OF_SERVICE":
                    outOfServiceVehicles++;
                    break;
            }
        }
        
        System.out.println("VEHICLE STATISTICS:");
        System.out.println("Total Vehicles: " + vehicles.size());
        System.out.println("Active Vehicles: " + (vehicles.size() - archivedVehicles));
        System.out.println("Available: " + availableVehicles);
        System.out.println("Rented: " + rentedVehicles);
        System.out.println("Out of Service: " + outOfServiceVehicles);
        System.out.println("Archived: " + archivedVehicles);
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
    /**
     * Get comprehensive system statistics
     * @param rentals List of all rentals
     * @param vehicles List of all vehicles
     * @return Map containing all system statistics
     */
    public static Map<String, Object> getSystemStatistics(List<Rental> rentals, List<Vehicle> vehicles) {
        Map<String, Object> stats = new HashMap<>();

        int totalRentals = rentals.size();
        long activeRentals = rentals.stream().filter(r -> r.getStatus() == RentalStatus.ACTIVE).count();
        long completedRentals = rentals.stream().filter(r -> r.getStatus() == RentalStatus.RETURNED).count();
        long pendingRentals = rentals.stream().filter(r -> r.getStatus() == RentalStatus.PENDING).count();
        double totalRevenue = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.RETURNED)
                .mapToDouble(Rental::getTotalFee)
                .sum();

        long totalVehicles = vehicles.size();
        long archivedVehicles = vehicles.stream().filter(Vehicle::isArchived).count();
        long activeVehicles = totalVehicles - archivedVehicles;
        long availableVehicles = vehicles.stream()
                .filter(v -> !v.isArchived() && "available".equalsIgnoreCase(v.getStatus())).count();
        long rentedVehicles = vehicles.stream()
                .filter(v -> !v.isArchived() && "rented".equalsIgnoreCase(v.getStatus())).count();
        long outOfServiceVehicles = vehicles.stream()
                .filter(v -> !v.isArchived() && "out_of_service".equalsIgnoreCase(v.getStatus())).count();

        stats.put("totalVehicles", totalVehicles);
        stats.put("activeVehicles", activeVehicles);
        stats.put("archivedVehicles", archivedVehicles);
        stats.put("availableVehicles", availableVehicles);
        stats.put("rentedVehicles", rentedVehicles);
        stats.put("outOfServiceVehicles", outOfServiceVehicles);
        stats.put("totalRentals", totalRentals);
        stats.put("activeRentals", activeRentals);
        stats.put("completedRentals", completedRentals);
        stats.put("pendingRentals", pendingRentals);
        stats.put("totalRevenue", totalRevenue);
        stats.put("averageRevenue", completedRentals > 0 ? totalRevenue / completedRentals : 0.0);

        List<String> headers = Arrays.asList("Metric", "Value");
        List<List<String>> data = Arrays.asList(
                Arrays.asList("Total Vehicles", String.valueOf(totalVehicles)),
                Arrays.asList("Active Vehicles", String.valueOf(activeVehicles)),
                Arrays.asList("Archived Vehicles", String.valueOf(archivedVehicles)),
                Arrays.asList("Available Vehicles", String.valueOf(availableVehicles)),
                Arrays.asList("Rented Vehicles", String.valueOf(rentedVehicles)),
                Arrays.asList("Out of Service Vehicles", String.valueOf(outOfServiceVehicles)),
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
} 