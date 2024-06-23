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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BlueNBTTest {

    @Test
    public void testBlueNBT() throws IOException {

        BlueNBT blueNBT = new BlueNBT();
        try (InputStream in = NBTReaderTest.class.getResourceAsStream("/level.dat")) {
            assert in != null;

            LevelFile<DataTag> testData = blueNBT.read(new GZIPInputStream(in), new TypeToken<>() {});
            DataTag data = testData.data;

            assertEquals(1, data.getDifficulty());
            assertFalse(data.isDifficultyLocked());
            assertEquals(14590, data.getRainTime());
            assertEquals(1687182273928L, data.getLastPlayed());
            assertEquals(0.2, data.getBorderDamagePerBlock());
            assertEquals("world", data.getLevelName());
        }

    }

    @Test
    public void testBlueNBT2() throws IOException {

        BlueNBT blueNBT = new BlueNBT();
        try (InputStream in = NBTReaderTest.class.getResourceAsStream("/3e6b6179-b774-450e-bd16-e6f24ec7185c.dat")) {
            assert in != null;

            PlayerData playerData = blueNBT.read(new GZIPInputStream(in), PlayerData.class);

            assertEquals(1, playerData.gameMode);
            assertEquals(-7630795211891119996L, playerData.worldUUIDLeast);
            assertEquals(-192242363273884439L, playerData.worldUUIDMost);
            assertEquals(3, playerData.position.length);
            assertEquals(341.356963005719, playerData.position[0], 0.000001);
            assertEquals(96.3574341535291, playerData.position[1], 0.000001);
            assertEquals(436.689526332998, playerData.position[2], 0.000001);

        }

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testObjectAdapter() throws IOException {

        BlueNBT blueNBT = new BlueNBT();
        try (InputStream in = NBTReaderTest.class.getResourceAsStream("/level.dat")) {
            assert in != null;

            Map<String, Object> testData = (Map<String, Object>) blueNBT.read(new GZIPInputStream(in), Object.class);
            Map<String, Object> data = (Map<String, Object>) testData.get("Data");

            assertEquals((byte) 1, data.get("Difficulty"));
            assertEquals((byte) 0, data.get("DifficultyLocked"));
            assertEquals(14590, data.get("rainTime"));
            assertEquals(1687182273928L, data.get("LastPlayed"));
            assertEquals(0.2d, data.get("BorderDamagePerBlock"));
            assertEquals("world", data.get("LevelName"));
        }

    }

    @Test
    public void testArrays() throws IOException {

        BlueNBT blueNBT = new BlueNBT();
        try (InputStream in = loadMcaFileChunk(0, 0)) {
            Chunk chunk = blueNBT.read(in, TypeToken.of(Chunk.class));

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

    @Test
    public void testEnumMap() throws IOException {
        EnumMap<TestEnum, String> testMap = new EnumMap<>(TestEnum.class);
        testMap.put(TestEnum.SOME_TEST, "someTestValue");
        testMap.put(TestEnum.TEST1, "test1Value");
        testMap.put(TestEnum.ABC, "abcValue");

        BlueNBT blueNBT = new BlueNBT();

        byte[] data;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            blueNBT.write(testMap, out, new TypeToken<>() {});
            data = out.toByteArray();
        }

        EnumMap<TestEnum, String> resultMap;
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            resultMap = blueNBT.read(in, new TypeToken<>() {});
        }

        assertEquals(testMap, resultMap);

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

        assert raf.readByte() == 2; // compression byte (2 => deflate-compressed)

        return new InflaterInputStream(new FileInputStream(raf.getFD()));
    }

    private enum TestEnum {
        TEST1,
        SOME_TEST,
        ABC
    }

    @SuppressWarnings({"unused", "MismatchedReadAndWriteOfArray"})
    private static class PlayerData {

        @NBTName("playerGameType")
        private int gameMode;

        @NBTName("Pos")
        private double[] position;

        @NBTName("WorldUUIDLeast")
        private long worldUUIDLeast;

        @NBTName("WorldUUIDMost")
        private long worldUUIDMost;

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
    private static class LevelFile<T> {
        private T data;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class DataTag extends DataTagSuper {
        private long lastPlayed;
        private double borderDamagePerBlock;
        private String levelName;
    }

    @Data
    private static class DataTagSuper {
        private int difficulty;
        private boolean difficultyLocked;
        private int rainTime;
    }

}
