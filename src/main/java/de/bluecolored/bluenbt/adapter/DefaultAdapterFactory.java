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

import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluenbt.*;
import lombok.Data;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultAdapterFactory implements TypeDeserializerFactory {

    public static final DefaultAdapterFactory INSTANCE = new DefaultAdapterFactory();

    @Override
    public <T> Optional<TypeDeserializer<T>> create(TypeToken<T> type, BlueNBT blueNBT) {
        return Optional.of(createFor(type, blueNBT));
    }

    public <T> TypeDeserializer<T> createFor(TypeToken<T> type, BlueNBT blueNBT) {
        return new DefaultAdapter<>(type, blueNBT.getConstructorConstructor().get(type), blueNBT);
    }

    static class DefaultAdapter<T> implements TypeDeserializer<T>  {

        private final TypeToken<T> type;
        private final ObjectConstructor<T> constructor;
        private final BlueNBT blueNBT;

        private final Map<String, FieldInfo> fields = new HashMap<>();

        public DefaultAdapter(TypeToken<T> type, ObjectConstructor<T> constructor, BlueNBT blueNBT) {
            this.type = type;
            this.constructor = constructor;
            this.blueNBT = blueNBT;

            Map<Class<? extends TypeDeserializer<?>>, TypeDeserializer<?>> typeDeserializerCache =
                    new HashMap<>();

            TypeToken<?> typeToken = type;
            Class<?> raw = typeToken.getRawType();
            while (raw != Object.class) {
                for (Field field : raw.getDeclaredFields()) {
                    field.setAccessible(true);

                    String name = field.getName();
                    NBTName nbtName = field.getAnnotation(NBTName.class);
                    if (nbtName != null) name = nbtName.value();

                    TypeToken<?> fieldType = TypeToken.get($Gson$Types.resolve(typeToken.getType(), raw, field.getGenericType()));
                    NBTDeserializer deserializerType = field.getAnnotation(NBTDeserializer.class);
                    if (deserializerType == null) {
                        deserializerType = fieldType.getRawType().getAnnotation(NBTDeserializer.class);
                    }

                    TypeDeserializer<?> typeDeserializer;
                    if (deserializerType != null) {
                        typeDeserializer = typeDeserializerCache.computeIfAbsent(deserializerType.value(), t -> {
                            try {
                                return t.getDeclaredConstructor().newInstance();
                            } catch (Exception ex) {
                                throw new RuntimeException("Failed to create Instance of TypeDeserializer!", ex);
                            }
                        });
                    } else {
                        typeDeserializer = reader -> blueNBT.read(reader, fieldType);
                    }

                    fields.put(name, new FieldInfo(field, typeDeserializer));
                }

                typeToken = TypeToken.get($Gson$Types.resolve(typeToken.getType(), raw, raw.getGenericSuperclass()));
                raw = typeToken.getRawType();
            }
        }


        @Override
        public T read(NBTReader reader) throws IOException {

            try {
                T object = constructor.construct();
                reader.beginCompound();

                while (reader.peek() != TagType.END) {
                    String name = blueNBT.getFieldNameTransformer().apply(reader.name());
                    FieldInfo fieldInfo = fields.get(name);
                    if (fieldInfo != null) {
                        fieldInfo.getField().set(object, fieldInfo.getTypeDeserializer().read(reader));
                    } else {
                        reader.skip();
                    }
                }

                reader.endCompound();
                return object;
            } catch (IllegalAccessException ex) {
                throw new IOException("Failed to create instance of type '" + type + "'!", ex);
            }
        }

    }

    @Data
    private static class FieldInfo {
        private final Field field;
        private final TypeDeserializer<?> typeDeserializer;
    }

}
