package de.bluecolored.bluenbt.adapter;

import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TypeDeserializer;
import de.bluecolored.bluenbt.TypeDeserializerFactory;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MapAdapterFactory implements TypeDeserializerFactory {

    public static final MapAdapterFactory INSTANCE = new MapAdapterFactory();

    @Override
    public <T> Optional<TypeDeserializer<T>> create(TypeToken<T> typeToken, BlueNBT blueNBT) {
        Type type = typeToken.getType();

        Class<? super T> rawType = typeToken.getRawType();
        if (!Map.class.isAssignableFrom(rawType)) {
            return Optional.empty();
        }

        Type[] keyAndValueTypes = $Gson$Types.getMapKeyAndValueTypes(type, rawType);
        TypeDeserializer<?> elementTypeDeserializer = blueNBT.getTypeDeserializer(TypeToken.get(keyAndValueTypes[1]));
        ObjectConstructor<T> constructor = blueNBT.getConstructorConstructor().get(typeToken);

        @SuppressWarnings({"unchecked", "rawtypes"})
        TypeDeserializer<T> result = new MapAdapter(elementTypeDeserializer, constructor);
        return Optional.of(result);
    }

    @RequiredArgsConstructor
    static class MapAdapter<E> implements TypeDeserializer<Map<? super String, E>>  {

        private final TypeDeserializer<E> typeDeserializer;
        private final ObjectConstructor<? extends Map<String, E>> constructor;

        @Override
        public Map<? super String, E> read(NBTReader reader) throws IOException {
            Map<String, E> map = constructor.construct();
            reader.beginCompound();
            while (reader.hasNext()) {
                String key = reader.name();
                E instance = typeDeserializer.read(reader);
                map.put(key, instance);
            }
            reader.endCompound();
            return map;
        }

    }

}
