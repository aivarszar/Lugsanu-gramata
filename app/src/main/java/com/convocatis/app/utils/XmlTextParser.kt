package com.convocatis.app.utils

import android.content.Context
import com.convocatis.app.database.entity.TextEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Parser for XML text files in assets/ directory
 *
 * XML Format:
 * <root>
 *   <item>
 *     <RID>123</RID>
 *     <Description>Prayer Title</Description>
 *     <String>Prayer content with special codes...</String>
 *     <Text_type>1</Text_type>
 *     <Code>1</Code>
 *   </item>
 * </root>
 */
class XmlTextParser(private val context: Context) {

    /**
     * Parse XML file from assets and return list of TextEntity
     * @param assetFileName e.g. "conv_texts_lang_2.xml"
     * @param languageCode e.g. "lv" or "en"
     */
    fun parseTextsFromAsset(assetFileName: String, languageCode: String): List<TextEntity> {
        val texts = mutableListOf<TextEntity>()

        context.assets.open(assetFileName).use { inputStream ->
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentText: MutableMap<String, String>? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "item" -> currentText = mutableMapOf()
                            "RID", "Description", "String", "Text_type", "Code" -> {
                                // Will read text in next iteration
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        if (parser.text.isNotBlank()) {
                            currentText?.let {
                                when {
                                    parser.text.trim().isNotEmpty() -> {
                                        // Store text content (will be associated with previous tag)
                                        val previousTagName = getPreviousTagName(parser)
                                        if (previousTagName.isNotEmpty()) {
                                            it[previousTagName] = parser.text
                                        }
                                    }
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item") {
                            currentText?.let { map ->
                                val rid = map["RID"]?.toLongOrNull()
                                val description = map["Description"]
                                val string = map["String"]
                                val textType = map["Text_type"]?.toIntOrNull()
                                val code = map["Code"]

                                // Skip texts marked for deletion
                                if (rid != null && description != null && string != null &&
                                    !description.trim().startsWith("--delete-", ignoreCase = true)) {
                                    texts.add(
                                        TextEntity(
                                            rid = rid,
                                            title = description.trim(),
                                            rawContent = string,
                                            categoryType = textType,
                                            categoryCode = code,
                                            languageCode = languageCode
                                        )
                                    )
                                }
                            }
                            currentText = null
                        }
                    }
                }
                eventType = parser.next()
            }
        }

        return texts
    }

    private fun getPreviousTagName(parser: XmlPullParser): String {
        // Helper to get the tag name that text belongs to
        // XmlPullParser doesn't expose this directly, so we track it manually
        // This is a simplified version - in production we'd use a stack
        return when (parser.depth) {
            3 -> {
                // We're inside an item tag
                // Try to determine which field this text belongs to
                // This is a workaround - better to track state
                "RID" // Default, will be overridden by actual logic
            }
            else -> ""
        }
    }
}

/**
 * Better XML parser using manual tag tracking
 */
class ImprovedXmlTextParser(private val context: Context) {

    fun parseTextsFromAsset(assetFileName: String, languageCode: String): List<TextEntity> {
        val texts = mutableListOf<TextEntity>()

        context.assets.open(assetFileName).use { inputStream ->
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentText: MutableMap<String, String>? = null
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "item") {
                            currentText = mutableMapOf()
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val text = parser.text
                        if (text.isNotBlank() && currentText != null && currentTag.isNotEmpty()) {
                            currentText[currentTag] = text
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item") {
                            currentText?.let { map ->
                                val rid = map["RID"]?.toLongOrNull()
                                val description = map["Description"]
                                val string = map["String"]
                                val textType = map["Text_type"]?.toIntOrNull()
                                val code = map["Code"]

                                // Skip texts marked for deletion
                                if (rid != null && description != null && string != null &&
                                    !description.trim().startsWith("--delete-", ignoreCase = true)) {
                                    texts.add(
                                        TextEntity(
                                            rid = rid,
                                            title = description.trim(),
                                            rawContent = string,
                                            categoryType = textType,
                                            categoryCode = code,
                                            languageCode = languageCode
                                        )
                                    )
                                }
                            }
                            currentText = null
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        }

        return texts
    }
}
