package io.takari.bpm.mvstore;

import io.takari.bpm.event.Event;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

final class EventCodec {

    private static final byte FORMAT_VERSION = 1;

    private static final int FLAG_EXCLUSIVE = 1;
    private static final int FLAG_EXECUTION_ID = 1 << 1;
    private static final int FLAG_DEFINITION_ID = 1 << 2;
    private static final int FLAG_SCOPE_ID = 1 << 3;
    private static final int FLAG_NAME = 1 << 4;
    private static final int FLAG_BUSINESS_KEY = 1 << 5;
    private static final int FLAG_EXPIRED_AT = 1 << 6;
    private static final int FLAG_PAYLOAD = 1 << 7;

    private EventCodec() {
    }

    record EventMetadata(UUID id, String processBusinessKey, String name, Long expiredAtMillis) {
    }

    static byte[] encode(Event event, Serializer serializer) {
        Objects.requireNonNull(event, "event must not be null");
        UUID id = Objects.requireNonNull(event.getId(), "event.id must not be null");

        UUID executionId = event.getExecutionId();
        String definitionId = event.getDefinitionId();
        UUID scopeId = event.getScopeId();
        String name = event.getName();
        String businessKey = event.getProcessBusinessKey();
        Date expiredAt = event.getExpiredAt();
        Object payload = event.getPayload();
        byte[] payloadBytes = null;
        if (payload != null) {
            payloadBytes = serializer.serialize(payload);
            if (payloadBytes == null) {
                throw new PersistenceException("Serializer returned null for non-null event payload of type "
                        + payload.getClass().getName());
            }
        }

        int flags = 0;
        long size = 2L + 16L; // version + flags + id

        if (event.isExclusive()) {
            flags |= FLAG_EXCLUSIVE;
        }
        if (executionId != null) {
            flags |= FLAG_EXECUTION_ID;
            size += 16L;
        }
        if (definitionId != null) {
            flags |= FLAG_DEFINITION_ID;
            size += 4L + 2L * definitionId.length();
        }
        if (scopeId != null) {
            flags |= FLAG_SCOPE_ID;
            size += 16L;
        }
        if (name != null) {
            flags |= FLAG_NAME;
            size += 4L + 2L * name.length();
        }
        if (businessKey != null) {
            flags |= FLAG_BUSINESS_KEY;
            size += 4L + 2L * businessKey.length();
        }
        if (expiredAt != null) {
            flags |= FLAG_EXPIRED_AT;
            size += 8L;
        }
        if (payloadBytes != null) {
            flags |= FLAG_PAYLOAD;
            size += 4L + payloadBytes.length;
        }

        if (size > Integer.MAX_VALUE) {
            throw new PersistenceException("Encoded event exceeds maximum buffer size: " + size);
        }

        ByteBuffer buf = ByteBuffer.allocate((int) size);
        buf.put(FORMAT_VERSION);
        buf.put((byte) flags);
        putUuid(buf, id);

        if (executionId != null) {
            putUuid(buf, executionId);
        }
        if (definitionId != null) {
            putString(buf, definitionId);
        }
        if (scopeId != null) {
            putUuid(buf, scopeId);
        }
        if (name != null) {
            putString(buf, name);
        }
        if (businessKey != null) {
            putString(buf, businessKey);
        }
        if (expiredAt != null) {
            buf.putLong(expiredAt.getTime());
        }
        if (payloadBytes != null) {
            putBytes(buf, payloadBytes);
        }

        return buf.array();
    }

