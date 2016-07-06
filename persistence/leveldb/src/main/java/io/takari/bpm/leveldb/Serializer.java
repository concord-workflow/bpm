package io.takari.bpm.leveldb;

public interface Serializer {

    Object fromBytes(byte[] value);

    byte[] toBytes(Object value);
}
