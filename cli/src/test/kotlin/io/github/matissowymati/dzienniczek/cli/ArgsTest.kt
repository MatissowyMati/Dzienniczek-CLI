package io.github.matissowymati.dzienniczek.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ArgsTest {
    @Test
    fun parsesOptionsAnywhereAndRepeatedValues() {
        val args = CliArgs(listOf("--json", "login", "jwt", "--token", "one", "--token=two", "--tenant", "school"))
        assertEquals(listOf("login", "jwt"), args.words)
        assertTrue(args.flag("json"))
        assertEquals(listOf("one", "two"), args.values("token"))
        assertEquals("school", args.value("tenant"))
    }

    @Test
    fun supportsEndOfOptions() {
        val args = CliArgs(listOf("profile", "use", "--", "--odd-name"))
        assertEquals(listOf("profile", "use", "--odd-name"), args.words)
        assertFalse(args.flag("odd-name"))
    }

    @Test
    fun parsesAgentOptionsAndValidatesTimeout() {
        val args = CliArgs(listOf("doctor", "--non-interactive", "--timeout", "45"))
        assertTrue(args.flag("non-interactive"))
        assertEquals(45, args.positiveInt("timeout", 30))
        assertFailsWith<CliError> { CliArgs(listOf("--timeout", "0")).positiveInt("timeout", 30) }
        assertFailsWith<CliError> { CliArgs(listOf("--timeout")).positiveInt("timeout", 30) }
        assertFailsWith<CliError> { CliArgs(listOf("doctor", "--timeout")).validate() }
        assertFailsWith<CliError> { CliArgs(listOf("--last-id", "-1")).nonNegativeInt("last-id", 0) }
    }
}
