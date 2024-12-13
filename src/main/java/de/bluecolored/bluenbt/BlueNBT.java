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
package de.bluecolored.bluenbt;

import de.bluecolored.bluenbt.adapter.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The heart of BlueNBT.
 * <p>Use this class to register your {@link TypeSerializer}s, {@link TypeDeserializer}s and {@link InstanceCreator}s and
 * (de)serialize any object to/from NBT.</p>
 */
public class BlueNBT {

    @SuppressWarnings("rawtypes")
    private static final TypeResolver NO_TYPE_RESOLVER = new TypeResolver() {
        @Override public TypeToken getBaseType() { throw new UnsupportedOperationException(); }
        @Override public TypeToken resolve(Object base) { throw new UnsupportedOperationException(); }
        @Override public Iterable getPossibleTypes() { throw new UnsupportedOperationException(); }
    };

    private final List<TypeSerializerFactory> serializerFactories = new ArrayList<>();
    private final List<TypeDeserializerFactory> deserializerFactories = new ArrayList<>();
    private final List<InstanceCreatorFactory> instanceCreatorFactories = new ArrayList<>();
    private final List<TypeResolverFactory> typeResolverFactories = new ArrayList<>();
    private final Map<TypeToken<?>, TypeSerializer<?>> typeSerializerMap = new ConcurrentHashMap<>();
    private final Map<TypeToken<?>, TypeDeserializer<?>> typeDeserializerMap = new ConcurrentHashMap<>();
    private final Map<TypeToken<?>, InstanceCreator<?>> instanceCreatorMap = new ConcurrentHashMap<>();
    private final Map<TypeToken<?>, TypeResolver<?, ?>> typeResolverMap = new ConcurrentHashMap<>();

    /**
     * The {@link NamingStrategy} this BlueNBT instance uses to determine the NBT-name
     * from a given {@link java.lang.reflect.Field}.
     * <p>Defaults to {@link NamingStrategy#FIELD_NAME}</p>
     */
    @Getter @Setter
    private NamingStrategy namingStrategy = NamingStrategy.FIELD_NAME;

    /**
     * Creates a fresh BlueNBT instance with the default configuration and default (de)serializers.
     */
    public BlueNBT() {
        register(TypeToken.of(Object.class), ObjectDeserializer.INSTANCE);
        register(ArrayAdapterFactory.INSTANCE);
        register(PrimitiveSerializerFactory.INSTANCE);
        register(PrimitiveDeserializerFactory.INSTANCE);
        register(CollectionAdapterFactory.INSTANCE);
        register(MapAdapterFactory.INSTANCE);
    }

    /**
     * Registers a {@link TypeAdapterFactory} for this BlueNBT instance to use for (de)serialization.
     */
    public synchronized void register(TypeAdapterFactory typeAdapterFactory) {
        serializerFactories.add(typeAdapterFactory);
        deserializerFactories.add(typeAdapterFactory);
        typeSerializerMap.clear();
    }

    /**
     * Registers a {@link TypeSerializerFactory} for this BlueNBT instance to use for serialization.
     */
    public synchronized void register(TypeSerializerFactory typeSerializerFactory) {
        serializerFactories.add(typeSerializerFactory);
    }

    /**
     * Registers a {@link TypeDeserializerFactory} for this BlueNBT instance to use for deserialization.
     */
    public synchronized void register(TypeDeserializerFactory typeDeserializerFactory) {
        deserializerFactories.add(typeDeserializerFactory);
    }

    /**
     * Registers a {@link InstanceCreatorFactory} for this BlueNBT instance to use for instance-creation.
     */
    public synchronized void register(InstanceCreatorFactory instanceCreatorFactory) {
        instanceCreatorFactories.add(instanceCreatorFactory);
    }

    /**
     * Registers a {@link TypeResolverFactory} for this BlueNBT instance to use for resolving types.
     */
    public synchronized void register(TypeResolverFactory typeResolverFactory) {
        typeResolverFactories.add(typeResolverFactory);
    }

