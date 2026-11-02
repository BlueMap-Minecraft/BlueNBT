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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class BulkArrayTest {

    private static final int BYTE_COUNT = 1024 * 8 * 2 + 7;
    private static final int INT_COUNT = 1024 * 8 / 4 * 3 + 7;
    private static final int LONG_COUNT = 1024 * 8 / 8 * 3 + 7;

    private final byte[] bytes;
    private final int[] ints;
    private final long[] longs;

    public BulkArrayTest() {
        Random random = new Random(42);
        bytes = new byte[BYTE_COUNT];
        random.nextBytes(bytes);
        ints = random.ints(INT_COUNT).toArray();
        longs = random.longs(LONG_COUNT).toArray();
    }

    private byte[] writeArrays() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (NBTWriter writer = new NBTWriter(out)) {
            writer.beginCompound();
            writer.name("bytes").value(bytes);
            writer.name("ints").value(ints);
            writer.name("longs").value(longs);
            writer.endCompound();
        }
        return out.toByteArray();
    }

    @Test
    public void testReadArrays() throws IOException {
        try (NBTReader reader = new NBTReader(writeArrays())) {
            reader.beginCompound();
            assertArrayEquals(bytes, reader.nextByteArray());
            assertArrayEquals(ints, reader.nextIntArray());
            assertArrayEquals(longs, reader.nextLongArray());
            reader.endCompound();
        }
    }

    @Test
    public void testReadArraysIntoSmallerBuffers() throws IOException {
        try (NBTReader reader = new NBTReader(writeArrays())) {
            reader.beginCompound();

            // skip byte array
            reader.skip();

            int[] intBuffer = new int[INT_COUNT - 1000];
            assertEquals(INT_COUNT, reader.nextIntArray(intBuffer));
            assertArrayEquals(Arrays.copyOf(ints, intBuffer.length), intBuffer);

            long[] longBuffer = new long[LONG_COUNT - 1000];
            assertEquals(LONG_COUNT, reader.nextLongArray(longBuffer));
            assertArrayEquals(Arrays.copyOf(longs, longBuffer.length), longBuffer);

            reader.endCompound();
        }
    }

    @Test
    public void testReadArraysIntoLargerBuffers() throws IOException {
        try (NBTReader reader = new NBTReader(writeArrays())) {
            reader.beginCompound();

            // skip byte array
            reader.skip();

            int[] intBuffer = new int[INT_COUNT + 10];
            Arrays.fill(intBuffer, 123);
            assertEquals(INT_COUNT, reader.nextIntArray(intBuffer));
            assertArrayEquals(ints, Arrays.copyOf(intBuffer, INT_COUNT));
            assertEquals(123, intBuffer[INT_COUNT]);

            long[] longBuffer = new long[LONG_COUNT + 10];
            assertEquals(LONG_COUNT, reader.nextArray(longBuffer));
            assertArrayEquals(longs, Arrays.copyOf(longBuffer, LONG_COUNT));

            reader.endCompound();
        }
    }

    @Test
    public void testReadArraysWidening() throws IOException {
        try (NBTReader reader = new NBTReader(writeArrays())) {
            reader.beginCompound();

            double[] fromBytes = reader.nextArray(double[]::new);
            for (int i = 0; i < bytes.length; i++) assertEquals(bytes[i], fromBytes[i]);

            long[] fromInts = reader.nextArrayAsLongArray();
            for (int i = 0; i < ints.length; i++) assertEquals(ints[i], fromInts[i]);

            float[] fromLongs = reader.nextArray(float[]::new);
            for (int i = 0; i < longs.length; i++) assertEquals((float) longs[i], fromLongs[i]);

            reader.endCompound();
        }

        try (NBTReader reader = new NBTReader(writeArrays())) {
            reader.beginCompound();

            short[] shortsFromBytes = reader.nextArray(short[]::new);
            for (int i = 0; i < bytes.length; i++) assertEquals(bytes[i], shortsFromBytes[i]);

            double[] fromInts = reader.nextArray(double[]::new);
            for (int i = 0; i < ints.length; i++) assertEquals(ints[i], fromInts[i]);

            // skip long array
            reader.skip();

            reader.endCompound();
        }

        try (NBTReader reader = new NBTReader(writeArrays())) {
            reader.beginCompound();

            int[] intsFromBytes = reader.nextArray(int[]::new);
            for (int i = 0; i < bytes.length; i++) assertEquals(bytes[i], intsFromBytes[i]);

            long[] longsFromInts = reader.nextArray(long[]::new);
            for (int i = 0; i < ints.length; i++) assertEquals(ints[i], longsFromInts[i]);

            // skip long array
            reader.skip();

            reader.endCompound();
        }
    }

    @Test
    public void testWriteSubRange() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (NBTWriter writer = new NBTWriter(out)) {
            writer.beginCompound();
            writer.name("bytes").value(bytes, 5, 100);
            writer.name("ints").value(ints, 3, INT_COUNT - 10);
            writer.name("longs").value(longs, 0, 0);
            writer.endCompound();
        }

        try (NBTReader reader = new NBTReader(out.toByteArray())) {
            reader.beginCompound();
            assertArrayEquals(Arrays.copyOfRange(bytes, 5, 105), reader.nextByteArray());
            assertArrayEquals(Arrays.copyOfRange(ints, 3, INT_COUNT - 7), reader.nextIntArray());
            assertArrayEquals(new long[0], reader.nextLongArray());
            reader.endCompound();
        }

        assertThrows(IndexOutOfBoundsException.class, () ->
                new NBTWriter(new ByteArrayOutputStream()).value(ints, 10, INT_COUNT));
    }

}
