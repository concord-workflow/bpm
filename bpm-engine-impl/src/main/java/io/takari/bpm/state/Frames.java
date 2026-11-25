package io.takari.bpm.state;

import io.takari.bpm.api.Variables;
import io.takari.bpm.state.Scopes.Scope;

import java.util.Objects;
import java.util.UUID;

/**
 * Variable frames. Call activities and subprocesses with a separate context have their own variables.
 * The process state holds the variables of the current scope's owner (see
 * {@link Scopes#getVariablesOwnerId(UUID)}); the variables of other owners are stored in their scopes.
 * The frames are switched when the current scope changes.
 */
public final class Frames {

    /**
     * Switches the current scope and the current variables.
     */
    public static ProcessInstance switchTo(ProcessInstance state, Scopes next) {
        Scopes prev = state.getScopes();
        UUID prevOwner = prev.getVariablesOwnerId(prev.getCurrentId());
        UUID nextOwner = next.getVariablesOwnerId(next.getCurrentId());

        if (Objects.equals(prevOwner, nextOwner)) {
            return state.setScopes(next);
        }

        Variables vars = state.getVariables();

        if (nextOwner == null) {
            // no current scope (e.g. the root scope is closed): the current variables remain the root's
            return state.setScopes(next);
        }

        if (prevOwner == null) {
            // no current scope: the current variables are the root's
            prevOwner = next.getRootId();
            if (prevOwner == null || prevOwner.equals(nextOwner)) {
                return state.setScopes(next);
            }
        }

        if (!next.values().containsKey(prevOwner)) {
            // the previous owner is finished, its variables were merged into the current ones
            return state.setScopes(next);
        }

        // save the variables of the previous owner
        next = next.setVariables(prevOwner, vars);

        Scope s = next.values().get(nextOwner);
        if (s.getVariables() != null) {
            vars = s.getVariables();
        }

        return state.setScopes(next).setVariables(vars);
    }

    /**
     * Makes the current scope the owner of its own variables. The current variables are saved as
     * the parent's variables.
     */
    public static ProcessInstance enter(ProcessInstance state, Variables variables) {
        Scopes scopes = state.getScopes();
        UUID scopeId = scopes.getCurrentId();
        UUID parentOwner = scopes.getVariablesOwnerId(scopeId);

        if (parentOwner != null) {
            scopes = scopes.setVariables(parentOwner, state.getVariables());
        }

        scopes = scopes.markVariablesOwner(scopeId);
        return state.setScopes(scopes).setVariables(variables);
    }

    /**
     * @return {@code true} if the current scope owns its variables.
     */
    public static boolean isCurrentScopeOwner(ProcessInstance state) {
        Scopes scopes = state.getScopes();
        UUID id = scopes.getCurrentId();
        if (id == null) {
            return false;
        }
        Scope s = scopes.values().get(id);
        return s != null && s.isOwnsVariables();
    }

    /**
     * @return the variables of the parent of the current scope's owner.
     */
    public static Variables getParentVariables(ProcessInstance state) {
        Scopes scopes = state.getScopes();
        UUID owner = scopes.getVariablesOwnerId(scopes.getCurrentId());
        if (owner == null) {
            return state.getVariables();
        }

        Scope s = scopes.values().get(owner);
        UUID parentOwner = scopes.getVariablesOwnerId(s.getParentId());
        if (parentOwner == null) {
            return state.getVariables();
        }

        Variables v = scopes.values().get(parentOwner).getVariables();
        return v != null ? v : state.getVariables();
    }

    private Frames() {
    }
}
