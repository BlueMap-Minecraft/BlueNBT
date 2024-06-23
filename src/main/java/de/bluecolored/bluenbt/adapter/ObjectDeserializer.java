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
import java.util.*;

/**
 * A {@link TypeDeserializer} deserializing NBT-Data into a construct of {@link Map}s, {@link List}s, Arrays and primitives
 * closely representing the actual NBT-Data structure.
 * <blockquote><pre>
 *     NBT-Tag -> Deserialized Java-Object
 *     --------------------
 *     COMPOUND -> LinkedHashMap&lt;String, Object&gt;
 *     LIST -> ArrayList&lt;Object&gt;
 *     STRING -> String
 *     BYTE -> byte
 *     SHORT -> short
 *     INT -> int
 *     LONG -> long
 *     FLOAT -> float
 *     DOUBLE -> double
 *     BYTE_ARRAY -> byte[]
 *     INT_ARRAY -> int[]
 *     LONG_ARRAY -> long[]
 * </pre></blockquote>
 */
public class ObjectDeserializer implements TypeDeserializer<Object>  {

    public static final ObjectDeserializer INSTANCE = new ObjectDeserializer();

    @Override
    public Object read(NBTReader reader) throws IOException {
        TagType type = reader.peek();
        switch (type) {

            case COMPOUND:
                Map<String, Object> map = new LinkedHashMap<>();
                reader.beginCompound();
                while (reader.hasNext())
                    map.put(reader.name(), read(reader));
                reader.endCompound();
                return map;

            case LIST:
                List<Object> list = new ArrayList<>(reader.beginList());
                while (reader.hasNext())
                    list.add(read(reader));
                reader.endList();
                return list;

            case STRING: return reader.nextString();
            case BYTE: return reader.nextByte();
            case SHORT: return reader.nextShort();
            case INT: return reader.nextInt();
            case LONG: return reader.nextLong();
            case FLOAT: return reader.nextFloat();
            case DOUBLE: return reader.nextDouble();
            case BYTE_ARRAY: return reader.nextByteArray();
            case INT_ARRAY: return reader.nextIntArray();
            case LONG_ARRAY: return reader.nextLongArray();

            case END:
            default:
                throw new IllegalStateException("Found unexpected " + type + " tag.");

        }
    }

}
