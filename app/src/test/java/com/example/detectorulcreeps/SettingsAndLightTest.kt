package com.example.detectorulcreeps

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsAndLightTest {
    @Test fun lightLevels() {
        assertEquals(LightLevel.DARK, lightLevel(0f))
        assertEquals(LightLevel.DARK, lightLevel(9.9f))
        assertEquals(LightLevel.DIM, lightLevel(10f))
        assertEquals(LightLevel.DIM, lightLevel(49f))
        assertEquals(LightLevel.BRIGHT, lightLevel(50f))
        assertEquals(LightLevel.BRIGHT, lightLevel(400f))
    }

    @Test fun thresholdIsClamped() {
        assertEquals(MIN_THRESHOLD, clampThreshold(0.01f), 0f)
        assertEquals(MAX_THRESHOLD, clampThreshold(5f), 0f)
        assertEquals(0.6f, clampThreshold(0.6f), 0f)
        assertEquals(DEFAULT_THRESHOLD, clampThreshold(Float.NaN), 0f)
    }

    @Test fun customThresholdChangesRisk() {
        assertEquals(RiskLevel.LOW, FusionModule(threshold = 0.8f).fuse(0.7f, 0.7f).level)
        assertEquals(RiskLevel.HIGH, FusionModule(threshold = 0.3f).fuse(0.4f, 0.4f).level)
    }
}
