package com.tether.app.domain.command

sealed class TetherCommand {
    data class Mood(val mood: String, val note: String?) : TetherCommand()
    data class Tug(val message: String?) : TetherCommand()
    data class Peluk(val message: String) : TetherCommand()
    data class Solusi(val message: String) : TetherCommand()
    data class Dadu(val options: List<String>, val chosen: String) : TetherCommand()
    data class Unknown(val raw: String, val reason: String) : TetherCommand()
}

data class CommandSuggestion(
    val command: String,
    val syntax: String,
    val description: String
)

object TetherCommandParser {

    val ALL_SUGGESTIONS = listOf(
        CommandSuggestion("/mood", "/mood <happy|sad|tired|kangen|fokus|ovt> [catatan]", "Ubah mood & status obrolan"),
        CommandSuggestion("/tug", "/tug [pesan]", "Tarik tali Tether & getaran haptic"),
        CommandSuggestion("/peluk", "/peluk <pesan>", "Mode Peluk (dengerin aja tanpa solusi)"),
        CommandSuggestion("/solusi", "/solusi <pesan>", "Butuh Solusi (minta masukan objektif)"),
        CommandSuggestion("/dadu", "/dadu [opsi1, opsi2, ...]", "Putar dadu anti-'Terserah'")
    )

    fun isCommand(text: String): Boolean {
        return text.trimStart().startsWith("/")
    }

    fun getSuggestions(query: String): List<CommandSuggestion> {
        val trimmed = query.trim()
        if (!trimmed.startsWith("/")) return emptyList()
        val cmdPrefix = trimmed.substringBefore(" ").lowercase()
        return ALL_SUGGESTIONS.filter { it.command.startsWith(cmdPrefix) }
    }

    fun parse(rawText: String): TetherCommand {
        val trimmed = rawText.trim()
        if (!trimmed.startsWith("/")) {
            return TetherCommand.Unknown(rawText, "Bukan format perintah (harus diawali '/')")
        }

        val parts = trimmed.split("\\s+".toRegex(), limit = 2)
        val cmd = parts[0].lowercase()
        val rest = if (parts.size > 1) parts[1].trim() else ""

        return when (cmd) {
            "/mood" -> {
                if (rest.isEmpty()) {
                    TetherCommand.Unknown(trimmed, "Format: /mood <happy|sad|tired|kangen|fokus|ovt> [catatan]")
                } else {
                    val moodParts = rest.split("\\s+".toRegex(), limit = 2)
                    val mood = moodParts[0]
                    val note = if (moodParts.size > 1) moodParts[1] else null
                    TetherCommand.Mood(mood, note)
                }
            }
            "/tug" -> {
                val msg = if (rest.isNotEmpty()) rest else null
                TetherCommand.Tug(msg)
            }
            "/peluk" -> {
                if (rest.isEmpty()) {
                    TetherCommand.Unknown(trimmed, "Format: /peluk <pesan curhat kamu>")
                } else {
                    TetherCommand.Peluk(rest)
                }
            }
            "/solusi" -> {
                if (rest.isEmpty()) {
                    TetherCommand.Unknown(trimmed, "Format: /solusi <masalah yang butuh solusi>")
                } else {
                    TetherCommand.Solusi(rest)
                }
            }
            "/dadu" -> {
                val options = if (rest.isEmpty()) {
                    listOf("1", "2", "3", "4", "5", "6")
                } else {
                    rest.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }

                if (options.isEmpty()) {
                    TetherCommand.Unknown(trimmed, "Format: /dadu [opsi1, opsi2, ...]")
                } else {
                    val chosen = options.random()
                    TetherCommand.Dadu(options, chosen)
                }
            }
            else -> TetherCommand.Unknown(trimmed, "Perintah '$cmd' tidak dikenal")
        }
    }
}
