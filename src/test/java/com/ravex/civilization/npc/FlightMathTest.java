package com.ravex.civilization.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlightMathTest {
    @Test void wrapAndApproachUseShortestArc() {
        assertEquals(-170.0F, FlightMath.wrapDegrees(190.0F), 0.001F);
        assertEquals(170.0F, FlightMath.wrapDegrees(-190.0F), 0.001F);
        // from 170 to -170 the short way is +20 (through 180), not -340
        assertEquals(180.0F, FlightMath.approach(170.0F, -170.0F, 10.0F), 0.001F);
        assertEquals(15.0F, FlightMath.approach(0.0F, 90.0F, 15.0F), 0.001F);
        assertEquals(-15.0F, FlightMath.approach(0.0F, -90.0F, 15.0F), 0.001F);
        assertEquals(10.0F, FlightMath.approach(8.0F, 10.0F, 15.0F), 0.001F);
    }

    @Test void yawFacesOffsets() {
        assertEquals(0.0F, FlightMath.yawTo(0, 5), 0.01F);      // +Z = south = yaw 0
        assertEquals(-90.0F, FlightMath.yawTo(5, 0), 0.01F);    // +X = east = yaw -90
        assertEquals(90.0F, FlightMath.yawTo(-5, 0), 0.01F);
    }

    @Test void pitchClimbsDivesAndIsClamped() {
        assertTrue(FlightMath.pitchTo(10, 10) < 0, "target above => negative pitch (look up)");
        assertTrue(FlightMath.pitchTo(-10, 10) > 0, "target below => positive pitch (look down)");
        assertEquals(-50.0F, FlightMath.pitchTo(1000, 1), 0.001F);
        assertEquals(45.0F, FlightMath.pitchTo(-1000, 1), 0.001F);
    }

    @Test void landingFlattensNearGround() {
        assertTrue(FlightMath.landingPitch(30) > FlightMath.landingPitch(8));
        assertTrue(FlightMath.landingPitch(8) > FlightMath.landingPitch(2));
        assertTrue(FlightMath.landingPitch(2) < 0, "flare: nose up just above the ground");
    }

    @Test void maceBonusMatchesVanillaFormula() {
        assertEquals(0.0F, FlightMath.maceBonus(0), 0.001F);
        assertEquals(12.0F, FlightMath.maceBonus(3), 0.001F);
        assertEquals(22.0F, FlightMath.maceBonus(8), 0.001F);
        assertEquals(24.0F, FlightMath.maceBonus(10), 0.001F);
    }
}
