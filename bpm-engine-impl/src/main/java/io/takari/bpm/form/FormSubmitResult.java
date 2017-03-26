package io.takari.bpm.form;

import java.io.Serializable;
import java.util.List;

public class FormSubmitResult implements Serializable {

    public static FormSubmitResult ok(String processBusinessKey) {
        return new FormSubmitResult(processBusinessKey, null);
    }

    private final String processBusinessKey;
    private final List<ValidationError> errors;

    public FormSubmitResult(String processBusinessKey, List<ValidationError> errors) {
        this.processBusinessKey = processBusinessKey;
        this.errors = errors;
    }

    public String getProcessBusinessKey() {
        return processBusinessKey;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public boolean isValid() {
        return errors == null || errors.isEmpty();
    }

    public static class ValidationError implements Serializable {

        /**
         * This {@code fieldName} will be used for global validation errors.
         */
        public static final String GLOBAL_ERROR = "_global";

        private final String fieldName;
        private final String error;

        public ValidationError(String fieldName, String error) {
            this.fieldName = fieldName;
            this.error = error;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getError() {
            return error;
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
