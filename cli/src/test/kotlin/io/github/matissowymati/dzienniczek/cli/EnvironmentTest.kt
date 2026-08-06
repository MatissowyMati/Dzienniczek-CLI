package io.github.matissowymati.dzienniczek.cli

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnvironmentTest {
    @Test
    fun loadsLegacyEduVulcanKeysWithoutExposingValues() {
        val file = Files.createTempFile("dzienniczek-env", ".test")
        Files.writeString(file, "EDUVULCAN_LOGIN=test-user\nEDUVULCAN_PASSWRD=test-password\n")

        Env.load(arrayOf("--env-file", file.toString()))

        assertEquals("eduvulcan", Env.inferredProvider())
        assertEquals("test-user", Env.get("DZIENNICZEK_USERNAME"))
        assertEquals("test-password", Env.get("DZIENNICZEK_PASSWORD"))
        assertTrue(Env.available("DZIENNICZEK_PASSWORD"))
    }
}
