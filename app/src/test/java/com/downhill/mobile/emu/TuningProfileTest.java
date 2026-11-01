package com.downhill.mobile.emu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TuningProfileTest {

    @Test
    public void lowEndGpuProfileHasTheSettingsThatActuallyMatter() {
        List<TuningProfile.Setting> rows = TuningProfile.forTarget("armsx2");
        assertTrue(rows.size() >= 12);

        boolean resolution = false;
        boolean renderer = false;
        boolean mtvu = false;
        for (TuningProfile.Setting s : rows) {
            if (s.what.contains("Internal Resolution")) {
                resolution = s.value.startsWith("1x");
            }
            if (s.what.equals("Renderer")) {
                renderer = s.value.contains("OpenGL");
            }
            if (s.what.contains("MTVU")) {
                mtvu = s.value.equals("Ligado");
            }
            assertFalse("every row needs a reason", s.why.isEmpty());
            assertFalse(s.where.isEmpty());
        }
        assertTrue("native resolution is mandatory on a Mali-G52", resolution);
        assertTrue("OpenGL renderer expected", renderer);
        assertTrue("MTVU must be on", mtvu);
    }

    @Test
    public void rowsAreUniqueAndTextDumpIsReadable() {
        List<TuningProfile.Setting> rows = TuningProfile.forTarget("armsx2");
        Set<String> keys = new HashSet<>();
        for (TuningProfile.Setting s : rows) {
            assertTrue("duplicated row " + s, keys.add(s.where + "|" + s.what));
        }
        String text = TuningProfile.asText("armsx2", rows);
        assertTrue(text.contains("armsx2"));
        for (TuningProfile.Setting s : rows) {
            assertTrue(text.contains(s.value));
        }
    }

    @Test
    public void retroArchGetsItsOwnProfile() {
        List<TuningProfile.Setting> rows = TuningProfile.forTarget("retroarch");
        assertFalse(rows.isEmpty());
        boolean gl = false;
        for (TuningProfile.Setting s : rows) {
            if (s.what.contains("Video Driver")) {
                gl = s.value.equals("gl");
            }
        }
        assertTrue(gl);
        assertEquals(TuningProfile.forTarget(null).size(), TuningProfile.forTarget("armsx2").size());
    }
}
