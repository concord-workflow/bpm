package io.takari.bpm.form;

import io.takari.bpm.api.ExecutionException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemFormStorage implements FormStorage {

    private final Map<UUID, Form> forms = new HashMap<>();

    @Override
    public void save(Form form) throws ExecutionException {
        synchronized (forms) {
            forms.put(form.getFormInstanceId(), form);
        }
    }

    @Override
    public void complete(UUID formInstanceId) throws ExecutionException {
        synchronized (forms) {
            forms.remove(formInstanceId);
        }
    }

    @Override
    public Form get(UUID formInstanceId) throws ExecutionException {
        synchronized (forms) {
            return forms.get(formInstanceId);
        }
    }

    public Map<UUID, Form> getForms() {
        return forms;
    }
}
