import java.time.LocalDate;

public class Rental {
    private int id;
    private Customer customer;
    private Vehicle vehicle;
    private LocalDate startDate;
    private LocalDate endDate;
    private RentalStatus status;
    private double totalFee; // 预计价格
    private double actualFee; // 实际价格（还车时计算）
    private boolean insuranceSelected;
    private String username; // 关联的登录用户名
    private boolean dueSoonReminderSent; // 归还前一天提醒是否已发送
    private boolean overdueReminderSent; // 逾期提醒是否已发送

    public Rental(int id, Customer customer, Vehicle vehicle, LocalDate startDate, LocalDate endDate, 
                  RentalStatus status, double totalFee, boolean insuranceSelected, String username) {
        this.id = id;
        this.customer = customer;
        this.vehicle = vehicle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.totalFee = totalFee;
        this.actualFee = 0.0; // 初始化为0，还车时计算
        this.insuranceSelected = insuranceSelected;
        this.username = username;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public RentalStatus getStatus() { return status; }
    public void setStatus(RentalStatus status) { this.status = status; }

    public double getTotalFee() { return totalFee; }
    public void setTotalFee(double totalFee) { this.totalFee = totalFee; }

    public double getActualFee() { return actualFee; }
    public void setActualFee(double actualFee) { this.actualFee = actualFee; }

    public boolean isInsuranceSelected() { return insuranceSelected; }
    public void setInsuranceSelected(boolean insuranceSelected) { this.insuranceSelected = insuranceSelected; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isDueSoonReminderSent() { return dueSoonReminderSent; }
    public void setDueSoonReminderSent(boolean dueSoonReminderSent) { this.dueSoonReminderSent = dueSoonReminderSent; }

    public boolean isOverdueReminderSent() { return overdueReminderSent; }
    public void setOverdueReminderSent(boolean overdueReminderSent) { this.overdueReminderSent = overdueReminderSent; }

    public int getRentalDays() {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public boolean isOverdue() {
        return LocalDate.now().isAfter(endDate) && status == RentalStatus.ACTIVE;
    }

    public boolean isDueSoon() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return tomorrow.equals(endDate) && status == RentalStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return String.format("Rental ID: %d, Customer: %s, Vehicle: %s %s, Period: %s to %s, Status: %s, Fee: RM%.2f",
                           id, customer.getName(), vehicle.getBrand(), vehicle.getModel(), 
                           startDate, endDate, status, totalFee);
    }
} 