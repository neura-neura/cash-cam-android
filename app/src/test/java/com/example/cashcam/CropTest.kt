package com.example.cashcam

import org.junit.Assert.*
import org.junit.Test

class CropTest {
    @Test fun squareCropKeepsCenterOfLandscapePhoto() {
        assertArrayEquals(floatArrayOf(500f, 0f, 3500f, 3000f), centerCrop(4000, 3000, 1f), .01f)
    }
    @Test fun portraitAndLandscapeCropsStayInsideSource() {
        for (ratio in listOf(16f / 9f, 1f, 9f / 16f)) {
            for ((w, h) in listOf(4000 to 3000, 3000 to 4000)) {
                val r = centerCrop(w, h, ratio)
                assertTrue(r[0] >= 0 && r[1] >= 0 && r[2] <= w && r[3] <= h)
                assertEquals(ratio, (r[2] - r[0]) / (r[3] - r[1]), .001f)
                assertEquals(w / 2f, (r[0] + r[2]) / 2, .01f)
                assertEquals(h / 2f, (r[1] + r[3]) / 2, .01f)
            }
        }
    }
}
