package de.bluecolored.bluenbt;

import lombok.Data;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BlueNBTTest {

    @Test
    public void testBlueNBT() throws IOException {

        BlueNBT blueNBT = new BlueNBT();
        try (InputStream in = NBTReaderTest.class.getResourceAsStream("/testdata.dat")) {
            assert in != null;

            LevelFile testData = blueNBT.read(new GZIPInputStream(in), LevelFile.class);
            DataTag data = testData.data;

            assertEquals(1, data.difficulty);
            assertFalse(data.difficultyLocked);
            assertEquals(14590, data.rainTime);
            assertEquals(1687182273928L, data.lastPlayed);
            assertEquals(0.2, data.borderDamagePerBlock);
            assertEquals("world", data.levelName);
        }

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
