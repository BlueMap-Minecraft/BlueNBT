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
import java.util.Arrays;
import java.util.Objects;

public class NBTReader implements Closeable {
    private static final String SKIPPED_NAME = "<skipped>";

    private final DataInputStream in;

    private int stackPosition = 0;
    private TagType[] stack = new TagType[32];
    private String[] nameStack = new String[32];
    private int[] listStack = new int[32];

    public NBTReader(@NotNull InputStream in) {
        Objects.requireNonNull(in);
        if (in instanceof DataInputStream)
            this.in = (DataInputStream) in;
        else
            this.in = new DataInputStream(in);
    }

    public TagType peek() throws IOException {
        return peek(false);
    }

    private TagType peek(boolean skipName) throws IOException {
        TagType peek = stack[stackPosition];
        if (peek == null) {
            peek = readTag();
            stack[stackPosition] = peek;

            if (peek != TagType.END) {
                if (skipName) {
                    skipUTF();
                    nameStack[stackPosition] = SKIPPED_NAME;
                } else {
                    nameStack[stackPosition] = in.readUTF();
                }
            }
        }
        return peek;
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

        return listLength;
    }

    public void endList() throws IOException {
        checkState(TagType.END);
        if (!inList()) throw new IllegalStateException("Can not end list. Current element is not in a list! At: " + path());
        reduceStack();
        next();
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

    public int[] nextIntArray() throws IOException {
        checkState(TagType.INT_ARRAY);
        next();
        int[] data = new int[in.readInt()];
        for (int i = 0; i < data.length; i++)
            data[i] = in.readInt();
        return data;
    }

    public long[] nextLongArray() throws IOException {
        checkState(TagType.LONG_ARRAY);
        next();
        long[] data = new long[in.readInt()];
        for (int i = 0; i < data.length; i++)
            data[i] = in.readLong();
        return data;
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
        if (peek(true) == TagType.END) throw new IllegalStateException("Can not skip END tag!");

        do {
            TagType type = peek(true);
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
                    in.skipNBytes(type.getSize());
                    next();
                    break;
                }

                case STRING: {
                    skipUTF();
                    next();
                    break;
                }

                case BYTE_ARRAY: {
                    long length = in.readInt();
                    in.skipNBytes(TagType.BYTE.getSize() * length);
                    next();
                    break;
                }

                case INT_ARRAY: {
                    long length = in.readInt();
                    in.skipNBytes(TagType.INT.getSize() * length);
                    next();
                    break;
                }

                case LONG_ARRAY: {
                    long length = in.readInt();
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

    public @Nullable String name() throws IOException {
        peek();
        return nameStack[stackPosition];
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

    public String path() {
        return "<?>"; // TODO
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

    private void checkState(TagType expected) throws IOException {
        if (peek() != expected)
            throw new IllegalStateException("Expected type " + expected + " but got " + peek() + ". At: " + path());
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}
