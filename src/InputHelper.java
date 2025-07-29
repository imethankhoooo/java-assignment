import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class InputHelper {

    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    public int getInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid integer.");
            }
        }
    }

    public double getDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
    }

    public String getString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }
    
    public boolean getConfirmation(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine();
        return input.equalsIgnoreCase("y");
    }

    public LocalDate getDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("today")) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use yyyy-MM-dd.");
            }
        }
    }
} 