    static Event decode(byte[] bytes, Serializer serializer) {
        if (bytes == null) {
            return null;
        }

        try {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            byte version = buf.get();
            if (version != FORMAT_VERSION) {
                throw new PersistenceException("Unsupported event format version: " + version);
            }

            int flags = buf.get() & 0xFF;
            boolean exclusive = (flags & FLAG_EXCLUSIVE) != 0;

            UUID id = getUuid(buf);
            UUID executionId = (flags & FLAG_EXECUTION_ID) != 0 ? getUuid(buf) : null;
            String definitionId = (flags & FLAG_DEFINITION_ID) != 0 ? getString(buf) : null;
            UUID scopeId = (flags & FLAG_SCOPE_ID) != 0 ? getUuid(buf) : null;
            String name = (flags & FLAG_NAME) != 0 ? getString(buf) : null;
            String businessKey = (flags & FLAG_BUSINESS_KEY) != 0 ? getString(buf) : null;
            Date expiredAt = (flags & FLAG_EXPIRED_AT) != 0 ? new Date(buf.getLong()) : null;

            Object payload = null;
            if ((flags & FLAG_PAYLOAD) != 0) {
                byte[] payloadBytes = getBytes(buf);
                payload = serializer.deserialize(payloadBytes);
            }

            if (buf.hasRemaining()) {
                throw new PersistenceException("Unexpected trailing bytes in encoded event: " + buf.remaining());
            }

            return new Event(id, executionId, definitionId, scopeId, name, businessKey, exclusive, expiredAt, payload);
        } catch (BufferUnderflowException e) {
            throw new PersistenceException("Truncated event record", e);
        }
    }

    static EventMetadata decodeMetadata(byte[] bytes) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            byte version = buf.get();
            if (version != FORMAT_VERSION) {
                throw new PersistenceException("Unsupported event format version: " + version);
            }

            int flags = buf.get() & 0xFF;
            UUID id = getUuid(buf);

            if ((flags & FLAG_EXECUTION_ID) != 0) {
                skipBytes(buf, 16);
            }
            if ((flags & FLAG_DEFINITION_ID) != 0) {
                skipString(buf);
            }
            if ((flags & FLAG_SCOPE_ID) != 0) {
                skipBytes(buf, 16);
            }

            String name = (flags & FLAG_NAME) != 0 ? getString(buf) : null;
            String businessKey = (flags & FLAG_BUSINESS_KEY) != 0 ? getString(buf) : null;
            Long expiredAtMillis = (flags & FLAG_EXPIRED_AT) != 0 ? buf.getLong() : null;

            return new EventMetadata(id, businessKey, name, expiredAtMillis);
        } catch (BufferUnderflowException e) {
            throw new PersistenceException("Truncated event record", e);
        }
    }

    private static void putUuid(ByteBuffer buf, UUID uuid) {
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
    }

    private static UUID getUuid(ByteBuffer buf) {
        return new UUID(buf.getLong(), buf.getLong());
    }

    private static void putString(ByteBuffer buf, String value) {
        int len = value.length();
        buf.putInt(len);
        for (int i = 0; i < len; i++) {
            buf.putChar(value.charAt(i));
        }
    }

    private static String getString(ByteBuffer buf) {
        int charLen = buf.getInt();
        if (charLen < 0 || charLen > buf.remaining() / 2) {
            throw new PersistenceException("Invalid string length in encoded event: " + charLen);
        }
        char[] chars = new char[charLen];
        for (int i = 0; i < charLen; i++) {
            chars[i] = buf.getChar();
        }
        return new String(chars);
    }

    private static void skipString(ByteBuffer buf) {
        int charLen = buf.getInt();
        if (charLen < 0 || charLen > buf.remaining() / 2) {
            throw new PersistenceException("Invalid string length in encoded event: " + charLen);
        }
        buf.position(buf.position() + charLen * 2);
    }

    private static void putBytes(ByteBuffer buf, byte[] data) {
        buf.putInt(data.length);
        buf.put(data);
    }

    private static byte[] getBytes(ByteBuffer buf) {
        int len = buf.getInt();
        if (len < 0 || len > buf.remaining()) {
            throw new PersistenceException("Invalid field length in encoded event: " + len);
        }
        byte[] data = new byte[len];
        buf.get(data);
        return data;
    }

    private static void skipBytes(ByteBuffer buf, int count) {
        if (count < 0 || count > buf.remaining()) {
            throw new PersistenceException("Truncated event record");
        }
        buf.position(buf.position() + count);
    }
}
