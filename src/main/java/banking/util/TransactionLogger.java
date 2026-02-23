package banking.util;

import banking.model.Transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thread-safe transaction logger for a single account.
 * Uses a synchronized list to safely record transactions from multiple threads.
 */
public class TransactionLogger {

    private final String accountId;
    private final List<Transaction> history;

    public TransactionLogger(String accountId) {
        this.accountId = accountId;
        // Wrap in synchronized list for safe multi-threaded access
        this.history = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Log a transaction event.
     */
    public void log(String typeStr, BigDecimal amount, BigDecimal before,
                    BigDecimal after, String description) {

        Transaction.Type type;
        switch (typeStr) {
            case "DEPOSIT":      type = Transaction.Type.DEPOSIT;       break;
            case "WITHDRAWAL":   type = Transaction.Type.WITHDRAWAL;    break;
            case "TRANSFER_IN":  type = Transaction.Type.TRANSFER_IN;   break;
            case "TRANSFER_OUT": type = Transaction.Type.TRANSFER_OUT;  break;
            default:             type = Transaction.Type.DEPOSIT;
        }

        Transaction tx = new Transaction(accountId, type, amount, before, after, description);
        history.add(tx);
        System.out.println("  TX  " + tx);
    }

    /**
     * Returns an unmodifiable view of transaction history.
     */
    public List<Transaction> getHistory() {
        synchronized (history) {
            return Collections.unmodifiableList(new ArrayList<>(history));
        }
    }

    /**
     * Prints a formatted summary of all transactions.
     */
    public void printSummary() {
        synchronized (history) {
            System.out.println("\n━━━ Transaction History [Account: " + accountId + "] ━━━");
            if (history.isEmpty()) {
                System.out.println("  No transactions recorded.");
            } else {
                history.forEach(tx -> System.out.println("  " + tx));
            }
            System.out.println("━━━ Total Transactions: " + history.size() + " ━━━\n");
        }
    }
}
