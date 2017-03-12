package io.takari.bpm.model;

import java.util.Collections;
import java.util.Map;

public final class ProcessDefinitionHelper {

    public static String dump(ProcessDefinition pd) {
        Map<String, SourceMap> sms = Collections.emptyMap();
        if (pd instanceof SourceAwareProcessDefinition) {
            sms = ((SourceAwareProcessDefinition) pd).getSourceMaps();
        }

        StringBuilder b = new StringBuilder();
        b.append("===================================\n").append("\tID: ").append(pd.getId()).append("\n");
        dump(b, pd, sms, 2);
        return b.toString();
    }

    private static StringBuilder pad(StringBuilder b, int level) {
        for (int i = 0; i < level; i++) {
            b.append("\t");
        }
        return b;
    }

    private static void dump(StringBuilder b, ProcessDefinition pd, Map<String, SourceMap> sourceMaps, int level) {
        for (AbstractElement e : pd.getChildren()) {
            pad(b, level).append(e.getClass().getSimpleName()).append(" [").append(e.getId()).append("]");

            if (e instanceof SequenceFlow) {
                SequenceFlow f = (SequenceFlow) e;
                b.append(" ").append(f.getFrom()).append(" -> ").append(f.getTo());
                if (f.getExpression() != null) {
                    b.append(" ").append(f.getExpression());
                }
            } else if (e instanceof ServiceTask) {
                ServiceTask t = (ServiceTask) e;
                b.append(" ").append(t.getType()).append(" ").append(t.getExpression());
                if (t.getIn() != null) {
                    pad(b.append("\n"), level + 1).append("IN: ").append(t.getIn());
                }
                if (t.getOut() != null) {
                    pad(b.append("\n"), level + 1).append("OUT: ").append(t.getOut());
                }
            }

            b.append("\n");

            SourceMap sm = sourceMaps.get(e.getId());
            if (sm != null) {
                pad(b, level + 1).append("source: ").append(sm).append("\n");
            }

            if (e instanceof ProcessDefinition) {
                dump(b, (ProcessDefinition) e, sourceMaps, level + 1);
            }
        }
    }

    private ProcessDefinitionHelper() {
    }
}
