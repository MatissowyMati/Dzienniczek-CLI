package io.github.matissowymati.dzienniczek.cli

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class McpServerTest {
    @Test
    fun mapsStructuredArgumentsToSafeCliInvocation() {
        val arguments = mcpCliArguments(buildJsonObject {
            put("polecenie", "schedule")
            put("profil", "uczen")
            put("od", "2026-09-01")
            put("do", "2026-09-07")
            put("rozmiarStrony", 100)
            put("skrot", true)
        })

        assertEquals(
            listOf(
                "schedule", "--profile", "uczen", "--from", "2026-09-01", "--to", "2026-09-07",
                "--page-size", "100", "--brief", "--json", "--compact", "--non-interactive",
            ),
            arguments,
        )
    }

    @Test
    fun rejectsMutatingCommandsAndUnsafeFreeFormArguments() {
        assertFalse("logout" in MCP_READ_ONLY_COMMANDS)
        assertFalse("auto-login-token" in MCP_READ_ONLY_COMMANDS)
        assertFalse("accounts" in MCP_READ_ONLY_COMMANDS)
        assertFailsWith<CliError> {
            mcpCliArguments(buildJsonObject { put("polecenie", "logout") })
        }
    }

    @Test
    fun rejectsLimitsOutsideTheDocumentedRange() {
        assertFailsWith<CliError> {
            mcpCliArguments(buildJsonObject {
                put("polecenie", "grades")
                put("rozmiarStrony", 501)
            })
        }
        assertFailsWith<CliError> {
            mcpCliArguments(buildJsonObject {
                put("polecenie", "grades")
                put("limitCzasu", 0)
            })
        }
    }
}
