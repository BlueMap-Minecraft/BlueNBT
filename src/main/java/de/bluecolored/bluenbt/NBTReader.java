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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Array;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * Can be used to directly read raw nbt-data from any {@link InputStream}.
 */
public class NBTReader implements Closeable {
    private static final String UNKNOWN_NAME = "<unknown>";

    private final DataLogInputStream log;
    private final DataInputStream in;

    private int stackPosition = 0;
    private TagType[] stack = new TagType[32];
    private String[] nameStack = new String[32];
    private int[] listStack = new int[32];

    private @Nullable ArrayBuffer buffer;

    public NBTReader(byte @NotNull [] data) {
        this(new ByteArrayInputStream(data));
    }

    public NBTReader(@NotNull InputStream in) {
        Objects.requireNonNull(in);
        this.log = new DataLogInputStream(in);
        this.in = new DataInputStream(log);
    }

    public TagType peek() throws IOException {
        TagType peek = stack[stackPosition];
        if (peek == null) {
            peek = readTag();
            stack[stackPosition] = peek;
        }
        return peek;
    }

    public String name() throws IOException {
        String name = nameStack[stackPosition];
        if (name == null) {
            if (peek() != TagType.END) {
                name = in.readUTF();
            } else {
                name = UNKNOWN_NAME;
            }
            nameStack[stackPosition] = name;
        }
        return name;
    }

    public void beginCompound() throws IOException {
        checkState(TagType.COMPOUND);
        advanceStack();
    }

    public void endCompound() throws IOException {
        checkState(TagType.END);
        if (!inCompound()) throw new IllegalStateException("Can not end compound. Current element is not in a compound! At: " + path());
        reduceStack();
        next();
    }

    public int beginList() throws IOException {
        checkState(TagType.LIST);

        TagType listType = readTag();
        int length = in.readInt();
        if (length < 0) length = 0;

        advanceStack();
        stack[stackPosition] = length == 0 ? TagType.END : listType;
        listStack[stackPosition] = length;
        nameStack[stackPosition] = UNKNOWN_NAME;

        return length;
    }

    public void endList() throws IOException {
        checkState(TagType.END);
        if (!inList()) throw new IllegalStateException("Can not end list. Current element is not in a list! At: " + path());
        reduceStack();
        next();
    }

    public boolean hasNext() throws IOException {
        return peek() != TagType.END;
    }

    public byte nextByte() throws IOException {
        checkState(TagType.BYTE);
        next();
        return in.readByte();
    }

    public short nextShort() throws IOException {
        checkState(TagType.SHORT);
        next();
        return in.readShort();
    }

    public int nextInt() throws IOException {
        checkState(TagType.INT);
        next();
        return in.readInt();
    }

    public long nextLong() throws IOException {
        checkState(TagType.LONG);
        next();
        return in.readLong();
    }

    public float nextFloat() throws IOException {
        checkState(TagType.FLOAT);
        next();
        return in.readFloat();
    }

    public double nextDouble() throws IOException {
        checkState(TagType.DOUBLE);
        next();
        return in.readDouble();
    }

    public String nextString() throws IOException {
        checkState(TagType.STRING);
        next();
        return in.readUTF();
    }

    public byte[] nextByteArray() throws IOException {
        checkState(TagType.BYTE_ARRAY);
        int length = readArrayLength();
        byte[] data = new byte[length];
        in.readFully(data);
        next();
        return data;
    }

    public int nextByteArray(byte[] target) throws IOException {
        checkState(TagType.BYTE_ARRAY);
        int length = readArrayLength();
        int readLength = Math.min(length, target.length);
        in.readFully(target, 0, readLength);
        in.skipNBytes(length - readLength);
        next();
        return length;
    }

    public int[] nextIntArray() throws IOException {
        checkState(TagType.INT_ARRAY);
        int length = readArrayLength();
        int[] data = new int[length];
        readInts(data, data.length);
        next();
        return data;
    }

    public int nextIntArray(int[] target) throws IOException {
        checkState(TagType.INT_ARRAY);
        int length = readArrayLength();
        int readLength = Math.min(length, target.length);
        readInts(target, readLength);
        in.skipNBytes((long) (length - readLength) * TagType.INT.getSize());
        next();
        return length;
    }

    public long[] nextLongArray() throws IOException {
        checkState(TagType.LONG_ARRAY);
        int length = readArrayLength();
        long[] data = new long[length];
        readLongs(data, data.length);
        next();
        return data;
    }

