package com.bangla.voicewriter

object PunctuationEngine {

    /**
     * Formats and cleans spoken Bengali text by normalizing Dari (।),
     * Comma (,), and question marks with proper spacing.
     */
    fun formatBanglaText(rawText: String): String {
        var text = rawText.trim()
        if (text.isEmpty()) return ""

        // Normalize multiple spaces
        text = text.replace(Regex("\\s+"), " ")

        // Fix punctuation spacing: remove space before punctuation, ensure single space after
        text = text.replace(Regex("\\s+([।,?!])"), "$1")
        text = text.replace(Regex("([।,?!])(?=[^\\s।,?!])"), "$1 ")

        // Ensure ending sentence with a Dari (।) if not already punctuated
        if (!text.endsWith("।") && !text.endsWith("?") && !text.endsWith("!")) {
            text += "।"
        }

        return text
    }
}
