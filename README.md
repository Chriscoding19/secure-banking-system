# Secure Banking System

A multi-threaded banking system implementing **Peterson's Algorithm** for mutual exclusion and lock mechanisms to guarantee thread safety.

---

## Architecture

```
secure-banking/
├── src/main/java/banking/
│   ├── BankingSystemDemo.java          ← Main entry point / demo
│   ├── lock/
│   │   ├── PetersonLock.java           ← Peterson's Algorithm (2 threads)
│   │   └── FilterLock.java             ← Filter Lock (N threads)
│   ├── account/
│   │   ├── BankAccount.java            ← Thread-safe account (uses PetersonLock)
│   │   └── InsufficientFundsException.java
│   ├── service/
│   │   └── BankingService.java         ← Multi-account manager + transfers
│   ├── model/
│   │   └── Transaction.java            ← Immutable transaction record
│   └── util/
│       └── TransactionLogger.java      ← Thread-safe audit logger
```

---

## Concurrency Mechanisms

### Peterson's Algorithm
The core mutual exclusion primitive for two threads:

```java
// Thread wants to enter critical section:
flag[threadId] = true;   // "I want in"
turn = other;             // Yield to other thread first

// Spin-wait while other thread wants in AND it's their turn
while (flag[other] && turn == other) {
    Thread.onSpinWait();  // CPU hint: we're spinning
}
// → Critical section (balance update) happens here ←

// Thread exits:
flag[threadId] = false;  // "I'm done"
```

**Guarantees provided:**
| Property | Description |
|---|---|
| **Mutual Exclusion** | Only one thread modifies the balance at a time |
| **Progress** | If no thread is in the critical section, one will enter |
| **Bounded Waiting** | No thread waits forever - starvation-free |

### Filter Lock (N-thread extension)
Generalizes Peterson's to N threads using N-1 "waiting levels." Each level allows one thread through while keeping others waiting - a tournament-style algorithm.

### Deadlock Prevention
Transfers between accounts acquire locks in **consistent lexicographic order** by account ID, preventing circular wait (the necessary condition for deadlock).

---

## How to Run

### Option 1: Compile & Run Manually

```bash
# From the project root
find src -name "*.java" -print | xargs javac -d out/
java -cp out banking.BankingSystemDemo
```

### Option 2: With Maven (add pom.xml)

```bash
mvn compile exec:java -Dexec.mainClass="banking.BankingSystemDemo"
```

---

## Demo Scenarios

The demo runs 4 scenarios:

| Demo | What it shows |
|---|---|
| **Demo 1** | Basic deposit, withdrawal, overdraft protection |
| **Demo 2** | 2 threads making 50 deposits each - Peterson's prevents lost updates |
| **Demo 3** | Simultaneous bidirectional transfers - deadlock prevention |
| **Demo 4** | Stress test: 200 interleaved deposit/withdraw pairs - balance stays consistent |

---

## Key Concepts

- **Critical Section**: The balance modification code that only one thread may execute at a time
- **Race Condition**: Without locks, two threads reading the same balance simultaneously causes lost updates (e.g., two deposits of $100 result in only $100 total instead of $200)
- **volatile**: Java keyword ensuring all threads see the most recent value of shared variables - required for Peterson's correctness under the Java Memory Model
- **Busy-waiting (spin-lock)**: Peterson's loops until it's safe - acceptable for very short critical sections

---

## Expected Output (Demo 2)

```
→ 2 threads, 50 deposits of $10.00 each...
Expected balance : $1000.00
Actual balance   : $1000.00
Peterson's mutex : PASSED — No race conditions!
```

Without Peterson's lock, the expected vs actual balance would differ due to lost updates.

---

## 🛠 Technologies

- **Language**: Java 11+
- **Concurrency**: Peterson's Algorithm, Filter Lock, `volatile`, `CountDownLatch`, `ExecutorService`
- **Data**: `BigDecimal` for precise monetary arithmetic (avoids floating-point errors)
- **Collections**: `ConcurrentHashMap`, `Collections.synchronizedList`