    public int nextLongArray(long[] target) throws IOException {
        checkState(TagType.LONG_ARRAY);
        int length = readArrayLength();
        int readLength = Math.min(length, target.length);
        readLongs(target, readLength);
        in.skipNBytes((long) (length - readLength) * TagType.LONG.getSize());
        next();
        return length;
    }

    /**
     * Reads any type of array (BYTE_ARRAY, INT_ARRAY or LONG_ARRAY) and returns it as a byte-array.
     */
    public byte[] nextArrayAsByteArray() throws IOException {
        if (peek() == TagType.BYTE_ARRAY) return nextByteArray();
        return nextArray(byte[]::new);
    }

    /**
     * Reads any type of array (BYTE_ARRAY, INT_ARRAY or LONG_ARRAY) and returns it as an int-array.
     */
    public int[] nextArrayAsIntArray() throws IOException {
        if (peek() == TagType.INT_ARRAY) return nextIntArray();
        return nextArray(int[]::new);
    }

    /**
     * Reads any type of array (BYTE_ARRAY, INT_ARRAY or LONG_ARRAY) and returns it as a long-array.
     */
    public long[] nextArrayAsLongArray() throws IOException {
        if (peek() == TagType.LONG_ARRAY) return nextLongArray();
        return nextArray(long[]::new);
    }

    /**
     * Reads any type of array (BYTE_ARRAY, INT_ARRAY or LONG_ARRAY) into the provided bufferArray.
     * @param bufferArray The array that will be used to store the data.<br>
     *                    If the length in the data is smaller than this buffer, the rest of the buffer will remain unchanged.<br>
     *                    If the length in the data is greater than this buffer, the remaining data will be skipped and discarded.
     * @return The actual size of the data -> The number of data-elements that have been read OR discarded.
     */
    public int nextArray(Object bufferArray) throws IOException {
        checkState();
        TagType type = peek();
        if (!isArrayType(type)) throw new IllegalStateException("Expected any array-type but got " + peek() + ". At: " + path());
        int length = readArrayLength();
        switch (type) {
            case BYTE_ARRAY: readByteArray(length, bufferArray); break;
            case INT_ARRAY: readIntArray(length, bufferArray); break;
            case LONG_ARRAY: readLongArray(length, bufferArray); break;
        }
        next();
        return length;
    }

    /**
     * Reads any type of array (BYTE_ARRAY, INT_ARRAY or LONG_ARRAY) and returns it into the array created by the provided generator.
     * @param generator The generator creating a new array that will be populated with the data.
     * @return The actual size of the data -> The number of data-elements that have been read OR discarded.
     */
    public <A> A nextArray(IntFunction<A> generator) throws IOException {
        checkState();
        TagType type = peek();
        if (!isArrayType(type)) throw new IllegalStateException("Expected any array-type but got " + peek() + ". At: " + path());
        int length = readArrayLength();
        A bufferArray = generator.apply(length);
        switch (type) {
            case BYTE_ARRAY: readByteArray(length, bufferArray); break;
            case INT_ARRAY: readIntArray(length, bufferArray); break;
            case LONG_ARRAY: readLongArray(length, bufferArray); break;
        }
        next();
        return bufferArray;
    }

    private int readArrayLength() throws IOException {
        int length = in.readInt();
        if (length < 0) throw new IOException("Invalid array-length: " + length + ". At: " + path());
        return length;
    }

