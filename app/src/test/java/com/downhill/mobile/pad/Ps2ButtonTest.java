package com.downhill.mobile.pad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class Ps2ButtonTest {

    @Test
    public void allSeventeenPs2ControlsAndFiveExtrasArePresent() {
        int hardware = 0;
        for (Ps2Button b : Ps2Button.values()) {
            if (b.isHardwareKey()) {
                hardware++;
            }
        }
        // D-pad(4) + face(4) + L1/L2/R1/R2(4) + L3/R3/Select/Start(4) = 16 + 2 sticks handled separately
        assertEquals(16, hardware);
        assertEquals(5, Ps2Button.of(Ps2Button.Group.UTILITY).size());
        assertEquals(4, Ps2Button.of(Ps2Button.Group.DPAD).size());
        assertEquals(4, Ps2Button.of(Ps2Button.Group.FACE).size());
        assertEquals(4, Ps2Button.of(Ps2Button.Group.SHOULDER).size());
        assertEquals(4, Ps2Button.of(Ps2Button.Group.SYSTEM).size());
    }

    @Test
    public void everyKeyHasAnEmojiAndAnExplanation() {
        for (Ps2Button b : Ps2Button.values()) {
            assertNotNull(b.name(), b.emoji);
            assertFalse(b.name() + " has no emoji", b.emoji.isEmpty());
            assertFalse(b.name() + " emoji is a broken codepoint", b.emoji.contains("?"));
            int cp = b.emoji.codePointAt(0);
            assertEquals(b.name() + " emoji must be a symbol glyph",
                    Character.OTHER_SYMBOL, Character.getType(cp));
            assertNotNull(b.name() + " has no hint", b.hint);
            assertTrue(b.name() + " hint too short", b.hint.length() > 8);
            assertTrue(b.name() + " has no label", b.shortLabel.length() > 1);
        }
    }

    @Test
    public void emojiCodepointsSurviveTheSourceEncoding() {
        assertEquals(0x2B06, Ps2Button.DPAD_UP.emoji.codePointAt(0));            // arrow up
        assertEquals(0x1F6B4, Ps2Button.CROSS.hint.codePointBefore(Ps2Button.CROSS.hint.length())); // cyclist
        assertEquals(0x23F8, Ps2Button.START.emoji.codePointAt(0));             // pause
        assertEquals(0x1F4BE, Ps2Button.SAVE.emoji.codePointAt(0));             // floppy
        assertEquals(0x1F3AE, Ps2Button.R3.emoji.codePointAt(0));               // gamepad
    }

    @Test
    public void gamepadAndKeyboardCodesAreUnique() {
        Set<Integer> pad = new HashSet<>();
        Set<Integer> kb = new HashSet<>();
        for (Ps2Button b : Ps2Button.values()) {
            if (b.isHardwareKey()) {
                assertTrue("duplicated gamepad code on " + b, pad.add(b.gamepadCode));
                assertTrue("duplicated keyboard code on " + b, kb.add(b.keyboardCode));
                assertTrue("bad keyboard code on " + b, b.keyboardCode > 0);
            } else {
                assertEquals(Ps2Button.Codes.KEY_NONE, b.gamepadCode);
                assertEquals(Ps2Button.Codes.KEY_NONE, b.keyboardCode);
            }
        }
    }

    @Test
    public void androidJoypadConstantsAreUsed() {
        assertEquals(96, Ps2Button.CROSS.gamepadCode);   // KEYCODE_BUTTON_A
        assertEquals(97, Ps2Button.CIRCLE.gamepadCode);  // KEYCODE_BUTTON_B
        assertEquals(99, Ps2Button.SQUARE.gamepadCode);  // KEYCODE_BUTTON_X
        assertEquals(100, Ps2Button.TRIANGLE.gamepadCode); // KEYCODE_BUTTON_Y
        assertEquals(102, Ps2Button.L1.gamepadCode);
        assertEquals(103, Ps2Button.R1.gamepadCode);
        assertEquals(104, Ps2Button.L2.gamepadCode);
        assertEquals(105, Ps2Button.R2.gamepadCode);
        assertEquals(106, Ps2Button.L3.gamepadCode);
        assertEquals(107, Ps2Button.R3.gamepadCode);
        assertEquals(108, Ps2Button.START.gamepadCode);
        assertEquals(109, Ps2Button.SELECT.gamepadCode);
        assertEquals(19, Ps2Button.DPAD_UP.gamepadCode);
        assertEquals(22, Ps2Button.DPAD_RIGHT.gamepadCode);
    }

    @Test
    public void lookupByCode() {
        assertEquals(Ps2Button.CROSS, Ps2Button.byGamepadCode(96));
        assertNull(Ps2Button.byGamepadCode(12345));
    }
}
