package io.takari.bpm.lock;

public interface LockManager {

    /**
     * Locks the specified process instance. Should be reentrant.
     * @param processBusinessKey
     */
    void lock(String processBusinessKey);

    /**
     * Unlocks the specified process instance.
     * @param processBusinessKey
     */
    void unlock(String processBusinessKey);
}
