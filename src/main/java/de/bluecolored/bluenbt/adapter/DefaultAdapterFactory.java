package de.bluecolored.bluenbt.adapter;

import de.bluecolored.bluenbt.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultAdapterFactory implements TypeDeserializerFactory {

    public static final DefaultAdapterFactory INSTANCE = new DefaultAdapterFactory();
    private static final Map<Class<? extends TypeDeserializer<?>>, TypeDeserializer<?>> TYPE_DESERIALIZER_CACHE =
            new ConcurrentHashMap<>();

    @Override
    public <T> Optional<TypeDeserializer<?>> create(Class<T> type) {
        return Optional.of(createFor(type));
    }

    public <T> TypeDeserializer<T> createFor(Class<T> type) {
        return new DefaultAdapter<>(type);
    }

    static class DefaultAdapter<T> implements TypeDeserializer<T>  {

        private final Class<T> type;
        private final Constructor<T> noArgConstructor;

        DefaultAdapter(Class<T> type) {
            this.type = type;

            Constructor<T> constructor = null;
            try {
                constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
            } catch (Exception ignore) {}
            this.noArgConstructor = constructor;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T read(NBTReader reader, BlueNBT blueNBT) throws IOException {
            if (noArgConstructor == null) throw new IOException("Failed to deserialize type '" + type + "': No zero arg constructor!");

            try {
                T object = this.noArgConstructor.newInstance();
                reader.beginCompound();

                while (reader.peek() != TagType.END) {
                    String name = blueNBT.getFieldNameTransformer().apply(reader.name());
                    try {
                        Field field = this.type.getDeclaredField(name);
                        field.setAccessible(true);
                        Class<?> fieldType = field.getType();

                        NBTDeserializer deserializerType = field.getAnnotation(NBTDeserializer.class);
                        if (deserializerType == null) {
                            deserializerType = fieldType.getAnnotation(NBTDeserializer.class);
                        }

                        if (deserializerType != null) {
                            TypeDeserializer<T> deserializer = (TypeDeserializer<T>) TYPE_DESERIALIZER_CACHE.computeIfAbsent(deserializerType.value(), t -> {
                                try {
                                    return t.getDeclaredConstructor().newInstance();
                                } catch (Exception ex) {
                                    throw new RuntimeException("Failed to create Instance of TypeDeserializer!", ex);
                                }
                            });
                            field.set(object, deserializer.read(reader, blueNBT));
                        } else {
                            field.set(object, blueNBT.read(reader, fieldType));
                        }
                    } catch (NoSuchFieldException ignore) {
                        reader.skip();
                    }
                }

                reader.endCompound();
                return object;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                throw new IOException("Failed to create instance of type '" + type + "'!", ex);
            }
        }

    }

}
