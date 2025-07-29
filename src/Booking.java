import java.time.LocalDate;

public class Booking {
    private LocalDate startDate;
    private LocalDate endDate;

    public Booking(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean overlaps(LocalDate otherStart, LocalDate otherEnd) {
        return !this.startDate.isAfter(otherEnd) && !otherStart.isAfter(this.endDate);
    }
} 