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
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class MapAdapterFactory implements TypeAdapterFactory {

    public static final MapAdapterFactory INSTANCE = new MapAdapterFactory();

    @Override
    public <T> Optional<TypeAdapter<T>> create(TypeToken<T> typeToken, BlueNBT blueNBT) {
        Type type = typeToken.getType();

        Class<? super T> rawType = typeToken.getRawType();
        if (!Map.class.isAssignableFrom(rawType)) {
            return Optional.empty();
        }

        Type[] keyAndValueTypes = TypeUtil.getMapKeyAndValueTypes(type, rawType);

        Function<?, String> toStringFunction;
        Function<String, ?> fromStringFunction;
        Class<?> keyType = TypeUtil.getRawType(keyAndValueTypes[0]);
        if (String.class.equals(keyType)) {
            toStringFunction = Function.identity();
            fromStringFunction = Function.identity();
        } else if (Enum.class.isAssignableFrom(keyType)) {
            toStringFunction = (Function<Enum<?>, String>) Enum::name;
            //noinspection unchecked, rawtypes
            fromStringFunction = (Function<String, Enum>) name -> Enum.valueOf((Class<? extends Enum>) keyAndValueTypes[0], name);
        } else {
            // key-type not supported
            return Optional.empty();
        }

        TypeToken<?> valueType = TypeToken.get(keyAndValueTypes[1]);
        TypeSerializer<?> elementTypeSerializer = blueNBT.getTypeSerializer(valueType);
        TypeDeserializer<?> elementTypeDeserializer = blueNBT.getTypeDeserializer(valueType);

        ObjectConstructor<T> constructor = blueNBT.createObjectConstructor(typeToken);

        @SuppressWarnings({"unchecked", "rawtypes"})
        TypeAdapter<T> result = new MapAdapter(
                toStringFunction,
                fromStringFunction,
                elementTypeSerializer,
                elementTypeDeserializer,
                constructor
        );
        return Optional.of(result);
    }

    @RequiredArgsConstructor
    static class MapAdapter<K, E> implements TypeAdapter<Map<K, E>>  {

        private final Function<K, String> toStringFunction;
        private final Function<String, K> fromStringFunction;

        private final TypeSerializer<E> typeSerializer;
        private final TypeDeserializer<E> typeDeserializer;

        private final ObjectConstructor<? extends Map<K, E>> constructor;

        @Override
        public Map<K, E> read(NBTReader reader) throws IOException {
            Map<K, E> map = constructor.construct();
            reader.beginCompound();
            while (reader.hasNext()) {
                String keyString = reader.name();
                K key = fromStringFunction.apply(keyString);
                E instance = typeDeserializer.read(reader);
                map.put(key, instance);
            }
            reader.endCompound();
            return map;
        }

        @Override
        public void write(Map<K, E> value, NBTWriter writer) throws IOException {
            writer.beginCompound();
            for (Map.Entry<K, E> entry : value.entrySet()) {
                String keyString = toStringFunction.apply(entry.getKey());
                writer.name(keyString);
                typeSerializer.write(entry.getValue(), writer);
            }
            writer.endCompound();
        }

    }

}
