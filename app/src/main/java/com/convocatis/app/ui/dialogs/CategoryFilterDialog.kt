package com.convocatis.app.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.convocatis.app.R
import com.convocatis.app.utils.TextTypesParser

/**
 * Two-step dialog for hierarchical category filtering (Type -> Code)
 * Step 1: Select Type (or "All")
 * Step 2: Select Code within Type (or dismiss to keep Type-only filter)
 */
class CategoryFilterDialog : DialogFragment() {

    private var onFilterSelected: ((TextTypesParser.CategoryFilter) -> Unit)? = null
    private var currentFilter: TextTypesParser.CategoryFilter? = null

    companion object {
        private const val ARG_CURRENT_TYPE = "current_type"
        private const val ARG_CURRENT_CODE = "current_code"
        private const val ARG_CURRENT_DISPLAY = "current_display"

        fun newInstance(
            currentFilter: TextTypesParser.CategoryFilter? = null,
            onFilterSelected: (TextTypesParser.CategoryFilter) -> Unit
        ): CategoryFilterDialog {
            val dialog = CategoryFilterDialog()
            dialog.onFilterSelected = onFilterSelected

            currentFilter?.let { filter ->
                dialog.arguments = Bundle().apply {
                    filter.type?.let { putInt(ARG_CURRENT_TYPE, it) }
                    filter.code?.let { putString(ARG_CURRENT_CODE, it) }
                    putString(ARG_CURRENT_DISPLAY, filter.displayText)
                }
            }

            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val parser = TextTypesParser(requireContext())

        // Restore current filter from arguments
        currentFilter = arguments?.let { args ->
            val type = if (args.containsKey(ARG_CURRENT_TYPE)) args.getInt(ARG_CURRENT_TYPE) else null
            val code = args.getString(ARG_CURRENT_CODE)
            val display = args.getString(ARG_CURRENT_DISPLAY) ?: "All"
            TextTypesParser.CategoryFilter(type, code, display)
        } ?: TextTypesParser.CategoryFilter.all()

        // Show Type selection dialog (Step 1)
        return createTypeSelectionDialog(parser)
    }

    /**
     * Step 1: Show Type categories and "All" option
     */
    private fun createTypeSelectionDialog(parser: TextTypesParser): Dialog {
        val typeDescriptions = parser.getTypeToDescriptionMap()
        val typeList = mutableListOf<Pair<Int?, String>>()

        // Add "All" option
        typeList.add(Pair(null, "All"))

        // Add all types sorted by number
        typeDescriptions.keys.sorted().forEach { typeNum ->
            typeList.add(Pair(typeNum, typeDescriptions[typeNum] ?: "Type $typeNum"))
        }

        val adapter = TypeAdapter(typeList, currentFilter?.type)
        val listView = ListView(requireContext())
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedType = typeList[position].first

            if (selectedType == null) {
                // User selected "All" - apply and close
                onFilterSelected?.invoke(TextTypesParser.CategoryFilter.all())
                dismiss()
            } else {
                // User selected a Type - show Code selection dialog (Step 2)
                showCodeSelectionDialog(parser, selectedType, typeList[position].second)
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Select Category")
            .setView(listView)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
    }

    /**
     * Step 2: Show Code subcategories for selected Type
     */
    private fun showCodeSelectionDialog(parser: TextTypesParser, selectedType: Int, typeDescription: String) {
        val hierarchy = parser.getTypeCodeHierarchy()
        val codes = hierarchy[selectedType] ?: emptyList()

        if (codes.isEmpty()) {
            // No subcategories - apply Type-only filter
            onFilterSelected?.invoke(TextTypesParser.CategoryFilter(selectedType, null, typeDescription))
            dismiss()
            return
        }

        val codeList = mutableListOf<Pair<String?, String>>()

        // Add "All in this category" option
        codeList.add(Pair(null, "All in $typeDescription"))

        // Add all codes
        codes.forEach { (code, description) ->
            codeList.add(Pair(code, description))
        }

        val adapter = CodeAdapter(codeList, currentFilter?.code)
        val listView = ListView(requireContext())
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedCode = codeList[position].first

            if (selectedCode == null) {
                // User selected "All in this category" - apply Type-only filter
                onFilterSelected?.invoke(TextTypesParser.CategoryFilter(selectedType, null, typeDescription))
            } else {
                // User selected specific Code - apply Type+Code filter
                onFilterSelected?.invoke(
                    TextTypesParser.CategoryFilter(
                        selectedType,
                        selectedCode,
                        codeList[position].second
                    )
                )
            }
            dismiss()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Select Subcategory")
            .setView(listView)
            .setNegativeButton("Back") { _, _ ->
                // User dismissed - apply Type-only filter
                onFilterSelected?.invoke(TextTypesParser.CategoryFilter(selectedType, null, typeDescription))
                dismiss()
            }
            .create()

        dialog.show()
        // Dismiss the Type selection dialog
        this.dialog?.dismiss()
    }

    /**
     * Adapter for Type selection
     */
    private inner class TypeAdapter(
        private val types: List<Pair<Int?, String>>,
        private val currentType: Int?
    ) : ArrayAdapter<Pair<Int?, String>>(
        requireContext(),
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
            val isSelected = typeNum == currentType
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

    /**
     * Adapter for Code selection
     */
    private inner class CodeAdapter(
        private val codes: List<Pair<String?, String>>,
        private val currentCode: String?
    ) : ArrayAdapter<Pair<String?, String>>(
        requireContext(),
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
            val isSelected = code == currentCode
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
