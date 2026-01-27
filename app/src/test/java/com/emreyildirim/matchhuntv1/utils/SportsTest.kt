package com.emreyildirim.matchhuntv1.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SportsTest {

    @Test
    fun `getSportInfo finds sport by Turkish name`() {
        val info = Sports.getSportInfo("Futbol")

        assertNotNull(info)
        assertEquals("Football", info?.nameEn)
    }

    @Test
    fun `getSportInfo finds sport by English name ignoring case`() {
        val info = Sports.getSportInfo("basketball")

        assertNotNull(info)
        assertEquals("Basketbol", info?.name)
    }

    @Test
    fun `getSportInfo returns null for unknown sport`() {
        val info = Sports.getSportInfo("Chess")

        assertNull(info)
    }

    @Test
    fun `list contains all sports names`() {
        val expectedCount = Sports.allSports.size

        assertEquals(expectedCount, Sports.list.size)
    }
}

























