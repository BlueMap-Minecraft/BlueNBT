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

import com.google.gson.reflect.TypeToken;
import lombok.Data;
import net.querz.nbt.mca.CompressionType;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BlueNBTTest {

    @Test
    public void testBlueNBT() throws IOException {

        BlueNBT blueNBT = new BlueNBT();
        try (InputStream in = NBTReaderTest.class.getResourceAsStream("/level.dat")) {
            assert in != null;

            LevelFile testData = blueNBT.read(new GZIPInputStream(in), TypeToken.get(LevelFile.class));
            DataTag data = testData.data;

            assertEquals(1, data.difficulty);
            assertFalse(data.difficultyLocked);
            assertEquals(14590, data.rainTime);
            assertEquals(1687182273928L, data.lastPlayed);
            assertEquals(0.2, data.borderDamagePerBlock);
            assertEquals("world", data.levelName);
        }

    }

    @Test
    public void testArrays() throws IOException {

        BlueNBT blueNBT = new BlueNBT();
        try (InputStream in = loadMcaFileChunk(0, 0)) {
            Chunk chunk = blueNBT.read(in, TypeToken.get(Chunk.class));

            assertEquals(25, chunk.getSections().size());

            Section section = chunk.getSections().get(3);
            assertEquals(section.getBlockStates().getData()[0], 1162219257593856L);
            assertEquals(section.getBlockStates().getPalette().length, 26);

            BlockState blockState = section.getBlockStates().getPalette()[6];
            assertEquals(blockState.getProperties().size(), 7);
            assertEquals(blockState.getProperties().get("down"), "true");
            assertEquals(blockState.getProperties().get("east"), "false");
            assertEquals(blockState.getName(), "minecraft:sculk_vein");
        }

    }

    @SuppressWarnings({"resource", "SameParameterValue"})
    private InputStream loadMcaFileChunk(int chunkX, int chunkZ) throws IOException {
        Path regionFile = Files.createTempFile(null, null);
        try (InputStream in = NBTReaderTest.class.getResourceAsStream("/r.0.0.mca")) {
            assert in != null;
            Files.copy(in, regionFile, StandardCopyOption.REPLACE_EXISTING);
        }

        RandomAccessFile raf = new RandomAccessFile(regionFile.toFile(), "r");

        int xzChunk = Math.floorMod(chunkZ, 32) * 32 + Math.floorMod(chunkX, 32);

        raf.seek(xzChunk * 4L);
        int offset = raf.read() << 16;
        offset |= (raf.read() & 0xFF) << 8;
        offset |= raf.read() & 0xFF;
        offset *= 4096;

        int size = raf.readByte() * 4096;
        if (size == 0) return null;

        raf.seek(offset + 4); // +4 skip chunk size

        byte compressionTypeByte = raf.readByte();
        CompressionType compressionType = CompressionType.getFromID(compressionTypeByte);
        if (compressionType == null) {
            throw new IOException("Invalid compression type " + compressionTypeByte);
        }

        return compressionType.decompress(new FileInputStream(raf.getFD()));
    }

    @Data
    private static class Chunk {
        private List<Section> sections = Collections.emptyList();
    }

    @Data
    private static class Section {
        private int y;
        @NBTName("block_states")
        private BlockStates blockStates = new BlockStates();
        private byte[] blockLight = new byte[0];
    }

    @Data
    private static class BlockStates {
        private BlockState[] palette = new BlockState[0];
        private long[] data = new long[0];
    }

    @Data
    private static class BlockState {
        private String name = "minecraft:air";
        private Map<String, String> properties = Collections.emptyMap();
    }

    @Data
    private static class LevelFile {
        private DataTag data;
    }

    @Data
    private static class DataTag {
        private int difficulty;
        private boolean difficultyLocked;
        private int rainTime;
        private long lastPlayed;
        private double borderDamagePerBlock;
        private String levelName;
    }

}
