package de.bluecolored.bluenbt.adapter;

import de.bluecolored.bluenbt.*;

import java.io.IOException;
import java.util.Optional;

public class PrimitiveAdapterFactory implements TypeDeserializerFactory {

    public static final PrimitiveAdapterFactory INSTANCE = new PrimitiveAdapterFactory();

    @Override
    public <T> Optional<TypeDeserializer<?>> create(Class<T> type) {

        if (type.equals(boolean.class) || type.equals(Boolean.class))
            return Optional.of((TypeDeserializer<Boolean>) this::readBool);

        if (type.equals(byte.class) || type.equals(Byte.class))
            return Optional.of((TypeDeserializer<Byte>) this::readByte);

        if (type.equals(short.class) || type.equals(Short.class))
            return Optional.of((TypeDeserializer<Short>) this::readShort);

        if (type.equals(char.class) || type.equals(Character.class))
            return Optional.of((TypeDeserializer<Character>) this::readChar);

        if (type.equals(int.class) || type.equals(Integer.class))
            return Optional.of((TypeDeserializer<Integer>) this::readInt);

        if (type.equals(long.class) || type.equals(Long.class))
            return Optional.of((TypeDeserializer<Long>) this::readLong);

        if (type.equals(float.class) || type.equals(Float.class))
            return Optional.of((TypeDeserializer<Float>) this::readFloat);

        if (type.equals(double.class) || type.equals(Double.class))
            return Optional.of((TypeDeserializer<Double>) this::readDouble);

        if (type.equals(byte[].class))
            return Optional.of(this::readByteArray);

        if (type.equals(int[].class))
            return Optional.of(this::readIntArray);

        if (type.equals(long[].class))
            return Optional.of(this::readLongArray);

        if (type.equals(String.class))
            return Optional.of(this::readString);

        return Optional.empty();
    }

    private Boolean readBool(NBTReader reader, BlueNBT blueNBT) throws IOException {
        return readNumber(reader, blueNBT).byteValue() > 0;
    }

    private Byte readByte(NBTReader reader, BlueNBT blueNBT) throws IOException {
        return readNumber(reader, blueNBT).byteValue();
    }

    private Short readShort(NBTReader reader, BlueNBT blueNBT) throws IOException {
        return readNumber(reader, blueNBT).shortValue();
    }

    private Character readChar(NBTReader reader, BlueNBT blueNBT) throws IOException {
        return (char) readNumber(reader, blueNBT).shortValue();
    }

    private Integer readInt(NBTReader reader, BlueNBT blueNBT) throws IOException {
        return readNumber(reader, blueNBT).intValue();
    }

    private Long readLong(NBTReader reader, BlueNBT blueNBT) throws IOException {
        return readNumber(reader, blueNBT).longValue();
    }

    private Float readFloat(NBTReader reader, BlueNBT blueNBT) throws IOException {
        return readNumber(reader, blueNBT).floatValue();
    }

    private Double readDouble(NBTReader reader, BlueNBT blueNBT) throws IOException {
        return readNumber(reader, blueNBT).doubleValue();
    }

    private byte[] readByteArray(NBTReader reader, BlueNBT blueNBT) throws IOException {
        return reader.nextByteArray();
    }

    private int[] readIntArray(NBTReader reader, BlueNBT blueNBT) throws IOException {
        return reader.nextIntArray();
    }

    private long[] readLongArray(NBTReader reader, BlueNBT blueNBT) throws IOException {
        return reader.nextLongArray();
    }

    private String readString(NBTReader reader, BlueNBT blueNBT) throws IOException {
        return reader.nextString();
    }

    private Number readNumber(NBTReader reader, BlueNBT blueNBT) throws IOException {
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
