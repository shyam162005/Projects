import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

// Represents a single record (income/expense)
class RecordEntry {
    private String type; // Income or Expense
    private String category;
    private double amount;
    private LocalDateTime dateTime;
    private String description;

    public RecordEntry(String type, String category, double amount, String description) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.dateTime = LocalDateTime.now();
        this.description = description;
    }

    public RecordEntry(String type, String category, double amount, LocalDateTime dateTime, String description) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.dateTime = dateTime;
        this.description = description;
    }

    public String getType() { return type; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public LocalDateTime getDateTime() { return dateTime; }
    public String getDescription() { return description; }

    public String toCSV() {
        return type + "," + category + "," + amount + "," + 
               dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "," + description;
    }

    public static RecordEntry fromCSV(String csvLine) {
        try {
            String[] parts = csvLine.split(",", 5);
            String type = parts[0];
            String category = parts[1];
            double amount = Double.parseDouble(parts[2]);
            LocalDateTime dateTime = LocalDateTime.parse(parts[3], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String description = parts[4];
            return new RecordEntry(type, category, amount, dateTime, description);
        } catch (Exception e) {
            System.out.println("Error reading line: " + csvLine);
            return null;
        }
    }

    public YearMonth getYearMonth() {
        return YearMonth.from(dateTime);
    }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return String.format("%-8s | %-10s | Rs.%-10.2f | %-20s | %s", type, category, amount, fmt.format(dateTime), description);
    }
}

public class FinanceTracker {
    private static String username;
    private static String userFile;
    private static final String USER_DB = "users.txt"; // stores username + hashed password
    private static ArrayList<RecordEntry> records = new ArrayList<>();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (!loginSystem(sc)) return; // Login first
        loadRecords();

        int choice;
        while (true) {
            System.out.println("\n===== PERSONAL FINANCE TRACKER =====");
            System.out.println("Logged in as: " + username);
            System.out.println("Current Balance: Rs." + calculateBalance());
            System.out.println("------------------------------------");
            System.out.println("1. Add Income");
            System.out.println("2. Add Expense");
            System.out.println("3. View All Transactions");
            System.out.println("4. Monthly Summary");
            System.out.println("5. Category-wise Summary");
            System.out.println("6. View Transaction Count");
            System.out.println("7. View Current Balance");
            System.out.println("8. Logout & Exit");
            System.out.print("Enter your choice: ");
            
            try {
                choice = Integer.parseInt(sc.nextLine());
            } catch (Exception e) {
                System.out.println("Invalid input. Try again.");
                continue;
            }

            switch (choice) {
                case 1 -> addRecord(sc, "Income");
                case 2 -> addRecord(sc, "Expense");
                case 3 -> viewAll();
                case 4 -> viewMonthlySummary(sc);
                case 5 -> viewCategorySummary();
                case 6 -> viewTransactionCount();
                case 7 -> System.out.println("\n💰 Current Balance: Rs." + calculateBalance());
                case 8 -> {
                    saveRecords();
                    System.out.println("👋 Logged out. Thank you for using Finance Tracker!");
                    sc.close();
                    return;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    // ---------------- LOGIN SYSTEM ----------------

    private static boolean loginSystem(Scanner sc) {
        System.out.println("===== LOGIN / REGISTER =====");
        System.out.print("Enter username: ");
        username = sc.nextLine().trim();
        userFile = "transactions_" + username + ".csv";

        Map<String, String> users = loadUsers();
        if (users.containsKey(username)) {
            System.out.print("Enter password: ");
            String password = sc.nextLine();
            String hashedInput = hashPassword(password);
            if (hashedInput.equals(users.get(username))) {
                System.out.println("✅ Login successful!");
                return true;
            } else {
                System.out.println("❌ Incorrect password!");
                return false;
            }
        } else {
            System.out.println("No account found. Create a new one.");
            System.out.print("Set password: ");
            String password = sc.nextLine();
            users.put(username, hashPassword(password));
            saveUsers(users);
            System.out.println("✅ Account created successfully!");
            return true;
        }
    }

    private static Map<String, String> loadUsers() {
        Map<String, String> users = new HashMap<>();
        File file = new File(USER_DB);
        if (!file.exists()) return users;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) users.put(parts[0], parts[1]);
            }
        } catch (IOException e) {
            System.out.println("Error reading users file: " + e.getMessage());
        }
        return users;
    }

