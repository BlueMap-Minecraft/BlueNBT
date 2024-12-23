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

import java.io.IOException;

public interface TypeResolver<T, B> {

    TypeToken<B> getBaseType();

    TypeToken<? extends T> resolve(B base);

    Iterable<TypeToken<? extends T>> getPossibleTypes();

    /**
     * Called when the raw type data could be read fine, but parsing it against the base type threw an exception.<br>
     * Can be used to recover from errors with some default value.<br>
     * Defaults to just rethrowing the exception.
     */
    default T onException(IOException parseException) throws IOException {
        throw parseException;
    }

    /**
     * Called when the raw type data could be read fine, the base type could also be parsed,
     * but parsing it against the resolved type threw an exception.<br>
     * Can be used to recover from errors with some default value.<br>
     * Defaults to calling {@link #onException(IOException)}
     */
    default T onException(IOException parseException, B base) throws IOException {
        return onException(parseException);
    }

}
