package io.takari.bpm.mvstore;

/**
 * Serializes process state and event payloads to and from byte arrays.
 * <p>
 * Implementations must be thread-safe. Passing {@code null} returns {@code null};
 * serializing a non-null value must return a non-null byte array.
 * Failures should throw {@link PersistenceException}.
 */
public interface Serializer {

    byte[] serialize(Object value);

    Object deserialize(byte[] bytes);
}
