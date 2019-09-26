package io.takari.bpm.model.form;

import io.takari.bpm.misc.CoverageIgnore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class FormField implements Serializable {

    private static transient final Map<String, OptionIndex> allowedOptions = new HashMap<>();

    @CoverageIgnore
    @SuppressWarnings("unchecked")
    public static <T> Option<T> registerOption(String fieldType, String name, Class<T> type) {
        synchronized (allowedOptions) {
            OptionIndex idx = allowedOptions.get(fieldType);
            if (idx == null) {
                idx = new OptionIndex(Option::new);
                allowedOptions.put(fieldType, idx);
            }

            return (Option<T>) idx.register(name, type);
        }
    }

    private final String name;
    private final String type;
    private final String label;
    private final Object defaultValue;
    private final Object allowedValue;
    private final Cardinality cardinality;
    private final Map<String, Object> options;

    public FormField(String name, String type, String label, Object defaultValue, Object allowedValue, Cardinality cardinality, Map<String, Object> options) {
        this.name = name;
        this.type = type;
        this.label = label;
        this.defaultValue = defaultValue;
        this.allowedValue = allowedValue;
        this.cardinality = cardinality;
        this.options = options;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Object getAllowedValue() {
        return allowedValue;
    }

    public Cardinality getCardinality() {
        return cardinality;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public <T> T getOption(Option<T> o) {
        if (options == null) {
            return null;
        }

        Object v = options.get(o.name);
        if (v == null) {
            return null;
        }

        return o.cast(v);
    }

    public enum Cardinality {

        /**
         * stuff
         */
        ONE_AND_ONLY_ONE,

        /**
         * stuff?
         */
        ONE_OR_NONE,

        /**
         * stuff+
         */
        AT_LEAST_ONE,

        /**
         * stuff*
         */
        ANY;

        public String toSymbol() {
            switch (this) {
                case ONE_AND_ONLY_ONE:
                    return "";
                case ONE_OR_NONE:
                    return "?";
                case AT_LEAST_ONE:
                    return "+";
                case ANY:
                    return "*";
                default:
                    throw new IllegalArgumentException("Unknown cardinality type: " + this);
            }
        }

        public static Cardinality fromSymbol(String s) {
            if (s == null || s.isEmpty()) {
                return ONE_AND_ONLY_ONE;
            }

            switch (s) {
                case "?":
                    return ONE_OR_NONE;
                case "+":
                    return AT_LEAST_ONE;
                case "*":
                    return ANY;
                default:
                    return ONE_AND_ONLY_ONE;
            }
        }
    }

    public static final class Option<T> {

        private final String name;
        private final Class<T> type;

        public Option(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }

        public T cast(Object v) {
            if (v == null) {
                return null;
            }

            Class<?> other = v.getClass();
            if (!type.isAssignableFrom(other)) {
                throw new IllegalArgumentException("Invalid value type: expected " + type + ", got " + other);
            }

            return type.cast(v);
        }

        @Override
        @CoverageIgnore
        public String toString() {
            return "Option{" +
                    "name='" + name + '\'' +
                    ", type=" + type +
                    '}';
        }
    }

    public static final class OptionIndex {

        private final Map<String, Option<?>> options = new HashMap<>();
        private final BiFunction<String, Class<?>, Option<?>> optionMaker;

        public OptionIndex(BiFunction<String, Class<?>, Option<?>> optionMaker) {
            this.optionMaker = optionMaker;
        }

        public Option<?> register(String name, Class<?> type) {
            synchronized (options) {
                if (options.containsKey(name)) {
                    throw new IllegalStateException("Option '" + name + "' is already registered in this context. " +
                            "Check for duplicate declarations in the code");
                }

                Option<?> o = optionMaker.apply(name, type);
                options.put(name, o);
                return o;
            }
        }

        public boolean contains(Option<?> o) {
            synchronized (options) {
                Option<?> q = options.get(o.name);
                return q != null && q.type.equals(o.type);
            }
        }
    }

    public static final class Builder {

        private final String name;
        private final String type;

        private String label;
        private Object defaultValue;
        private Object allowedValue;
        private Cardinality cardinality;
        private Map<String, Object> options;

        public Builder(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public Builder(FormField prev) {
            this.name = prev.getName();
            this.type = prev.getType();
            this.label = prev.getLabel();
            this.defaultValue = prev.getDefaultValue();
            this.allowedValue = prev.getAllowedValue();
            this.cardinality = prev.getCardinality();
            this.options = prev.getOptions();
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder allowedValue(Object allowedValue) {
            this.allowedValue = allowedValue;
            return this;
        }

        public Builder cardinality(Cardinality cardinality) {
            this.cardinality = cardinality;
            return this;
        }

        public Builder options(Map<Option<?>, ?> other) {
            if (this.options == null) {
                this.options = new HashMap<>();
            }

            for (Map.Entry<Option<?>, ?> e : other.entrySet()) {
                String k = e.getKey().name;
                Object v = e.getValue();
                this.options.put(k, v);
            }

            return this;
        }

        public <T> Builder option(Option<T> o, T value) {
            OptionIndex idx = allowedOptions.get(type);
            if (idx == null || !idx.contains(o)) {
                throw new IllegalArgumentException("Type '" + type + "' doesn't allow " + o);
            }

            if (value == null) {
                return this;
            }

            if (options == null) {
                options = new HashMap<>();
            }

            options.put(o.name, value);
            return this;
        }

        public FormField build() {
            if (label == null) {
                label = name;
            }

            if (cardinality == null) {
                cardinality = Cardinality.ONE_AND_ONLY_ONE;
            }

            return new FormField(name, type, label, defaultValue, allowedValue, cardinality, options);
        }
    }
}
