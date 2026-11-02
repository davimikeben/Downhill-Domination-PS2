package com.downhill.mobile.emu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

public class EmulatorTargetTest {

    @Test
    public void prefersArmsx2ThenAetherThenNetherThenRetroArch() {
        assertEquals("armsx2", EmulatorTarget.ALL.get(0).id);
        assertEquals("aethersx2", EmulatorTarget.ALL.get(1).id);
        assertEquals("nethersx2", EmulatorTarget.ALL.get(2).id);
        assertEquals("retroarch", EmulatorTarget.ALL.get(3).id);
    }

    @Test
    public void picksTheBestInstalledCore() {
        assertEquals("armsx2", EmulatorTarget.firstInstalled(
                new HashSet<>(Arrays.asList("com.armsx2.android", "com.retroarch"))).id);
        assertEquals("retroarch", EmulatorTarget.firstInstalled(
                new HashSet<>(Arrays.asList("com.retroarch"))).id);
        assertNull(EmulatorTarget.firstInstalled(new HashSet<>()));
    }

    @Test
    public void everyTargetKnowsItsPackages() {
        for (EmulatorTarget t : EmulatorTarget.ALL) {
            assertFalse(t.id, t.packages.isEmpty());
            assertNotNull(t.label);
            assertFalse(t.label.isEmpty());
            assertFalse(t.tuningNote.isEmpty());
            assertTrue("first package must look like a package name: " + t.packages,
                    t.packages.iterator().next().contains("."));
        }
        assertTrue(EmulatorTarget.allPackages().contains("com.retroarch"));
        assertTrue(EmulatorTarget.allPackages().contains("xyz.aethersx2.android"));
        assertEquals("armsx2", EmulatorTarget.byId("armsx2").id);
        assertNull(EmulatorTarget.byId("nope"));
    }
}
