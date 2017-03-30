package io.takari.bpm.form;

import io.takari.bpm.model.form.FormField.Cardinality;

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
     * @param fieldName
     * @param requiredCardinality
     * @param value
     * @return
     */
    String invalidCardinality(String formId, String fieldName, Cardinality requiredCardinality, Object value);

    /**
     * Expected a string value.
     *
     * @param formId
     * @param fieldName
     * @param idx
     * @param value
     * @return
     */
    String expectedString(String formId, String fieldName, Integer idx, Object value);

    /**
     * Expected an integer value.
     *
     * @param formId
     * @param fieldName
     * @param idx
     * @param value
     * @return
     */
    String expectedInteger(String formId, String fieldName, Integer idx, Object value);

    /**
     * Expected a decimal value.
     *
     * @param formId
     * @param fieldName
     * @param idx
     * @param value
     * @return
     */
    String expectedDecimal(String formId, String fieldName, Integer idx, Object value);


    /**
     * A string value doesn't match the specified pattern.
     *
     * @param formId
     * @param fieldName
     * @param idx
     * @param pattern
     * @param value
     * @return
     */
    String doesntMatchPattern(String formId, String fieldName, Integer idx, String pattern, Object value);

    /**
     * Value must be within the specified range.
     *
     * @param formId
     * @param fieldName
     * @param idx
     * @param min
     * @param max
     * @param value
     * @return
     */
    String integerRangeError(String formId, String fieldName, Integer idx, Long min, Long max, Object value);

    /**
     * Value must be within the specified range.
     *
     * @param formId
     * @param fieldName
     * @param idx
     * @param min
     * @param max
     * @param value
     * @return
     */
    String decimalRangeError(String formId, String fieldName, Integer idx, Double min, Double max, Object value);

    /**
     * Value is not allowed.
     *
     * @param formId
     * @param fieldName
     * @param idx
     * @param allowed
     * @param value
     * @return
     */
    String valueNotAllowed(String formId, String fieldName, Integer idx, Object allowed, Object value);
}