    private void readByteArray(int length, Object bufferArray) throws IOException {
        checkState(TagType.BYTE_ARRAY);
        int readLength = Math.min(length, Array.getLength(bufferArray));
        switch (bufferArray) {
            case byte  [] a -> in.readFully(a, 0, readLength);
            case short [] a -> readBulk(a, readLength, Byte.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = buffer.array[i]; });
            case int   [] a -> readBulk(a, readLength, Byte.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = buffer.array[i]; });
            case long  [] a -> readBulk(a, readLength, Byte.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = buffer.array[i]; });
            case float [] a -> readBulk(a, readLength, Byte.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = buffer.array[i]; });
            case double[] a -> readBulk(a, readLength, Byte.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = buffer.array[i]; });
            default -> throw new IllegalArgumentException("Unsupported array type: " + bufferArray.getClass().getSimpleName());
        }
        in.skipNBytes(length - readLength);
    }

    private void readIntArray(int length, Object bufferArray) throws IOException {
        checkState(TagType.INT_ARRAY);
        int readLength = Math.min(length, Array.getLength(bufferArray));
        switch (bufferArray) {
            case byte  [] a -> readBulk(a, readLength, Integer.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = (byte) readAsIntArray(buffer.array, i); });
            case short [] a -> readBulk(a, readLength, Integer.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = (short) readAsIntArray(buffer.array, i); });
            case int   [] a -> readInts(a, readLength);
            case long  [] a -> readBulk(a, readLength, Integer.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = readAsIntArray(buffer.array, i); });
            case float [] a -> readBulk(a, readLength, Integer.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = readAsIntArray(buffer.array, i); });
            case double[] a -> readBulk(a, readLength, Integer.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = readAsIntArray(buffer.array, i); });
            default -> throw new IllegalArgumentException("Unsupported array type: " + bufferArray.getClass().getSimpleName());
        }
        in.skipNBytes((long) (length - readLength) * TagType.INT.getSize());
    }

    private void readLongArray(int length, Object bufferArray) throws IOException {
        checkState(TagType.LONG_ARRAY);
        int readLength = Math.min(length, Array.getLength(bufferArray));
        switch (bufferArray) {
            case byte  [] a -> readBulk(a, readLength, Long.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = (byte) readAsLongArray(buffer.array, i); });
            case short [] a -> readBulk(a, readLength, Long.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = (short) readAsLongArray(buffer.array, i); });
            case int   [] a -> readBulk(a, readLength, Long.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = (int) readAsLongArray(buffer.array, i); });
            case long  [] a -> readLongs(a, readLength);
            case float [] a -> readBulk(a, readLength, Long.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = readAsLongArray(buffer.array, i); });
            case double[] a -> readBulk(a, readLength, Long.BYTES, (buffer, target, offs, len) -> { for (int i = 0; i < len; i++) target[offs + i] = readAsLongArray(buffer.array, i); });
            default -> throw new IllegalArgumentException("Unsupported array type: " + bufferArray.getClass().getSimpleName());
        }
        in.skipNBytes((long) (length - readLength) * TagType.LONG.getSize());
    }

    /**
     * Reads the entire next element and returns it as a raw nbt-data byte-array.
     */
    public byte[] raw() throws IOException {
        TagType type = peek();
        String name = name();

        log.startLog();

        // write tag-id and name back into log
        DataOutputStream dOut = new DataOutputStream(log.log);
        dOut.write(type.getId());
        dOut.writeUTF(name);
        dOut.flush();

        // skip element, writing it into the log
        skip();

        return log.stopLog();
    }

    /**
     * Skips over the next element.
     */
    public void skip() throws IOException {
        skip(0);
    }

    /**
     * Skips over the next element.
     * @param out The number of nesting-levels it should skip out of.<br>
     *            E.g. If this is 1 this will skip until the end of the current Compound or List and consume the end.
     */
    public void skip(int out) throws IOException {
        if (out < 0) throw new IllegalArgumentException("'out' can not be negative!");
        if (out == 0 && peek() == TagType.END) throw new IllegalStateException("Can not skip END tag!");

        do {
            TagType type = peek();
            switch (type) {

                case END: {
                    if (inList()) endList();
                    else endCompound();
                    out--;
                    break;
                }

                case BYTE:
                case SHORT:
                case INT:
                case LONG:
                case FLOAT:
                case DOUBLE: {
                    checkState();
                    in.skipNBytes(type.getSize());
                    next();
                    break;
                }

                case STRING: {
                    checkState();
                    skipUTF();
                    next();
                    break;
                }

                case BYTE_ARRAY: {
                    checkState();
                    long length = readArrayLength();
                    in.skipNBytes(TagType.BYTE.getSize() * length);
                    next();
                    break;
                }

                case INT_ARRAY: {
                    checkState();
                    long length = readArrayLength();
                    in.skipNBytes(TagType.INT.getSize() * length);
                    next();
                    break;
                }

                case LONG_ARRAY: {
                    checkState();
                    long length = readArrayLength();
                    in.skipNBytes(TagType.LONG.getSize() * length);
                    next();
                    break;
                }

                case COMPOUND: {
                    beginCompound();
                    out++;
                    break;
                }

                case LIST: {
                    int length = beginList();
                    if (length < 0) length = 0;
                    TagType listType = peek();
                    out++;

                    // fast skip list if type size is known
                    if (listType.getSize() != -1) {
                        in.skipNBytes((long) listType.getSize() * length);
                        listStack[stackPosition] = 0;
                        stack[stackPosition] = TagType.END;
                    }

                    break;
                }

            }
        } while (out > 0);
    }

    public int remainingListItems() {
        return listStack[stackPosition];
    }

    public boolean inCompound() {
        return stackPosition > 0 && stack[stackPosition - 1] == TagType.COMPOUND;
    }

    public boolean inList() {
        return stackPosition > 0 && stack[stackPosition - 1] == TagType.LIST;
    }

    public String path() throws IOException {
        // ensure the name is read
        name();

        StringBuilder sb = new StringBuilder();

        // start with 1 since the 0th element is always the root-compound
        for(int i = 1; i <= stackPosition; i++) {
            if (i > 1) {
                if (stack[i-1] == TagType.LIST) {
                    sb.append('[').append(listStack[i]).append(']');
                } else {
                    sb.append('.').append(nameStack[i]);
                }
            } else {
                sb.append(nameStack[i]);
            }
        }
        return sb.toString();
    }

    private void next() {
        if (inList()) {
            listStack[stackPosition]--;
            if (listStack[stackPosition] == 0)
                stack[stackPosition] = TagType.END;
        } else {
            stack[stackPosition] = null;
            nameStack[stackPosition] = null;
        }
    }

    private void advanceStack() {
        stackPosition++;

        if (stackPosition == stack.length) {
            int newLength = stack.length * 2;
            stack = Arrays.copyOf(stack, newLength);
            nameStack = Arrays.copyOf(nameStack, newLength);
            listStack = Arrays.copyOf(listStack, newLength);
        }

        stack[stackPosition] = null;
        nameStack[stackPosition] = null;
        listStack[stackPosition] = 0;
    }

    private void reduceStack() {
        if (stackPosition == 0)
            throw new IllegalStateException("Can not reduce empty stack!");

        stackPosition--;
    }

    private TagType readTag() throws IOException {
        int tagId = in.read();
        if (tagId == -1) throw new EOFException();
        return TagType.forId(tagId);
    }

    private void skipUTF() throws IOException {
        int length = in.readUnsignedShort();
        in.skipNBytes(length);
    }

    private void checkState() throws IOException {
        checkState(null);
    }

    private void checkState(@Nullable TagType expected) throws IOException {
        TagType type = peek();
        if (expected != null && type != expected)
            throw new IllegalStateException("Expected type " + expected + " but got " + peek() + ". At: " + path());

        // skip name if it has not been read yet to make sure we are ready read the value
        if (nameStack[stackPosition] == null) {
            nameStack[stackPosition] = UNKNOWN_NAME;
            if (type != TagType.END) skipUTF();
        }
    }

    private void readInts(int[] target, int length) throws IOException {
        readBulk(target, length, Integer.BYTES, (buffer, t, offs, len) -> buffer.ints.get(0, t, offs, len));
    }

    private void readLongs(long[] target, int length) throws IOException {
        readBulk(target, length, Long.BYTES, (buffer, t, offs, len) -> buffer.longs.get(0, t, offs, len));
    }

    private <T> void readBulk(T target, int length, int elementSize, BufferAction<T> transfer) throws IOException {
        if (length <= 0) return;

        ArrayBuffer buffer = buffer();
        int read = 0;
        int bufferLength = Math.floorDiv(ArrayBuffer.CAPACITY, elementSize);

        while (read < length) {
            int readLength = Math.min(length - read, bufferLength);
            in.readFully(buffer.array, 0, readLength * elementSize);
            transfer.perform(buffer, target, read, readLength);
            read += readLength;
        }
    }

    private static boolean isArrayType(TagType type) {
        return  type == TagType.BYTE_ARRAY ||
                type == TagType.INT_ARRAY ||
                type == TagType.LONG_ARRAY;
    }

    private static final VarHandle BYTE_TO_INT_ARRAY = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle BYTE_TO_LONG_ARRAY = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

    private static int readAsIntArray(byte[] buffer, int index) {
        return (int) BYTE_TO_INT_ARRAY.get(buffer, index << 2);
    }

    private static long readAsLongArray(byte[] buffer, int index) {
        return (long) BYTE_TO_LONG_ARRAY.get(buffer, index << 3);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    private ArrayBuffer buffer() {
        if (buffer == null) buffer = new ArrayBuffer();
        return buffer;
    }

    private interface BufferAction<T> {
        void perform(ArrayBuffer buffer, T target, int offset, int length) throws IOException;
    }

}
