import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BankingSystem {

    // ══════════════════════════════════════════════════════════════
    //  ANSI COLOR CONSTANTS
    // ══════════════════════════════════════════════════════════════
    static final String RESET   = "\u001B[0m";
    static final String BOLD    = "\u001B[1m";
    static final String CYAN    = "\u001B[36m";
    static final String GREEN   = "\u001B[32m";
    static final String YELLOW  = "\u001B[33m";
    static final String RED     = "\u001B[31m";
    static final String BLUE    = "\u001B[34m";
    static final String MAGENTA = "\u001B[35m";

    static final Scanner sc = new Scanner(System.in);
    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    // ══════════════════════════════════════════════════════════════
    //  DATA STORAGE
    // ══════════════════════════════════════════════════════════════
    static Map<String, String[]>         users       = new HashMap<>();
    // users: username -> [passwordHash, role, linkedAccountId]

    static Map<String, String[]>         accounts    = new LinkedHashMap<>();
    // accounts: accountId -> [ownerName, type, balance, status, email, phone]

    static Map<String, List<String[]>>   transactions = new LinkedHashMap<>();
    // transactions: accountId -> list of [txnId, type, amount, balanceAfter, description, timestamp]

    static int accountSeed  = 1000;
    static int txnCounters[] = new int[9999]; // per account seed index

    // ══════════════════════════════════════════════════════════════
    //  MAIN
    // ══════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        seedData();
        printBanner();
        printInfo("Default Admin  →  username: admin  |  password: admin123");
        printInfo("Demo Customer  →  username: priya  |  password: priya123");
        System.out.println();

        while (true) {
            printSectionHeader("MAIN MENU");
            System.out.println(CYAN + "   [1]  Login");
            System.out.println("   [0]  Exit" + RESET);
            System.out.println();
            int choice = readInt("Select option: ", 0, 1);
            if (choice == 0) { printInfo("Thank you for using Nexus Bank. Goodbye!"); break; }
            handleLogin();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  LOGIN
    // ══════════════════════════════════════════════════════════════
    static void handleLogin() {
        printSectionHeader("SECURE LOGIN");
        int MAX_ATTEMPTS = 3;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String username = readString("Username : ");
            String password = readPassword("Password : ");
            String[] user   = users.get(username.toLowerCase());

            if (user != null && user[0].equals(hashPassword(password))) {
                printSuccess("Login successful! Welcome, " + username.toUpperCase() + ".");
                System.out.println();
                if (user[1].equals("ADMIN")) showAdminMenu();
                else                         showCustomerMenu(username.toLowerCase());
                return;
            } else {
                int remaining = MAX_ATTEMPTS - attempt;
                if (remaining > 0) printError("Invalid credentials. " + remaining + " attempt(s) remaining.");
                else               printError("Too many failed attempts. Returning to main menu.");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ADMIN MENU
    // ══════════════════════════════════════════════════════════════
    static void showAdminMenu() {
        while (true) {
            printSectionHeader("ADMIN DASHBOARD");
            System.out.println(CYAN
                    + "   [1]  Create New Account\n"
                    + "   [2]  View All Accounts\n"
                    + "   [3]  Search Account by Name\n"
                    + "   [4]  View Account Details & Statement\n"
                    + "   [5]  Freeze / Unfreeze Account\n"
                    + "   [6]  Register Customer Login\n"
                    + "   [7]  Apply Monthly Interest to All\n"
                    + "   [8]  Bank Analytics & Reports\n"
                    + "   [0]  Logout" + RESET);
            System.out.println();
            int choice = readInt("Select option: ", 0, 8);
            System.out.println();
            switch (choice) {
                case 1 -> adminCreateAccount();
                case 2 -> adminViewAllAccounts();
                case 3 -> adminSearchByName();
                case 4 -> adminViewAccountDetails();
                case 5 -> adminToggleFreeze();
                case 6 -> adminRegisterCustomer();
                case 7 -> adminApplyInterest();
                case 8 -> adminShowAnalytics();
                case 0 -> { printInfo("Admin logged out."); return; }
            }
        }
    }

    static void adminCreateAccount() {
        printSectionHeader("CREATE NEW ACCOUNT");
        String name  = readString("Account Holder Name : ");
        String email = readString("Email Address       : ");
        String phone = readString("Phone Number        : ");
        System.out.println(CYAN + "\n  Account Type:" + RESET);
        System.out.println("   [1] Savings  (Min 500,  4% p.a.)");
        System.out.println("   [2] Current  (Min 1000, 2% p.a.)");
        int    typeChoice = readInt("Select type: ", 1, 2);
        String type       = (typeChoice == 1) ? "SAVINGS" : "CURRENT";
        double minDeposit = (typeChoice == 1) ? 500 : 1000;
        double deposit    = readPositiveDouble("Initial Deposit : ");

        if (deposit < minDeposit) {
            printError("Minimum opening deposit for " + type + " is " + minDeposit);
            pause(); return;
        }
        String id = createAccount(name, type, deposit, email, phone);
        printSuccess("Account created! Account ID: " + BOLD + id + RESET);
        pause();
    }

    static void adminViewAllAccounts() {
        printSectionHeader("ALL ACCOUNTS");
        if (accounts.isEmpty()) { printInfo("No accounts found."); pause(); return; }
        printTableHeader(String.format("%-12s %-20s %-10s %-14s %s", "Account ID","Owner","Type","Balance","Status"));
        for (var e : accounts.entrySet()) printAccountRow(e.getKey(), e.getValue());
        printDivider();
        printInfo("Total accounts: " + accounts.size());
        pause();
    }

    static void adminSearchByName() {
        printSectionHeader("SEARCH BY NAME");
        String query = readString("Enter name to search: ").toLowerCase();
        boolean found = false;
        printTableHeader(String.format("%-12s %-20s %-10s %-14s %s","Account ID","Owner","Type","Balance","Status"));
        for (var e : accounts.entrySet()) {
            if (e.getValue()[0].toLowerCase().contains(query)) {
                printAccountRow(e.getKey(), e.getValue());
                found = true;
            }
        }
        if (!found) printInfo("No accounts found matching '" + query + "'.");
        pause();
    }

    static void adminViewAccountDetails() {
        printSectionHeader("ACCOUNT DETAILS");
        String id = readString("Enter Account ID: ").toUpperCase();
        if (!accounts.containsKey(id)) { printError("Account not found: " + id); pause(); return; }
        printAccountCard(id);
        printMiniStatement(id, 10);
        pause();
    }

    static void adminToggleFreeze() {
        printSectionHeader("FREEZE / UNFREEZE ACCOUNT");
        String id = readString("Enter Account ID: ").toUpperCase();
        if (!accounts.containsKey(id)) { printError("Account not found: " + id); pause(); return; }
        String[] acc = accounts.get(id);
        System.out.println(CYAN + "\n  Current Status: " + BOLD + acc[3] + RESET);
        System.out.println("   [1] Freeze   [2] Activate   [0] Cancel");
        int c = readInt("Select: ", 0, 2);
        if      (c == 1) { acc[3] = "FROZEN"; printSuccess("Account " + id + " FROZEN."); }
        else if (c == 2) { acc[3] = "ACTIVE"; printSuccess("Account " + id + " ACTIVATED."); }
        pause();
    }

    static void adminRegisterCustomer() {
        printSectionHeader("REGISTER CUSTOMER LOGIN");
        String accountId = readString("Account ID to link : ").toUpperCase();
        if (!accounts.containsKey(accountId)) { printError("Account not found."); pause(); return; }
        String username  = readString("New Username       : ").toLowerCase();
        if (users.containsKey(username))      { printError("Username already taken."); pause(); return; }
        String password  = readPassword("New Password       : ");
        users.put(username, new String[]{ hashPassword(password), "CUSTOMER", accountId });
        printSuccess("Customer '" + username + "' registered and linked to " + accountId);
        pause();
    }

    static void adminApplyInterest() {
        printSectionHeader("APPLY MONTHLY INTEREST");
        printInfo("This credits monthly interest to ALL active accounts.");
        String confirm = readString("Type YES to confirm: ");
        if (confirm.equalsIgnoreCase("YES")) {
            int count = 0;
            for (var e : accounts.entrySet()) {
                String[] acc = e.getValue();
                if (!acc[3].equals("ACTIVE")) continue;
                double rate    = acc[1].equals("SAVINGS") ? 0.04 : 0.02;
                double balance = Double.parseDouble(acc[2]);
                double interest = balance * (rate / 12);
                acc[2] = String.valueOf(balance + interest);
                String desc = String.format("Monthly interest at %.1f%% p.a.", rate * 100);
                recordTransaction(e.getKey(), "INTEREST_CREDIT", interest,
                        Double.parseDouble(acc[2]), desc);
                count++;
            }
            printSuccess("Interest applied to " + count + " active account(s).");
        } else {
            printInfo("Cancelled.");
        }
        pause();
    }

    static void adminShowAnalytics() {
        printSectionHeader("BANK ANALYTICS & REPORTS");
        double totalFunds = 0, totalBalances = 0;
        long savings = 0, current = 0, active = 0, frozen = 0;

        for (String[] acc : accounts.values()) {
            double bal = Double.parseDouble(acc[2]);
            totalFunds    += bal;
            totalBalances += bal;
            if (acc[1].equals("SAVINGS")) savings++; else current++;
            if (acc[3].equals("ACTIVE"))  active++;  else if (acc[3].equals("FROZEN")) frozen++;
        }
        double avg = accounts.isEmpty() ? 0 : totalBalances / accounts.size();

        System.out.println(BOLD + GREEN);
        System.out.printf("  %-35s %.2f%n",  "Total Funds Under Management:", totalFunds);
        System.out.printf("  %-35s %.2f%n",  "Average Account Balance:",      avg);
        System.out.printf("  %-35s %d%n",    "Active Accounts:",              active);
        System.out.printf("  %-35s %d%n",    "Savings Accounts:",             savings);
        System.out.printf("  %-35s %d%n",    "Current Accounts:",             current);
        System.out.printf("  %-35s %d%n",    "Frozen Accounts:",              frozen);
        System.out.println(RESET);

        // Top 5 by balance
        printSectionHeader("TOP 5 ACCOUNTS BY BALANCE");
        printTableHeader(String.format("%-12s %-20s %-10s %s","Account ID","Owner","Type","Balance"));
        accounts.entrySet().stream()
                .sorted((a, b) -> Double.compare(
                        Double.parseDouble(b.getValue()[2]),
                        Double.parseDouble(a.getValue()[2])))
                .limit(5)
                .forEach(e -> System.out.printf("  %-12s %-20s %-10s %.2f%n",
                        e.getKey(), e.getValue()[0], e.getValue()[1],
                        Double.parseDouble(e.getValue()[2])));
        pause();
    }

    // ══════════════════════════════════════════════════════════════
    //  CUSTOMER MENU
    // ══════════════════════════════════════════════════════════════
    static void showCustomerMenu(String username) {
        String accountId = users.get(username)[2];
        while (true) {
            String[] acc = accounts.get(accountId);
            printSectionHeader("CUSTOMER PORTAL — Welcome, " + acc[0]);
            System.out.println(CYAN
                    + "   [1]  View Account Summary\n"
                    + "   [2]  Deposit Money\n"
                    + "   [3]  Withdraw Money\n"
                    + "   [4]  Transfer Funds\n"
                    + "   [5]  View Mini Statement (Last 10)\n"
                    + "   [6]  View Full Statement\n"
                    + "   [0]  Logout" + RESET);
            System.out.println();
            int choice = readInt("Select option: ", 0, 6);
            System.out.println();
            switch (choice) {
                case 1 -> printAccountCard(accountId);
                case 2 -> customerDeposit(accountId);
                case 3 -> customerWithdraw(accountId);
                case 4 -> customerTransfer(accountId);
                case 5 -> printMiniStatement(accountId, 10);
                case 6 -> printMiniStatement(accountId, Integer.MAX_VALUE);
                case 0 -> { printInfo("Goodbye, " + acc[0] + "!"); return; }
            }
            if (choice != 0) pause();
        }
    }

    static void customerDeposit(String accountId) {
        printSectionHeader("DEPOSIT MONEY");
        String[] acc = accounts.get(accountId);
        System.out.printf(CYAN + "  Current Balance: " + BOLD + "%.2f%n" + RESET, Double.parseDouble(acc[2]));
        double amount = readPositiveDouble("Deposit Amount: ");
        double newBal = Double.parseDouble(acc[2]) + amount;
        acc[2] = String.valueOf(newBal);
        recordTransaction(accountId, "DEPOSIT", amount, newBal, "Cash deposit");
        printSuccess(String.format("%.2f deposited. New Balance: %.2f", amount, newBal));
    }

    static void customerWithdraw(String accountId) {
        printSectionHeader("WITHDRAW MONEY");
        String[] acc    = accounts.get(accountId);
        double balance  = Double.parseDouble(acc[2]);
        double minBal   = acc[1].equals("SAVINGS") ? 500 : 1000;
        System.out.printf(CYAN + "  Current Balance: " + BOLD + "%.2f%n" + RESET, balance);
        System.out.printf(YELLOW + "  Available to withdraw: %.2f%n" + RESET, balance - minBal);
        double amount   = readPositiveDouble("Withdrawal Amount: ");
        if (balance - amount < minBal) {
            printError(String.format("Cannot withdraw. Must maintain min balance of %.2f. Available: %.2f",
                    minBal, balance - minBal));
            return;
        }
        double newBal = balance - amount;
        acc[2] = String.valueOf(newBal);
        recordTransaction(accountId, "WITHDRAWAL", amount, newBal, "Cash withdrawal");
        printSuccess(String.format("%.2f withdrawn. New Balance: %.2f", amount, newBal));
    }

    static void customerTransfer(String accountId) {
        printSectionHeader("FUND TRANSFER");
        String[] acc   = accounts.get(accountId);
        double balance = Double.parseDouble(acc[2]);
        double minBal  = acc[1].equals("SAVINGS") ? 500 : 1000;
        System.out.printf(CYAN + "  Your Balance: " + BOLD + "%.2f%n" + RESET, balance);
        String toId    = readString("Recipient Account ID : ").toUpperCase();
        if (!accounts.containsKey(toId)) { printError("Recipient account not found."); return; }
        if (toId.equals(accountId))      { printError("Cannot transfer to your own account."); return; }
        double amount  = readPositiveDouble("Transfer Amount      : ");
        if (balance - amount < minBal) {
            printError(String.format("Insufficient funds. Available for transfer: %.2f", balance - minBal));
            return;
        }
        String[] toAcc = accounts.get(toId);
        System.out.printf(YELLOW + "  Sending to: %s (%s)%n" + RESET, toAcc[0], toAcc[1]);
        String confirm = readString("Confirm? (yes/no): ");
        if (!confirm.equalsIgnoreCase("yes")) { printInfo("Transfer cancelled."); return; }

        double newFromBal = balance - amount;
        double newToBal   = Double.parseDouble(toAcc[2]) + amount;
        acc[2]   = String.valueOf(newFromBal);
        toAcc[2] = String.valueOf(newToBal);
        recordTransaction(accountId, "TRANSFER_OUT", amount, newFromBal, "Transfer to "   + toId);
        recordTransaction(toId,      "TRANSFER_IN",  amount, newToBal,   "Transfer from " + accountId);
        printSuccess(String.format("%.2f transferred to %s. Remaining Balance: %.2f",
                amount, toId, newFromBal));
    }

    // ══════════════════════════════════════════════════════════════
    //  CORE DATA METHODS
    // ══════════════════════════════════════════════════════════════
    static String createAccount(String name, String type, double deposit,
                                String email, String phone) {
        String id = "ACC" + (++accountSeed);
        accounts.put(id, new String[]{ name, type, String.valueOf(deposit), "ACTIVE", email, phone });
        transactions.put(id, new ArrayList<>());
        recordTransaction(id, "DEPOSIT", deposit, deposit, "Account opening deposit");
        return id;
    }

    static void recordTransaction(String accountId, String type, double amount,
                                  double balanceAfter, String description) {
        List<String[]> txnList = transactions.computeIfAbsent(accountId, k -> new ArrayList<>());
        int idx = Integer.parseInt(accountId.replace("ACC", "")) - 1000;
        String txnId = accountId + "-TXN" + String.format("%04d", ++txnCounters[idx]);
        txnList.add(new String[]{
            txnId, type,
            String.valueOf(amount),
            String.valueOf(balanceAfter),
            description,
            LocalDateTime.now().format(FMT)
        });
    }

    static String hashPassword(String password) {
        int hash = 0;
        for (char c : password.toCharArray()) hash = hash * 31 + c;
        return Integer.toHexString(hash);
    }

    // ══════════════════════════════════════════════════════════════
    //  SEED DATA
    // ══════════════════════════════════════════════════════════════
    static void seedData() {
        users.put("admin", new String[]{ hashPassword("admin123"), "ADMIN", "" });

        String id1 = createAccount("Priya Sharma", "SAVINGS", 10000, "priya@mail.com", "9876543210");
        String id2 = createAccount("Rahul Verma",  "CURRENT", 25000, "rahul@mail.com", "9123456789");
        String id3 = createAccount("Anita Patel",  "SAVINGS",  5000, "anita@mail.com", "9988776655");
        String id4 = createAccount("Vikram Singh", "CURRENT", 50000, "vikram@mail.com","9001234567");

        // Some demo transactions
        String[] acc1 = accounts.get(id1);
        acc1[2] = String.valueOf(Double.parseDouble(acc1[2]) + 5000);
        recordTransaction(id1, "DEPOSIT", 5000, Double.parseDouble(acc1[2]), "Cash deposit");

        String[] acc2 = accounts.get(id2);
        double transfer = 3000;
        acc2[2] = String.valueOf(Double.parseDouble(acc2[2]) - transfer);
        acc1[2] = String.valueOf(Double.parseDouble(acc1[2]) + transfer);
        recordTransaction(id2, "TRANSFER_OUT", transfer, Double.parseDouble(acc2[2]), "Transfer to " + id1);
        recordTransaction(id1, "TRANSFER_IN",  transfer, Double.parseDouble(acc1[2]), "Transfer from " + id2);

        users.put("priya", new String[]{ hashPassword("priya123"), "CUSTOMER", id1 });
        users.put("rahul", new String[]{ hashPassword("rahul123"), "CUSTOMER", id2 });
    }

    // ══════════════════════════════════════════════════════════════
    //  UI DISPLAY METHODS
    // ══════════════════════════════════════════════════════════════
    static void printBanner() {
        System.out.println(BOLD + BLUE);
        System.out.println("  ╔══════════════════════════════════════════════════════════╗");
        System.out.println("  ║          NEXUS BANK — CORE BANKING SYSTEM v2.0          ║");
        System.out.println("  ║          Secure  •  Reliable  •  Intelligent            ║");
        System.out.println("  ╚══════════════════════════════════════════════════════════╝");
        System.out.println(RESET);
    }

    static void printSectionHeader(String title) {
        System.out.println();
        System.out.println(BOLD + MAGENTA + "  ┌─────────────────────────────────────────────────────────┐");
        System.out.printf(                  "  │  %-55s│%n", title);
        System.out.println(                 "  └─────────────────────────────────────────────────────────┘" + RESET);
    }

    static void printTableHeader(String header) {
        System.out.println(BOLD + YELLOW + "  " + header + RESET);
        System.out.println("  " + "─".repeat(header.length()));
    }

    static void printAccountRow(String id, String[] acc) {
        System.out.printf("  %-12s %-20s %-10s %-14.2f %s%n",
                id, acc[0], acc[1], Double.parseDouble(acc[2]), acc[3]);
    }

    static void printAccountCard(String id) {
        String[] acc = accounts.get(id);
        System.out.println();
        System.out.println(BOLD + CYAN + "  ┌──── ACCOUNT CARD ────────────────────────────────────┐" + RESET);
        System.out.printf(CYAN + "  │  Account ID   : %-36s│%n", id);
        System.out.printf(CYAN + "  │  Holder Name  : %-36s│%n", acc[0]);
        System.out.printf(CYAN + "  │  Account Type : %-36s│%n", acc[1]);
        System.out.printf(CYAN + "  │  Balance      : %-36.2f│%n", Double.parseDouble(acc[2]));
        System.out.printf(CYAN + "  │  Status       : %-36s│%n", acc[3]);
        System.out.printf(CYAN + "  │  Email        : %-36s│%n", acc[4]);
        System.out.printf(CYAN + "  │  Phone        : %-36s│%n", acc[5]);
        System.out.println(CYAN + "  └─────────────────────────────────────────────────────┘" + RESET);
    }

    static void printMiniStatement(String accountId, int limit) {
        List<String[]> txns = transactions.getOrDefault(accountId, new ArrayList<>());
        int start = Math.max(0, txns.size() - limit);
        System.out.println();
        int shown = Math.min(limit, txns.size());
        printTableHeader("STATEMENT — Last " + shown + " Transaction(s)");
        System.out.printf("  %-26s %-16s %-13s %-14s %s%n",
                "Date & Time", "Type", "Amount", "Balance", "Description");
        printDivider();
        if (txns.isEmpty()) {
            printInfo("No transactions yet.");
        } else {
            for (String[] t : txns.subList(start, txns.size())) {
                double amt = Double.parseDouble(t[2]);
                String sign = (t[1].equals("WITHDRAWAL") || t[1].equals("TRANSFER_OUT")) ? "-" : "+";
                System.out.printf("  %-26s %-16s %s%-12.2f %-14.2f %s%n",
                        t[5], t[1], sign, amt, Double.parseDouble(t[3]), t[4]);
            }
        }
        printDivider();
    }

    static void printDivider()              { System.out.println("  " + "─".repeat(70)); }
    static void printSuccess(String msg)    { System.out.println(GREEN  + "  ✔  " + msg + RESET); }
    static void printError(String msg)      { System.out.println(RED    + "  ✘  " + msg + RESET); }
    static void printInfo(String msg)       { System.out.println(YELLOW + "  ℹ  " + msg + RESET); }
    static void pause() {
        System.out.print(YELLOW + "\n  Press ENTER to continue..." + RESET);
        sc.nextLine();
    }

    // ══════════════════════════════════════════════════════════════
    //  INPUT HELPERS
    // ══════════════════════════════════════════════════════════════
    static String readString(String prompt) {
        while (true) {
            System.out.print(CYAN + "  " + prompt + RESET);
            String val = sc.nextLine().trim();
            if (!val.isEmpty()) return val;
            System.out.println(RED + "  Input cannot be empty." + RESET);
        }
    }

    static String readPassword(String prompt) {
        System.out.print(CYAN + "  " + prompt + RESET);
        return sc.nextLine().trim();
    }

    static double readPositiveDouble(String prompt) {
        while (true) {
            try {
                System.out.print(CYAN + "  " + prompt + RESET);
                double val = Double.parseDouble(sc.nextLine().trim());
                if (val > 0) return val;
                System.out.println(RED + "  Must be greater than zero." + RESET);
            } catch (NumberFormatException e) {
                System.out.println(RED + "  Invalid number. Try again." + RESET);
            }
        }
    }

    static int readInt(String prompt, int min, int max) {
        while (true) {
            try {
                System.out.print(CYAN + "  " + prompt + RESET);
                int val = Integer.parseInt(sc.nextLine().trim());
                if (val >= min && val <= max) return val;
                System.out.printf(RED + "  Enter a number between %d and %d.%n" + RESET, min, max);
            } catch (NumberFormatException e) {
                System.out.println(RED + "  Invalid input. Enter a number." + RESET);
            }
        }
    }
}
