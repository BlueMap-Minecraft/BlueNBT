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
package de.bluecolored.bluenbt.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public enum CompressionType {

    NONE(0, t -> t, t -> t),
    GZIP(1, GZIPOutputStream::new, GZIPInputStream::new),
    ZLIB(2, DeflaterOutputStream::new, InflaterInputStream::new);

    private final byte id;
    private final ExceptionFunction<OutputStream, ? extends OutputStream, IOException> compressor;
    private final ExceptionFunction<InputStream, ? extends InputStream, IOException> decompressor;

    CompressionType(int id,
                    ExceptionFunction<OutputStream, ? extends OutputStream, IOException> compressor,
                    ExceptionFunction<InputStream, ? extends InputStream, IOException> decompressor) {
        this.id = (byte) id;
        this.compressor = compressor;
        this.decompressor = decompressor;
    }

    public byte getID() {
        return id;
    }

    public OutputStream compress(OutputStream out) throws IOException {
        return compressor.accept(out);
    }

    public InputStream decompress(InputStream in) throws IOException {
        return decompressor.accept(in);
    }

    public static CompressionType getFromID(byte id) {
        for (CompressionType c : CompressionType.values()) {
            if (c.id == id) {
                return c;
            }
        }
        return null;
    }
}
