package io.github.matissowymati.dzienniczek.cli

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnvironmentTest {
    @Test
    fun loadsLegacyEduVulcanKeysWithoutExposingValues() {
        val file = Files.createTempFile("dzienniczek-env", ".test")
        Files.writeString(
            file,
            "EDUVULCAN_LOGIN=test-user\nEDUVULCAN_PASSWRD=test-password\nDZIENNICZEK_TEST_ONLY=test-value\n",
        )

        Env.load(arrayOf("--env-file", file.toString()))

        assertEquals("eduvulcan", Env.inferredProvider())
        assertEquals("test-user", Env.get("DZIENNICZEK_USERNAME"))
        assertEquals("test-password", Env.get("DZIENNICZEK_PASSWORD"))
        assertTrue(Env.available("DZIENNICZEK_PASSWORD"))
        assertEquals("test-value", Env.get("DZIENNICZEK_TEST_ONLY"))

        Env.load(arrayOf("--no-env"))
        assertFalse(Env.available("DZIENNICZEK_TEST_ONLY"))
        assertEquals(null, Env.loadedPath)
    }
}
