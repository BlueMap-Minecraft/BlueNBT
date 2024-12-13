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
import java.lang.reflect.Array;
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
        advanceStack();

        TagType listType = readTag();
        int listLength = in.readInt();

        stack[stackPosition] = listLength == 0 ? TagType.END : listType;
        listStack[stackPosition] = listLength;
        nameStack[stackPosition] = UNKNOWN_NAME;

        return listLength;
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
        next();
        byte[] data = new byte[in.readInt()];
        in.readFully(data);
        return data;
    }

    public int nextByteArray(byte[] buffer) throws IOException {
        checkState(TagType.BYTE_ARRAY);
        next();
        int length = in.readInt();
        int readLength = Math.min(length, buffer.length);
        in.readFully(buffer, 0, readLength);
        skipNBytes(length - readLength);
        return length;
    }

    public int[] nextIntArray() throws IOException {
        checkState(TagType.INT_ARRAY);
        next();
        int[] data = new int[in.readInt()];
        for (int i = 0; i < data.length; i++)
            data[i] = in.readInt();
        return data;
    }

    public int nextIntArray(int[] buffer) throws IOException {
        checkState(TagType.INT_ARRAY);
        next();
        int length = in.readInt();
        int readLength = Math.min(length, buffer.length);
        for (int i = 0; i < readLength; i++)
            buffer[i] = in.readInt();
        skipNBytes((long) (length - readLength) * TagType.INT.getSize());
        return length;
    }

    public long[] nextLongArray() throws IOException {
        checkState(TagType.LONG_ARRAY);
        next();
        long[] data = new long[in.readInt()];
        for (int i = 0; i < data.length; i++)
            data[i] = in.readLong();
        return data;
    }

    public int nextLongArray(long[] buffer) throws IOException {
        checkState(TagType.LONG_ARRAY);
        next();
        int length = in.readInt();
        int readLength = Math.min(length, buffer.length);
        for (int i = 0; i < readLength; i++)
            buffer[i] = in.readLong();
        skipNBytes((long) (length - readLength) * TagType.LONG.getSize());
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
        int length = in.readInt();
        switch (type) {
            case BYTE_ARRAY: readByteArray(length, bufferArray); break;
            case INT_ARRAY: readIntArray(length, bufferArray); break;
            case LONG_ARRAY: readLongArray(length, bufferArray); break;
            default: throw new IllegalStateException("Expected any array-type but got " + peek() + ". At: " + path());
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
        int length = in.readInt();
        A bufferArray = generator.apply(length);
        switch (type) {
            case BYTE_ARRAY: readByteArray(length, bufferArray); break;
            case INT_ARRAY: readIntArray(length, bufferArray); break;
            case LONG_ARRAY: readLongArray(length, bufferArray); break;
            default: throw new IllegalStateException("Expected any array-type but got " + peek() + ". At: " + path());
        }
        next();
        return bufferArray;
    }

    private void readByteArray(int length, Object bufferArray) throws IOException {
        checkState(TagType.BYTE_ARRAY);
        int readLength = Math.min(length, Array.getLength(bufferArray));
        for (int i = 0; i < readLength; i++)
            Array.setByte(bufferArray, i, in.readByte());
        skipNBytes(length - readLength);
    }

    private void readIntArray(int length, Object bufferArray) throws IOException {
        checkState(TagType.INT_ARRAY);
        int readLength = Math.min(length, Array.getLength(bufferArray));
        for (int i = 0; i < readLength; i++)
            Array.setInt(bufferArray, i, in.readInt());
        skipNBytes((long) (length - readLength) * TagType.INT.getSize());
    }

    private void readLongArray(int length, Object bufferArray) throws IOException {
        checkState(TagType.LONG_ARRAY);
        int readLength = Math.min(length, Array.getLength(bufferArray));
        for (int i = 0; i < readLength; i++)
            Array.setLong(bufferArray, i, in.readLong());
        skipNBytes((long) (length - readLength) * TagType.LONG.getSize());
    }

    /**
     * Reads the entire next element and returns it as a raw nbt-data byte-array.
     */
    public byte[] raw() throws IOException {
        checkState();
        log.startLog();

        // write tag-id and name back into log
        DataOutputStream dOut = new DataOutputStream(log.log);
        dOut.write(peek().getId());
        dOut.writeUTF(name());
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
                    skipNBytes(type.getSize());
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
                    long length = in.readInt();
                    skipNBytes(TagType.BYTE.getSize() * length);
                    next();
                    break;
                }

                case INT_ARRAY: {
                    checkState();
                    long length = in.readInt();
                    skipNBytes(TagType.INT.getSize() * length);
                    next();
                    break;
                }

                case LONG_ARRAY: {
                    checkState();
                    long length = in.readInt();
                    skipNBytes(TagType.LONG.getSize() * length);
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
                    TagType listType = peek();
                    out++;

                    // fast skip list if type size is known
                    if (listType.getSize() != -1) {
                        in.skipBytes(listType.getSize() * length);
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
        checkState();
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
        skipNBytes(length);
    }

    /**
     * This method is taken here from a newer version of DataInputStream,
     * to ensure Java 11 compatibility.
     */
    private void skipNBytes(long n) throws IOException {
        while (n > 0) {
            long ns = in.skip(n);
            if (ns > 0 && ns <= n) {
                // adjust number to skip
                n -= ns;
            } else if (ns == 0) { // no bytes skipped
                // read one byte to check for EOS
                if (in.read() == -1) {
                    throw new EOFException();
                }
                // one byte read so decrement number to skip
                n--;
            } else { // skipped negative or too many bytes
                throw new IOException("Unable to skip exactly");
            }
        }
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

    @Override
    public void close() throws IOException {
        in.close();
    }

}
