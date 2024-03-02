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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PrimitiveSerializerFactory implements TypeSerializerFactory {

    public static final PrimitiveSerializerFactory INSTANCE = new PrimitiveSerializerFactory();

    private static final Map<Type, TypeSerializer<?>> TYPE_SERIALIZER_MAP = new HashMap<>();
    static {
        registerTypeSerializer(boolean.class, Boolean.class, TagType.BYTE, PrimitiveSerializerFactory::writeBool);
        registerTypeSerializer(byte.class, Byte.class, TagType.BYTE, PrimitiveSerializerFactory::writeByte);
        registerTypeSerializer(short.class, Short.class, TagType.SHORT, PrimitiveSerializerFactory::writeShort);
        registerTypeSerializer(char.class, Character.class, TagType.SHORT, PrimitiveSerializerFactory::writeChar);
        registerTypeSerializer(int.class, Integer.class, TagType.INT, PrimitiveSerializerFactory::writeInt);
        registerTypeSerializer(long.class, Long.class, TagType.LONG, PrimitiveSerializerFactory::writeLong);
        registerTypeSerializer(float.class, Float.class, TagType.FLOAT, PrimitiveSerializerFactory::writeFloat);
        registerTypeSerializer(double.class, Double.class, TagType.DOUBLE, PrimitiveSerializerFactory::writeDouble);
        registerTypeSerializer(String.class, null, TagType.STRING, PrimitiveSerializerFactory::writeString);
    }

    private static <T> void registerTypeSerializer(Type primitiveType, Type boxedType, TagType tag, TypeSerializer<T> typeSerializer) {
        TypeSerializer<T> tagTypeTypeSerializer = new TypeSerializer<T>() {
            @Override
            public void write(T value, NBTWriter writer) throws IOException {
                typeSerializer.write(value, writer);
            }

            @Override
            public TagType type() {
                return tag;
            }
        };

        TYPE_SERIALIZER_MAP.put(primitiveType, tagTypeTypeSerializer);
        if (boxedType != null) TYPE_SERIALIZER_MAP.put(boxedType, tagTypeTypeSerializer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<TypeSerializer<T>> create(TypeToken<T> typeToken, BlueNBT blueNBT) {
        TypeSerializer<?> typeSerializer = TYPE_SERIALIZER_MAP.get(typeToken.getRawType());

        if (typeSerializer != null)
            return Optional.of((TypeSerializer<T>) typeSerializer);

        return Optional.empty();
    }

    public static void writeBool(boolean value, NBTWriter writer) throws IOException {
        writer.value(value ? (byte) 1 : (byte) 0);
    }

    public static void writeByte(byte value, NBTWriter writer) throws IOException {
        writer.value(value);
    }

    public static void writeShort(short value, NBTWriter writer) throws IOException {
        writer.value(value);
    }

    public static void writeChar(char value, NBTWriter writer) throws IOException {
        writer.value(value);
    }

    public static void writeInt(int value, NBTWriter writer) throws IOException {
        writer.value(value);
    }

    public static void writeLong(long value, NBTWriter writer) throws IOException {
        writer.value(value);
    }

    public static void writeFloat(float value, NBTWriter writer) throws IOException {
        writer.value(value);
    }

    public static void writeDouble(double value, NBTWriter writer) throws IOException {
        writer.value(value);
    }

    public static void writeString(String value, NBTWriter writer) throws IOException {
        writer.value(value);
    }

}
