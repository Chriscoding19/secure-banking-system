package banking.lock;

/**
 * Filter Lock - Generalization of Peterson's Algorithm for N Threads
 *
 * The Filter Lock uses N-1 "waiting rooms" (levels). A thread must pass
 * through each level to enter the critical section. At each level, at least
 * one thread is guaranteed to pass through, while others wait.
 *
 * This is the standard academic extension of Peterson's 2-thread algorithm
 * to support arbitrary numbers of threads.
 */
public class FilterLock {

    private final int n;
    private volatile int[] level;   // level[i] = current level of thread i
    private volatile int[] victim;  // victim[l] = thread to "sacrifice" at level l

    public FilterLock(int n) {
        this.n = n;
        this.level = new int[n];
        this.victim = new int[n];
    }

    /**
     * Acquire the lock for thread with given id (0 to n-1).
     * Thread must pass through n-1 levels, each acting as a Peterson-style filter.
     */
    public void lock(int threadId) {
        for (int lev = 1; lev < n; lev++) {
            level[threadId] = lev;
            victim[lev] = threadId;

            // Spin while some other thread is at least at this level AND this thread is the victim
            boolean spinning = true;
            while (spinning) {
                spinning = false;
                for (int k = 0; k < n; k++) {
                    if (k != threadId && level[k] >= lev && victim[lev] == threadId) {
                        spinning = true;
                        Thread.onSpinWait();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Release the lock — reset this thread's level to 0.
     */
    public void unlock(int threadId) {
        level[threadId] = 0;
    }
}
