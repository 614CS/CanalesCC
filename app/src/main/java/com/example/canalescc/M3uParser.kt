package com.example.canalescc

data class Channel(
    val name: String,
    val url: String,
    val group: String = "",
    val logo: String = ""
)

object M3uParser {
    private val attr = Regex("([\\w-]+)=\"([^\"]*)\"")

    fun parse(text: String): List<Channel> {
        val out = mutableListOf<Channel>()
        var name: String? = null
        var group = ""
        var logo = ""
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF") -> {
                    val attrs = attr.findAll(line)
                        .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                    group = attrs["group-title"] ?: ""
                    logo = attrs["tvg-logo"] ?: ""
                    name = line.substringAfterLast(",", "").trim()
                        .ifEmpty { attrs["tvg-name"] ?: "Sin nombre" }
                }
                line.isEmpty() || line.startsWith("#") -> Unit
                else -> {
                    out += Channel(name ?: line, line, group, logo)
                    name = null; group = ""; logo = ""
                }
            }
        }
        return out
    }
}
