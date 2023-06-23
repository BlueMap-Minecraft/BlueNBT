package de.bluecolored.bluenbt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class NBTReaderTest {

    @Test
    public void testNbtReader() throws IOException {
        InputStream in = NBTReaderTest.class.getResourceAsStream("/testdata.dat");
        assert in != null;

        try (NBTReader reader = new NBTReader(new GZIPInputStream(in))) {
            // root
            assertEquals(TagType.COMPOUND, reader.peek());
            assertEquals("", reader.name());
            reader.beginCompound();

            assertEquals(TagType.COMPOUND, reader.peek());
            assertEquals("Data", reader.name());
            reader.beginCompound();

            assertEquals("Difficulty", reader.name());
            assertEquals(TagType.BYTE, reader.peek());
            assertEquals(1, reader.nextByte());

            assertEquals("thunderTime", reader.name());
            assertEquals(TagType.INT, reader.peek());
            assertEquals(51264, reader.nextInt());

            assertEquals("BorderSize", reader.name());
            assertEquals(TagType.DOUBLE, reader.peek());
            assertEquals(1000, reader.nextDouble());

            assertEquals("LastPlayed", reader.name());
            assertEquals(TagType.LONG, reader.peek());
            assertEquals(1687182273928L, reader.nextLong());

            reader.skip();
            reader.skip();
            reader.skip();
            reader.skip();
            reader.skip();

            assertEquals("version", reader.name());
            assertEquals(TagType.INT, reader.peek());
            assertEquals(19133, reader.nextInt());

            assertEquals("ServerBrands", reader.name());
            assertEquals(TagType.LIST, reader.peek());
            reader.beginList();
            assertEquals(TagType.STRING, reader.peek());
            assertEquals("Paper", reader.nextString());
            assertEquals(TagType.END, reader.peek());
            reader.endList();

            reader.skip();
            reader.skip();
            reader.skip();

            assertEquals("SpawnAngle", reader.name());
            assertEquals(TagType.FLOAT, reader.peek());
            assertEquals(0, reader.nextFloat());

            assertEquals("LevelName", reader.name());
            assertEquals(TagType.STRING, reader.peek());
            assertEquals("world", reader.nextString());

            reader.skip();

            assertEquals("ScheduledEvents", reader.name());
            reader.beginList();
            assertEquals(TagType.END, reader.peek());
            reader.endList();

            reader.skip();
            reader.skip();
            reader.skip();
            reader.skip();
            reader.skip();
            reader.skip();
            reader.skip();
            reader.skip();
            reader.skip();
            reader.skip();
            reader.skip();
            reader.skip();

            assertEquals("WorldGenSettings", reader.name());
            reader.beginCompound();
            assertEquals(0, reader.nextByte());
            assertEquals("generate_features", reader.name());
            reader.skip();
            assertEquals("dimensions", reader.name());
            reader.skip(); // skip over lots of nested compounds
            assertEquals("seed", reader.name());
            assertEquals(TagType.LONG, reader.peek());
            assertEquals(-6450009625622499088L, reader.nextLong());
            assertEquals(TagType.END, reader.peek());
            reader.endCompound(); // end WorldGenSettings compound

            assertEquals("rainTime", reader.name());
            assertEquals(TagType.INT, reader.peek());

            reader.skip(1); // skip over everything until we are out of the DATA compound

            assertEquals(TagType.END, reader.peek());
            reader.endCompound();

            assertThrows(EOFException.class, reader::peek);
        }
    }

}
