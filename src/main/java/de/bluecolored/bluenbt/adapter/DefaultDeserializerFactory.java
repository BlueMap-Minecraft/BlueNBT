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

import de.bluecolored.bluenbt.*;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;

/**
 * A {@link TypeDeserializerFactory} attempting to create {@link TypeDeserializer}s for any type based on the types fields using reflection.
 */
public class DefaultDeserializerFactory implements TypeDeserializerFactory {

    public static final DefaultDeserializerFactory INSTANCE = new DefaultDeserializerFactory();

    @Override
    public <T> Optional<TypeDeserializer<T>> create(TypeToken<T> type, BlueNBT blueNBT) {
        return Optional.of(createFor(type, blueNBT));
    }

    public <T> TypeDeserializer<T> createFor(TypeToken<T> type, BlueNBT blueNBT) {
        try {
            TypeResolver<T, ?> typeResolver = blueNBT.getTypeResolver(type);
            if (typeResolver != null)
                return new TypeResolvingAdapter<>(type, typeResolver, blueNBT);
            return new DefaultAdapter<>(type, blueNBT);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create Default-TypeSerializer for type: " + type, ex);
        }
    }

    static class TypeResolvingAdapter<T> implements TypeDeserializer<T>  {

        private final TypeResolver<T, Object> typeResolver;
        private final TypeToken<Object> baseType;
        private final TypeDeserializer<Object> baseDeserializer;
        private final Map<TypeToken<?>, TypeDeserializer<? extends T>> delegateDeserializers;
        private final TypeDeserializer<T> fallbackDeserializer;

        @SuppressWarnings("unchecked")
        public TypeResolvingAdapter(TypeToken<T> type, TypeResolver<T, ?> typeResolver,  BlueNBT blueNBT) {
            this.typeResolver = (TypeResolver<T, Object>) typeResolver;
            this.baseType = this.typeResolver.getBaseType();
            this.baseDeserializer = new DefaultAdapter<>(baseType, blueNBT);
            this.delegateDeserializers = new HashMap<>();
            for (TypeToken<? extends T> resolved : typeResolver.getPossibleTypes())
                delegateDeserializers.put(resolved, new DefaultAdapter<>(resolved, blueNBT));
            this.fallbackDeserializer = new DefaultAdapter<>(type, blueNBT);
        }

        @Override
        @SuppressWarnings("unchecked")
        public T read(NBTReader reader) throws IOException {
            // read next element as raw data
            byte[] data = reader.raw();

            // parse data first into base object
            Object base = baseDeserializer.read(new NBTReader(data));

            // resolve type
            TypeToken<? extends T> resolvedType = typeResolver.resolve(base);
            TypeDeserializer<? extends T> deserializer = delegateDeserializers.get(resolvedType);
            if (deserializer == null) deserializer = fallbackDeserializer;

            // shortcut if resolved type == base type
            if (resolvedType.equals(baseType))
                return (T) base;

            // parse data into final type
            return deserializer.read(new NBTReader(data));
        }

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
        private final InstanceCreator<T> constructor;

        private final Map<String, FieldAccessor> fields = new HashMap<>();

        private final Collection<PostSerializeAction<T>> postSerializeActions = new ArrayList<>(0);

        public DefaultAdapter(TypeToken<T> type, BlueNBT blueNBT) {
            this.type = type;
            this.constructor = blueNBT.getInstanceCreator(type);

            TypeToken<?> typeToken = type;
            Class<?> raw;
            while (typeToken != null && (raw = typeToken.getRawType()) != Object.class) {
                for (Field field : raw.getDeclaredFields()) {
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))
                        continue;

                    field.setAccessible(true);

                    String[] names = new String[]{ blueNBT.getNamingStrategy().apply(field) };
                    NBTName nbtName = field.getAnnotation(NBTName.class);
                    if (nbtName != null && nbtName.value().length > 0) names = nbtName.value();

                    TypeToken<?> fieldType = TypeToken.of(typeToken.resolve(field.getGenericType()));

                    TypeDeserializer<?> typeDeserializer;
                    Class<? extends TypeDeserializer<?>> deserializerType = findDeserializerType(field, fieldType.getRawType());
                    if (deserializerType != null) {
                        try {
                            typeDeserializer = createTypeDeserializerInstance(deserializerType, fieldType, blueNBT);
                        } catch (Exception ex) {
                            throw new IllegalStateException("Failed to create instance of TypeDeserializer: " + deserializerType, ex);
                        }
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

                for (Method method : raw.getDeclaredMethods()) {
                    if (
                            method.getAnnotation(NBTPostDeserialize.class) != null &&
                            method.getParameterCount() == 0
                    ) {
                        method.setAccessible(true);
                        postSerializeActions.add(method::invoke);
                    }
                }

                Type superType = typeToken.resolve(raw.getGenericSuperclass());
                typeToken = superType != null ? TypeToken.of(superType) : null;
            }
        }

        @Override
        public T read(NBTReader reader) throws IOException {
            try {
                T object = constructor.create();
                reader.beginCompound();

                while (reader.peek() != TagType.END) {
                    String name = reader.name();
                    FieldAccessor fieldInfo = fields.get(name);

                    if (fieldInfo != null) {
                        fieldInfo.read(object, reader);
                    } else {
                        reader.skip();
                    }
                }

                reader.endCompound();

                // run post serialization actions
                if (!postSerializeActions.isEmpty())
                    for (PostSerializeAction<T> action : postSerializeActions)
                        action.invoke(object);

                return object;
            } catch (IllegalAccessException | InvocationTargetException ex) {
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

        private TypeDeserializer<?> createTypeDeserializerInstance(
                Class<? extends TypeDeserializer<?>> deserializerType,
                TypeToken<?> fieldType,
                BlueNBT blueNBT
        ) throws ReflectiveOperationException {
            // try TypeToken & BlueNBT constructor
            try {
                return deserializerType.getDeclaredConstructor(TypeToken.class, BlueNBT.class).newInstance(fieldType, blueNBT);
            } catch (NoSuchMethodException ignore) {}

            // try TypeToken constructor
            try {
                return deserializerType.getDeclaredConstructor(TypeToken.class).newInstance(fieldType);
            } catch (NoSuchMethodException ignore) {}

            // try BlueNBT constructor
            try {
                return deserializerType.getDeclaredConstructor(BlueNBT.class).newInstance(blueNBT);
            } catch (NoSuchMethodException ignore) {}

            // use no-args constructor
            return deserializerType.getDeclaredConstructor().newInstance();
        }

    }

    @FunctionalInterface
    private interface FieldAccessor {
        void read(Object object, NBTReader reader) throws IOException, IllegalAccessException;
    }

    @FunctionalInterface
    private interface PostSerializeAction<T> {
        void invoke(T object) throws IOException, IllegalAccessException, InvocationTargetException;
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
