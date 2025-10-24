package com.convocatis.app.ui.views

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.convocatis.app.utils.TextTypesParser

/**
 * Dropdown menu for hierarchical category filtering (Type -> Code)
 * Two-step process: Shows Types, then Codes for selected Type
 */
class CategoryDropdownMenu(
    private val context: Context,
    private val onFilterSelected: (TextTypesParser.CategoryFilter) -> Unit
) {

    private var currentFilter: TextTypesParser.CategoryFilter? = null
    private val parser = TextTypesParser(context)
    private var currentDialog: AlertDialog? = null

    fun setCurrentFilter(filter: TextTypesParser.CategoryFilter?) {
        currentFilter = filter
    }

    fun show() {
        // Dismiss any existing dialog
        currentDialog?.dismiss()

        // Start with Type selection
        showTypeSelection()
    }

    fun dismiss() {
        currentDialog?.dismiss()
        currentDialog = null
    }

    private fun showTypeSelection() {
        val typeDescriptions = parser.getTypeToDescriptionMap()
        val typeList = mutableListOf<Pair<Int, String>>()

        // Add all types sorted by number (no "All" option)
        typeDescriptions.keys.sorted().forEach { typeNum ->
            typeList.add(Pair(typeNum, typeDescriptions[typeNum] ?: "Type $typeNum"))
        }

        val listView = ListView(context)
        val adapter = TypeAdapter(typeList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val (typeNum, typeDesc) = typeList[position]

            // Filter list immediately by Type
            onFilterSelected(TextTypesParser.CategoryFilter(typeNum, null, typeDesc))

            // Dismiss current dialog
            currentDialog?.dismiss()

            // Show codes for this type
            showCodeSelection(typeNum, typeDesc)
        }

        currentDialog = AlertDialog.Builder(context)
            .setTitle("Select Category")
            .setView(listView)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        currentDialog?.show()
    }

    private fun showCodeSelection(typeNum: Int, typeDescription: String) {
        val hierarchy = parser.getTypeCodeHierarchy()
        val codes = hierarchy[typeNum] ?: emptyList()

        if (codes.isEmpty()) {
            // No subcategories - we're done
            return
        }

        val codeList = codes.toMutableList()

        val listView = ListView(context)
        val adapter = CodeAdapter(codeList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val (code, description) = codeList[position]
            onFilterSelected(TextTypesParser.CategoryFilter(typeNum, code, description))
            currentDialog?.dismiss()
        }

        currentDialog = AlertDialog.Builder(context)
            .setTitle("Select Subcategory")
            .setView(listView)
            .setNegativeButton("Back") { dialog, _ ->
                dialog.dismiss()
                // Type-only filter already applied
            }
            .create()

        currentDialog?.show()
    }

    private inner class TypeAdapter(
        private val types: List<Pair<Int, String>>
    ) : ArrayAdapter<Pair<Int, String>>(
        context,
        android.R.layout.simple_list_item_1,
        types
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)

            val (typeNum, description) = types[position]
            val textView = view.findViewById<TextView>(android.R.id.text1)

            textView.text = description

            // Highlight if this is the current type
            val isSelected = currentFilter?.type == typeNum
            if (isSelected) {
                textView.setTextColor(0xFF1976D2.toInt())
                textView.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                textView.setTextColor(0xFF000000.toInt())
                textView.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            textView.setPadding(40, 32, 40, 32)
            textView.textSize = 16f

            return view
        }
    }

    private inner class CodeAdapter(
        private val codes: List<Pair<String, String>>
    ) : ArrayAdapter<Pair<String, String>>(
        context,
        android.R.layout.simple_list_item_1,
        codes
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)

            val (code, description) = codes[position]
            val textView = view.findViewById<TextView>(android.R.id.text1)

            textView.text = description

            // Highlight if this is the current code
            val isSelected = currentFilter?.code == code
            if (isSelected) {
                textView.setTextColor(0xFF1976D2.toInt())
                textView.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                textView.setTextColor(0xFF000000.toInt())
                textView.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            textView.setPadding(40, 32, 40, 32)
            textView.textSize = 16f

            return view
        }
    }
}
