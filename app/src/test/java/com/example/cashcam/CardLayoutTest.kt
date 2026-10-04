package com.example.cashcam

import org.junit.Assert.*
import org.junit.Test

class CardLayoutTest {
    @Test fun squareKeepsItsCurrentSizeAndOtherFormatsFollowThePhoto() {
        val square = cardLayout(1f)
        val landscape = cardLayout(16f / 9f)
        val portrait = cardLayout(9f / 16f)
        assertEquals(1240, square.height)
        assertEquals(1044, landscape.height)
        assertEquals(2211, portrait.height)
        assertEquals(736, square.imageHeight)
        assertTrue(landscape.height < square.height)
        assertTrue(portrait.imageHeight > square.imageHeight)
        assertEquals(960, landscape.imageWidth)
        assertEquals(960, portrait.imageWidth)
        assertEquals(16f / 9f, landscape.imageWidth.toFloat() / landscape.imageHeight, .001f)
        assertEquals(9f / 16f, portrait.imageWidth.toFloat() / portrait.imageHeight, .001f)
        assertTrue(portrait.imageHeight.toFloat() / portrait.height > .75f)
    }
}
