package de.bluecolored.bluenbt;

public interface TypeResolver<T, B> {

    TypeToken<B> getBaseType();

    TypeToken<? extends T> resolve(B base);

    Iterable<TypeToken<? extends T>> getPossibleTypes();

}
