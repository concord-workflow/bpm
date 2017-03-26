package io.takari.bpm.model.form;

import io.takari.bpm.model.form.FormField.Option;

/**
 * Standard form field types and options.
 */
public final class DefaultFormFields {

    public static final class StringField {

        public static final String TYPE = "string";
        public static final Option<String> PATTERN = FormField.registerOption(TYPE, "pattern", String.class);
    }

    public static final class IntegerField {

        public static final String TYPE = "int";
        public static final Option<Long> MIN = FormField.registerOption(TYPE, "min", Long.class);
        public static final Option<Long> MAX = FormField.registerOption(TYPE, "max", Long.class);
    }

    public static final class DecimalField {

        public static final String TYPE = "decimal";
        public static final Option<Double> MIN = FormField.registerOption(TYPE, "min", Double.class);
        public static final Option<Double> MAX = FormField.registerOption(TYPE, "max", Double.class);
    }

    private DefaultFormFields() {
    }
}
