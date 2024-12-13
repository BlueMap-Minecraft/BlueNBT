package de.bluecolored.bluenbt;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

class DataLogInputStream extends InputStream {

    protected final InputStream in;

    protected boolean isLogging;
    protected ByteArrayOutputStream log;

    public DataLogInputStream(InputStream in) {
        this.in = in;
        this.isLogging = false;
        this.log = new ByteArrayOutputStream();
    }

    public void startLog() {
        log.reset();
        isLogging = true;
    }

    public byte[] stopLog() {
        isLogging = false;
        return log.toByteArray();
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (isLogging && b != -1) log.write(b);
        return b;
    }

    @Override
    public int read(byte @NotNull [] b) throws IOException {
        int l = in.read(b);
        if (isLogging && l != -1) log.write(b, 0, l);
        return l;
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        int l = in.read(b, off, len);
        if (isLogging && l != -1) log.write(b, off, l);
        return l;
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        byte[] d = in.readNBytes(len);
        if (isLogging) log.write(d, 0, Math.min(d.length, len));
        return d;
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        int l = in.readNBytes(b, off, len);
        if (isLogging && l != -1) log.write(b, off, l);
        return l;
    }

    @Override
    public long skip(long n) throws IOException {
        if (!isLogging) return in.skip(n);
        return super.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}
