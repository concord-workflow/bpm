package io.takari.bpm.form;

import io.takari.bpm.api.ExecutionException;

import java.util.UUID;

/**
 * Form instance storage.
 */
public interface FormStorage {

    Form get(UUID formInstanceId) throws ExecutionException;

    void save(Form form) throws ExecutionException;

    void complete(UUID formInstanceId) throws ExecutionException;
}
