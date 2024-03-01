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

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class NBTWriterTest {

    @Test
    public void testNbtWriter() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NBTWriter writer = new NBTWriter(out);

        assertFalse(writer.inCompound());
        assertFalse(writer.inList());

        writer.beginCompound();

        assertTrue(writer.inCompound());
        assertFalse(writer.inList());

        writer.name("testByte").value((byte) 10);
        writer.name("testShort").value((short) -23);
        writer.name("testInt").value(1034);
        writer.name("testLong").value(289374678734L);
        writer.name("testFloat").value(-2.653f);
        writer.name("testDouble").value(4.653d);
        writer.name("testCompound").beginCompound();

        assertTrue(writer.inCompound());
        assertFalse(writer.inList());

        writer.name("testList").beginList(3);

        assertTrue(writer.inList());
        assertFalse(writer.inCompound());

        writer.value(0.43d);
        writer.value(-0.43d);
        writer.value(1d);
        writer.endList(); // testList

        writer.name("testByteArray").value(new byte[]{0, 110, 30, 20, 3, -4});
        writer.name("testIntArray").value(new int[]{0, -10342, 30, 20, 3, -4});
        writer.name("testLongArray").value(new long[]{0, 110, 289374678734L, 20, 3, -4});

        assertTrue(writer.inCompound());
        assertFalse(writer.inList());

        writer.endCompound(); // testCompound
        writer.endCompound(); // root

        assertFalse(writer.inCompound());
        assertFalse(writer.inList());

        byte[] data = out.toByteArray();

        ByteArrayInputStream in = new ByteArrayInputStream(data);
        NBTReader reader = new NBTReader(in);

        reader.beginCompound();

        assertEquals(TagType.BYTE, reader.peek());
        assertEquals("testByte", reader.name());
        assertEquals(10, reader.nextByte());

        assertEquals(TagType.SHORT, reader.peek());
        assertEquals("testShort", reader.name());
        assertEquals(-23, reader.nextShort());

        assertEquals(TagType.INT, reader.peek());
        assertEquals("testInt", reader.name());
        assertEquals(1034, reader.nextInt());

        assertEquals(TagType.LONG, reader.peek());
        assertEquals("testLong", reader.name());
        assertEquals(289374678734L, reader.nextLong());

        assertEquals(TagType.FLOAT, reader.peek());
        assertEquals("testFloat", reader.name());
        assertEquals(-2.653f, reader.nextFloat());

        assertEquals(TagType.DOUBLE, reader.peek());
        assertEquals("testDouble", reader.name());
        assertEquals(4.653d, reader.nextDouble());

        assertEquals(TagType.COMPOUND, reader.peek());
        assertEquals("testCompound", reader.name());
        reader.beginCompound();

        assertEquals(TagType.LIST, reader.peek());
        assertEquals("testList", reader.name());
        assertEquals(3, reader.beginList());

        assertEquals(TagType.DOUBLE, reader.peek());
        assertEquals(0.43d, reader.nextDouble());
        assertEquals(-0.43d, reader.nextDouble());
        assertEquals(1d, reader.nextDouble());

        assertEquals(TagType.END, reader.peek());
        reader.endList();

        assertEquals(TagType.BYTE_ARRAY, reader.peek());
        assertEquals("testByteArray", reader.name());
        assertArrayEquals(new byte[]{0, 110, 30, 20, 3, -4}, reader.nextByteArray());

        assertEquals(TagType.INT_ARRAY, reader.peek());
        assertEquals("testIntArray", reader.name());
        assertArrayEquals(new int[]{0, -10342, 30, 20, 3, -4}, reader.nextIntArray());

        assertEquals(TagType.LONG_ARRAY, reader.peek());
        assertEquals("testLongArray", reader.name());
        assertArrayEquals(new long[]{0, 110, 289374678734L, 20, 3, -4}, reader.nextLongArray());

        assertEquals(TagType.END, reader.peek());
        reader.endCompound();

        assertEquals(TagType.END, reader.peek());
        reader.endCompound();

    }

}
