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
import java.util.Map;
import java.util.Optional;

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

        // only String keys are supported
        if (!String.class.equals(keyAndValueTypes[0])) return Optional.empty();

        TypeToken<?> valueType = TypeToken.get(keyAndValueTypes[1]);
        TypeSerializer<?> elementTypeSerializer = blueNBT.getTypeSerializer(valueType);
        TypeDeserializer<?> elementTypeDeserializer = blueNBT.getTypeDeserializer(valueType);
        ObjectConstructor<T> constructor = blueNBT.createObjectConstructor(typeToken);

        @SuppressWarnings({"unchecked", "rawtypes"})
        TypeAdapter<T> result = new MapAdapter(elementTypeSerializer, elementTypeDeserializer, constructor);
        return Optional.of(result);
    }

    @RequiredArgsConstructor
    static class MapAdapter<E> implements TypeAdapter<Map<String, E>>  {

        private final TypeSerializer<E> typeSerializer;
        private final TypeDeserializer<E> typeDeserializer;
        private final ObjectConstructor<? extends Map<String, E>> constructor;

        @Override
        public Map<String, E> read(NBTReader reader) throws IOException {
            Map<String, E> map = constructor.construct();
            reader.beginCompound();
            while (reader.hasNext()) {
                String key = reader.name();
                E instance = typeDeserializer.read(reader);
                map.put(key, instance);
            }
            reader.endCompound();
            return map;
        }

        @Override
        public void write(Map<String, E> value, NBTWriter writer) throws IOException {
            writer.beginCompound();
            for (Map.Entry<String, E> entry : value.entrySet()) {
                writer.name(entry.getKey());
                typeSerializer.write(entry.getValue(), writer);
            }
            writer.endCompound();
        }

    }

}
