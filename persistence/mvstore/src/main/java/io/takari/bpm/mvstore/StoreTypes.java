package io.takari.bpm.mvstore;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.h2.mvstore.type.StringDataType;

import java.nio.ByteBuffer;
import java.util.UUID;

final class StoreTypes {

    private StoreTypes() {
    }

    enum Unit {
        INSTANCE
    }

    static final class UnitDataType extends BasicDataType<Unit> {

        static final UnitDataType INSTANCE = new UnitDataType();

        private UnitDataType() {
        }

        @Override
        public int compare(Unit a, Unit b) {
            return 0;
        }

        @Override
        public int getMemory(Unit obj) {
            return 0;
        }

        @Override
        public void write(WriteBuffer buff, Unit data) {
            // intentionally 0 bytes
        }

        @Override
        public Unit read(ByteBuffer buff) {
            return Unit.INSTANCE;
        }

        @Override
        public Unit[] createStorage(int size) {
            return new Unit[size];
        }
    }

    static final class UuidDataType extends BasicDataType<UUID> {

        static final UuidDataType INSTANCE = new UuidDataType();

        private UuidDataType() {
        }

        @Override
        public int compare(UUID a, UUID b) {
            return compareNullableUuids(a, b);
        }

        @Override
        public int getMemory(UUID obj) {
            return 32;
        }

        @Override
        public void write(WriteBuffer buff, UUID data) {
            buff.putLong(data.getMostSignificantBits());
            buff.putLong(data.getLeastSignificantBits());
        }

        @Override
        public UUID read(ByteBuffer buff) {
            long msb = buff.getLong();
            long lsb = buff.getLong();
            return new UUID(msb, lsb);
        }

        @Override
        public UUID[] createStorage(int size) {
            return new UUID[size];
        }
    }

    record BusinessKeyIndexKey(String businessKey, String eventName, UUID eventId)
            implements Comparable<BusinessKeyIndexKey> {

        @Override
        public int compareTo(BusinessKeyIndexKey other) {
            int cmp = compareNullableStrings(this.businessKey, other.businessKey);
            if (cmp != 0) {
                return cmp;
            }
            cmp = compareNullableStrings(this.eventName, other.eventName);
            if (cmp != 0) {
                return cmp;
            }
            return compareNullableUuids(this.eventId, other.eventId);
        }
    }

    static final class BusinessKeyIndexKeyDataType extends BasicDataType<BusinessKeyIndexKey> {

        static final BusinessKeyIndexKeyDataType INSTANCE = new BusinessKeyIndexKeyDataType();

        private BusinessKeyIndexKeyDataType() {
        }

        @Override
        public int compare(BusinessKeyIndexKey a, BusinessKeyIndexKey b) {
            return a.compareTo(b);
        }

        @Override
        public int getMemory(BusinessKeyIndexKey obj) {
            int mem = 64;
            if (obj.businessKey() != null) {
                mem += StringDataType.INSTANCE.getMemory(obj.businessKey());
            }
            if (obj.eventName() != null) {
                mem += StringDataType.INSTANCE.getMemory(obj.eventName());
            }
            return mem;
        }

        @Override
        public void write(WriteBuffer buff, BusinessKeyIndexKey data) {
            writeNullableString(buff, data.businessKey());
            writeNullableString(buff, data.eventName());
            if (data.eventId() == null) {
                buff.put((byte) 0);
            } else {
                buff.put((byte) 1);
                buff.putLong(data.eventId().getMostSignificantBits());
                buff.putLong(data.eventId().getLeastSignificantBits());
            }
        }

        @Override
        public BusinessKeyIndexKey read(ByteBuffer buff) {
            String businessKey = readNullableString(buff);
            String eventName = readNullableString(buff);
            byte hasId = buff.get();
            UUID eventId = null;
            if (hasId != 0) {
                eventId = new UUID(buff.getLong(), buff.getLong());
            }
            return new BusinessKeyIndexKey(businessKey, eventName, eventId);
        }

        @Override
        public BusinessKeyIndexKey[] createStorage(int size) {
            return new BusinessKeyIndexKey[size];
        }
    }

    record ExpiredEventIndexKey(long expiredAtMillis, UUID eventId)
            implements Comparable<ExpiredEventIndexKey> {

        @Override
        public int compareTo(ExpiredEventIndexKey other) {
            int cmp = Long.compare(this.expiredAtMillis, other.expiredAtMillis);
            if (cmp != 0) {
                return cmp;
            }
            return compareNullableUuids(this.eventId, other.eventId);
        }
    }

    static final class ExpiredEventIndexKeyDataType extends BasicDataType<ExpiredEventIndexKey> {

        static final ExpiredEventIndexKeyDataType INSTANCE = new ExpiredEventIndexKeyDataType();

        private ExpiredEventIndexKeyDataType() {
        }

        @Override
        public int compare(ExpiredEventIndexKey a, ExpiredEventIndexKey b) {
            return a.compareTo(b);
        }

        @Override
        public int getMemory(ExpiredEventIndexKey obj) {
            return 48;
        }

        @Override
        public void write(WriteBuffer buff, ExpiredEventIndexKey data) {
            buff.putLong(data.expiredAtMillis());
            buff.putLong(data.eventId().getMostSignificantBits());
            buff.putLong(data.eventId().getLeastSignificantBits());
        }

        @Override
        public ExpiredEventIndexKey read(ByteBuffer buff) {
            long expiredAtMillis = buff.getLong();
            UUID eventId = new UUID(buff.getLong(), buff.getLong());
            return new ExpiredEventIndexKey(expiredAtMillis, eventId);
        }

        @Override
        public ExpiredEventIndexKey[] createStorage(int size) {
            return new ExpiredEventIndexKey[size];
        }
    }

    private static int compareNullableStrings(String a, String b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }

    private static int compareNullableUuids(UUID a, UUID b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }

    private static void writeNullableString(WriteBuffer buff, String value) {
        if (value == null) {
            buff.put((byte) 0);
        } else {
            buff.put((byte) 1);
            // StringDataType preserves all UTF-16 code units (including unpaired surrogates) losslessly.
            StringDataType.INSTANCE.write(buff, value);
        }
    }

    private static String readNullableString(ByteBuffer buff) {
        byte present = buff.get();
        if (present == 0) {
            return null;
        }
        if (present != 1) {
            throw new PersistenceException("Corrupt index key string marker: " + present);
        }
        return StringDataType.INSTANCE.read(buff);
    }
}
