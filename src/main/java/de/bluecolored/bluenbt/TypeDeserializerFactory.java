package de.bluecolored.bluenbt;

import com.google.gson.reflect.TypeToken;

import java.util.Optional;

@FunctionalInterface
public interface TypeDeserializerFactory {

    <T> Optional<TypeDeserializer<T>> create(TypeToken<T> type, BlueNBT blueNBT);

}
