package banking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable record of a single banking transaction.
 */
public class Transaction {

    public enum Type { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT }

    private final String transactionId;
    private final String accountId;
    private final Type type;
    private final BigDecimal amount;
    private final BigDecimal balanceBefore;
    private final BigDecimal balanceAfter;
    private final String description;
    private final LocalDateTime timestamp;
    private final String threadName;

    public Transaction(String accountId, Type type, BigDecimal amount,
                       BigDecimal balanceBefore, BigDecimal balanceAfter, String description) {
        this.transactionId  = UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        this.accountId      = accountId;
        this.type           = type;
        this.amount         = amount;
        this.balanceBefore  = balanceBefore;
        this.balanceAfter   = balanceAfter;
        this.description    = description;
        this.timestamp      = LocalDateTime.now();
        this.threadName     = Thread.currentThread().getName();
    }

    // --- Getters ---
    public String getTransactionId()  { return transactionId; }
    public String getAccountId()      { return accountId; }
    public Type getType()             { return type; }
    public BigDecimal getAmount()     { return amount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public BigDecimal getBalanceAfter()  { return balanceAfter; }
    public String getDescription()    { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getThreadName()     { return threadName; }

    @Override
    public String toString() {
        return String.format(
            "[%s] %-12s | TxID: %-12s | Amt: $%10.2f | Before: $%10.2f | After: $%10.2f | Thread: %-20s | %s",
            timestamp.toLocalTime(), type, transactionId, amount,
            balanceBefore, balanceAfter, threadName, description
        );
    }
}
