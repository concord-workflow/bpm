package io.takari.bpm.lock;

import io.takari.bpm.misc.CoverageIgnore;

public class NoopLockManager implements LockManager {

    @Override
    @CoverageIgnore
    public void lock(String processBusinessKey) {
    }

    @Override
    @CoverageIgnore
    public void unlock(String processBusinessKey) {
    }
}
