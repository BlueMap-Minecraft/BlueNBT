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

import com.google.gson.InstanceCreator;
import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluenbt.adapter.*;
import de.bluecolored.bluenbt.internal.ConstructorConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.*;

public class BlueNBT {

    private final List<TypeSerializerFactory> serializerFactories = new ArrayList<>();
    private final List<TypeDeserializerFactory> deserializerFactories = new ArrayList<>();
    private final Map<TypeToken<?>, TypeSerializer<?>> typeSerializerMap = new HashMap<>();
    private final Map<TypeToken<?>, TypeDeserializer<?>> typeDeserializerMap = new HashMap<>();
    private final Map<Type, InstanceCreator<?>> instanceCreators = new HashMap<>();

    private final ConstructorConstructor constructorConstructor = new ConstructorConstructor(instanceCreators);

    @Getter @Setter
    private FieldNameTransformer fieldNameTransformer = s -> {
        if (s.isEmpty()) return s;
        char first = s.charAt(0);
        if (Character.isUpperCase(first))
            return Character.toLowerCase(first) + s.substring(1);
        return s;
    };

    public BlueNBT() {
        register(ObjectAdapterFactory.INSTANCE);
        register(ArrayAdapterFactory.INSTANCE);
        register(PrimitiveSerializerFactory.INSTANCE);
        register(PrimitiveDeserializerFactory.INSTANCE);
        register(CollectionAdapterFactory.INSTANCE);
        register(MapAdapterFactory.INSTANCE);
    }

    public void register(TypeAdapterFactory typeAdapterFactory) {
        serializerFactories.add(typeAdapterFactory);
        deserializerFactories.add(typeAdapterFactory);
    }

    public void register(TypeSerializerFactory typeSerializerFactory) {
        serializerFactories.add(typeSerializerFactory);
    }

    public void register(TypeDeserializerFactory typeDeserializerFactory) {
        deserializerFactories.add(typeDeserializerFactory);
    }

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

    public <T> void register(Type type, InstanceCreator<T> instanceCreator) {
        this.instanceCreators.put(type, instanceCreator);
    }

    public <T> TypeSerializer<T> getTypeSerializer(TypeToken<T> type) {
        @SuppressWarnings("unchecked")
        TypeSerializer<T> serializer = (TypeSerializer<T>) typeSerializerMap.get(type);

        if (serializer == null) {
            for (int i = serializerFactories.size() - 1; i >= 0; i--) {
                TypeSerializerFactory factory = serializerFactories.get(i);
                serializer = factory.create(type, this).orElse(null);
                if (serializer != null) break;
            }

            if (serializer == null)
                serializer = DefaultSerializerFactory.INSTANCE.createFor(type, this);

            typeSerializerMap.put(type, serializer);
        }

        return serializer;
    }

    public <T> TypeDeserializer<T> getTypeDeserializer(TypeToken<T> type) {
        @SuppressWarnings("unchecked")
        TypeDeserializer<T> deserializer = (TypeDeserializer<T>) typeDeserializerMap.get(type);

        if (deserializer == null) {
            for (int i = deserializerFactories.size() - 1; i >= 0; i--) {
                TypeDeserializerFactory factory = deserializerFactories.get(i);
                deserializer = factory.create(type, this).orElse(null);
                if (deserializer != null) break;
            }

            if (deserializer == null)
                deserializer = DefaultDeserializerFactory.INSTANCE.createFor(type, this);

            typeDeserializerMap.put(type, deserializer);
        }

        return deserializer;
    }

    public <T> void write(T object, OutputStream out, TypeToken<T> type) throws IOException {
        write(object, new NBTWriter(out), type);
    }

    public <T> void write(T object, NBTWriter out, TypeToken<T> type) throws IOException {
        getTypeSerializer(type).write(object, out);
    }

    public <T> void write(T object, OutputStream out, Class<T> type) throws IOException {
        write(object, out, TypeToken.get(type));
    }

    public <T> void write(T object, NBTWriter out, Class<T> type) throws IOException {
        write(object, out, TypeToken.get(type));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void write(Object object, OutputStream out) throws IOException {
        write(object, out, (Class) object.getClass());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void write(Object object, NBTWriter out) throws IOException {
        write(object, out, (Class) object.getClass());
    }

    public <T> T read(InputStream in, TypeToken<T> type) throws IOException {
        return read(new NBTReader(in), type);
    }

    public <T> T read(NBTReader in, TypeToken<T> type) throws IOException {
        return getTypeDeserializer(type).read(in);
    }

    public <T> T read(InputStream in, Class<T> type) throws IOException {
        return read(in, TypeToken.get(type));
    }

    public <T> T read(NBTReader in, Class<T> type) throws IOException {
        return read(in, TypeToken.get(type));
    }

    public Object read(InputStream in, Type type) throws IOException {
        return read(in, TypeToken.get(type));
    }

    public Object read(NBTReader in, Type type) throws IOException {
        return read(in, TypeToken.get(type));
    }

    public <T> ObjectConstructor<T> createObjectConstructor(TypeToken<T> type) {
        return constructorConstructor.get(type);
    }

}
