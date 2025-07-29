import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;

public class ReminderService {
    private RentalSystem rentalSystem;

    public ReminderService(RentalSystem rentalSystem) {
        this.rentalSystem = rentalSystem;
    }

    public void checkAndSendReminders() {
        List<Rental> activeRentals = rentalSystem.getActiveRentals();
        
        for (Rental rental : activeRentals) {
            if (rental.isOverdue()) {
                sendOverdueReminder(rental);
            } else if (rental.isDueSoon()) {
                sendDueReminder(rental);
            }
        }
    }

    private void sendOverdueReminder(Rental rental) {
        System.out.println("\n=== OVERDUE REMINDER ===");
        System.out.println("Customer: " + rental.getCustomer().getName());
        System.out.println("Vehicle: " + rental.getVehicle().getBrand() + " " + rental.getVehicle().getModel());
        System.out.println("Due Date: " + rental.getEndDate());
        System.out.println("Days Overdue: " + java.time.temporal.ChronoUnit.DAYS.between(rental.getEndDate(), LocalDate.now()));
        System.out.println("Please return the vehicle immediately!");
        System.out.println("========================\n");
    }

    private void sendDueReminder(Rental rental) {
        System.out.println("\n=== DUE REMINDER ===");
        System.out.println("Customer: " + rental.getCustomer().getName());
        System.out.println("Vehicle: " + rental.getVehicle().getBrand() + " " + rental.getVehicle().getModel());
        System.out.println("Due Date: " + rental.getEndDate());
        System.out.println("Please prepare to return the vehicle tomorrow");
        System.out.println("===================\n");
    }

    public List<Rental> getOverdueRentals() {
        List<Rental> overdueRentals = new ArrayList<>();
        List<Rental> activeRentals = rentalSystem.getActiveRentals();
        
        for (Rental rental : activeRentals) {
            if (rental.isOverdue()) {
                overdueRentals.add(rental);
            }
        }
        return overdueRentals;
    }

    public List<Rental> getDueSoonRentals() {
        List<Rental> dueSoonRentals = new ArrayList<>();
        List<Rental> activeRentals = rentalSystem.getActiveRentals();
        
        for (Rental rental : activeRentals) {
            if (rental.isDueSoon()) {
                dueSoonRentals.add(rental);
            }
        }
        return dueSoonRentals;
    }

    public void displayReminderSummary() {
        List<Rental> overdueRentals = getOverdueRentals();
        List<Rental> dueSoonRentals = getDueSoonRentals();

        System.out.println("\n=== REMINDER SUMMARY ===");
        System.out.println("Overdue Rentals: " + overdueRentals.size());
        System.out.println("Due Soon Rentals: " + dueSoonRentals.size());
        
        if (!overdueRentals.isEmpty()) {
            System.out.println("\nOverdue Details:");
            for (Rental rental : overdueRentals) {
                System.out.println("- " + rental.toString());
            }
        }
        
        if (!dueSoonRentals.isEmpty()) {
            System.out.println("\nDue Soon Details:");
            for (Rental rental : dueSoonRentals) {
                System.out.println("- " + rental.toString());
            }
        }
        System.out.println("========================\n");
    }
} 