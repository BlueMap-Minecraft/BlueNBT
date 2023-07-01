package de.bluecolored.bluenbt;

import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluenbt.testutil.CompressionType;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
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
            assertEquals(section.getBlock_states().getData()[0], 1162219257593856L);
            assertEquals(section.getBlock_states().getPalette().length, 26);

            BlockState blockState = section.getBlock_states().getPalette()[6];
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
        if (size == 0) {
            throw new NoSuchElementException("Chunk does not exist");
        }

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
        private BlockStates block_states = new BlockStates();
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
