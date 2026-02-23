package banking.lock;

/**
 * Peterson's Algorithm - Mutual Exclusion for Two Threads
 *
 * Peterson's algorithm guarantees:
 *   1. Mutual Exclusion  - Only one thread enters the critical section at a time
 *   2. Progress          - If no thread is in the critical section, one will eventually enter
 *   3. Bounded Waiting   - A thread won't wait forever to enter its critical section
 *
 * Note: volatile ensures visibility of shared variables across CPU cores (Java Memory Model).
 */
public class PetersonLock {

    // flag[i] = true means thread i wants to enter the critical section
    private volatile boolean[] flag = new boolean[2];

    // turn indicates whose turn it is to enter when both want access
    private volatile int turn;

    /**
     * Acquire the lock for the given thread (0 or 1).
     * Busy-waits until it is safe to proceed.
     *
     * @param threadId 0 or 1
     */
    public void lock(int threadId) {
        int other = 1 - threadId;
        flag[threadId] = true;       // Signal: "I want to enter"
        turn = other;                 // Yield priority to the other thread

        // Busy-wait while the other thread wants to enter AND it's their turn
        while (flag[other] && turn == other) {
            Thread.onSpinWait();      // CPU hint: we're in a spin loop (Java 9+)
        }
        // Critical section begins here
    }

    /**
     * Release the lock for the given thread.
     *
     * @param threadId 0 or 1
     */
    public void unlock(int threadId) {
        flag[threadId] = false;       // Signal: "I'm done, others can enter"
    }

    /**
     * Returns whether a thread currently holds (or is competing for) the lock.
     */
    public boolean isLocked(int threadId) {
        return flag[threadId];
    }
}
