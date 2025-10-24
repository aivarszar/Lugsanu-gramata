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
 * Dialog for hierarchical category filtering (Type -> Code)
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
        val filters = parser.getAllFilters()

        // Restore current filter from arguments
        currentFilter = arguments?.let { args ->
            val type = if (args.containsKey(ARG_CURRENT_TYPE)) args.getInt(ARG_CURRENT_TYPE) else null
            val code = args.getString(ARG_CURRENT_CODE)
            val display = args.getString(ARG_CURRENT_DISPLAY) ?: "All"
            TextTypesParser.CategoryFilter(type, code, display)
        } ?: TextTypesParser.CategoryFilter.all()

        // Create adapter for the list
        val adapter = CategoryFilterAdapter(filters, currentFilter)

        val listView = ListView(requireContext())
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedFilter = filters[position]
            onFilterSelected?.invoke(selectedFilter)
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Filter by Category")
            .setView(listView)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
    }

    /**
     * Custom adapter to show hierarchy with indentation and highlight current selection
     */
    private inner class CategoryFilterAdapter(
        private val filters: List<TextTypesParser.CategoryFilter>,
        private val currentFilter: TextTypesParser.CategoryFilter?
    ) : ArrayAdapter<TextTypesParser.CategoryFilter>(
        requireContext(),
        android.R.layout.simple_list_item_1,
        filters
    ) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)

            val filter = filters[position]
            val textView = view.findViewById<TextView>(android.R.id.text1)

            textView.text = filter.displayText

            // Highlight current selection
            val isSelected = currentFilter?.let { current ->
                current.type == filter.type && current.code == filter.code
            } ?: false

            if (isSelected) {
                textView.setTextColor(0xFF1976D2.toInt()) // Blue
                textView.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                textView.setTextColor(0xFF000000.toInt()) // Black
                textView.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            // Apply padding based on hierarchy level
            val paddingLeft = if (filter.displayText.startsWith("  →")) {
                60 // Indent for subcategories
            } else {
                20 // Normal padding for types and "All"
            }
            textView.setPadding(paddingLeft, 16, 20, 16)

            return view
        }
    }
}
