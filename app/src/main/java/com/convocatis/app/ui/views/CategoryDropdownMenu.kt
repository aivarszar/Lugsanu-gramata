package com.convocatis.app.ui.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.LinearLayout
import com.convocatis.app.database.entity.TextEntity
import com.convocatis.app.utils.TextTypesParser

/**
 * Dropdown menu for hierarchical category filtering (Type -> Code)
 * Shows as a proper dropdown with ListView
 * Automatically hides empty categories and subcategories
 */
class CategoryDropdownMenu(
    private val context: Context,
    private val anchorView: View,
    private val onFilterSelected: (TextTypesParser.CategoryFilter) -> Unit,
    private val availableTexts: List<TextEntity> = emptyList() // For filtering empty categories
) {

    private var popupWindow: PopupWindow? = null
    private var currentFilter: TextTypesParser.CategoryFilter? = null
    private val parser = TextTypesParser(context)
    private var currentTypeNum: Int? = null // Track current type for back navigation

    fun setCurrentFilter(filter: TextTypesParser.CategoryFilter?) {
        currentFilter = filter
    }

    fun show() {
        if (popupWindow?.isShowing == true) {
            popupWindow?.dismiss()
            return
        }

        // If a subcategory (code) is selected, show that type's codes
        // Otherwise show type selection
        if (currentFilter?.code != null && currentFilter?.type != null) {
            val typeDesc = parser.getTypeToDescriptionMap()[currentFilter!!.type] ?: "Category"
            showCodeSelection(currentFilter!!.type!!, typeDesc)
        } else {
            showTypeSelection()
        }
    }

    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    private fun showTypeSelection() {
        val typeDescriptions = parser.getTypeToDescriptionMap()
        val hierarchy = parser.getTypeCodeHierarchy()
        val typeList = mutableListOf<Pair<Int, String>>()

        // Add types that have either:
        // 1. At least one subcategory with texts, OR
        // 2. Texts directly in that type (no code)
        typeDescriptions.keys.sorted().forEach { typeNum ->
            // Check if type has any texts
            val hasTextsInType = availableTexts.any { it.categoryType == typeNum }

            // Check if type has any subcategories with texts
            val subcodes = hierarchy[typeNum] ?: emptyList()
            val hasNonEmptySubcategories = subcodes.any { (code, _) ->
                availableTexts.any { text ->
                    text.categoryType == typeNum &&
                    (text.categoryCode == code || text.categoryCode?.split(",")?.map { it.trim() }?.contains(code) == true)
                }
            }

            // Only add type if it has texts or non-empty subcategories
            if (hasTextsInType || hasNonEmptySubcategories) {
                typeList.add(Pair(typeNum, typeDescriptions[typeNum] ?: "Type $typeNum"))
            }
        }

        // Debug logging
        android.util.Log.d("CategoryDropdown", "Found ${typeList.size} non-empty types: $typeList")

        val contentView = createDropdownView(typeList, true)
        showPopup(contentView)
    }

    private fun showCodeSelection(typeNum: Int, typeDescription: String) {
        val hierarchy = parser.getTypeCodeHierarchy()
        val allCodes = hierarchy[typeNum] ?: emptyList()

        // Filter out subcategories that have no texts
        val codesWithTexts = allCodes.filter { (code, _) ->
            availableTexts.any { text ->
                text.categoryType == typeNum &&
                (text.categoryCode == code || text.categoryCode?.split(",")?.map { it.trim() }?.contains(code) == true)
            }
        }

        if (codesWithTexts.isEmpty()) {
            dismiss()
            return
        }

        currentTypeNum = typeNum
        val contentView = createDropdownView(codesWithTexts, false, typeNum)

        // Dismiss old popup and show new one
        popupWindow?.dismiss()
        showPopup(contentView)
    }

    private fun <T> createDropdownView(items: List<T>, isTypeSelection: Boolean, typeNum: Int? = null): View {
        val containerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            layoutParams = ViewGroup.LayoutParams(
                (context.resources.displayMetrics.widthPixels * 0.75).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Add header with navigation buttons
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(8, 8, 8, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        if (isTypeSelection) {
            // Type selection view: home button to show all
            val homeButton = ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_mylocation) // Home icon
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(48, 48)
                setOnClickListener {
                    currentTypeNum = null
                    onFilterSelected(TextTypesParser.CategoryFilter.all())
                    dismiss()
                }
            }
            headerLayout.addView(homeButton)
        } else {
            // Code selection view: back button (to type selection) + home button (to All)
            val backButton = ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_revert) // Back icon
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(48, 48)
                rotation = 180f // Flip icon to point left
                setOnClickListener {
                    currentTypeNum = null
                    popupWindow?.dismiss()
                    showTypeSelection()
                }
            }
            headerLayout.addView(backButton)

            // Spacer between back and home buttons
            val spacer = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(16, 1)
            }
            headerLayout.addView(spacer)

            val homeButton = ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_mylocation) // Home icon
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(48, 48)
                setOnClickListener {
                    currentTypeNum = null
                    onFilterSelected(TextTypesParser.CategoryFilter.all())
                    dismiss()
                }
            }
            headerLayout.addView(homeButton)
        }

        // Spacer to push close button to right
        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                1,
                1f // weight
            )
        }
        headerLayout.addView(spacer)

        // Close button
        val closeButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(48, 48)
            setOnClickListener { dismiss() }
        }
        headerLayout.addView(closeButton)

        containerLayout.addView(headerLayout)

        // Create ListView
        val listView = ListView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            dividerHeight = 1
        }

        if (isTypeSelection) {
            @Suppress("UNCHECKED_CAST")
            val typeList = items as List<Pair<Int, String>>
            val adapter = TypeAdapter(typeList)
            listView.adapter = adapter

            listView.setOnItemClickListener { _, _, position, _ ->
                val (selectedType, typeDesc) = typeList[position]
                onFilterSelected(TextTypesParser.CategoryFilter(selectedType, null, typeDesc))
                showCodeSelection(selectedType, typeDesc)
            }
        } else {
            @Suppress("UNCHECKED_CAST")
            val codeList = items as List<Pair<String, String>>
            val adapter = CodeAdapter(codeList)
            listView.adapter = adapter

            listView.setOnItemClickListener { _, _, position, _ ->
                val (code, description) = codeList[position]
                onFilterSelected(TextTypesParser.CategoryFilter(typeNum!!, code, description))
                dismiss()
            }
        }

        containerLayout.addView(listView)
        return containerLayout
    }

    private fun showPopup(contentView: View) {
        popupWindow = PopupWindow(
            contentView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
            elevation = 8f
            isOutsideTouchable = true
            isFocusable = true

            // Show below anchor view
            showAsDropDown(anchorView, 0, 0, Gravity.END)
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

            val isSelected = currentFilter?.type == typeNum
            if (isSelected) {
                textView.setTextColor(0xFF1976D2.toInt())
                textView.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                textView.setTextColor(0xFF000000.toInt())
                textView.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            textView.setPadding(40, 24, 40, 24)
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

            val isSelected = currentFilter?.code == code
            if (isSelected) {
                textView.setTextColor(0xFF1976D2.toInt())
                textView.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                textView.setTextColor(0xFF000000.toInt())
                textView.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            textView.setPadding(40, 24, 40, 24)
            textView.textSize = 16f

            return view
        }
    }
}
