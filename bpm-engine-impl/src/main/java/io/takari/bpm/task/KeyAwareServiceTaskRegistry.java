package io.takari.bpm.task;

public interface KeyAwareServiceTaskRegistry extends ServiceTaskRegistry {

    boolean containsKey(String key);
}
