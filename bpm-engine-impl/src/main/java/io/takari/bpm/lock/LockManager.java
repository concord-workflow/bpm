package io.takari.bpm.lock;

public interface LockManager {

    void lock(String processBusinessKey);

    void unlock(String processBusinessKey);
}
