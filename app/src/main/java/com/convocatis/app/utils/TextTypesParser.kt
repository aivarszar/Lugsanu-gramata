package com.convocatis.app.utils

import android.content.Context
import com.convocatis.app.database.entity.TextTypeEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Parser for conv_texts_types.xml
 */
class TextTypesParser(private val context: Context) {

    fun parseTypesFromAsset(): List<TextTypeEntity> {
        val types = mutableListOf<TextTypeEntity>()

        context.assets.open("conv_texts_types.xml").use { inputStream ->
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentType: MutableMap<String, String>? = null
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "item") {
                            currentType = mutableMapOf()
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val text = parser.text
                        if (text.isNotBlank() && currentType != null && currentTag.isNotEmpty()) {
                            currentType[currentTag] = text
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item") {
                            currentType?.let { map ->
                                val rid = map["RID"]?.toLongOrNull()
                                val description = map["Description"]
                                val type = map["Type"]?.toIntOrNull()
                                val code = map["Code"]

                                if (rid != null && description != null && code != null && code.isNotEmpty()) {
                                    types.add(
                                        TextTypeEntity(
                                            rid = rid,
                                            description = description,
                                            type = type,
                                            code = code
                                        )
                                    )
                                }
                            }
                            currentType = null
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        }

        return types
    }

    /**
     * Get map of code -> description for unique codes
     * Returns first description found for each code
     */
    fun getCodeToDescriptionMap(): Map<String, String> {
        val types = parseTypesFromAsset()
        val map = mutableMapOf<String, String>()

        types.forEach { type ->
            if (type.code != null && !map.containsKey(type.code)) {
                map[type.code] = type.description
            }
        }

        return map
    }
}
