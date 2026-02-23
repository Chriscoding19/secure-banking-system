package banking.account;

import banking.lock.PetersonLock;
import banking.util.TransactionLogger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thread-Safe Bank Account
 *
 * Uses Peterson's Algorithm (via PetersonLock) to guarantee mutual exclusion
 * on critical sections — deposit, withdrawal, and balance reads.
 *
 * Peterson's lock is used directly to demonstrate the algorithm. In production
 * code, one would use java.util.concurrent.locks.ReentrantLock.
 */
public class BankAccount {

    private final String accountId;
    private final String ownerName;
    private final String accountType;
    private BigDecimal balance;
    private final LocalDateTime createdAt;

    // Peterson Lock (works for exactly 2 competing threads)
    private final PetersonLock petersonLock;

    // Thread-local ID mapping: each thread gets ID 0 or 1
    private final ThreadLocal<Integer> threadId;
    private volatile int nextThreadId = 0;

    private final TransactionLogger logger;

    public BankAccount(String ownerName, String accountType, BigDecimal initialBalance) {
        this.accountId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.ownerName = ownerName;
        this.accountType = accountType;
        this.balance = initialBalance;
        this.createdAt = LocalDateTime.now();
        this.petersonLock = new PetersonLock();
        this.logger = new TransactionLogger(accountId);

        // Assign thread IDs 0 or 1 as threads first access this account
        this.threadId = ThreadLocal.withInitial(() -> {
            synchronized (BankAccount.this) {
                return nextThreadId++ % 2;
            }
        });
    }

    /**
     * Deposit funds into the account.
     * CRITICAL SECTION: Protected by Peterson's Algorithm.
     *
     * @param amount Amount to deposit (must be positive)
     * @param description Transaction description
     * @return New balance
     * @throws IllegalArgumentException if amount is non-positive
     */
    public BigDecimal deposit(BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive: " + amount);
        }

        int tid = threadId.get();
        petersonLock.lock(tid);             // --- ENTER CRITICAL SECTION ---
        try {
            BigDecimal before = balance;
            balance = balance.add(amount);
            logger.log("DEPOSIT", amount, before, balance, description);
            return balance;
        } finally {
            petersonLock.unlock(tid);       // --- EXIT CRITICAL SECTION ---
        }
    }

    /**
     * Withdraw funds from the account.
     * CRITICAL SECTION: Protected by Peterson's Algorithm.
     *
     * @param amount Amount to withdraw (must be positive and <= balance)
     * @param description Transaction description
     * @return New balance
     * @throws InsufficientFundsException if balance is too low
     */
    public BigDecimal withdraw(BigDecimal amount, String description) throws InsufficientFundsException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive: " + amount);
        }

        int tid = threadId.get();
        petersonLock.lock(tid);             // --- ENTER CRITICAL SECTION ---
        try {
            if (amount.compareTo(balance) > 0) {
                throw new InsufficientFundsException(
                    String.format("Insufficient funds. Balance: $%.2f, Requested: $%.2f",
                        balance, amount)
                );
            }
            BigDecimal before = balance;
            balance = balance.subtract(amount);
            logger.log("WITHDRAWAL", amount, before, balance, description);
            return balance;
        } finally {
            petersonLock.unlock(tid);       // --- EXIT CRITICAL SECTION ---
        }
    }

    /**
     * Get current balance (thread-safe read via Peterson Lock).
     */
    public BigDecimal getBalance() {
        int tid = threadId.get();
        petersonLock.lock(tid);
        try {
            return balance;
        } finally {
            petersonLock.unlock(tid);
        }
    }

    // --- Getters ---
    public String getAccountId()   { return accountId; }
    public String getOwnerName()   { return ownerName; }
    public String getAccountType() { return accountType; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return String.format("BankAccount[id=%s, owner=%s, type=%s, balance=$%.2f]",
            accountId, ownerName, accountType, balance);
    }
}
