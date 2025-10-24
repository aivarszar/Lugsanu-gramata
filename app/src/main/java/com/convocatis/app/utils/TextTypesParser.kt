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

        try {
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
                                    val code = map["Code"] ?: ""  // Empty string if null

                                    if (rid != null && description != null) {
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
        } catch (e: Exception) {
            android.util.Log.e("TextTypesParser", "Error parsing types XML", e)
        }

        android.util.Log.d("TextTypesParser", "Parsed ${types.size} total type entities")
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

    /**
     * Get map of type number -> description for unique types
     * Example: 1 -> "Prayers", 5 -> "Songs"
     */
    fun getTypeToDescriptionMap(): Map<Int, String> {
        val types = parseTypesFromAsset()
        val map = mutableMapOf<Int, String>()

        android.util.Log.d("TextTypesParser", "Total types parsed: ${types.size}")

        // Get all types with empty or null codes (these are the parent categories)
        // Prefer longer descriptions (usually more specific) or Latvian language descriptions
        types.forEach { type ->
            android.util.Log.d("TextTypesParser", "Type: ${type.type}, Code: '${type.code}', Desc: ${type.description}")
            if (type.type != null && (type.code == null || type.code.isEmpty())) {
                val currentDesc = map[type.type]
                // Use this description if we don't have one yet, or if it's longer (more specific)
                if (currentDesc == null || type.description.length > currentDesc.length) {
                    map[type.type] = type.description
                    android.util.Log.d("TextTypesParser", "Added type ${type.type}: ${type.description}")
                }
            }
        }

        android.util.Log.d("TextTypesParser", "Final map size: ${map.size}, content: $map")
        return map
    }

    /**
     * Get hierarchical structure: Type -> List of (Code, Description)
     * Example: 1 -> [(1, "Rosary"), (2, "Simple prayers"), ...]
     *          5 -> [(1, "Advent Songs"), (2, "Christmas Songs"), ...]
     */
    fun getTypeCodeHierarchy(): Map<Int, List<Pair<String, String>>> {
        val types = parseTypesFromAsset()
        val hierarchy = mutableMapOf<Int, MutableList<Pair<String, String>>>()

        // Group by type, collect code-description pairs
        types.forEach { type ->
            if (type.type != null && type.code != null && type.code.isNotEmpty()) {
                if (!hierarchy.containsKey(type.type)) {
                    hierarchy[type.type] = mutableListOf()
                }
                // Add unique code-description pairs
                val codeDescPair = Pair(type.code, type.description)
                if (!hierarchy[type.type]!!.contains(codeDescPair)) {
                    hierarchy[type.type]!!.add(codeDescPair)
                }
            }
        }

        // Sort codes within each type
        hierarchy.values.forEach { list ->
            list.sortBy { it.first.toIntOrNull() ?: Int.MAX_VALUE }
        }

        return hierarchy
    }

    /**
     * Data class representing a category filter choice
     */
    data class CategoryFilter(
        val type: Int?,
        val code: String?,
        val displayText: String
    ) {
        companion object {
            fun all() = CategoryFilter(null, null, "All")
        }
    }

    /**
     * Get all available filters in a flat list for selection
     * Includes "All", all Types, and all Type->Code combinations
     */
    fun getAllFilters(): List<CategoryFilter> {
        val filters = mutableListOf<CategoryFilter>()

        // Add "All" option
        filters.add(CategoryFilter.all())

        val typeDescriptions = getTypeToDescriptionMap()
        val hierarchy = getTypeCodeHierarchy()

        // Add types and their codes
        typeDescriptions.keys.sorted().forEach { typeNum ->
            val typeDesc = typeDescriptions[typeNum] ?: "Type $typeNum"

            // Add type-level filter
            filters.add(CategoryFilter(typeNum, null, typeDesc))

            // Add code-level filters under this type
            hierarchy[typeNum]?.forEach { (code, codeDesc) ->
                filters.add(CategoryFilter(typeNum, code, "  → $codeDesc"))
            }
        }

        return filters
    }
}
