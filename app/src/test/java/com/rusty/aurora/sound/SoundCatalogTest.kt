package com.rusty.aurora.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoundCatalogTest {

    private val rain = SoundInfo("rain", "Rain", SoundSource.Asset("sounds/rain.mp3"))
    private val ocean = SoundInfo("ocean_waves", "Ocean Waves", SoundSource.Asset("sounds/ocean_waves.mp3"))
    private val customLullaby = SoundInfo("custom_123", "My Lullaby", SoundSource.CustomUri("content://x/y"))

    @Test
    fun `merges built-in and custom entries`() {
        val merged = SoundCatalog.merge(builtIn = listOf(rain, ocean), custom = listOf(customLullaby))

        assertEquals(listOf(rain, ocean, customLullaby), merged)
    }

    @Test
    fun `built-in entries win an id collision with a custom entry`() {
        val conflictingCustom = SoundInfo("rain", "Someone's Rain", SoundSource.CustomUri("content://x/z"))

        val merged = SoundCatalog.merge(builtIn = listOf(rain), custom = listOf(conflictingCustom))

        assertEquals(listOf(rain), merged)
    }

    @Test
    fun `findById locates an entry by id`() {
        val catalog = listOf(rain, ocean)

        assertEquals(ocean, SoundCatalog.findById(catalog, "ocean_waves"))
        assertNull(SoundCatalog.findById(catalog, "nonexistent"))
    }
}
