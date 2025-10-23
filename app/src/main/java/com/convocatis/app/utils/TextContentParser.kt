package com.convocatis.app.utils

import android.util.Log
import com.convocatis.app.database.entity.TextEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Parses text content with special formatting codes and splits into pages
 *
 * Special codes:
 * - | (pipe) - Page break
 * - |N^text - Repeat text N times
 * - |%RID - Include text with RID
 * - |N^%RID - Include text with RID, N times
 * - >>header<< - Section header (becomes <h2>)
 * - &gt;&gt; / &lt;&lt; - HTML entities for >> and <<
 */
class TextContentParser(
    private val textDao: com.convocatis.app.database.dao.TextDao
) {

    companion object {
        private const val TAG = "TextContentParser"
        private const val MAX_RECURSION_DEPTH = 5
    }

    /**
     * Represents a single page of text
     */
    data class Page(
        val pageNumber: Int,
        val content: String,
        val totalPages: Int
    )

    /**
     * Parse text entity into pages
     */
    suspend fun parseToPages(text: TextEntity): List<Page> = withContext(Dispatchers.Default) {
        try {
            val rawPages = text.rawContent.split("|")
            val processedPages = mutableListOf<String>()

            rawPages.forEach { rawPage ->
                if (rawPage.isNotBlank()) {
                    val processed = processPage(rawPage.trim(), depth = 0)
                    processedPages.add(processed)
                }
            }

            // Create Page objects with numbering
            processedPages.mapIndexed { index, content ->
                Page(
                    pageNumber = index + 1,
                    content = content,
                    totalPages = processedPages.size
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing text: ${text.title}", e)
            // Return single page with raw content on error
            listOf(Page(1, text.rawContent, 1))
        }
    }

    /**
     * Process a single page, resolving all special codes
     */
    private suspend fun processPage(rawPage: String, depth: Int): String {
        if (depth > MAX_RECURSION_DEPTH) {
            Log.w(TAG, "Max recursion depth reached, stopping...")
            return rawPage
        }

        var content = rawPage

        // 1. Resolve repetitions: 10^text or 10^%123
        content = resolveRepetitions(content, depth)

        // 2. Resolve references: %123
        content = resolveReferences(content, depth)

        // 3. Convert headers: >>text<< → <h2>text</h2>
        content = resolveHeaders(content)

        // 4. Basic formatting
        content = applyBasicFormatting(content)

        return content
    }

    /**
     * Resolve repetitions: N^text
     */
    private suspend fun resolveRepetitions(text: String, depth: Int): String {
        val regex = Regex("""(\d+)\^(.+?)(?=\||$)""", RegexOption.DOT_MATCHES_ALL)
        var result = text

        regex.findAll(text).forEach { match ->
            try {
                val count = match.groupValues[1].toIntOrNull() ?: 1
                val content = match.groupValues[2]

                // Process the repeated content (might contain references)
                val processedContent = if (content.startsWith("%")) {
                    resolveReferences(content, depth + 1)
                } else {
                    content
                }

                val repeated = buildString {
                    repeat(count) {
                        append(processedContent)
                        if (it < count - 1) append("\n")
                    }
                }

                result = result.replace(match.value, repeated)
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving repetition: ${match.value}", e)
            }
        }

        return result
    }

    /**
     * Resolve references: %RID
     */
    private suspend fun resolveReferences(text: String, depth: Int): String {
        val regex = Regex("""%(\d+)""")
        var result = text

        regex.findAll(text).forEach { match ->
            try {
                val rid = match.groupValues[1].toLongOrNull()
                if (rid != null) {
                    val referencedText = textDao.getTextByRid(rid)
                    if (referencedText != null) {
                        // Recursively process referenced text (without splitting by |)
                        val processed = processPage(referencedText.rawContent, depth + 1)
                        result = result.replace(match.value, processed)
                    } else {
                        Log.w(TAG, "Referenced text not found: RID=$rid")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving reference: ${match.value}", e)
            }
        }

        return result
    }

    /**
     * Convert headers: >>text<< → <h2>text</h2>
     */
    private fun resolveHeaders(text: String): String {
        var result = text

        // Handle HTML entities first
        result = result.replace("&gt;&gt;", ">>")
        result = result.replace("&lt;&lt;", "<<")

        // Convert >>header<< to <h2>header</h2>
        result = result.replace(Regex(""">>(.+?)<<""")) { match ->
            "<h2>${match.groupValues[1].trim()}</h2>"
        }

        return result
    }

    /**
     * Apply basic HTML formatting
     */
    private fun applyBasicFormatting(text: String): String {
        var result = text

        // Wrap paragraphs (double newlines)
        result = result.replace("\n\n", "</p><p>")

        // Single newlines to <br>
        result = result.replace("\n", "<br>")

        // Wrap in paragraph if not already HTML
        if (!result.contains("<h2>") && !result.contains("<p>")) {
            result = "<p>$result</p>"
        }

        return result
    }
}
