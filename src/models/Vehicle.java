package models;

import enums.VehicleStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Vehicle {

    private String vehicleID;
    private String plateNo;
    private String carBrand;
    private String carModel;
    private String carType;
    private String fuelType;
    private String color;
    private int purchaseYear;
    private double capacity;
    private String condition;
    private double insuranceRate;
    private VehicleStatus status; // Enum-backed vehicle status
    private boolean archived; // true = archived, false = active

    private double basePrice;
    private Map<Integer, Double> longTermDiscounts;
    private List<Booking> schedule;
    private static final int BUFFER_DAYS = 2;

    public Vehicle(String vehicleID, String plateNo, String carBrand, String carModel,
            String carType, String fuelType, String color, int purchaseYear,
            double capacity, String condition, double insuranceRate, String available) {
        this.vehicleID = vehicleID;
        this.plateNo = plateNo;
        this.carBrand = carBrand;
        this.carModel = carModel;
        this.carType = carType;
        this.fuelType = fuelType;
        this.color = color;
        this.purchaseYear = purchaseYear;
        this.capacity = capacity;
        this.condition = condition;
        this.insuranceRate = insuranceRate;
        this.status = parseStatus(available);
        this.archived = false;

        this.basePrice = 50.0;
        this.longTermDiscounts = new HashMap<>();
        this.schedule = new ArrayList<>();
    }

    public Vehicle(String vehicleID, String plateNo, String carBrand, String carModel,
            String carType, String fuelType, String color, int purchaseYear,
            double capacity, String condition, double insuranceRate, String available,
            double basePrice, Map<Integer, Double> longTermDiscounts) {
        this(vehicleID, plateNo, carBrand, carModel, carType, fuelType, color,
                purchaseYear, capacity, condition, insuranceRate, available);
        this.basePrice = basePrice;
        this.longTermDiscounts = longTermDiscounts != null ? longTermDiscounts : new HashMap<>();
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void setInsuranceRate(double insuranceRate) {
        this.insuranceRate = insuranceRate;
    }

    public void setAvailable(String available) {
        this.status = parseStatus(available);
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public void setLongTermDiscounts(Map<Integer, Double> longTermDiscounts) {
        this.longTermDiscounts = longTermDiscounts != null ? longTermDiscounts : new HashMap<>();
    }

    public String getVehicleID() {
        return vehicleID;
    }

    public String getPlateNo() {
        return plateNo;
    }

    public String getBrand() {
        return carBrand;
    }

    public String getModel() {
        return carModel;
    }

    public String getType() {
        return carType;
    }

    public String getFuelType() {
        return fuelType;
    }

    public String getColor() {
        return color;
    }

    public int getYear() {
        return purchaseYear;
    }

    public double getCapacity() {
        return capacity;
    }

    public String getCondition() {
        return condition;
    }

    public double getInsuranceRate() {
        return insuranceRate;
    }

    public String getAvailable() {
        return statusToString(status);
    }

    public boolean isArchived() {
        return archived;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public Map<Integer, Double> getLongTermDiscounts() {
        return longTermDiscounts;
    }

    public List<Booking> getSchedule() {
        return schedule;
    }

    public int getId() {
        try {
            return Integer.parseInt(vehicleID);
        } catch (NumberFormatException e) {
            return vehicleID.hashCode();
        }
    }

    public String getCarPlate() {
        return plateNo;
    }

    public String getVehicleType() {
        return carType;
    }

    public String getStatus() {
        return statusToString(status);
    }

    public VehicleStatus getVehicleStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = parseStatus(status);
    }

    public void setVehicleStatus(VehicleStatus status) {
        this.status = status;
    }

    public boolean isAvailable(LocalDate startDate, LocalDate endDate) {

        if (status != VehicleStatus.AVAILABLE) {
            return false;
        }

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
        schedule.removeIf(booking
                -> booking.getStartDate().equals(startDate) && booking.getEndDate().equals(endDate));
    }

    public boolean hasFutureBookings() {
        LocalDate today = LocalDate.now();
        for (Booking booking : schedule) {
            if (booking.getStartDate().isAfter(today) || booking.getStartDate().equals(today)) {
                return true;
            }
        }
        return false;
    }

    public Booking getNextBooking() {
        LocalDate today = LocalDate.now();
        Booking nextBooking = null;
        for (Booking booking : schedule) {
            if (booking.getStartDate().isAfter(today) || booking.getStartDate().equals(today)) {
                if (nextBooking == null || booking.getStartDate().isBefore(nextBooking.getStartDate())) {
                    nextBooking = booking;
                }
            }
        }
        return nextBooking;
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

    /**
     * check if can extend rental (same user extension does not need buffer)
     */
    public boolean isAvailableForExtension(LocalDate startDate, LocalDate endDate, String username) {
        if (!(status == VehicleStatus.AVAILABLE || status == VehicleStatus.RENTED || status == VehicleStatus.RESERVED)) {
            return false;
        }

        for (Booking booking : schedule) {
            if (overlapsDirectly(startDate, endDate, booking.getStartDate(), booking.getEndDate())) {
                return false;
            }
        }
        return true;
    }

    /**
     * add booking for extension (no global availability or buffer check)
     */
    public void addBookingForExtension(LocalDate startDate, LocalDate endDate) {
        for (Booking booking : schedule) {
            if (overlapsDirectly(startDate, endDate, booking.getStartDate(), booking.getEndDate())) {
                throw new IllegalArgumentException("Vehicle is not available for the extended period");
            }
        }
        schedule.add(new Booking(startDate, endDate));
    }

    private boolean overlapsDirectly(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }

    /**
     * get discount rate for rental days
     */
    public double getDiscountForDays(int days) {
        double maxDiscount = 0.0;
        for (Map.Entry<Integer, Double> entry : longTermDiscounts.entrySet()) {
            if (days >= entry.getKey() && entry.getValue() > maxDiscount) {
                maxDiscount = entry.getValue();
            }
        }
        return maxDiscount;
    }

    @Override
    public String toString() {
        return String.format(
                "║ %-8s %-10s %-10s %-12s %-15s %-8s %-10s %-6d %-8.1f %-10s %-12.2f %-15s ║",
                vehicleID, plateNo, carBrand, carModel, carType,
                fuelType, color, purchaseYear, capacity,
                condition, basePrice, statusToString(status)
        );
    }

    private static VehicleStatus parseStatus(String value) {
        if (value == null) {
            return VehicleStatus.AVAILABLE;
        }
        String v = value.trim().toLowerCase();
        switch (v) {
            case "available":
                return VehicleStatus.AVAILABLE;
            case "reserved":
                return VehicleStatus.RESERVED;
            case "rented":
                return VehicleStatus.RENTED;
            case "out_of_service":
                return VehicleStatus.OUT_OF_SERVICE;
            case "archived":
                return VehicleStatus.AVAILABLE; // archived handled by separate flag
            default:
                return VehicleStatus.AVAILABLE;
        }
    }

    private static String statusToString(VehicleStatus status) {
        if (status == null) {
            return "available";
        }
        switch (status) {
            case AVAILABLE:
                return "available";
            case RESERVED:
                return "reserved";
            case RENTED:
                return "rented";
            case OUT_OF_SERVICE:
                return "out_of_service";
            default:
                return "available";
        }
    }
}
