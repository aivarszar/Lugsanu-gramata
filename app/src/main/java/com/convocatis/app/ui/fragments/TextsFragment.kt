package com.convocatis.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.convocatis.app.ConvocatisApplication
import com.convocatis.app.MainActivity
import com.convocatis.app.R
import com.convocatis.app.database.entity.TextEntity
import com.convocatis.app.ui.dialogs.CategoryFilterDialog
import com.convocatis.app.utils.FavoritesManager
import com.convocatis.app.utils.TextTypesParser
import kotlinx.coroutines.launch

class TextsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TextsAdapter
    private lateinit var favoritesManager: FavoritesManager
    private lateinit var prefs: android.content.SharedPreferences

    private var sortAscending = true
    private var showOnlyFavorites = false
    private var searchTerm = ""
    private var currentFilter: TextTypesParser.CategoryFilter = TextTypesParser.CategoryFilter.all()

    companion object {
        private const val PREF_LAST_FILTER_TYPE = "last_filter_type"
        private const val PREF_LAST_FILTER_CODE = "last_filter_code"
        private const val PREF_LAST_FILTER_DISPLAY = "last_filter_display"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_texts, container, false)

        favoritesManager = FavoritesManager(requireContext())
        prefs = requireContext().getSharedPreferences("convocatis_prefs", android.content.Context.MODE_PRIVATE)

        // Restore last filter from SharedPreferences
        restoreLastFilter()

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = TextsAdapter(
            favoritesManager = favoritesManager,
            onItemClick = { textEntity ->
                (activity as? MainActivity)?.showTextReadingFragment(textEntity)
            },
            onFavoriteClick = { textEntity ->
                favoritesManager.toggleFavorite(textEntity.rid)
                loadTexts() // Refresh list
            }
        )
        recyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load texts from database
        loadTexts()

        // Listen for search from MainActivity
        (activity as? MainActivity)?.onSearchTermChangedListener = { term ->
            searchTerm = term
            loadTexts()
        }
    }

    private fun loadTexts() {
        // Check if view is still available
        if (!isAdded || view == null) {
            return
        }

        val database = ConvocatisApplication.getInstance().database
        database.textDao().getAllTexts().observe(viewLifecycleOwner) { allTexts ->
            var filteredTexts = allTexts

            // Filter by search term
            if (searchTerm.isNotEmpty()) {
                filteredTexts = filteredTexts.filter {
                    it.title.contains(searchTerm, ignoreCase = true) ||
                    it.rawContent.contains(searchTerm, ignoreCase = true)
                }
            }

            // Filter by category (hierarchical: Type and/or Code)
            filteredTexts = when {
                // Filter by both Type and Code
                currentFilter.type != null && currentFilter.code != null -> {
                    filteredTexts.filter {
                        it.categoryType == currentFilter.type && it.categoryCode == currentFilter.code
                    }
                }
                // Filter by Type only
                currentFilter.type != null -> {
                    filteredTexts.filter { it.categoryType == currentFilter.type }
                }
                // No filter (show all)
                else -> filteredTexts
            }

            // Sort alphabetically
            filteredTexts = if (sortAscending) {
                filteredTexts.sortedBy { it.title }
            } else {
                filteredTexts.sortedByDescending { it.title }
            }

            // If favorites filter is ON, sort favorites to top (all texts still visible)
            if (showOnlyFavorites) {
                val favoriteRids = favoritesManager.getFavorites()
                filteredTexts = filteredTexts.sortedByDescending { favoriteRids.contains(it.rid) }
            }

            adapter.submitList(filteredTexts)
        }
    }

    fun toggleSort() {
        sortAscending = !sortAscending
        loadTexts()
    }

    fun toggleFavoritesFilter() {
        showOnlyFavorites = !showOnlyFavorites
        loadTexts()
    }

    fun getSortAscending() = sortAscending
    fun getShowOnlyFavorites() = showOnlyFavorites

    /**
     * Show category filter dropdown menu
     */
    fun showCategoryFilterDropdown() {
        // Use toolbar as anchor for dropdown
        val toolbar = (activity as? MainActivity)?.findViewById<androidx.appcompat.widget.Toolbar>(
            com.convocatis.app.R.id.toolbar
        )
        if (toolbar != null) {
            val dropdown = com.convocatis.app.ui.views.CategoryDropdownMenu(
                context = requireContext(),
                anchorView = toolbar,
                onFilterSelected = { filter ->
                    currentFilter = filter
                    saveLastFilter()
                    loadTexts()
                }
            )
            dropdown.setCurrentFilter(currentFilter)
            dropdown.show()
        }
    }










    /**
     * Restore last filter from SharedPreferences
     */
    private fun restoreLastFilter() {
        val type = if (prefs.contains(PREF_LAST_FILTER_TYPE)) {
            prefs.getInt(PREF_LAST_FILTER_TYPE, -1).takeIf { it != -1 }
        } else null

        val code = prefs.getString(PREF_LAST_FILTER_CODE, null)
        val display = prefs.getString(PREF_LAST_FILTER_DISPLAY, "All") ?: "All"

        currentFilter = TextTypesParser.CategoryFilter(type, code, display)
    }

    /**
     * Save current filter to SharedPreferences
     */
    private fun saveLastFilter() {
        prefs.edit().apply {
            currentFilter.type?.let { putInt(PREF_LAST_FILTER_TYPE, it) } ?: remove(PREF_LAST_FILTER_TYPE)
            currentFilter.code?.let { putString(PREF_LAST_FILTER_CODE, it) } ?: remove(PREF_LAST_FILTER_CODE)
            putString(PREF_LAST_FILTER_DISPLAY, currentFilter.displayText)
            apply()
        }
    }
}

class TextsAdapter(
    private val favoritesManager: FavoritesManager,
    private val onItemClick: (TextEntity) -> Unit,
    private val onFavoriteClick: (TextEntity) -> Unit
) : RecyclerView.Adapter<TextsAdapter.ViewHolder>() {

    private var texts = listOf<TextEntity>()

    fun submitList(newTexts: List<TextEntity>) {
        texts = newTexts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_text, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(texts[position])
    }

    override fun getItemCount() = texts.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView: TextView = view.findViewById(R.id.titleText)
        private val favoriteIcon: TextView = view.findViewById(R.id.favoriteIcon)

        fun bind(text: TextEntity) {
            titleView.text = text.title

            // Set favorite icon (dot or star)
            val isFavorite = favoritesManager.isFavorite(text.rid)
            favoriteIcon.text = if (isFavorite) "★" else "●"
            favoriteIcon.setTextColor(
                if (isFavorite) 0xFF000000.toInt() // Black star
                else 0xFF999999.toInt() // Gray dot
            )

            // Click on item -> open text
            itemView.setOnClickListener { onItemClick(text) }

            // Click on star -> toggle favorite
            favoriteIcon.setOnClickListener {
                onFavoriteClick(text)
            }
        }
    }
}
