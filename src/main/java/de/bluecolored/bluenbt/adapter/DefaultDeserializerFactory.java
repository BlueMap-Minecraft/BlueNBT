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
import de.bluecolored.bluenbt.ObjectConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class DefaultDeserializerFactory implements TypeDeserializerFactory {

    public static final DefaultDeserializerFactory INSTANCE = new DefaultDeserializerFactory();

    @Override
    public <T> Optional<TypeDeserializer<T>> create(TypeToken<T> type, BlueNBT blueNBT) {
        return Optional.of(createFor(type, blueNBT));
    }

    public <T> TypeDeserializer<T> createFor(TypeToken<T> type, BlueNBT blueNBT) {
        return new DefaultAdapter<>(type, blueNBT.createObjectConstructor(type), blueNBT);
    }

    static class DefaultAdapter<T> implements TypeDeserializer<T>  {

        private static final Map<Type, Function<Field, FieldAccessor>> SPECIAL_ACCESSORS = Map.of(
                boolean.class, field -> (object, reader) -> field.setBoolean(object, PrimitiveDeserializerFactory.readBool(reader)),
                byte.class, field -> (object, reader) -> field.setByte(object, PrimitiveDeserializerFactory.readByte(reader)),
                short.class, field -> (object, reader) -> field.setShort(object, PrimitiveDeserializerFactory.readShort(reader)),
                char.class, field -> (object, reader) -> field.setChar(object, PrimitiveDeserializerFactory.readChar(reader)),
                int.class, field -> (object, reader) -> field.setInt(object, PrimitiveDeserializerFactory.readInt(reader)),
                long.class, field -> (object, reader) -> field.setLong(object, PrimitiveDeserializerFactory.readLong(reader)),
                float.class, field -> (object, reader) -> field.setFloat(object, PrimitiveDeserializerFactory.readFloat(reader)),
                double.class, field -> (object, reader) -> field.setDouble(object, PrimitiveDeserializerFactory.readDouble(reader))
        );

        private final TypeToken<T> type;
        private final ObjectConstructor<T> constructor;
        private final BlueNBT blueNBT;

        private final Map<String, FieldAccessor> fields = new HashMap<>();

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

                    String[] names = new String[]{ field.getName() };
                    NBTName nbtName = field.getAnnotation(NBTName.class);
                    if (nbtName != null) names = nbtName.value();

                    TypeToken<?> fieldType = TypeToken.get(TypeUtil.resolve(typeToken.getType(), raw, field.getGenericType()));

                    TypeDeserializer<?> typeDeserializer;
                    Class<? extends TypeDeserializer<?>> deserializerType = findDeserializerType(field, fieldType.getRawType());
                    if (deserializerType != null) {
                        typeDeserializer = typeDeserializerCache.computeIfAbsent(deserializerType, t -> {
                            try {
                                return t.getDeclaredConstructor().newInstance();
                            } catch (Exception ex) {
                                throw new RuntimeException("Failed to create Instance of TypeDeserializer!", ex);
                            }
                        });
                    } else if (SPECIAL_ACCESSORS.containsKey(fieldType.getType())) {
                        FieldAccessor accessor = SPECIAL_ACCESSORS.get(fieldType.getType()).apply(field);
                        for (String name : names)
                            fields.put(name, accessor);
                        continue;
                    } else {
                        typeDeserializer = reader -> blueNBT.read(reader, fieldType);
                    }

                    FieldAccessor accessor = new TypeDeserializerFieldAccessor(field, typeDeserializer);
                    for (String name : names)
                        fields.put(name, accessor);
                }

                typeToken = TypeToken.get(TypeUtil.resolve(typeToken.getType(), raw, raw.getGenericSuperclass()));
                raw = typeToken.getRawType();
            }
        }

        @Override
        public T read(NBTReader reader) throws IOException {

            try {
                T object = constructor.construct();
                reader.beginCompound();

                while (reader.peek() != TagType.END) {
                    String name = reader.name();
                    FieldAccessor fieldInfo = fields.get(name);

                    if (fieldInfo == null) {
                        name = blueNBT.getFieldNameTransformer().apply(name);
                        fieldInfo = fields.get(name);
                    }

                    if (fieldInfo != null) {
                        fieldInfo.read(object, reader);
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

        private @Nullable Class<? extends TypeDeserializer<?>> findDeserializerType(Field field, Class<?> type) {
            NBTDeserializer fieldDeserializer = field.getAnnotation(NBTDeserializer.class);
            if (fieldDeserializer != null) return fieldDeserializer.value();

            NBTAdapter fieldAdapter = field.getAnnotation(NBTAdapter.class);
            if (fieldAdapter != null) return fieldAdapter.value();

            NBTDeserializer typeDeserializer = type.getAnnotation(NBTDeserializer.class);
            if (typeDeserializer != null) return typeDeserializer.value();

            NBTAdapter typeAdapter = type.getAnnotation(NBTAdapter.class);
            if (typeAdapter != null) return typeAdapter.value();

            return null;
        }

    }

    private interface FieldAccessor {
        void read(Object object, NBTReader reader) throws IOException, IllegalAccessException;
    }

    @RequiredArgsConstructor
    private static class TypeDeserializerFieldAccessor implements FieldAccessor {
        private final Field field;
        private final TypeDeserializer<?> typeDeserializer;

        @Override
        public void read(Object object, NBTReader reader) throws IOException, IllegalAccessException {
            field.set(object, typeDeserializer.read(reader));
        }
    }

}
