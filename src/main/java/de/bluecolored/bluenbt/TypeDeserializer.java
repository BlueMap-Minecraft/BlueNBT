package de.bluecolored.bluenbt;

import java.io.IOException;

@FunctionalInterface
public interface TypeDeserializer<T> {

    T read(NBTReader reader, BlueNBT blueNBT) throws IOException;

}
