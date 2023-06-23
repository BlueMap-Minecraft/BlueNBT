package de.bluecolored.bluenbt;

import de.bluecolored.bluenbt.adapter.DefaultAdapterFactory;
import de.bluecolored.bluenbt.adapter.PrimitiveAdapterFactory;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;

public class BlueNBT {

    private final List<TypeDeserializerFactory> deserializerFactories = new ArrayList<>();
    private final Map<Class<?>, TypeDeserializer<?>> typeDeserializerMap = new HashMap<>();

    @Getter @Setter
    private Function<String, String> fieldNameTransformer = s -> {
        if (s.isEmpty()) return s;
        char first = s.charAt(0);
        if (Character.isUpperCase(first))
            return Character.toLowerCase(first) + s.substring(1);
        return s;
    };

    public BlueNBT() {
        register(PrimitiveAdapterFactory.INSTANCE);
    }

    public void register(TypeDeserializerFactory typeDeserializerFactory) {
        deserializerFactories.add(typeDeserializerFactory);
    }

    public <T> void register(Class<T> type, TypeDeserializer<T> typeDeserializer) {
        deserializerFactories.add(new TypeDeserializerFactory() {
            @Override
            public <U> Optional<TypeDeserializer<?>> create(Class<U> createType) {
                if (createType.equals(type))
                    return Optional.of(typeDeserializer);
                return Optional.empty();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public <T> TypeDeserializer<T> getTypeDeserializer(Class<T> type) {
        TypeDeserializer<T> deserializer = (TypeDeserializer<T>) typeDeserializerMap.get(type);

        if (deserializer == null) {
            for (int i = deserializerFactories.size() - 1; i >= 0; i--) {
                TypeDeserializerFactory factory = deserializerFactories.get(i);
                deserializer = (TypeDeserializer<T>) factory.create(type).orElse(null);
                if (deserializer != null) break;
            }

            if (deserializer == null)
                deserializer = DefaultAdapterFactory.INSTANCE.createFor(type);
            typeDeserializerMap.put(type, deserializer);
        }

        return deserializer;
    }

    public <T> T read(InputStream in, Class<T> type) throws IOException {
        return read(new NBTReader(in), type);
    }

    public <T> T read(NBTReader in, Class<T> type) throws IOException {
        return getTypeDeserializer(type).read(in, this);
    }

}
