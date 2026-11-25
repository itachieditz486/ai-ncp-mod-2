package com.ravex.civilization.npc;

/** Pure math used by Yassine's flight/combat (no Minecraft classes, so it can be unit tested). */
public final class FlightMath {
    public static float wrapDegrees(float d) {
        d = d % 360.0F;
        if (d >= 180.0F) d -= 360.0F;
        if (d < -180.0F) d += 360.0F;
        return d;
    }

    /** Move 'current' toward 'target' by at most 'step' degrees along the shortest arc. */
    public static float approach(float current, float target, float step) {
        float diff = wrapDegrees(target - current);
        if (diff > step) diff = step;
        if (diff < -step) diff = -step;
        return current + diff;
    }

    /** Minecraft yaw (degrees) that faces the offset (dx, dz). */
    public static float yawTo(double dx, double dz) {
        return (float) (Math.atan2(-dx, dz) * (180.0 / Math.PI));
    }

    /** Minecraft pitch (negative = up) needed to reach a point dy above/below at horizontal distance h. Clamped to flyable angles. */
    public static float pitchTo(double dy, double horizontal) {
        float p = (float) (-Math.atan2(dy, Math.max(horizontal, 0.01)) * (180.0 / Math.PI));
        return Math.max(-50.0F, Math.min(45.0F, p));
    }

    /** Glide pitch while landing: steep when high, flat near the ground (flare). */
    public static float landingPitch(int groundDistance) {
        return groundDistance > 14 ? 28.0F : groundDistance > 5 ? 10.0F : -6.0F;
    }

    /** Vanilla mace smash bonus: 4/block for the first 3 blocks fallen, 2/block for the next 5, 1/block after. */
    public static float maceBonus(float fall) {
        if (fall <= 0) return 0;
        if (fall <= 3) return 4 * fall;
        if (fall <= 8) return 12 + 2 * (fall - 3);
        return 22 + (fall - 8);
    }

    private FlightMath() {}
}
