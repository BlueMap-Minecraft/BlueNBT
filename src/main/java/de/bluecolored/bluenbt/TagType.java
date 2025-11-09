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
import lombok.RequiredArgsConstructor;

import java.util.NoSuchElementException;

/**
 * The raw NBT data-type
 */
@RequiredArgsConstructor
@Getter
public enum TagType {

    END         ( 0, -1),
    BYTE        ( 1, 1),
    SHORT       ( 2, 2),
    INT         ( 3, 4),
    LONG        ( 4, 8),
    FLOAT       ( 5, 4),
    DOUBLE      ( 6, 8),
    BYTE_ARRAY  ( 7, -1),
    STRING      ( 8, -1),
    LIST        ( 9, -1),
    COMPOUND    (10, -1),
    INT_ARRAY   (11, -1),
    LONG_ARRAY  (12, -1);

    private static final TagType[] TAG_TYPE_ID_MAP = new TagType[13];
    static {
         for (TagType type : TagType.values())
             TAG_TYPE_ID_MAP[type.id] = type;
    }

    private final int id;
    private final int size;

    public static TagType forId(int id) {
        TagType type;
        if (id >= TAG_TYPE_ID_MAP.length || (type = TAG_TYPE_ID_MAP[id]) == null)
            throw new NoSuchElementException("There is no TagType for id: " + id);
        return type;
    }

}
