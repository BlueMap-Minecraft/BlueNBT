package de.bluecolored.bluenbt.adapter;

import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.Primitives;
import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluenbt.*;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Optional;

public class ArrayAdapterFactory implements TypeDeserializerFactory {

    public static final ArrayAdapterFactory INSTANCE = new ArrayAdapterFactory();

    @Override
    public <T> Optional<TypeDeserializer<T>> create(TypeToken<T> typeToken, BlueNBT blueNBT) {
        Type type = typeToken.getType();
        if (!(type instanceof GenericArrayType || type instanceof Class && ((Class<?>) type).isArray())) {
            return Optional.empty();
        }

        Type componentType = $Gson$Types.getArrayComponentType(type);
        TypeDeserializer<?> componentTypeDeserializer = blueNBT.getTypeDeserializer(TypeToken.get(componentType));

        @SuppressWarnings({"unchecked", "rawtypes"})
        TypeDeserializer<T> result = new ArrayAdapter($Gson$Types.getRawType(componentType), componentTypeDeserializer);
        return Optional.of(result);
    }

    @RequiredArgsConstructor
    static class ArrayAdapter<E> implements TypeDeserializer<Object>  {

        private final Class<E> type;
        private final TypeDeserializer<E> typeDeserializer;

        @Override
        public Object read(NBTReader reader) throws IOException {
            TagType tag = reader.peek();

            if (tag == TagType.BYTE_ARRAY && type.equals(byte.class)) {
                return reader.nextByteArray();
            }

            if (tag == TagType.INT_ARRAY && type.equals(int.class)) {
                return reader.nextIntArray();
            }

            if (tag == TagType.LONG_ARRAY && type.equals(long.class)) {
                return reader.nextLongArray();
            }

            int length = reader.beginList();
            Object array = Array.newInstance(type, length);
            for (int i = 0; i < length; i++) {
                E instance = typeDeserializer.read(reader);
                Array.set(array, i, instance);
            }
            reader.endList();
            return array;
        }

    }

}
