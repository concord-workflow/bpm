package io.takari.bpm.form;

import io.takari.bpm.model.form.FormField;

public interface FormValidatorLocale {

    /**
     * The form has no fields defined.
     *
     * @param formId
     * @return
     */
    String noFieldsDefined(String formId);

    /**
     * Invalid cardinality of a value.
     *
     * @param formId
     * @param field
     * @param value
     * @return
     */
    String invalidCardinality(String formId, FormField field, Object value);

    /**
     * Expected a string value.
     *
     * @param formId
     * @param field
     * @param idx
     * @param value
     * @return
     */
    String expectedString(String formId, FormField field, Integer idx, Object value);

    /**
     * Expected an integer value.
     *
     * @param formId
     * @param field
     * @param idx
     * @param value
     * @return
     */
    String expectedInteger(String formId, FormField field, Integer idx, Object value);

    /**
     * Expected a decimal value.
     *
     * @param formId
     * @param field
     * @param idx
     * @param value
     * @return
     */
    String expectedDecimal(String formId, FormField field, Integer idx, Object value);

    /**
     * Expected a boolean value.
     *
     * @param formId
     * @param field
     * @param idx
     * @param value
     * @return
     */
    String expectedBoolean(String formId, FormField field, Integer idx, Object value);

    /**
     * A string value doesn't match the specified pattern.
     *
     * @param formId
     * @param field
     * @param idx
     * @param pattern
     * @param value
     * @return
     */
    String doesntMatchPattern(String formId, FormField field, Integer idx, String pattern, Object value);

    /**
     * Value must be within the specified range.
     *
     * @param formId
     * @param field
     * @param idx
     * @param min
     * @param max
     * @param value
     * @return
     */
    String integerRangeError(String formId, FormField field, Integer idx, Long min, Long max, Object value);

    /**
     * Value must be within the specified range.
     *
     * @param formId
     * @param field
     * @param idx
     * @param min
     * @param max
     * @param value
     * @return
     */
    String decimalRangeError(String formId, FormField field, Integer idx, Double min, Double max, Object value);

    /**
     * Value is not allowed.
     *
     * @param formId
     * @param field
     * @param idx
     * @param allowed
     * @param value
     * @return
     */
    String valueNotAllowed(String formId, FormField field, Integer idx, Object allowed, Object value);
}
