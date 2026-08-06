package io.github.matissowymati.dzienniczek.api.hebe

import kotlinx.serialization.json.Json

internal val apiJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