    private static void saveUsers(Map<String, String> users) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(USER_DB))) {
            for (var entry : users.entrySet()) {
                pw.println(entry.getKey() + "," + entry.getValue());
            }
        } catch (IOException e) {
            System.out.println("Error saving user data: " + e.getMessage());
        }
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not found!");
        }
    }

    // ---------------- RECORDS / TRANSACTIONS ----------------

    private static void addRecord(Scanner sc, String type) {
        System.out.print("Enter category: ");
        String category = sc.nextLine();
        System.out.print("Enter amount: ");
        double amount = Double.parseDouble(sc.nextLine());
        System.out.print("Enter description: ");
        String desc = sc.nextLine();

        RecordEntry record = new RecordEntry(type, category, amount, desc);
        records.add(record);
        saveRecords();
        System.out.println(type + " added successfully!");
    }

    private static void viewAll() {
        if (records.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        System.out.println("\n--- All Transactions ---");
        records.forEach(System.out::println);
    }

    private static void viewMonthlySummary(Scanner sc) {
        System.out.print("Enter month (1-12): ");
        int month = Integer.parseInt(sc.nextLine());
        System.out.print("Enter year (e.g. 2025): ");
        int year = Integer.parseInt(sc.nextLine());

        YearMonth target = YearMonth.of(year, month);
        double income = 0, expense = 0;

        for (RecordEntry r : records) {
            if (r.getYearMonth().equals(target)) {
                if (r.getType().equalsIgnoreCase("Income")) income += r.getAmount();
                else expense += r.getAmount();
            }
        }

        System.out.println("\n--- Summary for " + target.getMonth() + " " + year + " ---");
        System.out.println("Total Income:  Rs." + income);
        System.out.println("Total Expense: Rs." + expense);
        System.out.println("Net Savings:   Rs." + (income - expense));
    }

    private static void viewCategorySummary() {
        if (records.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        Map<String, Double> incomeTotals = new HashMap<>();
        Map<String, Double> expenseTotals = new HashMap<>();

        for (RecordEntry r : records) {
            if (r.getType().equalsIgnoreCase("Income")) {
                incomeTotals.put(r.getCategory(),
                        incomeTotals.getOrDefault(r.getCategory(), 0.0) + r.getAmount());
            } else {
                expenseTotals.put(r.getCategory(),
                        expenseTotals.getOrDefault(r.getCategory(), 0.0) + r.getAmount());
            }
        }

        System.out.println("\n--- Category-wise Summary ---");
        System.out.println("\nIncome Categories:");
        incomeTotals.forEach((cat, amt) -> System.out.printf("  %-15s : Rs.%.2f%n", cat, amt));

        System.out.println("\nExpense Categories:");
        expenseTotals.forEach((cat, amt) -> System.out.printf("  %-15s : Rs.%.2f%n", cat, amt));
    }

    private static void viewTransactionCount() {
        System.out.println("\nTotal Transactions Recorded: " + records.size());
    }

    private static double calculateBalance() {
        double balance = 0;
        for (RecordEntry r : records) {
            if (r.getType().equalsIgnoreCase("Income")) balance += r.getAmount();
            else balance -= r.getAmount();
        }
        return balance;
    }

    private static void saveRecords() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(userFile))) {
            for (RecordEntry r : records) {
                pw.println(r.toCSV());
            }
        } catch (IOException e) {
            System.out.println("Error saving records: " + e.getMessage());
        }
    }

    private static void loadRecords() {
        File file = new File(userFile);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                RecordEntry record = RecordEntry.fromCSV(line);
                if (record != null) records.add(record);
            }
        } catch (IOException e) {
            System.out.println("Error loading records: " + e.getMessage());
        }
    }
}
