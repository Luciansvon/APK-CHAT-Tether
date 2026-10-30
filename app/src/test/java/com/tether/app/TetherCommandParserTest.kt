package com.tether.app

import com.tether.app.domain.command.TetherCommand
import com.tether.app.domain.command.TetherCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TetherCommandParserTest {

    @Test
    fun testIsCommandDetection() {
        assertTrue(TetherCommandParser.isCommand("/mood"))
        assertTrue(TetherCommandParser.isCommand("   /tug"))
        assertFalse(TetherCommandParser.isCommand("halo /mood"))
        assertFalse(TetherCommandParser.isCommand("selamat pagi"))
    }

    @Test
    fun testParseMoodWithNote() {
        val cmd = TetherCommandParser.parse("/mood sad lagi capek banget hari ini")
        assertTrue(cmd is TetherCommand.Mood)
        val moodCmd = cmd as TetherCommand.Mood
        assertEquals("sad", moodCmd.mood)
        assertEquals("lagi capek banget hari ini", moodCmd.note)
    }

    @Test
    fun testParseMoodWithoutNote() {
        val cmd = TetherCommandParser.parse("/mood happy")
        assertTrue(cmd is TetherCommand.Mood)
        val moodCmd = cmd as TetherCommand.Mood
        assertEquals("happy", moodCmd.mood)
        assertNull(moodCmd.note)
    }

    @Test
    fun testParseTug() {
        val cmdWithoutMsg = TetherCommandParser.parse("/tug")
        assertTrue(cmdWithoutMsg is TetherCommand.Tug)
        assertNull((cmdWithoutMsg as TetherCommand.Tug).message)

        val cmdWithMsg = TetherCommandParser.parse("/tug kangen kamu!")
        assertTrue(cmdWithMsg is TetherCommand.Tug)
        assertEquals("kangen kamu!", (cmdWithMsg as TetherCommand.Tug).message)
    }

    @Test
    fun testParsePeluk() {
        val cmd = TetherCommandParser.parse("/peluk dengerin aku ya lagi sedih")
        assertTrue(cmd is TetherCommand.Peluk)
        assertEquals("dengerin aku ya lagi sedih", (cmd as TetherCommand.Peluk).message)

        val emptyCmd = TetherCommandParser.parse("/peluk")
        assertTrue(emptyCmd is TetherCommand.Unknown)
    }

    @Test
    fun testParseSolusi() {
        val cmd = TetherCommandParser.parse("/solusi enaknya pilih tawaran A atau B?")
        assertTrue(cmd is TetherCommand.Solusi)
        assertEquals("enaknya pilih tawaran A atau B?", (cmd as TetherCommand.Solusi).message)
    }

    @Test
    fun testParseDadu() {
        val cmdDefault = TetherCommandParser.parse("/dadu")
        assertTrue(cmdDefault is TetherCommand.Dadu)
        val daduDefault = cmdDefault as TetherCommand.Dadu
        assertEquals(6, daduDefault.options.size)
        assertTrue(daduDefault.options.contains(daduDefault.chosen))

        val cmdCustom = TetherCommandParser.parse("/dadu Ramen, Nasi Padang, Burger")
        assertTrue(cmdCustom is TetherCommand.Dadu)
        val daduCustom = cmdCustom as TetherCommand.Dadu
        assertEquals(listOf("Ramen", "Nasi Padang", "Burger"), daduCustom.options)
        assertTrue(daduCustom.options.contains(daduCustom.chosen))
    }

    @Test
    fun testSuggestionsFiltering() {
        val suggestions = TetherCommandParser.getSuggestions("/m")
        assertEquals(1, suggestions.size)
        assertEquals("/mood", suggestions[0].command)

        val all = TetherCommandParser.getSuggestions("/")
        assertEquals(5, all.size)
    }
}