    /**
     * Registers a {@link TypeAdapter} for this BlueNBT instance to use for (de)serialization of the specified type.
     */
    public <T> void register(TypeToken<T> type, TypeAdapter<T> typeAdapter) {
        register(new TypeAdapterFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <U> Optional<TypeAdapter<U>> create(TypeToken<U> createType, BlueNBT blueNBT) {
                if (createType.equals(type))
                    return Optional.of((TypeAdapter<U>) typeAdapter);
                return Optional.empty();
            }
        });
    }

    /**
     * Registers a {@link TypeSerializer} for this BlueNBT instance to use for serialization of the specified type.
     */
    public <T> void register(TypeToken<T> type, TypeSerializer<T> typeSerializer) {
        register(new TypeSerializerFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <U> Optional<TypeSerializer<U>> create(TypeToken<U> createType, BlueNBT blueNBT) {
                if (createType.equals(type))
                    return Optional.of((TypeSerializer<U>) typeSerializer);
                return Optional.empty();
            }
        });
    }

    /**
     * Registers a {@link TypeDeserializer} for this BlueNBT instance to use for deserialization of the specified type.
     */
    public <T> void register(TypeToken<T> type, TypeDeserializer<T> typeDeserializer) {
        register(new TypeDeserializerFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <U> Optional<TypeDeserializer<U>> create(TypeToken<U> createType, BlueNBT blueNBT) {
                if (createType.equals(type))
                    return Optional.of((TypeDeserializer<U>) typeDeserializer);
                return Optional.empty();
            }
        });
    }

    /**
     * Registers a {@link InstanceCreator} for this BlueNBT instance to use for instance-creation of the specified type.
     */
    public <T> void register(TypeToken<T> type, InstanceCreator<T> instanceCreator) {
        register(new InstanceCreatorFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <U> Optional<? extends InstanceCreator<U>> create(TypeToken<U> createType, BlueNBT blueNBT) {
                if (createType.equals(type))
                    return Optional.of((InstanceCreator<U>) instanceCreator);
                return Optional.empty();
            }
        });
    }

    /**
     * Registers a {@link TypeResolver} for this BlueNBT instance to use for instance-creation of the specified type.
     */
    public <T> void register(TypeToken<T> type, TypeResolver<T, ?> typeResolver) {
        register(new TypeResolverFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <U> Optional<? extends TypeResolver<U, ?>> create(TypeToken<U> createType, BlueNBT blueNBT) {
                if (createType.equals(type))
                    return Optional.of((TypeResolver<U, ?>) typeResolver);
                return Optional.empty();
            }
        });
    }

    /**
     * Returns the {@link TypeSerializer} for the given type
     */
    @SuppressWarnings("unchecked")
    public <T> TypeSerializer<T> getTypeSerializer(TypeToken<T> type) {
        TypeSerializer<T> serializer = (TypeSerializer<T>) typeSerializerMap.get(type);
        if (serializer != null && (!(serializer instanceof FutureTypeSerializer))) return serializer;

        synchronized (this) {
            serializer = (TypeSerializer<T>) typeSerializerMap.get(type);
            if (serializer != null ) return serializer;

            FutureTypeSerializer<T> future = new FutureTypeSerializer<>();
            typeSerializerMap.put(type, future); // set future before creation of new serializers to avoid recursive creation

            for (int i = serializerFactories.size() - 1; i >= 0; i--) {
                TypeSerializerFactory factory = serializerFactories.get(i);
                serializer = factory.create(type, this).orElse(null);
                if (serializer != null) break;
            }

            if (serializer == null)
                serializer = DefaultSerializerFactory.INSTANCE.createFor(type, this);

            future.complete(serializer);
            typeSerializerMap.put(type, serializer);
        }

        return serializer;
    }

    /**
     * Returns the {@link TypeDeserializer} for the given type
     */
    @SuppressWarnings("unchecked")
    public <T> TypeDeserializer<T> getTypeDeserializer(TypeToken<T> type) {
        TypeDeserializer<T> deserializer = (TypeDeserializer<T>) typeDeserializerMap.get(type);
        if (deserializer != null && (!(deserializer instanceof FutureTypeDeserializer))) return deserializer;

        synchronized (this) {
            deserializer = (TypeDeserializer<T>) typeDeserializerMap.get(type);
            if (deserializer != null) return deserializer;

            FutureTypeDeserializer<T> future = new FutureTypeDeserializer<>();
            typeDeserializerMap.put(type, future); // set future before creation of new deserializers to avoid recursive creation

            for (int i = deserializerFactories.size() - 1; i >= 0; i--) {
                TypeDeserializerFactory factory = deserializerFactories.get(i);
                deserializer = factory.create(type, this).orElse(null);
                if (deserializer != null) break;
            }

            if (deserializer == null)
                deserializer = DefaultDeserializerFactory.INSTANCE.createFor(type, this);

            future.complete(deserializer);
            typeDeserializerMap.put(type, deserializer);
        }

        return deserializer;
    }

    /**
     * Returns the {@link InstanceCreator} for the given type
     */
    @SuppressWarnings("unchecked")
    public <T> InstanceCreator<T> getInstanceCreator(TypeToken<T> type) {
        InstanceCreator<T> instanceCreator = (InstanceCreator<T>) instanceCreatorMap.get(type);
        if (instanceCreator != null && (!(instanceCreator instanceof FutureInstanceCreator))) return instanceCreator;

        synchronized (this) {
            instanceCreator = (InstanceCreator<T>) instanceCreatorMap.get(type);
            if (instanceCreator != null) return instanceCreator;

            FutureInstanceCreator<T> future = new FutureInstanceCreator<>();
            instanceCreatorMap.put(type, future); // set future before creation of new deserializers to avoid recursive creation

            for (int i = instanceCreatorFactories.size() - 1; i >= 0; i--) {
                InstanceCreatorFactory factory = instanceCreatorFactories.get(i);
                instanceCreator = factory.create(type, this).orElse(null);
                if (instanceCreator != null) break;
            }

            if (instanceCreator == null)
                instanceCreator = DefaultInstanceCreatorFactory.INSTANCE.createFor(type, this);

            future.complete(instanceCreator);
            instanceCreatorMap.put(type, instanceCreator);
        }

        return instanceCreator;
    }

    /**
     * Returns the {@link InstanceCreator} for the given type or null if there is none
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> @Nullable TypeResolver<T, ?> getTypeResolver(TypeToken<T> type) {
        TypeResolver<T, ?> typeResolver = (TypeResolver<T, ?>) typeResolverMap.get(type);
        if (typeResolver != null && (!(typeResolver instanceof FutureInstanceCreator))) return typeResolver;

        synchronized (this) {
            typeResolver = (TypeResolver<T, ?>) typeResolverMap.get(type);
            if (typeResolver != null) return typeResolver;

            FutureTypeResolver<T, ?> future = new FutureTypeResolver<>();
            typeResolverMap.put(type, future); // set future before creation of new deserializers to avoid recursive creation

            for (int i = typeResolverFactories.size() - 1; i >= 0; i--) {
                TypeResolverFactory factory = typeResolverFactories.get(i);
                typeResolver = factory.create(type, this).orElse(null);
                if (typeResolver != null) break;
            }

            if (typeResolver == null)
                typeResolver = NO_TYPE_RESOLVER;

            future.complete((TypeResolver) typeResolver);
            typeResolverMap.put(type, typeResolver);
        }

        return typeResolver == NO_TYPE_RESOLVER ? null : typeResolver;
    }

    /**
     * Serializes an object to NBT using the specified type for serialization, and writes it to the given
     * {@link OutputStream}
     * @param object The Object that should be serialized
     * @param out The {@link OutputStream} to write the data to
     * @param type The type that should be used during serialization
     * @throws IOException If an {@link IOException} occurred
     */
    public <T> void write(T object, OutputStream out, TypeToken<T> type) throws IOException {
        write(object, new NBTWriter(out), type);
    }

    /**
     * Serializes an object to NBT using the specified type for serialization, and writes it to the given
     * {@link NBTWriter}
     * @param object The Object that should be serialized
     * @param out The {@link NBTWriter} to write the data to
     * @param type The type that should be used during serialization
     * @throws IOException If an {@link IOException} occurred
     */
    public <T> void write(T object, NBTWriter out, TypeToken<T> type) throws IOException {
        getTypeSerializer(type).write(object, out);
    }

    /**
     * Serializes an object to NBT using the specified {@link Class} for serialization, and writes it to the given
     * {@link OutputStream}
     * @param object The Object that should be serialized
     * @param out The {@link OutputStream} to write the data to
     * @param type The {@link Class} that should be used during serialization
     * @throws IOException If an {@link IOException} occurred
     */
    public <T> void write(T object, OutputStream out, Class<T> type) throws IOException {
        write(object, out, TypeToken.of(type));
    }

    /**
     * Serializes an object to NBT using the specified {@link Class} for serialization, and writes it to the given
     * {@link NBTWriter}
     * @param object The Object that should be serialized
     * @param out The {@link NBTWriter} to write the data to
     * @param type The {@link Class} that should be used during serialization
     * @throws IOException If an {@link IOException} occurred
     */
    public <T> void write(T object, NBTWriter out, Class<T> type) throws IOException {
        write(object, out, TypeToken.of(type));
    }

    /**
     * Serializes an object to NBT and writes it to the given {@link OutputStream}
     * @param object The Object that should be serialized
     * @param out The {@link OutputStream} to write the data to
     * @throws IOException If an {@link IOException} occurred
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void write(Object object, OutputStream out) throws IOException {
        write(object, out, (Class) object.getClass());
    }

    /**
     * Serializes an object to NBT and writes it to the given {@link NBTWriter}
     * @param object The Object that should be serialized
     * @param out The {@link NBTWriter} to write the data to
     * @throws IOException If an {@link IOException} occurred
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void write(Object object, NBTWriter out) throws IOException {
        write(object, out, (Class) object.getClass());
    }

    /**
     * Reads an object from the given {@link InputStream} and deserializes it from NBT to the given type.
     * @param in The {@link InputStream} to read from
     * @param type The type of the object
     * @return The deserialized object
     * @throws IOException If an {@link IOException} occurred
     */
    public <T> T read(InputStream in, TypeToken<T> type) throws IOException {
        return read(new NBTReader(in), type);
    }

    /**
     * Reads an object from the given {@link NBTReader} and deserializes it from NBT to the given type.
     * @param in The {@link NBTReader} to read from
     * @param type The type of the object
     * @return The deserialized object
     * @throws IOException If an {@link IOException} occurred
     */
    public <T> T read(NBTReader in, TypeToken<T> type) throws IOException {
        return getTypeDeserializer(type).read(in);
    }

    /**
     * Reads an object from the given {@link InputStream} and deserializes it from NBT to the given type.
     * @param in The {@link InputStream} to read from
     * @param type The type of the object
     * @return The deserialized object
     * @throws IOException If an {@link IOException} occurred
     */
    public <T> T read(InputStream in, Class<T> type) throws IOException {
        return read(in, TypeToken.of(type));
    }

    /**
     * Reads an object from the given {@link NBTReader} and deserializes it from NBT to the given type.
     * @param in The {@link NBTReader} to read from
     * @param type The type of the object
     * @return The deserialized object
     * @throws IOException If an {@link IOException} occurred
     */
    public <T> T read(NBTReader in, Class<T> type) throws IOException {
        return read(in, TypeToken.of(type));
    }

    /**
     * Reads an object from the given {@link InputStream} and deserializes it from NBT to the given type.
     * @param in The {@link InputStream} to read from
     * @param type The type of the object
     * @return The deserialized object
     * @throws IOException If an {@link IOException} occurred
     */
    public Object read(InputStream in, Type type) throws IOException {
        return read(in, TypeToken.of(type));
    }

    /**
     * Reads an object from the given {@link NBTReader} and deserializes it from NBT to the given type.
     * @param in The {@link NBTReader} to read from
     * @param type The type of the object
     * @return The deserialized object
     * @throws IOException If an {@link IOException} occurred
     */
    public Object read(NBTReader in, Type type) throws IOException {
        return read(in, TypeToken.of(type));
    }

    private static class FutureTypeSerializer<T> implements TypeSerializer<T> {

        private TypeSerializer<T> value;

        public void complete(TypeSerializer<T> value) {
            if (this.value != null) throw new IllegalStateException("FutureTypeSerializer already completed!");
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public void write(T value, NBTWriter writer) throws IOException {
            if (this.value == null) throw new IllegalStateException("FutureTypeSerializer not completed!");
            this.value.write(value, writer);
        }

        @Override
        public TagType type() {
            if (this.value == null) throw new IllegalStateException("FutureTypeSerializer is not ready!");
            return this.value.type();
        }

    }

    private static class FutureTypeDeserializer<T> implements TypeDeserializer<T> {

        private TypeDeserializer<T> value;

        public void complete(TypeDeserializer<T> value) {
            if (this.value != null) throw new IllegalStateException("FutureTypeDeserializer already completed!");
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public T read(NBTReader reader) throws IOException {
            if (this.value == null) throw new IllegalStateException("FutureTypeDeserializer is not ready!");
            return this.value.read(reader);
        }

    }

    private static class FutureInstanceCreator<T> implements InstanceCreator<T> {

        private InstanceCreator<T> value;

        public void complete(InstanceCreator<T> value) {
            if (this.value != null) throw new IllegalStateException("FutureInstanceCreator already completed!");
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public T create() {
            if (this.value == null) throw new IllegalStateException("FutureInstanceCreator is not ready!");
            return this.value.create();
        }

    }

    private static class FutureTypeResolver<T, B> implements TypeResolver<T, B> {

        private TypeResolver<T, B> value;

        public void complete(TypeResolver<T, B> value) {
            if (this.value != null) throw new IllegalStateException("FutureTypeResolver already completed!");
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public TypeToken<B> getBaseType() {
            if (this.value == null) throw new IllegalStateException("FutureTypeResolver is not ready!");
            return this.value.getBaseType();
        }

        @Override
        public TypeToken<? extends T> resolve(B base) {
            if (this.value == null) throw new IllegalStateException("FutureTypeResolver is not ready!");
            return this.value.resolve(base);
        }

        @Override
        public Iterable<TypeToken<? extends T>> getPossibleTypes() {
            if (this.value == null) throw new IllegalStateException("FutureTypeResolver is not ready!");
            return this.value.getPossibleTypes();
        }

    }

}
