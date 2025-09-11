package services;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.stream.Collectors;

import enums.*;
import models.*;
import models.Vehicle;
import models.Rental;
import models.Customer;
import models.Account;

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

    // ===== 系统统计功能 =====

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

    /**
     * Display system statistics in a formatted table
     * @param stats Statistics map from getSystemStatistics
     */
    public static void displaySystemStatistics(Map<String, Object> stats) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      SYSTEM STATISTICS                           ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");

        System.out.printf("║ Total Vehicles:        │ %-39d ║%n", stats.get("totalVehicles"));
        System.out.printf("║ Active Vehicles:       │ %-39d ║%n", stats.get("activeVehicles"));
        System.out.printf("║ Archived Vehicles:     │ %-39d ║%n", stats.get("archivedVehicles"));
        System.out.printf("║ Available Vehicles:    │ %-39d ║%n", stats.get("availableVehicles"));
        System.out.printf("║ Rented Vehicles:       │ %-39d ║%n", stats.get("rentedVehicles"));
        System.out.printf("║ Out of Service:        │ %-39d ║%n", stats.get("outOfServiceVehicles"));
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Total Rentals:         │ %-39d ║%n", stats.get("totalRentals"));
        System.out.printf("║ Active Rentals:        │ %-39d ║%n", stats.get("activeRentals"));
        System.out.printf("║ Completed Rentals:     │ %-39d ║%n", stats.get("completedRentals"));
        System.out.printf("║ Pending Rentals:       │ %-39d ║%n", stats.get("pendingRentals"));
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Total Revenue:         │ RM%-37.2f ║%n", stats.get("totalRevenue"));
        System.out.printf("║ Average Revenue/Rental:│ RM%-37.2f ║%n",
                         (Double) stats.get("averageRevenue"));
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }

    /**
     * Export system statistics to file
     * @param stats Statistics map from getSystemStatistics
     * @param scanner Scanner for user input
     */
    public static void exportSystemStatistics(Map<String, Object> stats, Scanner scanner) {
        @SuppressWarnings("unchecked")
        List<String> headers = (List<String>) stats.get("exportHeaders");
        @SuppressWarnings("unchecked")
        List<List<String>> data = (List<List<String>>) stats.get("exportData");

        ReportExportService exportService = new ReportExportService();
        exportService.promptForExport(scanner, "System Statistics", headers, data, "system_statistics");
    }

    // ===== 租赁系统报表功能 =====

    /**
     * Generate comprehensive rental system report
     * @param rentals List of all rentals
     * @param vehicles List of all vehicles
     * @param accounts List of all accounts
     * @return Formatted report string
     */
    public static String generateRentalSystemReport(List<Rental> rentals, List<Vehicle> vehicles, List<Account> accounts) {
        StringBuilder report = new StringBuilder();
        report.append("=== Rental System Report ===\n");
        report.append("Generated Time: ").append(java.time.LocalDate.now()).append("\n\n");

        // Vehicle statistics (excluding archived)
        report.append("Vehicle Statistics:\n");
        List<Vehicle> activeVehicles = vehicles.stream()
                .filter(v -> !v.isArchived())
                .collect(java.util.stream.Collectors.toList());
        
        Map<String, Long> vehicleStats = activeVehicles.stream()
                .collect(java.util.stream.Collectors.groupingBy(models.Vehicle::getStatus,
                        java.util.stream.Collectors.counting()));
        
        long archivedCount = vehicles.stream().filter(Vehicle::isArchived).count();
        report.append("  Total Vehicles: ").append(vehicles.size()).append("\n");
        report.append("  Active Vehicles: ").append(activeVehicles.size()).append("\n");
        report.append("  Archived Vehicles: ").append(archivedCount).append("\n");

        for (Map.Entry<String, Long> entry : vehicleStats.entrySet()) {
            report.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // Rental statistics
        report.append("\nRental Statistics:\n");
        Map<enums.RentalStatus, Long> rentalStats = rentals.stream()
                .collect(java.util.stream.Collectors.groupingBy(models.Rental::getStatus,
                        java.util.stream.Collectors.counting()));

        for (Map.Entry<enums.RentalStatus, Long> entry : rentalStats.entrySet()) {
            report.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // Income statistics
        report.append("\nIncome Statistics:\n");
        double totalRevenue = rentals.stream()
                .filter(r -> r.getStatus() == enums.RentalStatus.RETURNED)
                .mapToDouble(models.Rental::getActualFee)
                .sum();

        double pendingRevenue = rentals.stream()
                .filter(r -> r.getStatus() == enums.RentalStatus.PENDING || r.getStatus() == enums.RentalStatus.ACTIVE)
                .mapToDouble(models.Rental::getTotalFee)
                .sum();

        report.append("  Lease income completed: RM").append(String.format("%.2f", totalRevenue)).append("\n");
        report.append("  Lease income to be completed: RM").append(String.format("%.2f", pendingRevenue)).append("\n");
        report.append("  Total: RM").append(String.format("%.2f", totalRevenue + pendingRevenue)).append("\n");

        // Overdue rentals
        report.append("\nOverdue rentals:\n");
        List<Rental> overdueRentals = rentals.stream()
                .filter(r -> r.getStatus() == enums.RentalStatus.ACTIVE && r.isOverdue())
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

    /**
     * Export rental system data to file
     * @param rentals List of all rentals
     * @param vehicles List of all vehicles
     * @param accounts List of all accounts
     * @param filename Output filename
     * @return true if export successful, false otherwise
     */
    public static boolean exportRentalSystemData(List<Rental> rentals, List<Vehicle> vehicles, List<Account> accounts, String filename) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(filename))) {
            writer.println("=== Leasing system data export ===");
            writer.println("Export time: " + java.time.LocalDate.now());
            writer.println();

            // Export vehicle data (excluding archived)
            writer.println("=== Vehicle data ===");
            for (Vehicle vehicle : vehicles) {
                if (!vehicle.isArchived()) {
                    writer.println("ID: " + vehicle.getVehicleID() +
                            ", Plate: " + vehicle.getPlateNo() +
                            ", Brand: " + vehicle.getBrand() +
                            ", Model: " + vehicle.getModel() +
                            ", Type: " + vehicle.getType() +
                            ", Fuel: " + vehicle.getFuelType() +
                            ", Color: " + vehicle.getColor() +
                            ", Year: " + vehicle.getYear() +
                            ", Capacity: " + vehicle.getCapacity() +
                            ", Condition: " + vehicle.getCondition() +
                            ", Status: " + vehicle.getStatus() +
                            ", Price: RM" + vehicle.getBasePrice() + "/day");
                }
            }
            
            // Export archived vehicles separately if any exist
            long archivedCount = vehicles.stream().filter(Vehicle::isArchived).count();
            if (archivedCount > 0) {
                writer.println();
                writer.println("=== Archived Vehicle data ===");
                for (Vehicle vehicle : vehicles) {
                    if (vehicle.isArchived()) {
                        writer.println("ID: " + vehicle.getVehicleID() +
                                ", Plate: " + vehicle.getPlateNo() +
                                ", Brand: " + vehicle.getBrand() +
                                ", Model: " + vehicle.getModel() +
                                ", Type: " + vehicle.getType() +
                                ", Status: " + vehicle.getStatus() +
                                " (ARCHIVED)");
                    }
                }
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
            for (Account account : accounts) {
                writer.println("Username: " + account.getUsername() +
                        ", Role: " + account.getRole());
            }

            return true;
        } catch (java.io.IOException e) {
            System.err.println("Export data failed: " + e.getMessage());
            return false;
        }
    }

    // ===== 详细车辆报表功能 =====

    /**
     * Generate detailed vehicle report with comprehensive information
     * @param vehicles List of all vehicles
     * @param scanner Scanner for user input
     */
    public static void generateDetailedVehicleReport(List<Vehicle> vehicles, Scanner scanner) {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                            DETAILED VEHICLE REPORT                                            ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════════════════════════════════════════════════╣");

        // Active vehicles
        List<Vehicle> activeVehicles = vehicles.stream()
                .filter(v -> !v.isArchived())
                .collect(java.util.stream.Collectors.toList());

        System.out.println("║ ACTIVE VEHICLES                                                                                               ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════════════════════════════════════════════════╣");

        if (activeVehicles.isEmpty()) {
            System.out.println("║ No active vehicles found.                                                                                    ║");
        } else {
            for (Vehicle vehicle : activeVehicles) {
                System.out.printf("║ ID: %-8s │ Plate: %-10s │ %s %-15s │ Type: %-10s │ Status: %-12s ║%n",
                        vehicle.getVehicleID(),
                        vehicle.getPlateNo(),
                        vehicle.getBrand(),
                        vehicle.getModel(),
                        vehicle.getType(),
                        vehicle.getStatus());
                System.out.printf("║ Fuel: %-8s │ Color: %-10s │ Year: %-4d │ Capacity: %-6.1fL │ Price: RM%-8.2f/day ║%n",
                        vehicle.getFuelType(),
                        vehicle.getColor(),
                        vehicle.getYear(),
                        vehicle.getCapacity(),
                        vehicle.getBasePrice());
                System.out.printf("║ Condition: %-10s │ Insurance Rate: %-6.2f%% │ Future Bookings: %-3s                            ║%n",
                        vehicle.getCondition(),
                        vehicle.getInsuranceRate(),
                        vehicle.hasFutureBookings() ? "Yes" : "No");
                System.out.println("╠═══════════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
            }
        }

        // Archived vehicles
        List<Vehicle> archivedVehicles = vehicles.stream()
                .filter(Vehicle::isArchived)
                .collect(java.util.stream.Collectors.toList());

        if (!archivedVehicles.isEmpty()) {
            System.out.println("║ ARCHIVED VEHICLES                                                                                            ║");
            System.out.println("╠═══════════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
            for (Vehicle vehicle : archivedVehicles) {
                System.out.printf("║ ID: %-8s │ Plate: %-10s │ %s %-15s │ Type: %-10s │ [ARCHIVED]        ║%n",
                        vehicle.getVehicleID(),
                        vehicle.getPlateNo(),
                        vehicle.getBrand(),
                        vehicle.getModel(),
                        vehicle.getType());
                System.out.println("╠═══════════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
            }
        }

        System.out.println("║ SUMMARY                                                                                                       ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Total Vehicles: %-3d │ Active: %-3d │ Archived: %-3d                                               ║%n",
                vehicles.size(), activeVehicles.size(), archivedVehicles.size());
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════════════════════════════════════╝");

        // Prepare export data
        List<String> headers = Arrays.asList("Vehicle ID", "Plate No", "Brand", "Model", "Type", "Fuel", 
                "Color", "Year", "Capacity", "Condition", "Status", "Price/Day", "Insurance Rate", "Archived");
        List<List<String>> data = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            data.add(Arrays.asList(
                    vehicle.getVehicleID(),
                    vehicle.getPlateNo(),
                    vehicle.getBrand(),
                    vehicle.getModel(),
                    vehicle.getType(),
                    vehicle.getFuelType(),
                    vehicle.getColor(),
                    String.valueOf(vehicle.getYear()),
                    String.format("%.1f", vehicle.getCapacity()),
                    vehicle.getCondition(),
                    vehicle.getStatus(),
                    String.format("RM%.2f", vehicle.getBasePrice()),
                    String.format("%.2f%%", vehicle.getInsuranceRate()),
                    vehicle.isArchived() ? "Yes" : "No"
            ));
        }

        ReportExportService exportService = new ReportExportService();
        exportService.promptForExport(scanner, "Detailed Vehicle Report", headers, data, "detailed_vehicle_report");
    }

    /**
     * Generate vehicle status summary report
     * @param vehicles List of all vehicles
     */
    public static void generateVehicleStatusSummary(List<Vehicle> vehicles) {
        System.out.println("\n╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      VEHICLE STATUS SUMMARY                       ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");

        Map<String, Long> statusCount = vehicles.stream()
                .filter(v -> !v.isArchived())
                .collect(java.util.stream.Collectors.groupingBy(
                        Vehicle::getStatus, java.util.stream.Collectors.counting()));

        for (Map.Entry<String, Long> entry : statusCount.entrySet()) {
            System.out.printf("║ %-25s │ %-37d ║%n", entry.getKey(), entry.getValue());
        }

        long archivedCount = vehicles.stream().filter(Vehicle::isArchived).count();
        if (archivedCount > 0) {
            System.out.printf("║ %-25s │ %-37d ║%n", "ARCHIVED", archivedCount);
        }

        System.out.println("╚════════════════════════════════════════════════════════════════════╝");
    }
} 