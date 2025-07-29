import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;


public class Vehicle {
    private int id;
    private String brand;
    private String model;
    private String carPlate;    // Car plate number
    private VehicleType vehicleType; // Vehicle type
    private FuelType fuelType;      // Fuel type
    private VehicleStatus status;
    private double insuranceRate;
    private double basePrice; // Daily base price
    private Map<Integer, Double> longTermDiscounts;
    private List<Booking> schedule;        // Booking schedule
    private List<MaintenanceLog> maintenanceLogs; // Maintenance records
    private static final int BUFFER_DAYS = 2; // Buffer period in days
    
    // Original constructor for compatibility
    public Vehicle(int id, String brand, String model, VehicleStatus status, double insuranceRate) {
        this(id, brand, model, "UNKNOWN", VehicleType.CAR, FuelType.PETROL, status, insuranceRate, 50.0, null);
    }

    public Vehicle(int id, String brand, String model, VehicleStatus status, double insuranceRate, double basePrice, Map<Integer, Double> longTermDiscounts) {
        this(id, brand, model, "UNKNOWN", VehicleType.CAR, FuelType.PETROL, status, insuranceRate, basePrice, longTermDiscounts);
    }
    
    // New complete constructor
    public Vehicle(int id, String brand, String model, String carPlate, VehicleType vehicleType, 
                   FuelType fuelType, VehicleStatus status, double insuranceRate, double basePrice, 
                   Map<Integer, Double> longTermDiscounts) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.carPlate = carPlate;
        this.vehicleType = vehicleType;
        this.fuelType = fuelType;
        this.status = status;
        this.insuranceRate = insuranceRate;
        this.basePrice = basePrice;
        this.longTermDiscounts = longTermDiscounts != null ? longTermDiscounts : new HashMap<>();
        this.schedule = new ArrayList<>();
        this.maintenanceLogs = new ArrayList<>();
    }

    // Original getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public VehicleStatus getStatus() { return status; }
    public void setStatus(VehicleStatus status) { this.status = status; }

    public double getInsuranceRate() { return insuranceRate; }
    public void setInsuranceRate(double insuranceRate) { this.insuranceRate = insuranceRate; }

    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }

    public Map<Integer, Double> getLongTermDiscounts() { return longTermDiscounts; }
    public void setLongTermDiscounts(Map<Integer, Double> longTermDiscounts) { 
        this.longTermDiscounts = longTermDiscounts != null ? longTermDiscounts : new HashMap<>(); 
    }

    // New field getters and setters
    public String getCarPlate() { return carPlate; }
    public void setCarPlate(String carPlate) { this.carPlate = carPlate; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }

    public List<Booking> getSchedule() { return schedule; }
    
    public List<MaintenanceLog> getMaintenanceLogs() { return maintenanceLogs; }

    // Booking related methods
    public boolean isAvailable(LocalDate startDate, LocalDate endDate) {
        // Check vehicle status
        if (status == VehicleStatus.UNDER_MAINTENANCE || status == VehicleStatus.OUT_OF_SERVICE) {
            return false;
        }
        
        // Check for unresolved critical maintenance issues
        if (hasCriticalMaintenanceIssues()) {
            return false;
        }
        
        // Check for time conflicts (including buffer)
        for (Booking booking : schedule) {
            LocalDate bufferStart = booking.getStartDate().minusDays(BUFFER_DAYS);
            LocalDate bufferEnd = booking.getEndDate().plusDays(BUFFER_DAYS);
            
            if (overlapsWithBuffer(startDate, endDate, bufferStart, bufferEnd)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean overlapsWithBuffer(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }
    
    public void addBooking(LocalDate startDate, LocalDate endDate) {
        if (isAvailable(startDate, endDate)) {
            schedule.add(new Booking(startDate, endDate));
        } else {
            throw new IllegalArgumentException("Vehicle is not available for the requested period");
        }
    }
    
    public void removeBooking(LocalDate startDate, LocalDate endDate) {
        schedule.removeIf(booking -> 
            booking.getStartDate().equals(startDate) && booking.getEndDate().equals(endDate));
    }
    
    public void clearAllBookings() {
        schedule.clear();
    }
    
    public List<String> getUnavailablePeriods() {
        List<String> periods = new ArrayList<>();
        for (Booking booking : schedule) {
            LocalDate bufferStart = booking.getStartDate().minusDays(BUFFER_DAYS);
            LocalDate bufferEnd = booking.getEndDate().plusDays(BUFFER_DAYS);
            periods.add(String.format("%s to %s (includes %d-day buffer)", 
                       bufferStart, bufferEnd, BUFFER_DAYS));
        }
        return periods;
    }

    // Maintenance related methods
    public void addMaintenanceLog(MaintenanceLog log) {
        maintenanceLogs.add(log);
        updateStatusBasedOnMaintenance();
    }
    
    /**
     * Directly add maintenance record (for loading from JSON, does not trigger status update)
     */
    public void addMaintenanceLogDirect(MaintenanceLog log) {
        maintenanceLogs.add(log);
    }
    
    public boolean hasCriticalMaintenanceIssues() {
        for (MaintenanceLog log : maintenanceLogs) {
            if (log.isCritical()) {
                return true;
            }
        }
        return false;
    }
    
    public List<MaintenanceLog> getUnresolvedMaintenanceLogs() {
        List<MaintenanceLog> unresolved = new ArrayList<>();
        for (MaintenanceLog log : maintenanceLogs) {
            if (log.isUnresolved()) {
                unresolved.add(log);
            }
        }
        return unresolved;
    }
    
    public double getTotalMaintenanceCost() {
        double total = 0.0;
        for (MaintenanceLog log : maintenanceLogs) {
            total += log.getCost();
        }
        return total;
    }
    
    private void updateStatusBasedOnMaintenance() {
        if (hasCriticalMaintenanceIssues() && status == VehicleStatus.AVAILABLE) {
            setStatus(VehicleStatus.UNDER_MAINTENANCE);
        }
    }
    
    public void resolveMaintenanceLog(int logId, double cost) {
        for (MaintenanceLog log : maintenanceLogs) {
            if (log.getId() == logId) {
                log.setStatus(MaintenanceStatus.RESOLVED);
                log.setCost(cost);
                break;
            }
        }
        
        // If all critical issues are resolved, change status back to available
        if (!hasCriticalMaintenanceIssues() && status == VehicleStatus.UNDER_MAINTENANCE) {
            setStatus(VehicleStatus.AVAILABLE);
        }
    }

    public double getDiscountForDays(int days) {
        double maxDiscount = 0.0;
        for (Map.Entry<Integer, Double> entry : longTermDiscounts.entrySet()) {
            if (days >= entry.getKey() && entry.getValue() > maxDiscount) {
                maxDiscount = entry.getValue();
            }
        }
        return maxDiscount;
    }

    public boolean isAvailableForExtension(LocalDate startDate, LocalDate endDate, String username) {
        // Check vehicle status
        if (status == VehicleStatus.UNDER_MAINTENANCE || status == VehicleStatus.OUT_OF_SERVICE) {
            return false;
        }
        
        // Check for unresolved critical maintenance issues
        if (hasCriticalMaintenanceIssues()) {
            return false;
        }
        
        // Check for time conflicts (no buffer for same user extension)
        for (Booking booking : schedule) {
            // For same user extension, allow adjacent bookings without buffer
            if (!overlapsDirectly(startDate, endDate, booking.getStartDate(), booking.getEndDate())) {
                continue; // No overlap, check next booking
            } else {
                return false; // Direct overlap found
            }
        }
        return true;
    }
    
    private boolean overlapsDirectly(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }

    @Override
    public String toString() {
        return String.format("ID: %d, %s %s (%s), Car Plate: %s, Type: %s, Fuel: %s, Price: RM%.2f/day, Insurance: %.1f%%", 
                           id, brand, model, status, carPlate, vehicleType, fuelType, basePrice, insuranceRate * 100);
    }
} 