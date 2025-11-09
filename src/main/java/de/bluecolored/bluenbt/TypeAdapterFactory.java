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

import java.util.Optional;

/**
 * A factory for creating {@link TypeAdapter}s
 */
@FunctionalInterface
public interface TypeAdapterFactory extends TypeSerializerFactory, TypeDeserializerFactory {

    /**
     * Attempts to create and return a {@link TypeAdapter} for the given type, returning an empty {@link Optional} if the type
     * is not supported by this factory.
     * @param type The type the new {@link TypeAdapter} should be able to (de)serialize
     * @param blueNBT The currently used BlueNBT-instance
     * @return An {@link Optional} with the {@link TypeAdapter} or an empty Optional if the type is not supported
     */
    <T> Optional<? extends TypeAdapter<T>> create(TypeToken<T> type, BlueNBT blueNBT);

}
