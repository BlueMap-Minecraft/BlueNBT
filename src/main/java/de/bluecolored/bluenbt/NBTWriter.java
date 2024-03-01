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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

public class NBTWriter implements Closeable {

    private final DataOutputStream out;

    private int stackPosition = 0;
    private TagType[] stack = new TagType[32];
    @Nullable private String nextName = null;
    private int nextListLength = -1;

    public NBTWriter(@NotNull OutputStream out) {
        Objects.requireNonNull(out);
        if (out instanceof DataOutputStream)
            this.out = (DataOutputStream) out;
        else
            this.out = new DataOutputStream(out);
    }

    @Contract("_ -> this")
    public NBTWriter name(String name) {
        if (this.nextName != null)
            throw new IllegalStateException("The name was already set to '" + name + "'");

        this.nextName = name;
        return this;
    }

    public void beginCompound() throws IOException {
        tag(TagType.COMPOUND);
        advanceStack();
    }

    public void beginList(int length) throws IOException {
        tag(TagType.LIST);
        advanceStack();
        this.nextListLength = length;
    }

    public void endCompound() throws IOException {
        if (!inCompound()) throw new IllegalStateException("Not in a compound!");
        reduceStack();
        tag(TagType.END);
        afterValue();
    }

    public void endList() throws IOException {
        if (!inList()) throw new IllegalStateException("Not in a list!");
        reduceStack();
        afterValue();
    }

    public void value(byte value) throws IOException {
        tag(TagType.BYTE);
        out.writeByte(value);
        afterValue();
    }

    public void value(short value) throws IOException {
        tag(TagType.SHORT);
        out.writeShort(value);
        afterValue();
    }

    public void value(int value) throws IOException {
        tag(TagType.INT);
        out.writeInt(value);
        afterValue();
    }

    public void value(long value) throws IOException {
        tag(TagType.LONG);
        out.writeLong(value);
        afterValue();
    }

    public void value(float value) throws IOException {
        tag(TagType.FLOAT);
        out.writeFloat(value);
        afterValue();
    }

    public void value(double value) throws IOException {
        tag(TagType.DOUBLE);
        out.writeDouble(value);
        afterValue();
    }

    public void value(String value) throws IOException {
        tag(TagType.STRING);
        out.writeUTF(value);
        afterValue();
    }

    public void value(byte[] value) throws IOException {
        value(value, 0, value.length);
    }

    public void value(byte[] value, int off, int len) throws IOException {
        tag(TagType.BYTE_ARRAY);
        out.writeInt(value.length);
        out.write(value, off, len);
        afterValue();
    }

    public void value(int[] value) throws IOException {
        value(value, 0, value.length);
    }

    public void value(int[] value, int off, int len) throws IOException {
        tag(TagType.INT_ARRAY);
        out.writeInt(value.length);
        int lim = off + len;
        for (int i = off; i < lim; i++)
            out.writeInt(value[i]);
        afterValue();
    }

    public void value(long[] value) throws IOException {
        value(value, 0, value.length);
    }

    public void value(long[] value, int off, int len) throws IOException {
        tag(TagType.LONG_ARRAY);
        out.writeInt(value.length);
        int lim = off + len;
        for (int i = off; i < lim; i++)
            out.writeLong(value[i]);
        afterValue();
    }

    public boolean inCompound() {
        return stackPosition > 0 && stack[stackPosition - 1] == TagType.COMPOUND;
    }

    public boolean inList() {
        return stackPosition > 0 && stack[stackPosition - 1] == TagType.LIST;
    }

    private void tag(TagType tag) throws IOException {
        // init list if pending
        if (nextListLength != -1) {
            out.write(tag.getId());
            out.writeInt(nextListLength);
            stack[stackPosition] = tag;
            nextListLength = -1;
            return;
        }

        if (tag != TagType.END && stack[stackPosition] != null) {
            if (stack[stackPosition] == tag) {
                if (this.nextName != null)
                    throw new IllegalStateException("There is a name set. You can't use name() when writing a value inside a list or before end()!");
                return;
            }
            throw new IllegalStateException("Wrong tag-type. Expected type " + stack[stackPosition] + " but got " + tag);
        }
        stack[stackPosition] = tag;

        out.write(tag.getId());

        if (tag != TagType.END && !inList()) {
            if (this.nextName == null) {
                if (stackPosition > 0)
                    throw new IllegalStateException("Name is not set. Call name() before writing a value when not inside a list!");
                this.nextName = ""; // default name to empty string if at root-level
            }

            out.writeUTF(this.nextName);
            this.nextName = null;
        } else if (this.nextName != null) {
            throw new IllegalStateException("There is a name set. You can't use name() when writing a value inside a list or before end()!");
        }
    }

    private void afterValue() {
        if (!inList())
            stack[stackPosition] = null;
    }

    private void advanceStack() {
        stackPosition++;

        if (stackPosition >= stack.length) {
            int newLength = stack.length * 2;
            stack = Arrays.copyOf(stack, newLength);
        }

        stack[stackPosition] = null;
    }

    private void reduceStack() {
        if (stackPosition == 0)
            throw new IllegalStateException("Can not reduce empty stack!");

        stackPosition--;
    }

    @Override
    public void close() throws IOException {
        out.close();

        if (stackPosition > 0)
            throw new IOException("Incomplete document!");
    }

}
