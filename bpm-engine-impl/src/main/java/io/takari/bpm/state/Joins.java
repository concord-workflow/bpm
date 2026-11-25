package io.takari.bpm.state;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.Activations.Activation;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Joins {

    /**
     * Fails if any of the joins of the specified scope's container still holds tokens. Such tokens would be
     * lost: the container (the process, a subprocess or a call activity) is finished and nothing can complete
     * the join anymore. Usually it means an invalid process definition, e.g. a parallel join that waits for a
     * flow that can never be activated.
     */
    public static void assertNoWaitingTokens(ProcessInstance state, UUID scopeId) throws ExecutionException {
        if (scopeId == null) {
            return;
        }

        UUID containerId = Activations.containerOf(state.getScopes(), scopeId);
        List<Activation> waiting = state.getActivations().findWaiting(containerId);
        if (waiting.isEmpty()) {
            return;
        }

        String details = waiting.stream()
                .map(Activation::toString)
                .collect(Collectors.joining(", "));

        throw new ExecutionException("Process '%s' (%s) finished with tokens waiting in joins, the joins will never be completed: %s",
                state.getBusinessKey(), state.getId(), details);
    }

    private Joins() {
    }
}
