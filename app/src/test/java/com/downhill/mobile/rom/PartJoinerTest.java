package com.downhill.mobile.rom;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** The two release parts must join back into a byte identical image, whatever the order. */
public class PartJoinerTest {

    @Test
    public void sortsPartsNaturally() {
        List<String> names = Arrays.asList("Jogo.cso.001", "Jogo.cso.000", "Jogo.cso.010",
                "Jogo.cso.009", "leiame.txt");
        assertEquals(Arrays.asList("Jogo.cso.000", "Jogo.cso.001", "Jogo.cso.009",
                "Jogo.cso.010", "leiame.txt"), PartJoiner.sortedParts(names));
    }

    @Test
    public void suffixIsOnlyANumber() {
        assertEquals(0L, PartJoiner.suffix("a.cso.000"));
        assertEquals(12L, PartJoiner.suffix("a.bin.012"));
        assertEquals(-1L, PartJoiner.suffix("a.cso"));
        assertEquals(-1L, PartJoiner.suffix("a.cso.x1"));
        assertEquals(-1L, PartJoiner.suffix(null));
    }

    @Test
    public void joinedNameKeepsTheReleaseNameAndFallsBackOtherwise() {
        // the numeric suffix is dropped, the ".cso" in the middle is kept
        assertEquals("Downhill_Domination.cso",
                PartJoiner.joinedName(Arrays.asList("Downhill Domination.cso.000")));
        assertEquals(RomStore.CSO_NAME, PartJoiner.joinedName(Arrays.asList("parte-1.bin.000")));
        assertEquals(RomStore.CSO_NAME, PartJoiner.joinedName(Arrays.asList()));
    }

    @Test
    public void joinIsByteExactInTheGivenOrder() throws IOException {
        byte[] whole = new byte[5 * 1024 * 1024 + 12345];
        new Random(99).nextBytes(whole);
        int cut = whole.length / 2;
        byte[] a = Arrays.copyOfRange(whole, 0, cut);
        byte[] b = Arrays.copyOfRange(whole, cut, whole.length);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<String> names = PartJoiner.sortedParts(Arrays.asList("x.cso.001", "x.cso.000"));
        assertEquals("x.cso.000", names.get(0));
        List<InputStream> parts = new ArrayList<>();
        parts.add(new ByteArrayInputStream(a));
        parts.add(new ByteArrayInputStream(b));

        long written = PartJoiner.join(parts, out, null, () -> false);
        assertEquals(whole.length, written);
        assertArrayEquals(whole, out.toByteArray());
    }

    @Test
    public void reportsProgressAndHonoursCancel() {
        byte[] big = new byte[40 * 1024 * 1024];
        try {
            PartJoiner.join(Arrays.asList(new ByteArrayInputStream(big), new ByteArrayInputStream(big)),
                    new ByteArrayOutputStream(), (done, total) -> { }, () -> true);
            fail("cancel must abort");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("cancel"));
        }
    }
}
