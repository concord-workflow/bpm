package io.takari.bpm.mvstore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;

public class JavaSerializer implements Serializer {

    private final ObjectInputFilter inputFilter;

    public JavaSerializer() {
        this(null);
    }

    public JavaSerializer(ObjectInputFilter inputFilter) {
        this.inputFilter = inputFilter;
    }

    @Override
    public byte[] serialize(Object value) {
        if (value == null) {
            return null;
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(value);
            out.flush();
            return bos.toByteArray();
        } catch (IOException | RuntimeException e) {
            if (e instanceof PersistenceException pe) {
                throw pe;
            }
            throw new PersistenceException("Failed to serialize object of type " + value.getClass().getName(), e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ContextClassLoaderObjectInputStream(bis, inputFilter)) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            if (e instanceof PersistenceException pe) {
                throw pe;
            }
            throw new PersistenceException("Failed to deserialize object", e);
        }
    }

    private static final class ContextClassLoaderObjectInputStream extends ObjectInputStream {

        ContextClassLoaderObjectInputStream(InputStream in, ObjectInputFilter filter) throws IOException {
            super(in);
            if (filter != null) {
                ObjectInputFilter jvmFilter = ObjectInputFilter.Config.getSerialFilter();
                setObjectInputFilter(ObjectInputFilter.merge(filter, jvmFilter));
            }
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            try {
                return super.resolveClass(desc);
            } catch (ClassNotFoundException ex) {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                if (tccl == null) {
                    throw ex;
                }
                return Class.forName(desc.getName(), false, tccl);
            }
        }

        @Override
        protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
            try {
                return super.resolveProxyClass(interfaces);
            } catch (ClassNotFoundException ex) {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                if (tccl == null) {
                    throw ex;
                }
                Class<?>[] resolved = new Class<?>[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    resolved[i] = Class.forName(interfaces[i], false, tccl);
                }
                try {
                    @SuppressWarnings("deprecation")
                    Class<?> proxyClass = Proxy.getProxyClass(tccl, resolved);
                    return proxyClass;
                } catch (IllegalArgumentException e) {
                    throw new ClassNotFoundException(null, e);
                }
            }
        }
    }
}
