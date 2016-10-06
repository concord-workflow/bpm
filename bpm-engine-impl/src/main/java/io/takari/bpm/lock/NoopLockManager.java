package io.takari.bpm.lock;

public class NoopLockManager implements LockManager {

    @Override
    public void lock(String processBusinessKey) {
    }

    @Override
    public void unlock(String processBusinessKey) {
    }
}
