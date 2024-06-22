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

import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.InstanceCreator;
import de.bluecolored.bluenbt.InstanceCreatorFactory;
import de.bluecolored.bluenbt.TypeToken;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class DefaultInstanceCreatorFactory implements InstanceCreatorFactory {

    public static final DefaultInstanceCreatorFactory INSTANCE = new DefaultInstanceCreatorFactory();

    @Override
    public <T> Optional<? extends InstanceCreator<T>> create(TypeToken<T> type, BlueNBT blueNBT) {
        return Optional.empty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> InstanceCreator<T> createFor(TypeToken<T> type, BlueNBT blueNBT) {
        try {

            // try no-args constructor
            try {
                Constructor<? super T> constructor = type.getRawType().getDeclaredConstructor();
                constructor.setAccessible(true);
                return () -> {
                    try {
                        //noinspection unchecked
                        return (T) constructor.newInstance();
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException("Failed to create instance using no-args constructor: " + constructor, e);
                    }
                };
            } catch (NoSuchMethodException ignore) {}

            // use common default implementations
            Class<?> rawType = type.getRawType();
            if (type.is(Collection.class)) {
                if (rawType.isAssignableFrom(ArrayList.class)) return () -> (T) new ArrayList<>();
                if (rawType.isAssignableFrom(ArrayDeque.class)) return () -> (T) new ArrayDeque<>();
                if (rawType.isAssignableFrom(LinkedHashSet.class)) return () -> (T) new LinkedHashSet<>();
                if (rawType.isAssignableFrom(TreeSet.class)) return () -> (T) new TreeSet<>();
                if (rawType.isAssignableFrom(EnumSet.class)) {
                    Type elementType = getCollectionElementType(type);
                    if (elementType instanceof Class elementClass && Enum.class.isAssignableFrom(elementClass))
                        return () -> (T) EnumSet.noneOf(elementClass);
                    throw new IllegalStateException("Invalid EnumSet type: " + type);
                }
            }

            if (type.is(Map.class)) {
                if (rawType.isAssignableFrom(HashMap.class)) return () -> (T) new LinkedHashMap<>();
                if (rawType.isAssignableFrom(TreeMap.class)) return () -> (T) new TreeMap<>();
                if (rawType.isAssignableFrom(ConcurrentHashMap.class)) return () -> (T) new ConcurrentHashMap<>();
                if (rawType.isAssignableFrom(ConcurrentSkipListMap.class)) return () -> (T) new ConcurrentSkipListMap<>();
                if (rawType.isAssignableFrom(EnumMap.class)) {
                    Type keyType = getMapKeyAndValueTypes(type)[0];
                    if (keyType instanceof Class<?> keyClass && Enum.class.isAssignableFrom(keyClass))
                        return () -> (T) new EnumMap(keyClass);
                    throw new IllegalStateException("Invalid EnumMap type: " + type);
                }
            }

            // use unsafe
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            final Object unsafe = unsafeField.get(null);
            final Method allocateInstanceMethod = unsafeClass.getMethod("allocateInstance", Class.class);
            return () -> {
                try {
                    //noinspection unchecked
                    return (T) allocateInstanceMethod.invoke(unsafe, type.getRawType());
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot create instance of type: " + type, e);
                }
            };

        } catch (Exception ex) {
            throw new RuntimeException("Failed to create Default-InstanceCreator for type: " + type, ex);
        }
    }

    private Type getCollectionElementType(TypeToken<?> type) {
        return switch (type.getSupertype(Collection.class)) {
            case ParameterizedType parameterizedType -> parameterizedType.getActualTypeArguments()[0];
            case WildcardType wildcardType -> wildcardType.getUpperBounds()[0];
            default -> Object.class;
        };
    }

    private Type[] getMapKeyAndValueTypes(TypeToken<?> type) {
        if (type.is(Properties.class)) return new Type[] { String.class, String.class };
        return (type.getSupertype(Map.class) instanceof ParameterizedType parameterizedType) ?
                parameterizedType.getActualTypeArguments() :
                new Type[] { Object.class, Object.class };
    }

}
