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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public class TypeToken<T> {

    private final Class<? super T> rawType;
    private final Type type;

    /**
     * You can create any TypeToken instance by creating an anonymous subclass with type-parameters.<br>
     * Like this:
     * <blockquote><pre>
     *  TypeToken&lt; Map&lt;String, Collection&lt;Integer&gt;&gt; &gt; typeToken = new TypeToken&lt;&gt;() {};
     * </pre></blockquote>
     */
    @SuppressWarnings("unchecked")
    protected TypeToken() {
        if (!(getClass().getGenericSuperclass() instanceof ParameterizedType superclass))
            throw new IllegalArgumentException("TypeToken is missing a type-parameter");

        this.type = superclass.getActualTypeArguments()[0];
        this.rawType = (Class<? super T>) findRawType(type);
    }

    @SuppressWarnings("unchecked")
    private TypeToken(Type type) {
        this(type, (Class<? super T>) findRawType(type));
    }

    private TypeToken(Type type, Class<? super T> rawType) {
        Objects.requireNonNull(type, "type is null");
        Objects.requireNonNull(type, "rawType is null");

        this.type = type;
        this.rawType = rawType;
    }

    /**
     * Returns true if this type is a subtype of the given supertype, false if not.<br>
     * If this method returns true, this type can be cast to the given supertype.
     */
    public boolean is(Class<?> supertype) {
        return supertype.isAssignableFrom(rawType);
    }

    /**
     * Returns true if this type is representing an array, false if not.
     */
    public boolean isArray() {
        return type instanceof GenericArrayType ||
                type instanceof Class<?> clazz && clazz.isArray();
    }

    /**
     * <p>Returns the generic component-type of the array-type represented by this token.</p>
     * <p>Returns <code>null</code> if this type is not an array.</p>
     * @return the component-type od this array
     */
    public Type getComponentType() {
        return type instanceof GenericArrayType array ? array.getGenericComponentType() : rawType.getComponentType();
    }

    /**
     * <p>
     *     Returns the generic supertype of the type represented by this token with the provided <code>supertype</code>
     *     class.
     * </p>
     * <p>
     *     For example if this type is <code>ArrayList&lt;String&gt;</code> and the provided <code>supertype</code> is
     *     <code>Collection</code> then the returned type will be <code>Collection&lt;String&gt;</code>.
     * </p>
     * @param supertype The raw-supertype we are searching for
     * @return the generic supertype
     */
    public Type getSupertype(Class<?> supertype) {
        return resolve(type, rawType, findSupertype(type, rawType, supertype));
    }

    /**
     * <p>Resolves the given type using this type as context.</p>
     * <p>
     *      For example if this type is <code>{@link java.util.HashSet}&lt;String&gt;</code> and the given type is the
     *      type of the <code>{@link java.util.HashMap}&lt;E,Object&gt; map</code> field of the HashSet implementation,
     *      then this method would return <code>{@link java.util.HashMap}&lt;String,Object&gt;</code>.
     * </p>
     */
    public Type resolve(Type type) {
        return resolve(this.type, this.rawType, type);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypeToken<?> typeToken)) return false;
        return type.equals(typeToken.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return typeToString(type);
    }

    /**
     * Creates a new TypeToken for the provided type
     */
    public static TypeToken<?> of(Type type) {
        return new TypeToken<>(type);
    }

    /**
     * Creates a new TypeToken for the provided type
     */
    public static <T> TypeToken<T> of(Class<T> type) {
        return new TypeToken<>(type);
    }

    /**
     * Creates a new TypeToken representing the provided generic type with the specified type-arguments
     * <blockquote><pre>
     *  // Map&lt;String, Object&gt;
     *  TypeToken.of(Map.class, String.class, Object.class)
     * </pre></blockquote>
     */
    public static TypeToken<?> of(Type rawType, Type... typeArguments) {
        return new TypeToken<>(new ParameterizedTypeImpl(null, rawType, typeArguments));
    }

    /**
     * Creates a new TypeToken representing an array with the provided component type
     * <blockquote><pre>
     *  // String[]
     *  TypeToken.array(String.class)
     * </pre></blockquote>
     */
    public static TypeToken<?> array(Type componentType) {
        return new TypeToken<>(new GenericArrayTypeImpl(componentType));
    }

    @Getter
    @RequiredArgsConstructor
    private static class GenericArrayTypeImpl implements GenericArrayType {
        @NonNull private final Type genericComponentType;

        @Override
        public boolean equals(Object obj) {
            return obj instanceof GenericArrayType gt &&
                    Objects.equals(genericComponentType, gt.getGenericComponentType());
        }

        @Override
        public int hashCode() {
            return genericComponentType.hashCode();
        }

        @Override
        public String toString() {
            return typeToString(genericComponentType) + "[]";
        }

    }

    @Getter
    private static class ParameterizedTypeImpl implements ParameterizedType {

        @Nullable private final Type ownerType;
        private final Type rawType;
        private final Type[] actualTypeArguments;

        private ParameterizedTypeImpl(@Nullable Type ownerType, Type rawType, Type... actualTypeArguments) {
            Objects.requireNonNull(rawType, "rawType cannot be null");
            if (ownerType == null &&
                    rawType instanceof Class<?> rawClass &&
                    !Modifier.isStatic(rawClass.getModifiers()) &&
                    rawClass.getEnclosingClass() != null
            ) throw new NullPointerException("type %s requires an owner type".formatted(rawType));
            Objects.requireNonNull(actualTypeArguments, "actualTypeArguments cannot be null");
            if (Arrays.stream(actualTypeArguments).anyMatch(Objects::isNull))
                throw new NullPointerException("elements of actualTypeArguments cannot be null.");

            this.ownerType = ownerType;
            this.rawType = rawType;
            this.actualTypeArguments = Arrays.copyOf(actualTypeArguments, actualTypeArguments.length);
        }

        public Type[] getActualTypeArguments() {
            return Arrays.copyOf(actualTypeArguments, actualTypeArguments.length);
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ParameterizedTypeImpl that)) return false;
            return Objects.equals(ownerType, that.ownerType) &&
                    Objects.equals(rawType, that.rawType) &&
                    Arrays.equals(actualTypeArguments, that.actualTypeArguments);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(actualTypeArguments) ^
                    rawType.hashCode() ^
                    (ownerType != null ? ownerType.hashCode() : 0);
        }

        @Override
        public String toString() {
            return typeToString(rawType) + "<" + Arrays.stream(actualTypeArguments)
                    .map(TypeToken::typeToString)
                    .collect(Collectors.joining(", ")) + ">";
        }

    }

    @Getter
    private static final class WildcardTypeImpl implements WildcardType {

        private static final Type[] EMPTY = new Type[0];

        private final Type[] upperBounds;
        private final Type[] lowerBounds;

        public WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
            this.upperBounds = upperBounds != null && upperBounds.length > 0 ?
                    Arrays.copyOf(upperBounds, upperBounds.length) :
                    new Type[] { Object.class };

            this.lowerBounds = lowerBounds != null && lowerBounds.length > 0 ?
                    Arrays.copyOf(lowerBounds, lowerBounds.length) :
                    EMPTY;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WildcardTypeImpl that)) return false;
            return Arrays.equals(upperBounds, that.upperBounds) && Arrays.equals(lowerBounds, that.lowerBounds);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(lowerBounds) ^ Arrays.hashCode(upperBounds);
        }

        @Override
        public String toString() {
            if (lowerBounds.length > 0) {
                return "? super " + Arrays.stream(lowerBounds)
                        .map(TypeToken::typeToString)
                        .collect(Collectors.joining(" & "));
            } else if (upperBounds.length == 0 || upperBounds[0] == Object.class) {
                return "?";
            } else {
                return "? extends " + Arrays.stream(upperBounds)
                        .map(TypeToken::typeToString)
                        .collect(Collectors.joining(" & "));
            }
        }

    }

    private static Class<?> findRawType(Type type) {
        return switch (type) {
            case Class<?> clazz -> clazz;
            case ParameterizedType pType -> (Class<?>) pType.getRawType();
            case GenericArrayType array -> Array.newInstance(findRawType(array.getGenericComponentType()), 0).getClass();
            case WildcardType wildcard -> findRawType(wildcard.getUpperBounds()[0]);
            default -> Object.class;
        };
    }

    private static Type findSupertype(Type type, Class<?> rawType, Class<?> supertype) {
        if (supertype == rawType) return type;

        if (supertype.isInterface()) {
            Class<?>[] interfaces = rawType.getInterfaces();
            for (int i = 0; i < interfaces.length; ++i) {
                if (supertype.isAssignableFrom(interfaces[i]))
                    return TypeToken.of(rawType.getGenericInterfaces()[i])
                            .getSupertype(supertype);
            }
        }

        Class<?> parent = rawType.getSuperclass();
        if (parent != null && parent != Object.class && supertype.isAssignableFrom(parent)) {
            return TypeToken.of(rawType.getGenericSuperclass())
                    .getSupertype(supertype);
        }

        return supertype;
    }

    private static Type resolve(Type context, Class<?> rawContext, @Nullable Type type) {
        return switch (type) {

            case TypeVariable<?> typeVariable -> {
                GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
                if (!(genericDeclaration instanceof Class<?> declaringRawType)) yield typeVariable;

                Type declaringType = findSupertype(context, rawContext, declaringRawType);
                if (declaringType instanceof ParameterizedType declaringParameterized) {
                    int typeParameterIndex = indexOf(declaringRawType.getTypeParameters(), typeVariable);
                    yield resolve(context, rawContext, declaringParameterized.getActualTypeArguments()[typeParameterIndex]);
                }

                yield typeVariable;
            }

            case Class<?> classType -> {
                if (!classType.isArray()) yield classType;

                Type componentType = classType.getComponentType();
                Type resolvedComponentType = resolve(context, rawContext, componentType);
                yield componentType != resolvedComponentType ?
                        new GenericArrayTypeImpl(resolvedComponentType) :
                        classType;
            }

            case GenericArrayType genericArrayType -> {
                Type componentType = genericArrayType.getGenericComponentType();
                Type resolvedComponentType = resolve(context, rawContext, componentType);
                yield componentType != resolvedComponentType ?
                        new GenericArrayTypeImpl(resolvedComponentType) :
                        genericArrayType;
            }

            case ParameterizedType parameterizedType -> {
                Type ownerType = parameterizedType.getOwnerType();
                Type resolvedOwnerType = resolve(context, rawContext, ownerType);

                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                Type[] resolvedTypeArguments = typeArguments;
                for (int i = 0; i < typeArguments.length; i++) {
                    Type resolvedTypeArgument = resolve(context, rawContext, typeArguments[i]);
                    if (typeArguments[i] != resolvedTypeArgument && typeArguments == resolvedTypeArguments)
                        resolvedTypeArguments = Arrays.copyOf(typeArguments, typeArguments.length);
                    resolvedTypeArguments[i] = resolvedTypeArgument;
                }

                yield ownerType != resolvedOwnerType || typeArguments != resolvedTypeArguments ?
                        new ParameterizedTypeImpl(resolvedOwnerType, parameterizedType.getRawType(), resolvedTypeArguments) :
                        parameterizedType;
            }

            case WildcardType wildcardType -> {
                Type[] lowerBounds = wildcardType.getLowerBounds();
                Type[] upperBounds = wildcardType.getUpperBounds();

                Type[] resolvedLowerBounds = lowerBounds;
                Type[] resolvedUpperBounds = upperBounds;

                for (int i = 0; i < lowerBounds.length; i++) {
                    Type resolvedLowerBound = resolve(context, rawContext, lowerBounds[i]);
                    if (lowerBounds[i] != resolvedLowerBound && lowerBounds == resolvedLowerBounds)
                        resolvedLowerBounds = Arrays.copyOf(lowerBounds, lowerBounds.length);
                    resolvedLowerBounds[i] = resolvedLowerBound;
                }

                for (int i = 0; i < upperBounds.length; i++) {
                    Type resolvedUpperBound = resolve(context, rawContext, upperBounds[i]);
                    if (upperBounds[i] != resolvedUpperBound && upperBounds == resolvedUpperBounds)
                        resolvedUpperBounds = Arrays.copyOf(upperBounds, upperBounds.length);
                    resolvedUpperBounds[i] = resolvedUpperBound;
                }

                yield lowerBounds != resolvedLowerBounds || upperBounds != resolvedUpperBounds ?
                        new WildcardTypeImpl(resolvedUpperBounds, resolvedLowerBounds) :
                        wildcardType;
            }

            case null, default -> type;
        };
    }

    private static int indexOf(Object[] array, Object element) {
        for (int i = 0; i < array.length; i++)
            if (element.equals(array[i])) return i;
        throw new NoSuchElementException();
    }

    private static String typeToString(Type type) {
        return type instanceof Class<?> clazz ?
                clazz.getName() :
                type.toString();
    }

}
