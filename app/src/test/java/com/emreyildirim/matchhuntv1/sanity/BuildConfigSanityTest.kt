package com.emreyildirim.matchhuntv1.sanity

import com.emreyildirim.matchhuntv1.BuildConfig
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue
import org.testng.annotations.Test


class BuildConfigSanityTest {

    @Test
    fun mapsApiKeyMustNotBeBlank() {
        val key = BuildConfig.MAPS_API_KEY
        assertFalse(
            key.isNullOrBlank(),
            "MAPS_API_KEY local.properties uzerinden BuildConfig'e injekte edilmeli"
        )
    }

    @Test
    fun applicationIdMustMatchFirebaseConfig() {
        assertEquals(
            BuildConfig.APPLICATION_ID,
            "com.emreyildirim.matchhuntv1",
            "applicationId Firebase google-services.json ile birebir eslesmeli"
        )
    }

    @Test
    fun mapsApiKeyShouldLookLikeRealKey() {
        val key = BuildConfig.MAPS_API_KEY
        assertTrue(
            key.startsWith("AIza") && key.length >= 35,
            "MAPS_API_KEY beklenen formatta degil; local.properties'i kontrol et"
        )
    }
}
