package io.takari.bpm.actions;

import java.util.List;

/**
 * Moves a token from one set of elements to another and adjusts the expected
 * join activations accordingly. Used when a token deviates from the path that was
 * assumed when the expectations were registered, e.g. when an exclusive gateway
 * selects one of its flows or an activity is interrupted by a boundary event.
 */
public class RetargetFlowsAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String definitionId;
    private final List<String> fromIds;
    private final List<String> toIds;

    public RetargetFlowsAction(String definitionId, List<String> fromIds, List<String> toIds) {
        this.definitionId = definitionId;
        this.fromIds = List.copyOf(fromIds);
        this.toIds = List.copyOf(toIds);
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public List<String> getFromIds() {
        return fromIds;
    }

    public List<String> getToIds() {
        return toIds;
    }

    @Override
    public String toString() {
        return "RetargetFlowsAction [" +
                "definitionId=" + definitionId +
                ", fromIds=" + fromIds +
                ", toIds=" + toIds +
                ']';
    }
}
