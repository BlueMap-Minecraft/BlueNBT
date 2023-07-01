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

public class CollectionAdapterFactory implements TypeDeserializerFactory {

    public static final CollectionAdapterFactory INSTANCE = new CollectionAdapterFactory();

    @Override
    public <T> Optional<TypeDeserializer<T>> create(TypeToken<T> typeToken, BlueNBT blueNBT) {
        Type type = typeToken.getType();

        Class<? super T> rawType = typeToken.getRawType();
        if (!Collection.class.isAssignableFrom(rawType)) {
            return Optional.empty();
        }

        Type elementType = $Gson$Types.getCollectionElementType(type, rawType);
        TypeDeserializer<?> elementTypeDeserializer = blueNBT.getTypeDeserializer(TypeToken.get(elementType));
        ObjectConstructor<T> constructor = blueNBT.getConstructorConstructor().get(typeToken);

        @SuppressWarnings({"unchecked", "rawtypes"})
        TypeDeserializer<T> result = new CollectionAdapter(elementTypeDeserializer, constructor);
        return Optional.of(result);
    }

    @RequiredArgsConstructor
    static class CollectionAdapter<E> implements TypeDeserializer<Collection<E>>  {

        private final TypeDeserializer<E> typeDeserializer;
        private final ObjectConstructor<? extends Collection<E>> constructor;

        @Override
        public Collection<E> read(NBTReader reader) throws IOException {
            Collection<E> collection = constructor.construct();
            reader.beginList();
            while (reader.hasNext()) {
                E instance = typeDeserializer.read(reader);
                collection.add(instance);
            }
            reader.endList();
            return collection;
        }

    }

}
