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
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class DefaultSerializerFactory implements TypeSerializerFactory {

    public static final DefaultSerializerFactory INSTANCE = new DefaultSerializerFactory();

    @Override
    public <T> Optional<TypeSerializer<T>> create(TypeToken<T> type, BlueNBT blueNBT) {
        return Optional.of(createFor(type, blueNBT));
    }

    public <T> TypeSerializer<T> createFor(TypeToken<T> type, BlueNBT blueNBT) {
        return new DefaultAdapter<>(type, blueNBT);
    }

    static class DefaultAdapter<T> implements TypeSerializer<T>  {

        private static final Map<Type, Function<Field, SpecialFieldWriter>> SPECIAL_ACCESSORS = Map.of(
                boolean.class, field -> (object, writer) -> writer.value(field.getBoolean(object) ? (byte) 1 : (byte) 0),
                byte.class, field -> (object, writer) -> writer.value(field.getByte(object)),
                short.class, field -> (object, writer) -> writer.value(field.getShort(object)),
                char.class, field -> (object, writer) -> writer.value(field.getChar(object)),
                int.class, field -> (object, writer) -> writer.value(field.getInt(object)),
                long.class, field -> (object, writer) -> writer.value(field.getLong(object)),
                float.class, field -> (object, writer) -> writer.value(field.getFloat(object)),
                double.class, field -> (object, writer) -> writer.value(field.getDouble(object))
        );

        private final TypeToken<T> type;

        private final Map<String, FieldWriter> fields = new HashMap<>();

        public DefaultAdapter(TypeToken<T> type, BlueNBT blueNBT) {
            this.type = type;

            Map<Class<? extends TypeSerializer<?>>, TypeSerializer<?>> typeSerializerCache =
                    new HashMap<>();

            TypeToken<?> typeToken = type;
            Class<?> raw;
            while (typeToken != null && (raw = typeToken.getRawType()) != Object.class) {
                for (Field field : raw.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()))
                        continue;
                    field.setAccessible(true);

                    String[] names = new String[]{ field.getName() };
                    NBTName nbtName = field.getAnnotation(NBTName.class);
                    if (nbtName != null) names = nbtName.value();

                    TypeToken<?> fieldType = TypeToken.get(TypeUtil.resolve(typeToken.getType(), raw, field.getGenericType()));

                    TypeSerializer<?> typeSerializer;
                    Class<? extends TypeSerializer<?>> serializerType = findSerializerType(field, fieldType.getRawType());
                    if (serializerType != null) {
                        typeSerializer = typeSerializerCache.computeIfAbsent(serializerType, t -> {
                            try {
                                return t.getDeclaredConstructor().newInstance();
                            } catch (Exception ex) {
                                throw new RuntimeException("Failed to create Instance of TypeSerializer!", ex);
                            }
                        });
                    } else if (SPECIAL_ACCESSORS.containsKey(fieldType.getType())) {
                        FieldWriter accessor = SPECIAL_ACCESSORS.get(fieldType.getType()).apply(field);
                        for (String name : names)
                            fields.put(name, accessor);
                        continue;
                    } else {
                        typeSerializer = blueNBT.getTypeSerializer(fieldType);
                    }

                    FieldWriter accessor = new TypeSerializerFieldWriter<>(field, typeSerializer);
                    for (String name : names)
                        fields.put(name, accessor);
                }

                Type superType = TypeUtil.resolve(typeToken.getType(), raw, raw.getGenericSuperclass());
                typeToken = superType != null ? TypeToken.get(superType) : null;
            }
        }

        @Override
        public void write(T value, NBTWriter writer) throws IOException {
            try {
                writer.beginCompound();

                for (Map.Entry<String, FieldWriter> field : fields.entrySet()) {
                    field.getValue().write(field.getKey(), value, writer);
                }

                writer.endCompound();
            } catch (IllegalAccessException ex) {
                throw new IOException("Failed to create instance of type '" + type + "'!", ex);
            }
        }

        private @Nullable Class<? extends TypeSerializer<?>> findSerializerType(Field field, Class<?> type) {
            NBTSerializer fieldSerializer = field.getAnnotation(NBTSerializer.class);
            if (fieldSerializer != null) return fieldSerializer.value();

            NBTAdapter fieldAdapter = field.getAnnotation(NBTAdapter.class);
            if (fieldAdapter != null) return fieldAdapter.value();

            NBTSerializer typeSerializer = type.getAnnotation(NBTSerializer.class);
            if (typeSerializer != null) return typeSerializer.value();

            NBTAdapter typeAdapter = type.getAnnotation(NBTAdapter.class);
            if (typeAdapter != null) return typeAdapter.value();

            return null;
        }

    }

    private interface FieldWriter {
        void write(String name, Object object, NBTWriter writer) throws IOException, IllegalAccessException;
    }

    private interface SpecialFieldWriter extends FieldWriter {

        @Override
        default void write(String name, Object object, NBTWriter writer) throws IOException, IllegalAccessException {
            writer.name(name);
            writeValue(object, writer);
        }

        void writeValue(Object object, NBTWriter writer) throws IOException, IllegalAccessException;

    }

    @RequiredArgsConstructor
    private static class TypeSerializerFieldWriter<T> implements FieldWriter {
        private final Field field;
        private final TypeSerializer<T> typeSerializer;

        @Override
        @SuppressWarnings("unchecked")
        public void write(String name, Object object, NBTWriter writer) throws IOException, IllegalAccessException {
            T value = (T) field.get(object);
            if (value != null) {
                writer.name(name);
                typeSerializer.write(value, writer);
            }
        }
    }

}
