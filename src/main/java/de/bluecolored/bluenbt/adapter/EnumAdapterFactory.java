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

import java.io.IOException;
import java.util.Optional;

public class EnumAdapterFactory implements TypeAdapterFactory {

    public static final EnumAdapterFactory INSTANCE = new EnumAdapterFactory();

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Optional<? extends TypeAdapter<T>> create(TypeToken<T> type, BlueNBT blueNBT) {
        Class<? super T> rawType = type.getRawType();

        if (rawType.isEnum())
            return Optional.of((TypeAdapter<T>) new EnumAdapter(rawType));

        return Optional.empty();
    }

    static class EnumAdapter<E extends Enum<E>> implements TypeAdapter<E> {

        private final Class<E> enumType;
        private final E[] universe;

        public EnumAdapter(Class<E> enumType) {
            this.enumType = enumType;
            this.universe = enumType.getEnumConstants();
        }

        @Override
        public void write(E value, NBTWriter writer) throws IOException {
            writer.value(value.name());
        }

        @Override
        public TagType type() {
            return TagType.STRING;
        }

        @Override
        public E read(NBTReader reader) throws IOException {
            try {
                return switch (reader.peek()) {
                    case STRING -> Enum.valueOf(enumType, reader.nextString());
                    case LONG -> universe[(int) reader.nextLong()];
                    case INT -> universe[reader.nextInt()];
                    case SHORT -> universe[reader.nextShort()];
                    case BYTE -> universe[reader.nextByte()];
                    default -> throw new IOException("Can't convert type %s into enum-type %s at %s".formatted(reader.peek(), enumType, reader.path()));
                };
            } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
                throw new IOException("Invalid value for enum type %s at %s".formatted(enumType, reader.path()));
            }
        }

    }

}
