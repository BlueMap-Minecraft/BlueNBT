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

    private static Boolean readBool(NBTReader reader) throws IOException {
        if (reader.peek() == TagType.STRING)
            return Boolean.valueOf(reader.nextString());
        return readNumber(reader).byteValue() > 0;
    }

    private static Byte readByte(NBTReader reader) throws IOException {
        return readNumber(reader).byteValue();
    }

    private static Short readShort(NBTReader reader) throws IOException {
        return readNumber(reader).shortValue();
    }

    private static Character readChar(NBTReader reader) throws IOException {
        return (char) readNumber(reader).shortValue();
    }

    private static Integer readInt(NBTReader reader) throws IOException {
        return readNumber(reader).intValue();
    }

    private static Long readLong(NBTReader reader) throws IOException {
        return readNumber(reader).longValue();
    }

    private static Float readFloat(NBTReader reader) throws IOException {
        return readNumber(reader).floatValue();
    }

    private static Double readDouble(NBTReader reader) throws IOException {
        return readNumber(reader).doubleValue();
    }

    private static String readString(NBTReader reader) throws IOException {
        return reader.nextString();
    }

    private static Number readNumber(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        switch (type) {
            case BYTE: return reader.nextByte();
            case SHORT: return reader.nextShort();
            case INT: return reader.nextInt();
            case LONG: return reader.nextLong();
            case FLOAT: return reader.nextFloat();
            case DOUBLE: return reader.nextDouble();
            default: throw new IllegalStateException("Any number tag expected but got " + type + ". At: " + reader.path());
        }
    }

}
