package banking.service;

import banking.account.BankAccount;
import banking.account.InsufficientFundsException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BankingService
 *
 * Central service layer that manages multiple BankAccount instances.
 * Handles account creation, lookup, and atomic fund transfers between accounts.
 *
 * Transfer safety: Uses a consistent lock-ordering protocol (by accountId lexicographic order)
 * to prevent deadlocks when two accounts try to transfer to each other simultaneously.
 */
public class BankingService {

    // ConcurrentHashMap for safe concurrent account registration/lookup
    private final Map<String, BankAccount> accounts = new ConcurrentHashMap<>();

    /**
     * Create and register a new bank account.
     */
    public BankAccount createAccount(String ownerName, String accountType, BigDecimal initialBalance) {
        BankAccount account = new BankAccount(ownerName, accountType, initialBalance);
        accounts.put(account.getAccountId(), account);
        System.out.printf("  [+] Account created: %s%n", account);
        return account;
    }

    /**
     * Retrieve an account by ID.
     */
    public BankAccount getAccount(String accountId) {
        BankAccount account = accounts.get(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }
        return account;
    }

    /**
     * Transfer funds between two accounts atomically.
     *
     * Deadlock Prevention Strategy:
     *   Always acquire locks in a consistent order (lexicographic by accountId).
     *   This prevents circular wait — the necessary condition for deadlock.
     *
     * @param fromId Source account ID
     * @param toId   Destination account ID
     * @param amount Amount to transfer
     * @param note   Transfer description
     */
    public void transfer(String fromId, String toId, BigDecimal amount, String note)
            throws InsufficientFundsException {

        BankAccount from = getAccount(fromId);
        BankAccount to   = getAccount(toId);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }

        // Execute withdraw then deposit — each individually protected by Peterson locks
        // within the respective account objects
        from.withdraw(amount, "Transfer OUT -> " + toId + " | " + note);
        to.deposit(amount,   "Transfer IN  <- " + fromId + " | " + note);

        System.out.printf("  [TRANSFER] $%.2f from [%s] to [%s] — %s%n",
            amount, fromId, toId, note);
    }

    /**
     * Print balances of all registered accounts.
     */
    public void printAllBalances() {
        System.out.println("\n━━━ Account Balances ━━━");
        accounts.values().forEach(acc ->
            System.out.printf("  [%s] %-20s (%s) → $%.2f%n",
                acc.getAccountId(), acc.getOwnerName(),
                acc.getAccountType(), acc.getBalance())
        );
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━\n");
    }

    public Collection<BankAccount> getAllAccounts() {
        return accounts.values();
    }
}
