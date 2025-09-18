package services;

import enums.AccountRole;
import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import models.Account;
import models.Rental;

public class PaymentService {

    private static final String HISTORY_FILE = "payment_history.txt";
    private static final Map<String, Double> PROMO_CODES = new HashMap<>();
    static {
        PROMO_CODES.put("PROMO10", 0.10); // 10% discount
        PROMO_CODES.put("PROMO20", 0.20); // 20% discount
    }

    public void processPayment(Rental rental, Scanner scanner) {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║                   PAYMENT MODULE                   ║");
        System.out.println("╚════════════════════════════════════════════════════╝");

        // Apply promo code
        double discount = 0;
        System.out.print("Do you have a promo code? (yes/no): ");
        String hasPromo = scanner.nextLine().trim().toLowerCase();
        if (hasPromo.equals("yes") || hasPromo.equals("y")) {
            System.out.print("Enter promo code: ");
            String code = scanner.nextLine().trim().toUpperCase();
            if (PROMO_CODES.containsKey(code)) {
                discount = PROMO_CODES.get(code);
                System.out.println("Promo code applied! Discount: " + (int)(discount * 100) + "%");
            } else {
                System.out.println("Invalid promo code. No discount applied.");
            }
        }

        // Calculate late fee
        double lateFee = calculateLateFee(rental);

        // Calculate final amount
        double finalAmount = rental.getTotalFee() * (1 - discount) + lateFee;
        rental.setActualFee(finalAmount); // Update rental object

        displayInvoice(rental, discount, lateFee);

        if (finalAmount <= 0) {
            System.out.println("\nNo payment required. Thank you!");
            return;
        }

        // Confirmation loop
        while (true) {
            System.out.print("\nDo you confirm the above invoice? (yes/no): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (confirm.equals("yes") || confirm.equals("y") || confirm.equals("ye")) {
                break;
            } else if (confirm.equals("no") || confirm.equals("n")) {
                System.out.println("Payment cancelled by user.");
                return;
            } else {
                System.out.println("Invalid input. Please enter yes or no.");
            }
        }

        // Choose payment method
        boolean paid = false;
        while (!paid) {
            System.out.println("\nSelect Payment Method:");
            System.out.println("1. TouchNGo");
            System.out.println("2. Bank Transfer");
            System.out.println("3. GrabPay");
            System.out.println("0. Cancel Payment");
            System.out.print("Enter choice: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1": paid = handleTouchNGo(scanner, rental, finalAmount); break;
                case "2": paid = handleBankTransfer(scanner, rental, finalAmount); break;
                case "3": paid = handleGrabPay(scanner, rental, finalAmount); break;
                case "0": System.out.println("Payment cancelled."); return;
                default: System.out.println("Invalid choice. Please enter again.");
            }
        }

        // Save history
        savePaymentRecord(rental, discount, lateFee, "PAID");

        // Post-payment options
        postPaymentOptions(scanner, rental, discount, lateFee);
    }

    // ------------------- PAYMENT HANDLERS ---------------------
    private boolean handleTouchNGo(Scanner scanner, Rental rental, double amount) {
        String phone = getValidPhone(scanner, "TouchNGo");
        String pin = getValidPIN(scanner, "TouchNGo");

        System.out.printf("\nPayment of RM%.2f successful using TouchNGo (+60%s)\n", amount, phone);
        return true;
    }

    private boolean handleBankTransfer(Scanner scanner, Rental rental, double amount) {
        String bank = getBankChoice(scanner);
        String bankId = getValidPhone(scanner, "Bank");
        String password = getValidPIN(scanner, "Bank");

        Random rand = new Random();
        int tac = 100000 + rand.nextInt(900000);
        System.out.println("\n[SMS from " + bank + "] Your TAC code is: " + tac);

        String inputTac = "";
        while (!inputTac.equals(String.valueOf(tac))) {
            System.out.print("Enter TAC code: ");
            inputTac = scanner.nextLine();
            if (!inputTac.equals(String.valueOf(tac))) System.out.println("Invalid TAC. Try again.");
        }

        System.out.printf("\nPayment of RM%.2f successful via %s (User: +60%s)\n", amount, bank, bankId);
        return true;
    }

    private boolean handleGrabPay(Scanner scanner, Rental rental, double amount) {
        String phone = getValidPhone(scanner, "GrabPay");
        String pin = getValidPIN(scanner, "GrabPay");

        System.out.printf("\nPayment of RM%.2f successful via GrabPay (+60%s)\n", amount, phone);
        return true;
    }


    // ------------------- VALIDATORS ---------------------
    private String getValidPhone(Scanner scanner, String type) {
        String phone = "";
        while (!phone.matches("^01\\d{7,8}$")) {
            System.out.print("Enter phone number linked to " + type + " (+60): ");
            phone = scanner.nextLine().trim();
            if (!phone.matches("^01\\d{7,8}$")) System.out.println("Invalid number. Must start with 01 and contain 9–10 digits.");
        }
        return phone;
    }

