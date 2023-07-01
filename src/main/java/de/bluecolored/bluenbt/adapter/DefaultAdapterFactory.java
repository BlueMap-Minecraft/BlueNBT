package de.bluecolored.bluenbt.adapter;

import com.google.gson.internal.ObjectConstructor;
import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluenbt.*;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultAdapterFactory implements TypeDeserializerFactory {

    public static final DefaultAdapterFactory INSTANCE = new DefaultAdapterFactory();

    @Override
    public <T> Optional<TypeDeserializer<T>> create(TypeToken<T> type, BlueNBT blueNBT) {
        return Optional.of(createFor(type, blueNBT));
    }

    public <T> TypeDeserializer<T> createFor(TypeToken<T> type, BlueNBT blueNBT) {
        return new DefaultAdapter<>(type, blueNBT.getConstructorConstructor().get(type), blueNBT);
    }

    @RequiredArgsConstructor
    static class DefaultAdapter<T> implements TypeDeserializer<T>  {

        private final Map<Class<? extends TypeDeserializer<?>>, TypeDeserializer<?>> typeDeserializerCache =
                new ConcurrentHashMap<>();

        private final TypeToken<T> type;
        private final ObjectConstructor<T> constructor;
        private final BlueNBT blueNBT;

        @Override
        public T read(NBTReader reader) throws IOException {

            try {
                T object = constructor.construct();
                reader.beginCompound();

                while (reader.peek() != TagType.END) {
                    String name = blueNBT.getFieldNameTransformer().apply(reader.name());
                    try {
                        Field field = type.getRawType().getDeclaredField(name);
                        field.setAccessible(true);
                        Class<?> fieldType = field.getType();

                        NBTDeserializer deserializerType = field.getAnnotation(NBTDeserializer.class);
                        if (deserializerType == null) {
                            deserializerType = fieldType.getAnnotation(NBTDeserializer.class);
                        }

                        if (deserializerType != null) {
                            @SuppressWarnings("unchecked")
                            TypeDeserializer<T> deserializer = (TypeDeserializer<T>) typeDeserializerCache.computeIfAbsent(deserializerType.value(), t -> {
                                try {
                                    return t.getDeclaredConstructor().newInstance();
                                } catch (Exception ex) {
                                    throw new RuntimeException("Failed to create Instance of TypeDeserializer!", ex);
                                }
                            });
                            field.set(object, deserializer.read(reader));
                        } else {
                            field.set(object, blueNBT.read(reader, TypeToken.get(field.getGenericType())));
                        }
                    } catch (NoSuchFieldException ignore) {
                        reader.skip();
                    }
                }

                reader.endCompound();
                return object;
            } catch (IllegalAccessException ex) {
                throw new IOException("Failed to create instance of type '" + type + "'!", ex);
            }
        }

    }

}
