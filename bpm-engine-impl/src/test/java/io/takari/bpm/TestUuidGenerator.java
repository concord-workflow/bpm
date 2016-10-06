package io.takari.bpm;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class TestUuidGenerator implements UuidGenerator {

    private final AtomicLong counter = new AtomicLong();

    @Override
    public UUID generate() {
        String fmt = "00000000-0000-0000-0000-%012d";
        String s = String.format(fmt, counter.getAndIncrement());
        return UUID.fromString(s);
    }
}