    private String getValidPIN(Scanner scanner, String type) {
        String pin = "";
        while (!pin.matches("^\\d{6}$")) {
            System.out.print("Enter 6-digit " + type + " PIN: ");
            pin = scanner.nextLine().trim();
            if (!pin.matches("^\\d{6}$")) System.out.println("Invalid PIN. Must be exactly 6 digits.");
        }
        return pin;
    }

    private String getBankChoice(Scanner scanner) {
        String choice = "";
        String bank = "";
        while (choice.equals("")) {
            System.out.println("\nSelect Bank:");
            System.out.println("1. Public Bank");
            System.out.println("2. Maybank");
            System.out.println("3. Other Bank");
            System.out.print("Enter choice: ");
            choice = scanner.nextLine();
            switch (choice) {
                case "1": bank = "Public Bank"; break;
                case "2": bank = "Maybank"; break;
                case "3": bank = "Other Bank"; break;
                default: System.out.println("Invalid choice. Please enter again."); choice = "";
            }
        }
        return bank;
    }

    // ------------------- POST PAYMENT ---------------------
    private void postPaymentOptions(Scanner scanner, Rental rental, double discount, double lateFee) {
        double finalAmount = rental.getActualFee();
        displayReceipt(rental, discount, lateFee);

        while (true) {
            System.out.print("\nWould you like to download receipt as TXT, CSV, or skip? (txt/csv/skip): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            switch (choice) {
                case "txt":
                    String txtPath = generateTextReceipt(rental, discount, lateFee);
                    System.out.println("TXT receipt saved at: " + txtPath);
                    break;
                case "csv":
                    String csvPath = generateCSVReceipt(rental, discount, lateFee);
                    System.out.println("CSV receipt saved at: " + csvPath);
                    break;
                case "skip":
                    System.out.println("Skipped downloading receipt.");
                    break;
                default:
                    System.out.println("Invalid choice. Enter txt, csv, or skip.");
                    continue;
            }
            break;
        }

        // Ask to view payment history
        while (true) {
            System.out.print("\nDo you want to view your payment history? (yes/no): ");
            String historyChoice = scanner.nextLine().trim().toLowerCase();
            if (historyChoice.equals("yes") || historyChoice.equals("y") || historyChoice.equals("ye")) {
                viewPaymentHistory(rental.getCustomer().getName());
                break;
            } else if (historyChoice.equals("no") || historyChoice.equals("n")) {
                System.out.println("Skipped viewing payment history.");
                break;
            } else {
                System.out.println("Invalid input. Please enter yes or no.");
            }
        }

        System.out.println("\nThank you for using our service!");
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    // ------------------- HISTORY ---------------------
    private void savePaymentRecord(Rental rental, double discount, double lateFee, String status) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(HISTORY_FILE, true))) {
            bw.write(rental.getCustomer().getName() + "|" +
                    rental.getId() + "|" +
                    rental.getVehicle().getBrand() + " " + rental.getVehicle().getModel() + "|" +
                    rental.getTotalFee() + "|" +
                    discount + "|" +
                    lateFee + "|" +
                    rental.getActualFee() + "|" +
                    rental.getStartDate() + " to " + rental.getEndDate() + "|" +
                    status + "\n");
        } catch (IOException e) {
            System.out.println("Failed to save payment history: " + e.getMessage());
        }
    }

    public void viewPaymentHistory(String customerName) {
        System.out.println("\n========= PAYMENT HISTORY for " + customerName + " =========");
        boolean found = false;
        double total = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(HISTORY_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 9 && parts[0].equalsIgnoreCase(customerName)) {
                    System.out.printf("Rental ID: %s | Vehicle: %s | Base Fee: RM%s | Discount: %s | Late Fee: %s | Paid: RM%s | Period: %s | Status: %s\n",
                            parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], parts[7], parts[8]);
                    total += Double.parseDouble(parts[6]);
                    found = true;
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading payment history: " + e.getMessage());
        }

        if (!found) {
            System.out.println("No payment history found.");
        } else {
            System.out.printf("Total Spending: RM%.2f\n", total);
        }
    }

    // ------------------- INVOICE / RECEIPT ---------------------
    private double calculateLateFee(Rental rental) {
        LocalDate today = LocalDate.now();
        if (today.isAfter(rental.getEndDate())) {
            long daysLate = ChronoUnit.DAYS.between(rental.getEndDate(), today);
            return daysLate * 20; // RM20 per late day
        }
        return 0;
    }

    private void displayInvoice(Rental rental, double discount, double lateFee) {
        double baseFee = rental.getTotalFee();
        double finalAmount = rental.getActualFee();

        System.out.println("\n╔════════════════════ INVOICE DETAILS ════════════════════╗");
        System.out.printf("Rental ID      : %d\n", rental.getId());
        System.out.printf("Customer       : %s\n", rental.getCustomer().getName());
        System.out.printf("Vehicle        : %s %s (%s)\n",
                rental.getVehicle().getBrand(), rental.getVehicle().getModel(), rental.getVehicle().getCarPlate());
        System.out.printf("Rental Period  : %s - %s\n", rental.getStartDate(), rental.getEndDate());
        System.out.printf("Base Fee       : RM%.2f\n", baseFee);
        System.out.printf("Discount       : RM%.2f\n", baseFee * discount);
        System.out.printf("Late Fee       : RM%.2f\n", lateFee);
        System.out.printf("Final Fee      : RM%.2f\n", finalAmount);
        System.out.println("Reported Issues: None");
        System.out.println("==========================================================");
    }

    private void displayReceipt(Rental rental, double discount, double lateFee) {
        double finalAmount = rental.getActualFee();
        System.out.println("\n================== PAYMENT RECEIPT ==================");
        System.out.printf("Rental ID       : %d\n", rental.getId());
        System.out.printf("Customer        : %s\n", rental.getCustomer().getName());
        System.out.printf("Vehicle         : %s %s\n", rental.getVehicle().getBrand(), rental.getVehicle().getModel());
        System.out.printf("Car Plate       : %s\n", rental.getVehicle().getCarPlate());
        System.out.printf("Rental Period   : %s - %s\n", rental.getStartDate(), rental.getEndDate());
        System.out.printf("Base Fee        : RM%.2f\n", rental.getTotalFee());
        System.out.printf("Discount        : RM%.2f\n", rental.getTotalFee() * discount);
        System.out.printf("Late Fee        : RM%.2f\n", lateFee);
        System.out.printf("Final Amount    : RM%.2f\n", finalAmount);
        System.out.println("Payment Status  : PAID");
        System.out.println("=====================================================");
    }

    private String generateTextReceipt(Rental rental, double discount, double lateFee) {
        String fileName = "Receipt_Rental_" + rental.getId() + ".txt";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            bw.write("============= VEHICLE RENTAL PAYMENT RECEIPT =============\n");
            bw.write("Rental ID       : " + rental.getId() + "\n");
            bw.write("Customer        : " + rental.getCustomer().getName() + "\n");
            bw.write("Vehicle         : " + rental.getVehicle().getBrand() + " " + rental.getVehicle().getModel() + "\n");
            bw.write("Car Plate       : " + rental.getVehicle().getCarPlate() + "\n");
            bw.write("Rental Period   : " + rental.getStartDate() + " → " + rental.getEndDate() + "\n");
            bw.write("Base Fee        : RM" + String.format("%.2f", rental.getTotalFee()) + "\n");
            bw.write("Discount        : RM" + String.format("%.2f", rental.getTotalFee() * discount) + "\n");
            bw.write("Late Fee        : RM" + String.format("%.2f", lateFee) + "\n");
            bw.write("Final Amount    : RM" + String.format("%.2f", rental.getActualFee()) + "\n");
            bw.write("Payment Status  : PAID\n");
            bw.write("==========================================================\n");
            bw.write("Thank you for choosing CarSeek!\n");
        } catch (IOException e) {
            System.out.println("Failed to save receipt: " + e.getMessage());
        }
        return fileName;
    }

    private String generateCSVReceipt(Rental rental, double discount, double lateFee) {
        String fileName = "Receipt_Rental_" + rental.getId() + ".csv";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            bw.write("RentalID,Customer,Vehicle,CarPlate,RentalPeriod,BaseFee,Discount,LateFee,FinalAmount,PaymentStatus\n");
            bw.write(rental.getId() + "," +
                    rental.getCustomer().getName() + "," +
                    rental.getVehicle().getBrand() + " " + rental.getVehicle().getModel() + "," +
                    rental.getVehicle().getCarPlate() + "," +
                    rental.getStartDate() + " → " + rental.getEndDate() + "," +
                    String.format("%.2f", rental.getTotalFee()) + "," +
                    String.format("%.2f", rental.getTotalFee() * discount) + "," +
                    String.format("%.2f", lateFee) + "," +
                    String.format("%.2f", rental.getActualFee()) + "," +
                    "PAID\n");
        } catch (IOException e) {
            System.out.println("Failed to save CSV receipt: " + e.getMessage());
        }
        return fileName;
    }

    // ================= PAYMENT MANAGEMENT MENU =================

    /**
     * Display payment management menu
     * @param scanner Scanner for user input
     * @param account Current logged-in account
     */
    public void paymentManagementMenu(Scanner scanner, Account account) {
        while (true) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                         PAYMENT MANAGEMENT                       ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║ 1. View Payment History                                          ║");
            System.out.println("║ 0. Back to Main Menu                                             ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
            System.out.print("Select option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    viewPaymentHistoryMenu(scanner, account);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
            }
        }
    }

    /**
     * View payment history menu for user or admin
     * @param scanner Scanner for user input
     * @param account Current logged-in account
     */
    private void viewPaymentHistoryMenu(Scanner scanner, Account account) {
        System.out.println("\n=== Payment History ===");

        String customerName;
        if (account.getRole() == AccountRole.ADMIN) {
            System.out.print("Enter customer name (or press Enter for your own history): ");
            customerName = scanner.nextLine().trim();
            if (customerName.isEmpty()) {
                customerName = account.getFullName();
            }
        } else {
            customerName = account.getFullName();
        }

        viewPaymentHistory(customerName);
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

}

