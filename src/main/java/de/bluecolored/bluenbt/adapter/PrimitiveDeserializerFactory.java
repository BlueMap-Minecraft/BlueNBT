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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link TypeDeserializerFactory} creating {@link TypeDeserializer}s for any primitive type and {@link String}s
 */
public class PrimitiveDeserializerFactory implements TypeDeserializerFactory {

    public static final PrimitiveDeserializerFactory INSTANCE = new PrimitiveDeserializerFactory();

    private static final Map<Type, TypeDeserializer<?>> TYPE_DESERIALIZER_MAP = new HashMap<>();
    static {
        registerTypeDeserializer(boolean.class, Boolean.class, PrimitiveDeserializerFactory::readBool);
        registerTypeDeserializer(byte.class, Byte.class, PrimitiveDeserializerFactory::readByte);
        registerTypeDeserializer(short.class, Short.class, PrimitiveDeserializerFactory::readShort);
        registerTypeDeserializer(char.class, Character.class, PrimitiveDeserializerFactory::readChar);
        registerTypeDeserializer(int.class, Integer.class, PrimitiveDeserializerFactory::readInt);
        registerTypeDeserializer(long.class, Long.class, PrimitiveDeserializerFactory::readLong);
        registerTypeDeserializer(float.class, Float.class, PrimitiveDeserializerFactory::readFloat);
        registerTypeDeserializer(double.class, Double.class, PrimitiveDeserializerFactory::readDouble);
        registerTypeDeserializer(String.class, null, PrimitiveDeserializerFactory::readString);
    }

    private static void registerTypeDeserializer(Type primitiveType, Type boxedType, TypeDeserializer<?> typeDeserializer) {
        TYPE_DESERIALIZER_MAP.put(primitiveType, typeDeserializer);
        if (boxedType != null) TYPE_DESERIALIZER_MAP.put(boxedType, typeDeserializer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<TypeDeserializer<T>> create(TypeToken<T> typeToken, BlueNBT blueNBT) {
        TypeDeserializer<?> typeDeserializer = TYPE_DESERIALIZER_MAP.get(typeToken.getRawType());
        return Optional.ofNullable((TypeDeserializer<T>) typeDeserializer);
    }

    public static boolean readBool(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        return switch (type) {
            case BYTE -> reader.nextByte() != 0;
            case SHORT -> reader.nextShort() != 0;
            case INT -> reader.nextInt() != 0;
            case LONG -> reader.nextLong() != 0L;
            case FLOAT -> reader.nextFloat() != 0f;
            case DOUBLE -> reader.nextDouble() != 0d;
            case STRING -> Boolean.parseBoolean(reader.nextString());
            default -> throw new IllegalStateException("STRING or any number tag expected but got " + type +
                    ". At: " + reader.path());
        };
    }

    public static byte readByte(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        return switch (type) {
            case BYTE -> reader.nextByte();
            case SHORT -> (byte) reader.nextShort();
            case INT -> (byte) reader.nextInt();
            case LONG -> (byte) reader.nextLong();
            case FLOAT -> (byte) reader.nextFloat();
            case DOUBLE -> (byte) reader.nextDouble();
            case STRING -> Byte.parseByte(reader.nextString());
            default -> throw new IllegalStateException("BYTE tag expected but got " + type + ". At: " + reader.path());
        };
    }

    public static short readShort(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        return switch (type) {
            case BYTE -> reader.nextByte();
            case SHORT -> reader.nextShort();
            case INT -> (short) reader.nextInt();
            case LONG -> (short) reader.nextLong();
            case FLOAT -> (short) reader.nextFloat();
            case DOUBLE -> (short) reader.nextDouble();
            case STRING -> Short.parseShort(reader.nextString());
            default -> throw new IllegalStateException("SHORT tag expected but got " + type + ". At: " + reader.path());
        };
    }

    public static char readChar(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        return switch (type) {
            case BYTE -> (char) reader.nextByte();
            case SHORT -> (char) reader.nextShort();
            case INT -> (char) reader.nextInt();
            case LONG -> (char) reader.nextLong();
            case FLOAT -> (char) reader.nextFloat();
            case DOUBLE -> (char) reader.nextDouble();
            case STRING -> {
                String string = reader.nextString();
                if (string.length() != 1)
                    throw new IllegalStateException("Can not parse char from string '" + string + "'");
                yield string.charAt(0);
            }
            default -> throw new IllegalStateException("Any number tag expected but got " + type + ". At: " + reader.path());
        };
    }

    public static int readInt(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        return switch (type) {
            case BYTE -> reader.nextByte();
            case SHORT -> reader.nextShort();
            case INT -> reader.nextInt();
            case LONG -> (int) reader.nextLong();
            case FLOAT -> (int) reader.nextFloat();
            case DOUBLE -> (int) reader.nextDouble();
            case STRING -> Integer.parseInt(reader.nextString());
            default -> throw new IllegalStateException("INT tag expected but got " + type + ". At: " + reader.path());
        };
    }

    public static long readLong(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        return switch (type) {
            case LONG -> reader.nextLong();
            case BYTE -> reader.nextByte();
            case SHORT -> reader.nextShort();
            case INT -> reader.nextInt();
            case FLOAT -> (long) reader.nextFloat();
            case DOUBLE -> (long) reader.nextDouble();
            case STRING -> Long.parseLong(reader.nextString());
            default -> throw new IllegalStateException("LONG tag expected but got " + type + ". At: " + reader.path());
        };
    }

    public static float readFloat(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        return switch (type) {
            case FLOAT -> reader.nextFloat();
            case BYTE -> reader.nextByte();
            case SHORT -> reader.nextShort();
            case INT -> reader.nextInt();
            case LONG -> reader.nextLong();
            case DOUBLE -> (float) reader.nextDouble();
            case STRING -> Float.parseFloat(reader.nextString());
            default -> throw new IllegalStateException("FLOAT tag expected but got " + type + ". At: " + reader.path());
        };
    }

    public static double readDouble(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        return switch (type) {
            case DOUBLE -> reader.nextDouble();
            case BYTE -> reader.nextByte();
            case SHORT -> reader.nextShort();
            case INT -> reader.nextInt();
            case LONG -> reader.nextLong();
            case FLOAT -> reader.nextFloat();
            case STRING -> Double.parseDouble(reader.nextString());
            default -> throw new IllegalStateException("DOUBLE tag expected but got " + type + ". At: " + reader.path());
        };
    }

    public static String readString(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        return switch (type) {
            case STRING -> reader.nextString();
            case BYTE -> String.valueOf(reader.nextByte());
            case SHORT -> String.valueOf(reader.nextShort());
            case INT -> String.valueOf(reader.nextInt());
            case LONG -> String.valueOf(reader.nextLong());
            case FLOAT -> String.valueOf(reader.nextFloat());
            case DOUBLE -> String.valueOf(reader.nextDouble());
            default -> throw new IllegalStateException("STRING tag expected but got " + type + ". At: " + reader.path());
        };
    }

}
