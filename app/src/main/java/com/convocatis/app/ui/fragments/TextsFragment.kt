package com.convocatis.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.convocatis.app.ConvocatisApplication
import com.convocatis.app.MainActivity
import com.convocatis.app.R
import com.convocatis.app.database.entity.TextEntity
import com.convocatis.app.utils.FavoritesManager
import kotlinx.coroutines.launch

class TextsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TextsAdapter
    private lateinit var favoritesManager: FavoritesManager
    private lateinit var categorySpinner: Spinner

    private var sortAscending = true
    private var showOnlyFavorites = false
    private var searchTerm = ""
    private var selectedCategoryCode: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_texts, container, false)

        favoritesManager = FavoritesManager(requireContext())

        categorySpinner = view.findViewById(R.id.categoryCodeSpinner)
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

        // Setup category spinner
        setupCategorySpinner()

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

            // Filter by category code
            if (selectedCategoryCode != null && selectedCategoryCode != "all") {
                filteredTexts = filteredTexts.filter { it.categoryCode == selectedCategoryCode }
            }

            // Filter by favorites
            if (showOnlyFavorites) {
                val favoriteRids = favoritesManager.getFavorites()
                filteredTexts = filteredTexts.filter { favoriteRids.contains(it.rid) }
            }

            // Sort
            filteredTexts = if (sortAscending) {
                filteredTexts.sortedBy { it.title }
            } else {
                filteredTexts.sortedByDescending { it.title }
            }

            // Sort favorites first
            val favoriteRids = favoritesManager.getFavorites()
            filteredTexts = filteredTexts.sortedByDescending { favoriteRids.contains(it.rid) }

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

    private fun setupCategorySpinner() {
        lifecycleScope.launch {
            val database = ConvocatisApplication.getInstance().database
            val codes = database.textDao().getUniqueCategoryCodes()

            // Create spinner items with "All" option
            val spinnerItems = mutableListOf("All")
            spinnerItems.addAll(codes.map { "Type $it" })

            val spinnerAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                spinnerItems
            )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            categorySpinner.adapter = spinnerAdapter
            categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedCategoryCode = if (position == 0) {
                        "all"
                    } else {
                        codes[position - 1]
                    }
                    loadTexts()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedCategoryCode = "all"
                    loadTexts()
                }
            }
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
