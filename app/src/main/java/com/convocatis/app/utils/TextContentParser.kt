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
        val totalPages: Int,
        val staticHeader: String? = null,  // Static header shown on all repetition pages
        val repetitionIndex: Int? = null,   // Current repetition (1-based)
        val totalRepetitions: Int? = null   // Total number of repetitions
    )

    /**
     * Parse text entity into pages, handling repetitions specially
     */
    suspend fun parseToPages(text: TextEntity): List<Page> = withContext(Dispatchers.Default) {
        try {
            val rawPages = text.rawContent.split("|")
            val allPages = mutableListOf<Page>()
            var globalPageNumber = 1

            rawPages.forEach { rawPage ->
                if (rawPage.isNotBlank()) {
                    val trimmed = rawPage.trim()

                    // Check if this raw page contains a header (>>...<<)
                    val headerMatch = Regex(""">>(.+?)<<""", RegexOption.DOT_MATCHES_ALL).find(trimmed)

                    if (headerMatch != null) {
                        // This page contains a header
                        val headerText = headerMatch.groupValues[1].trim()
                        val headerStart = headerMatch.range.first
                        val headerEnd = headerMatch.range.last + 1

                        // Content BEFORE header
                        val contentBeforeHeader = trimmed.substring(0, headerStart).trim()

                        // Content AFTER header
                        val contentAfterHeader = if (headerEnd < trimmed.length) {
                            trimmed.substring(headerEnd).trim()
                        } else ""

                        // If there's content before header, create a separate page for it
                        if (contentBeforeHeader.isNotEmpty()) {
                            val processed = processPage(contentBeforeHeader, depth = 0)
                            allPages.add(
                                Page(
                                    pageNumber = globalPageNumber++,
                                    content = processed,
                                    totalPages = 0
                                )
                            )
                        }

                        // Now handle the header + content after it
                        // Check if content after header has repetitions
                        val repetitionMatch = Regex("""^\s*(\d+)\^(.+?)$""", RegexOption.DOT_MATCHES_ALL)
                            .find(contentAfterHeader)

                        if (repetitionMatch != null) {
                            // Header with repetitions
                            val count = repetitionMatch.groupValues[1].toIntOrNull() ?: 1
                            val repeatedContent = repetitionMatch.groupValues[2]

                            // Process the repeated content
                            val processedContent = if (repeatedContent.startsWith("%")) {
                                resolveReferences(repeatedContent, depth = 0)
                            } else {
                                processPage(repeatedContent, depth = 0)
                            }

                            // Create N pages with static header
                            repeat(count) { index ->
                                allPages.add(
                                    Page(
                                        pageNumber = globalPageNumber++,
                                        content = processedContent,
                                        totalPages = 0,
                                        staticHeader = headerText,
                                        repetitionIndex = index + 1,
                                        totalRepetitions = count
                                    )
                                )
                            }
                        } else if (contentAfterHeader.isNotEmpty()) {
                            // Header with regular content (no repetitions)
                            val processed = processPage(contentAfterHeader, depth = 0)
                            allPages.add(
                                Page(
                                    pageNumber = globalPageNumber++,
                                    content = processed,
                                    totalPages = 0,
                                    staticHeader = headerText,
                                    repetitionIndex = null,
                                    totalRepetitions = null
                                )
                            )
                        } else {
                            // Header only, no content after
                            allPages.add(
                                Page(
                                    pageNumber = globalPageNumber++,
                                    content = "<h2>$headerText</h2>",
                                    totalPages = 0
                                )
                            )
                        }
                    } else {
                        // No header - check if it's a repetition or regular page
                        val repetitionMatch = Regex("""^(\d+)\^(.+?)$""", RegexOption.DOT_MATCHES_ALL).find(trimmed)

                        if (repetitionMatch != null) {
                            // Repetition without header
                            val count = repetitionMatch.groupValues[1].toIntOrNull() ?: 1
                            val repeatedContent = repetitionMatch.groupValues[2]

                            val processedContent = if (repeatedContent.startsWith("%")) {
                                resolveReferences(repeatedContent, depth = 0)
                            } else {
                                processPage(repeatedContent, depth = 0)
                            }

                            repeat(count) { index ->
                                allPages.add(
                                    Page(
                                        pageNumber = globalPageNumber++,
                                        content = processedContent,
                                        totalPages = 0,
                                        staticHeader = null,
                                        repetitionIndex = index + 1,
                                        totalRepetitions = count
                                    )
                                )
                            }
                        } else {
                            // Regular page, no header, no repetitions
                            val processed = processPage(trimmed, depth = 0)
                            allPages.add(
                                Page(
                                    pageNumber = globalPageNumber++,
                                    content = processed,
                                    totalPages = 0
                                )
                            )
                        }
                    }
                }
            }

            // Update totalPages for all pages
            allPages.map { it.copy(totalPages = allPages.size) }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing text: ${text.title}", e)
            // Return single page with raw content on error
            listOf(Page(1, text.rawContent, 1))
        }
    }

    /**
     * Extract header from content if present
     */
    private fun extractHeaderFromContent(content: String): String? {
        val headerMatch = Regex("""<h2>(.+?)</h2>""", RegexOption.DOT_MATCHES_ALL).find(content)
        return headerMatch?.groupValues?.get(1)
    }

    /**
     * Check if page contains only a header (no other significant content)
     */
    private fun isPageOnlyHeader(content: String): Boolean {
        // Remove all h2 tags
        val withoutHeader = content.replace(Regex("""<h2>.+?</h2>""", RegexOption.DOT_MATCHES_ALL), "")

        // Remove HTML tags and whitespace
        val stripped = withoutHeader
            .replace(Regex("""<[^>]+>"""), "")
            .replace(Regex("""\s+"""), "")

        // If there's very little or no content left, it's only a header
        return stripped.length < 10
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
        // Use DOT_MATCHES_ALL so . matches newlines too
        result = result.replace(Regex(""">>(.+?)<<""", RegexOption.DOT_MATCHES_ALL)) { match ->
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
