package io.takari.bpm.leveldb;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.*;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.EventMapHelper.EventRecord;
import io.takari.bpm.ExecutionContextImpl;
import io.takari.bpm.commands.ExecutionCommand;
import io.takari.bpm.commands.HandleRaisedErrorCommand;
import io.takari.bpm.commands.MergeExecutionContextCommand;
import io.takari.bpm.commands.PersistExecutionCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.commands.ProcessEventMappingCommand;
import io.takari.bpm.commands.SuspendExecutionCommand;
import io.takari.bpm.event.Event;
import io.takari.bpm.event.ExpiredEvent;
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
                kryo.register(DefaultExecution.class);
                kryo.register(ExecutionContextImpl.class);
                kryo.register(EventRecord.class);

                kryo.register(ProcessElementCommand.class);
                kryo.register(ExecutionCommand.class);
                kryo.register(HandleRaisedErrorCommand.class);
                kryo.register(MergeExecutionContextCommand.class);
                kryo.register(SuspendExecutionCommand.class);
                kryo.register(ProcessEventMappingCommand.class);
                kryo.register(PersistExecutionCommand.class);

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
