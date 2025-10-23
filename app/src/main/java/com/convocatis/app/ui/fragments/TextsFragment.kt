package com.convocatis.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.convocatis.app.ConvocatisApplication
import com.convocatis.app.MainActivity
import com.convocatis.app.R
import com.convocatis.app.database.entity.TextEntity
import com.convocatis.app.utils.FavoritesManager

class TextsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TextsAdapter
    private lateinit var favoritesManager: FavoritesManager

    private var sortAscending = true
    private var showOnlyFavorites = false
    private var searchTerm = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_texts, container, false)

        favoritesManager = FavoritesManager(requireContext())

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

        // Load texts from database
        loadTexts()

        // Listen for search from MainActivity
        (activity as? MainActivity)?.onSearchTermChangedListener = { term ->
            searchTerm = term
            loadTexts()
        }

        return view
    }

    private fun loadTexts() {
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
        private val favoriteIcon: ImageView = view.findViewById(R.id.favoriteIcon)

        fun bind(text: TextEntity) {
            titleView.text = text.title

            // Set favorite icon
            val isFavorite = favoritesManager.isFavorite(text.rid)
            favoriteIcon.setImageResource(
                if (isFavorite) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
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
