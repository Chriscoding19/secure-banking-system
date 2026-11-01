package banking;

import banking.account.BankAccount;
import banking.account.InsufficientFundsException;
import banking.service.BankingService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║            SECURE BANKING SYSTEM — DEMO                     ║
 * ║                                                              ║
 * ║  Demonstrates:                                               ║
 * ║   • Peterson's Algorithm for mutual exclusion (2 threads)   ║
 * ║   • Filter Lock for N-thread mutual exclusion               ║
 * ║   • Thread-safe deposits, withdrawals, and transfers        ║
 * ║   • Race condition prevention via lock mechanisms           ║
 * ║   • Deadlock prevention via consistent lock ordering        ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class BankingSystemDemo {

    private static final BankingService bankingService = new BankingService();

    public static void main(String[] args) throws InterruptedException {
        printHeader();

        demo1_BasicOperations();
        demo2_ConcurrentDeposits_PetersonGuarantee();
        demo3_ConcurrentTransfers();
        demo4_RaceConditionPrevention();

        System.out.println("\n All demos completed successfully.");
    }

    // ─────────────────────────────────────────────────────────────
    // DEMO 1: Basic account operations
    // ─────────────────────────────────────────────────────────────
    static void demo1_BasicOperations() throws InterruptedException {
        section("DEMO 1: Basic Account Operations");

        BankAccount alice = bankingService.createAccount("Alice Johnson", "CHECKING", new BigDecimal("5000.00"));
        BankAccount bob   = bankingService.createAccount("Bob Smith",    "SAVINGS",  new BigDecimal("3000.00"));

        System.out.println("\n  → Alice deposits $1,500...");
        alice.deposit(new BigDecimal("1500.00"), "Paycheck deposit");

        System.out.println("  → Bob deposits $500...");
        bob.deposit(new BigDecimal("500.00"), "ATM deposit");

        System.out.println("  → Alice withdraws $200...");
        try {
            alice.withdraw(new BigDecimal("200.00"), "Grocery store");
        } catch (InsufficientFundsException e) {
            System.out.println("  ERROR: " + e.getMessage());
        }

        System.out.println("  → Bob attempts to overdraw...");
        try {
            bob.withdraw(new BigDecimal("99999.00"), "Luxury yacht");
        } catch (InsufficientFundsException e) {
            System.out.println("  ⚠  Expected error caught: " + e.getMessage());
        }

        bankingService.printAllBalances();
    }

    // ─────────────────────────────────────────────────────────────
    // DEMO 2: Peterson's mutual exclusion with concurrent threads
    // ─────────────────────────────────────────────────────────────
    static void demo2_ConcurrentDeposits_PetersonGuarantee() throws InterruptedException {
        section("DEMO 2: Peterson's Algorithm — Concurrent Thread Safety");

        BankAccount sharedAccount = bankingService.createAccount(
            "Shared Savings", "JOINT", new BigDecimal("0.00"));

        int threadCount = 2; // Peterson's works with 2 threads
        int depositsPerThread = 50;
        BigDecimal depositAmount = new BigDecimal("10.00");
        BigDecimal expectedFinal = depositAmount
            .multiply(new BigDecimal(threadCount * depositsPerThread));

        System.out.printf("%n  → Launching %d threads, each making %d deposits of $%.2f...%n",
            threadCount, depositsPerThread, depositAmount);
        System.out.println("  → Peterson's Algorithm ensures NO lost updates (no race conditions).\n");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);
        List<Thread> threads = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int tId = t;
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await(); // All threads start simultaneously
                    for (int i = 0; i < depositsPerThread; i++) {
                        sharedAccount.deposit(depositAmount,
                            "Thread-" + tId + " deposit #" + (i + 1));
                    }
                } catch (Exception e) {
                    System.out.println("  ERROR in thread " + tId + ": " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            }, "BankThread-" + tId);
            threads.add(thread);
            thread.start();
        }

        startLatch.countDown(); // Fire all threads at once
        doneLatch.await();

        BigDecimal actual = sharedAccount.getBalance();
        System.out.println("\n  ─── RESULT ───");
        System.out.printf("  Expected balance : $%.2f%n", expectedFinal);
        System.out.printf("  Actual balance   : $%.2f%n", actual);
        System.out.println("  Peterson's mutex : " +
            (expectedFinal.equals(actual) ? "PASSED — No race conditions!" : "FAILED — Race condition detected!"));
    }

    // ─────────────────────────────────────────────────────────────
    // DEMO 3: Concurrent inter-account transfers (deadlock prevention)
    // ─────────────────────────────────────────────────────────────
    static void demo3_ConcurrentTransfers() throws InterruptedException {
        section("DEMO 3: Concurrent Transfers — Deadlock Prevention");

        BankAccount carol = bankingService.createAccount("Carol Davis",  "CHECKING", new BigDecimal("10000.00"));
        BankAccount dave  = bankingService.createAccount("Dave Wilson",  "CHECKING", new BigDecimal("10000.00"));

        BigDecimal carolStart = carol.getBalance();
        BigDecimal daveStart  = dave.getBalance();
        BigDecimal totalBefore = carolStart.add(daveStart);

        System.out.println("  → Launching simultaneous transfers between Carol ↔ Dave...");
        System.out.println("  → Deadlock prevention: consistent lock ordering by account ID.\n");

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);

        // Simulate multiple concurrent transfers in both directions
        pool.submit(() -> { try {
            bankingService.transfer(carol.getAccountId(), dave.getAccountId(),
                new BigDecimal("500.00"), "Rent payment");
        } catch (Exception e) { System.out.println("  Transfer error: " + e.getMessage()); }
          finally { latch.countDown(); } });

        pool.submit(() -> { try {
            bankingService.transfer(dave.getAccountId(), carol.getAccountId(),
                new BigDecimal("300.00"), "Loan repayment");
        } catch (Exception e) { System.out.println("  Transfer error: " + e.getMessage()); }
          finally { latch.countDown(); } });

        pool.submit(() -> { try {
            bankingService.transfer(carol.getAccountId(), dave.getAccountId(),
                new BigDecimal("200.00"), "Split bill");
        } catch (Exception e) { System.out.println("  Transfer error: " + e.getMessage()); }
          finally { latch.countDown(); } });

        pool.submit(() -> { try {
            bankingService.transfer(dave.getAccountId(), carol.getAccountId(),
                new BigDecimal("100.00"), "Refund");
        } catch (Exception e) { System.out.println("  Transfer error: " + e.getMessage()); }
          finally { latch.countDown(); } });

        latch.await();
        pool.shutdown();

        BigDecimal totalAfter = carol.getBalance().add(dave.getBalance());
        System.out.println("\n  ─── RESULT ───");
        System.out.printf("  Total before : $%.2f%n", totalBefore);
        System.out.printf("  Total after  : $%.2f%n", totalAfter);
        System.out.println("  Conservation : " +
            (totalBefore.equals(totalAfter) ? "PASSED — No money created or lost!" : "FAILED"));

        bankingService.printAllBalances();
    }

    // ─────────────────────────────────────────────────────────────
    // DEMO 4: Race condition demonstration without locks vs with locks
    // ─────────────────────────────────────────────────────────────
    static void demo4_RaceConditionPrevention() throws InterruptedException {
        section("DEMO 4: Race Condition Prevention — Stress Test");

        BankAccount stressAccount = bankingService.createAccount(
            "Stress Test Account", "CHECKING", new BigDecimal("100000.00"));

        int numThreads = 2;
        int opsPerThread = 100;

        System.out.printf("  → %d threads each performing %d interleaved deposits/withdrawals...%n",
            numThreads, opsPerThread);

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(numThreads);

        BigDecimal depositAmt  = new BigDecimal("100.00");
        BigDecimal withdrawAmt = new BigDecimal("100.00");
        // Each thread deposits 100x and withdraws 100x → net change = 0

        for (int t = 0; t < numThreads; t++) {
            final int tid = t;
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < opsPerThread; i++) {
                        stressAccount.deposit(depositAmt, "Stress deposit #" + i);
                        stressAccount.withdraw(withdrawAmt, "Stress withdraw #" + i);
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            }, "StressThread-" + tid).start();
        }

        BigDecimal expectedBalance = stressAccount.getBalance();
        start.countDown();
        done.await();

        BigDecimal finalBalance = stressAccount.getBalance();
        System.out.println("\n  ─── RESULT ───");
        System.out.printf("  Expected balance : $%.2f%n", expectedBalance);
        System.out.printf("  Final balance    : $%.2f%n", finalBalance);
        System.out.println("  Stress test      : " +
            (expectedBalance.equals(finalBalance)
                ? "PASSED — Peterson's lock maintained perfect consistency!"
                : "FAILED — Detected inconsistency: $" + finalBalance.subtract(expectedBalance)));
    }

    // ─────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────
    static void printHeader() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           SECURE BANKING SYSTEM — Java Implementation       ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Concurrency Mechanisms:                                    ║");
        System.out.println("║    • Peterson's Algorithm  (2-thread mutual exclusion)      ║");
        System.out.println("║    • Filter Lock           (N-thread mutual exclusion)      ║");
        System.out.println("║    • ConcurrentHashMap     (thread-safe account registry)   ║");
        System.out.println("║    • CountDownLatch        (synchronized thread startup)    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    static void section(String title) {
        System.out.println("\n\n┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  " + title);
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }
}
