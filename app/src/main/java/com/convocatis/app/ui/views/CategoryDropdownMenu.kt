package com.convocatis.app.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import com.convocatis.app.R
import com.convocatis.app.utils.TextTypesParser

/**
 * Dropdown menu for hierarchical category filtering (Type -> Code)
 * Two-step process within a single dropdown:
 * Step 1: Shows Types
 * Step 2: After Type selection, shows Codes for that Type
 */
class CategoryDropdownMenu(
    private val context: Context,
    private val anchorView: View,
    private val onFilterSelected: (TextTypesParser.CategoryFilter) -> Unit
) {

    private var popupWindow: PopupWindow? = null
    private var currentFilter: TextTypesParser.CategoryFilter? = null
    private val parser = TextTypesParser(context)

    private var isShowingCodes = false
    private var selectedType: Int? = null
    private var selectedTypeDescription: String? = null

    fun setCurrentFilter(filter: TextTypesParser.CategoryFilter?) {
        currentFilter = filter
    }

    fun show() {
        if (popupWindow?.isShowing == true) {
            popupWindow?.dismiss()
            return
        }

        // Start with Type selection
        showTypeSelection()
    }

    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    private fun showTypeSelection() {
        isShowingCodes = false
        selectedType = null

        val typeDescriptions = parser.getTypeToDescriptionMap()
        val typeList = mutableListOf<Pair<Int, String>>()

        // Add all types sorted by number (no "All" option)
        typeDescriptions.keys.sorted().forEach { typeNum ->
            typeList.add(Pair(typeNum, typeDescriptions[typeNum] ?: "Type $typeNum"))
        }

        val listView = createListView()
        val adapter = TypeAdapter(typeList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val (typeNum, typeDesc) = typeList[position]
            selectedType = typeNum
            selectedTypeDescription = typeDesc

            // Filter list immediately by Type
            onFilterSelected(TextTypesParser.CategoryFilter(typeNum, null, typeDesc))

            // Show codes for this type
            showCodeSelection(typeNum, typeDesc)
        }

        showPopup(listView)
    }

    private fun showCodeSelection(typeNum: Int, typeDescription: String) {
        isShowingCodes = true

        val hierarchy = parser.getTypeCodeHierarchy()
        val codes = hierarchy[typeNum] ?: emptyList()

        if (codes.isEmpty()) {
            // No subcategories - dismiss
            dismiss()
            return
        }

        val codeList = mutableListOf<Pair<String, String>>()

        // Add all codes
        codes.forEach { (code, description) ->
            codeList.add(Pair(code, description))
        }

        val listView = createListView()
        val adapter = CodeAdapter(codeList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val (code, description) = codeList[position]
            onFilterSelected(TextTypesParser.CategoryFilter(typeNum, code, description))
            dismiss()
        }

        // Update popup content
        popupWindow?.dismiss()
        showPopup(listView)
    }

    private fun createListView(): ListView {
        return ListView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dividerHeight = 1
        }
    }

    private fun showPopup(contentView: View) {
        popupWindow?.dismiss()

        popupWindow = PopupWindow(
            contentView,
            600, // width in pixels
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // focusable
        ).apply {
            // Make it dismissible by clicking outside
            isOutsideTouchable = true
            isFocusable = true

            // Show below anchor view
            showAsDropDown(anchorView, 0, 0)
        }
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
