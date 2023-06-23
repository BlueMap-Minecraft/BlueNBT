package de.bluecolored.bluenbt;

import java.util.Optional;

@FunctionalInterface
public interface TypeDeserializerFactory {

    <T> Optional<TypeDeserializer<?>> create(Class<T> type);

}
