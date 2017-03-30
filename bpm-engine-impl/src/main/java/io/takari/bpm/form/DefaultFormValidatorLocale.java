package io.takari.bpm.form;

import io.takari.bpm.model.form.FormField.Cardinality;

public class DefaultFormValidatorLocale implements FormValidatorLocale {

    @Override
    public String noFieldsDefined(String formId) {
        return String.format("Form (%s) has no fields", formId);
    }

    @Override
    public String invalidCardinality(String formId, String fieldName, Cardinality requiredCardinality, Object value) {
        return String.format("%s: Invalid cardinality, expected %s", fieldName, spell(requiredCardinality));
    }

    @Override
    public String expectedString(String formId, String fieldName, Integer idx, Object value) {
        return String.format("Field %s: expected a string value, got %s", fieldName, idx, value);
    }

    @Override
    public String expectedInteger(String formId, String fieldName, Integer idx, Object value) {
        return String.format("Field %s: expected an integer value, got %s", fieldName(fieldName, idx), value);
    }

    @Override
    public String expectedDecimal(String formId, String fieldName, Integer idx, Object value) {
        return String.format("Field %s: expected a decimal value, got %s", fieldName(fieldName, idx), value);
    }

    @Override
    public String doesntMatchPattern(String formId, String fieldName, Integer idx, String pattern, Object value) {
        return String.format("Field %s: value '%s' doesn't match pattern '%s'", fieldName(fieldName, idx), pattern, value);
    }

    @Override
    public String integerRangeError(String formId, String fieldName, Integer idx, Long min, Long max, Object value) {
        return String.format("Field %s: value '%s' must be %s", fieldName(fieldName, idx), value, bounds(min, max));
    }

    @Override
    public String decimalRangeError(String formId, String fieldName, Integer idx, Double min, Double max, Object value) {
        return String.format("Field %s: value '%s' must be %s", fieldName(fieldName, idx), value, bounds(min, max));
    }

    @Override
    public String valueNotAllowed(String formId, String fieldName, Integer idx, Object allowed, Object value) {
        return String.format("Field %s: value '%s' is not allowed, valid values: %s", fieldName(fieldName, idx), value, allowed);
    }

    private static String fieldName(String fieldName, Integer idx) {
        String s = fieldName;
        if (idx != null) {
            s = s + "[" + idx + "]";
        }
        return s;
    }

    private static String spell(Cardinality c) {
        if (c == null) {
            throw new IllegalArgumentException("Cardinality can't be null");
        }

        switch (c) {
            case ANY:
                return "any number of values";
            case ONE_AND_ONLY_ONE:
                return "a single value";
            case ONE_OR_NONE:
                return "a single optional value";
            case AT_LEAST_ONE:
                return "at least a single value";
            default:
                throw new IllegalArgumentException("Unsupported cardinality type: " + c);
        }
    }

    private static String bounds(Object min, Object max) {
        if (min != null && max != null) {
            return String.format("within %s and %s (inclusive)", min, max);
        } else if (min == null) {
            return String.format("less or equal than %s", max);
        } else {
            return String.format("equal or greater than %s", min);
        }
    }
}
