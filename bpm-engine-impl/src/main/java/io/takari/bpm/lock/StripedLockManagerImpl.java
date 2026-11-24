package io.takari.bpm.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StripedLockManagerImpl implements LockManager {

    private static final Logger log = LoggerFactory.getLogger(StripedLockManagerImpl.class);

    private final Lock[] locks;
    private final int mask;

    public StripedLockManagerImpl(int concurrencyLevel) {
        if (concurrencyLevel <= 0) {
            throw new IllegalArgumentException("concurrencyLevel must be positive: " + concurrencyLevel);
        }
        int size = ceilingPowerOfTwo(concurrencyLevel);
        this.mask = size - 1;
        this.locks = new Lock[size];
        for (int i = 0; i < size; i++) {
            this.locks[i] = new ReentrantLock();
        }
    }

    @Override
    public void lock(String processBusinessKey) {
        log.debug("lock ['{}'] -> locking...", processBusinessKey);
        lockFor(processBusinessKey).lock();
        log.debug("lock ['{}'] -> locked", processBusinessKey);
    }

    @Override
    public void unlock(String processBusinessKey) {
        lockFor(processBusinessKey).unlock();
        log.debug("lock ['{}'] -> unlocked", processBusinessKey);
    }

    private Lock lockFor(String key) {
        Objects.requireNonNull(key, "processBusinessKey");
        int h = key.hashCode();
        h ^= (h >>> 20) ^ (h >>> 12);
        h = h ^ (h >>> 7) ^ (h >>> 4);
        return locks[h & mask];
    }

    private static int ceilingPowerOfTwo(int x) {
        if (x == 1) {
            return 1;
        }
        int n = Integer.highestOneBit(x - 1) << 1;
        return n > 0 ? n : 1 << 30;
    }
}
