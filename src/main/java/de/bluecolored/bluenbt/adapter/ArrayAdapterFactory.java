/*
 * This file is part of BlueNBT, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluenbt.adapter;

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

        Type componentType = TypeUtil.getArrayComponentType(type);
        TypeDeserializer<?> componentTypeDeserializer = blueNBT.getTypeDeserializer(TypeToken.get(componentType));

        @SuppressWarnings({"unchecked", "rawtypes"})
        TypeDeserializer<T> result = new ArrayAdapter(TypeUtil.getRawType(componentType), componentTypeDeserializer);
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

            if (tag == TagType.BYTE_ARRAY || tag == TagType.INT_ARRAY || tag == TagType.LONG_ARRAY) {
                return reader.nextArray(length -> Array.newInstance(type, length));
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
