package io.takari.bpm.leveldb;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import io.takari.bpm.commands.ActivityFinalizerCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.event.Event;
import io.takari.bpm.event.ExpiredEvent;
import io.takari.bpm.state.Events;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.Scopes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.UUID;

public class KryoSerializer implements Serializer {

    private final KryoPool kryoPool;

    public KryoSerializer() {

        KryoFactory factory = new KryoFactory() {
            @Override
            public Kryo create() {
                Kryo kryo = new Kryo();
                kryo.setReferences(true);
                kryo.register(UUID.class);
                kryo.register(Event.class);
                kryo.register(ExpiredEvent.class);
                kryo.register(HashSet.class);
                kryo.register(ProcessInstance.class);
                kryo.register(ExecutionContextImpl.class);
                kryo.register(Scopes.Scope.class);
                kryo.register(Events.EventRecord.class);

                kryo.register(ProcessElementCommand.class);
                kryo.register(ActivityFinalizerCommand.class);

                // TODO more classes

                kryo.setClassLoader(Thread.currentThread().getContextClassLoader());

                return kryo;
            }
        };

        this.kryoPool = new KryoPool.Builder(factory).softReferences().build();
    }

    @Override
    public byte[] toBytes(Object n) {
        Kryo kryo = kryoPool.borrow();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (Output output = new Output(bos)) {
            kryo.writeClassAndObject(output, n);
        } finally {
            kryoPool.release(kryo);
        }
        return bos.toByteArray();
    }

    @Override
    public Object fromBytes(byte[] bytes) {
        Kryo kryo = kryoPool.borrow();

        try (Input input = new Input(new ByteArrayInputStream(bytes))) {
            return kryo.readClassAndObject(input);
        } finally {
            kryoPool.release(kryo);
        }
    }
}
