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

public class PrimitiveAdapterFactory implements TypeDeserializerFactory {

    public static final PrimitiveAdapterFactory INSTANCE = new PrimitiveAdapterFactory();

    private static final Map<Type, TypeDeserializer<?>> TYPE_DESERIALIZER_MAP = new HashMap<>();
    static {
        registerTypeDeserializer(boolean.class, Boolean.class, PrimitiveAdapterFactory::readBool);
        registerTypeDeserializer(byte.class, Byte.class, PrimitiveAdapterFactory::readByte);
        registerTypeDeserializer(short.class, Short.class, PrimitiveAdapterFactory::readShort);
        registerTypeDeserializer(char.class, Character.class, PrimitiveAdapterFactory::readChar);
        registerTypeDeserializer(int.class, Integer.class, PrimitiveAdapterFactory::readInt);
        registerTypeDeserializer(long.class, Long.class, PrimitiveAdapterFactory::readLong);
        registerTypeDeserializer(float.class, Float.class, PrimitiveAdapterFactory::readFloat);
        registerTypeDeserializer(double.class, Double.class, PrimitiveAdapterFactory::readDouble);
        registerTypeDeserializer(String.class, null, PrimitiveAdapterFactory::readString);
    }

    private static void registerTypeDeserializer(Type primitiveType, Type boxedType, TypeDeserializer<?> typeDeserializer) {
        TYPE_DESERIALIZER_MAP.put(primitiveType, typeDeserializer);
        if (boxedType != null) TYPE_DESERIALIZER_MAP.put(boxedType, typeDeserializer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<TypeDeserializer<T>> create(TypeToken<T> typeToken, BlueNBT blueNBT) {
        TypeDeserializer<?> typeDeserializer = TYPE_DESERIALIZER_MAP.get(typeToken.getRawType());

        if (typeDeserializer != null)
            return Optional.of((TypeDeserializer<T>) typeDeserializer);

        return Optional.empty();
    }

    public static boolean readBool(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        switch (type) {
            case BYTE: return reader.nextByte() != 0;
            case SHORT: return reader.nextShort() != 0;
            case INT: return reader.nextInt() != 0;
            case LONG: return reader.nextLong() != 0L;
            case FLOAT: return reader.nextFloat() != 0f;
            case DOUBLE: return reader.nextDouble() != 0d;
            case STRING: return Boolean.parseBoolean(reader.nextString());
            default: throw new IllegalStateException("STRING or any number tag expected but got " + type + ". At: " + reader.path());
        }
    }

    public static byte readByte(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        switch (type) {
            case BYTE: return reader.nextByte();
            case SHORT: return (byte) reader.nextShort();
            case INT: return (byte) reader.nextInt();
            case LONG: return (byte) reader.nextLong();
            case FLOAT: return (byte) reader.nextFloat();
            case DOUBLE: return (byte) reader.nextDouble();
            case STRING: return Byte.parseByte(reader.nextString());
            default: throw new IllegalStateException("BYTE tag expected but got " + type + ". At: " + reader.path());
        }
    }

    public static short readShort(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        switch (type) {
            case BYTE: return reader.nextByte();
            case SHORT: return reader.nextShort();
            case INT: return (short) reader.nextInt();
            case LONG: return (short) reader.nextLong();
            case FLOAT: return (short) reader.nextFloat();
            case DOUBLE: return (short) reader.nextDouble();
            case STRING: return Short.parseShort(reader.nextString());
            default: throw new IllegalStateException("SHORT tag expected but got " + type + ". At: " + reader.path());
        }
    }

    public static char readChar(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        switch (type) {
            case BYTE: return (char) reader.nextByte();
            case SHORT: return (char) reader.nextShort();
            case INT: return (char) reader.nextInt();
            case LONG: return (char) reader.nextLong();
            case FLOAT: return (char) reader.nextFloat();
            case DOUBLE: return (char) reader.nextDouble();
            case STRING:
                String string = reader.nextString();
                if (string.length() != 1)
                    throw new IllegalStateException("Can not parse char from string '" + string + "'");
                return string.charAt(0);
            default: throw new IllegalStateException("Any number tag expected but got " + type + ". At: " + reader.path());
        }
    }

    public static int readInt(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        switch (type) {
            case BYTE: return reader.nextByte();
            case SHORT: return reader.nextShort();
            case INT: return reader.nextInt();
            case LONG: return (int) reader.nextLong();
            case FLOAT: return (int) reader.nextFloat();
            case DOUBLE: return (int) reader.nextDouble();
            case STRING: return Integer.parseInt(reader.nextString());
            default: throw new IllegalStateException("INT tag expected but got " + type + ". At: " + reader.path());
        }
    }

    public static long readLong(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        switch (type) {
            case LONG: return reader.nextLong();
            case BYTE: return reader.nextByte();
            case SHORT: return reader.nextShort();
            case INT: return reader.nextInt();
            case FLOAT: return (long) reader.nextFloat();
            case DOUBLE: return (long) reader.nextDouble();
            case STRING: return Long.parseLong(reader.nextString());
            default: throw new IllegalStateException("LONG tag expected but got " + type + ". At: " + reader.path());
        }
    }

    public static float readFloat(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        switch (type) {
            case FLOAT: return reader.nextFloat();
            case BYTE: return reader.nextByte();
            case SHORT: return reader.nextShort();
            case INT: return reader.nextInt();
            case LONG: return reader.nextLong();
            case DOUBLE: return (float) reader.nextDouble();
            case STRING: return Float.parseFloat(reader.nextString());
            default: throw new IllegalStateException("FLOAT tag expected but got " + type + ". At: " + reader.path());
        }
    }

    public static double readDouble(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        switch (type) {
            case DOUBLE: return reader.nextDouble();
            case BYTE: return reader.nextByte();
            case SHORT: return reader.nextShort();
            case INT: return reader.nextInt();
            case LONG: return reader.nextLong();
            case FLOAT: return reader.nextFloat();
            case STRING: return Double.parseDouble(reader.nextString());
            default: throw new IllegalStateException("DOUBLE tag expected but got " + type + ". At: " + reader.path());
        }
    }

    public static String readString(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        switch (type) {
            case STRING: return reader.nextString();
            case BYTE: return String.valueOf(reader.nextByte());
            case SHORT: return String.valueOf(reader.nextShort());
            case INT: return String.valueOf(reader.nextInt());
            case LONG: return String.valueOf(reader.nextLong());
            case FLOAT: return String.valueOf(reader.nextFloat());
            case DOUBLE: return String.valueOf(reader.nextDouble());
            default: throw new IllegalStateException("STRING tag expected but got " + type + ". At: " + reader.path());
        }
    }

}
