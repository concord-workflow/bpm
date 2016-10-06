package io.takari.bpm.state;

import java.io.Serializable;
import java.util.UUID;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.CommandStack;

public class ProcessInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String businessKey;
    private final Definitions definitions;
    private final ProcessStatus status;
    private final CommandStack stack;
    private final Variables variables;
    private final Activations activations;
    private final Scopes scopes;
    private final Events events;

    public ProcessInstance(UUID id, String businessKey, IndexedProcessDefinition definition) {
        this(id, businessKey, new Definitions(definition), ProcessStatus.RUNNING, new CommandStack(), new Variables(),
                new Activations(), new Scopes(), new Events());
    }

    private ProcessInstance(UUID id, String businessKey, Definitions definitions, ProcessStatus status, CommandStack stack,
            Variables variables, Activations activations, Scopes scopes, Events events) {

        this.id = id;
        this.businessKey = businessKey;
        this.definitions = definitions;
        this.status = status;
        this.stack = stack;
        this.variables = variables;
        this.activations = activations;
        this.scopes = scopes;
        this.events = events;
    }

    private ProcessInstance(ProcessInstance old, CommandStack stack) {
        this(old.id, old.businessKey, old.definitions, old.status, stack, old.variables, old.activations, old.scopes, old.events);
    }

    private ProcessInstance(ProcessInstance old, ProcessStatus status) {
        this(old.id, old.businessKey, old.definitions, status, old.stack, old.variables, old.activations, old.scopes, old.events);
    }

    private ProcessInstance(ProcessInstance old, Variables variables) {
        this(old.id, old.businessKey, old.definitions, old.status, old.stack, variables, old.activations, old.scopes, old.events);
    }

    private ProcessInstance(ProcessInstance old, Definitions definitions) {
        this(old.id, old.businessKey, definitions, old.status, old.stack, old.variables, old.activations, old.scopes, old.events);
    }

    private ProcessInstance(ProcessInstance old, Activations activations) {
        this(old.id, old.businessKey, old.definitions, old.status, old.stack, old.variables, activations, old.scopes, old.events);
    }

    private ProcessInstance(ProcessInstance old, Scopes scopes) {
        this(old.id, old.businessKey, old.definitions, old.status, old.stack, old.variables, old.activations, scopes, old.events);
    }

    private ProcessInstance(ProcessInstance old, Events events) {
        this(old.id, old.businessKey, old.definitions, old.status, old.stack, old.variables, old.activations, old.scopes, events);
    }

    public UUID getId() {
        return id;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public IndexedProcessDefinition getDefinition(String key) throws ExecutionException {
        IndexedProcessDefinition pd = definitions.get(key);
        if (pd == null) {
            throw new ExecutionException("Unknown process definition key: %s", key);
        }
        return pd;
    }

    public Definitions getDefinitions() {
        return definitions;
    }

    public ProcessInstance setDefinitions(Definitions defs) {
        return new ProcessInstance(this, defs);
    }

    public ProcessStatus getStatus() {
        return status;
    }

    public ProcessInstance setStatus(ProcessStatus status) {
        return new ProcessInstance(this, status);
    }

    public CommandStack getStack() {
        return stack;
    }

    public ProcessInstance setStack(CommandStack stack) {
        return new ProcessInstance(this, stack);
    }

    public Variables getVariables() {
        return variables;
    }

    public ProcessInstance setVariables(Variables variables) {
        return new ProcessInstance(this, variables);
    }

    public Activations getActivations() {
        return activations;
    }

    public ProcessInstance setActivations(Activations activations) {
        return new ProcessInstance(this, activations);
    }

    public Scopes getScopes() {
        return scopes;
    }

    public ProcessInstance setScopes(Scopes scopes) {
        return new ProcessInstance(this, scopes);
    }

    public Events getEvents() {
        return events;
    }

    public ProcessInstance setEvents(Events events) {
        return new ProcessInstance(this, events);
    }
}
