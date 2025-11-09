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

import de.bluecolored.bluenbt.*;
import de.bluecolored.bluenbt.TypeToken;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link TypeAdapterFactory} creating {@link TypeAdapter}s for any Array-Type
 */
public class ArrayAdapterFactory implements TypeAdapterFactory {

    public static final ArrayAdapterFactory INSTANCE = new ArrayAdapterFactory();

    @Override
    public <T> Optional<TypeAdapter<T>> create(TypeToken<T> typeToken, BlueNBT blueNBT) {
        if (!typeToken.isArray()) return Optional.empty();

        TypeToken<?> componentType = TypeToken.of(typeToken.getComponentType());
        TypeSerializer<?> componentTypeSerializer = blueNBT.getTypeSerializer(componentType);
        TypeDeserializer<?> componentTypeDeserializer = blueNBT.getTypeDeserializer(componentType);

        @SuppressWarnings({"unchecked", "rawtypes"})
        TypeAdapter<T> result = new ArrayAdapter(componentType.getRawType(), componentTypeSerializer, componentTypeDeserializer);
        return Optional.of(result);
    }

    @RequiredArgsConstructor
    static class ArrayAdapter<E> implements TypeAdapter<Object>  {

        private final Class<E> type;
        private final TypeSerializer<E> typeSerializer;
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

        @Override
        @SuppressWarnings("unchecked")
        public void write(Object value, NBTWriter writer) throws IOException {
            switch (value) {
                case byte[] bytes -> writer.value(bytes);
                case int[] ints -> writer.value(ints);
                case long[] longs -> writer.value(longs);
                default -> {
                    int length = Array.getLength(value);
                    if (length == 0) {
                        writer.beginList(length, typeSerializer.type());
                        writer.endList();
                    } else {
                        writer.beginList(length);
                        for (int i = 0; i < length; i++) {
                            typeSerializer.write(
                                    Objects.requireNonNull((E) Array.get(value, i), "'null' values are not supported in a list."),
                                    writer
                            );
                        }
                        writer.endList();
                    }
                }
            }
        }

        @Override
        public TagType type() {
            //noinspection IfCanBeSwitch
            if (type.equals(byte.class)) return TagType.BYTE_ARRAY;
            if (type.equals(int.class)) return TagType.INT_ARRAY;
            if (type.equals(long.class)) return TagType.LONG_ARRAY;
            return TagType.LIST;
        }

    }

}
