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
            var filteredTexts = allTexts.toMutableList()

            // Filter by search term
            if (searchTerm.isNotEmpty()) {
                filteredTexts = filteredTexts.filter {
                    it.title.contains(searchTerm, ignoreCase = true) ||
                    it.rawContent.contains(searchTerm, ignoreCase = true)
                }.toMutableList()
            }

            // Filter by category (hierarchical: Type and/or Code)
            filteredTexts = when {
                // Filter by both Type and Code
                currentFilter.type != null && currentFilter.code != null -> {
                    filteredTexts.filter {
                        // Support comma-separated codes (e.g., "21,22,1")
                        val codes = it.categoryCode?.split(",")?.map { code -> code.trim() } ?: emptyList()
                        it.categoryType == currentFilter.type &&
                            (it.categoryCode == currentFilter.code || codes.contains(currentFilter.code))
                    }.toMutableList()
                }
                // Filter by Type only
                currentFilter.type != null -> {
                    filteredTexts.filter { it.categoryType == currentFilter.type }.toMutableList()
                }
                // No filter (show all)
                else -> filteredTexts
            }

            // Sort alphabetically
            filteredTexts = if (sortAscending) {
                filteredTexts.sortedBy { it.title }.toMutableList()
            } else {
                filteredTexts.sortedByDescending { it.title }.toMutableList()
            }

            // If favorites filter is ON, sort favorites to top (all texts still visible)
            if (showOnlyFavorites) {
                val favoriteRids = favoritesManager.getFavorites()
                filteredTexts = filteredTexts.sortedByDescending { favoriteRids.contains(it.rid) }.toMutableList()
            }

            // Add synthetic advertisement entries at the top (always shown, regardless of filters)
            // Only add if search term is empty (so user can search without seeing ads)
            if (searchTerm.isEmpty()) {
                val adEntryLv = createAdvertisementEntry("lv")
                val adEntryEn = createAdvertisementEntry("en")

                // Insert at beginning
                filteredTexts.add(0, adEntryLv)
                filteredTexts.add(1, adEntryEn)
            }

            adapter.submitList(filteredTexts)
        }
    }

    /**
     * Create synthetic advertisement entry
     */
    private fun createAdvertisementEntry(languageCode: String): TextEntity {
        return if (languageCode == "lv") {
            TextEntity(
                rid = -1L, // Negative RID to avoid conflicts
                title = "⭐ Informācija un atgriezeniskā saite",
                rawContent = """
                    >>Sveicināti Convocatis!<<

                    Ja vēlies šādu programmu savam pasākumam, vai tev ir kādi ieteikumi vai problēmas ar šo programmu, raksti e-pastu:

                    <a href="mailto:aivarszar@gmail.com"><b>aivarszar@gmail.com</b></a>

                    ---

                    <h3>Par šo programmu</h3>

                    Convocatis ir radīta, lai palīdzētu organizēt un lasīt lūgšanu un dziesmu tekstus pasākumos.

                    <ul>
                      <li>✅ Vienkārša navigācija</li>
                      <li>✅ Tekstu meklēšana un šķirošana</li>
                      <li>✅ Izlūkotāko tekstu saglabāšana</li>
                      <li>✅ Daudzu lapu un atkārtošanas atbalsts</li>
                    </ul>

                    <h3>Kā var palīdzēt?</h3>

                    <ul>
                      <li>💬 Nosūti savus ieteikumus</li>
                      <li>🐛 Ziņo par problēmām</li>
                      <li>⭐ Novērtē aplikāciju</li>
                      <li>📤 Dalies ar draugiem</li>
                    </ul>

                    <br/>

                    Izstrādātājs: <a href="http://convocatis.net"><b>Convocatis</b></a><br/>
                    <a href="https://www.madonasdraudze.lv/Avize_2020/1.pdf"><b>Kalnā par Convocatis</b></a>

                    <br/><br/>
                    <img src="https://png.pngtree.com/png-vector/20211103/ourmid/pngtree-christian-religious-symbol-cross-brush-illustration-png-image_4020809.png" />

                    <p><small>Versija 1.0 | Izstrādāts ar ❤️ Latvijā</small></p>
                """.trimIndent(),
                categoryType = 0,
                categoryCode = null,
                languageCode = "lv"
            )
        } else {
            TextEntity(
                rid = -2L, // Different negative RID
                title = "⭐ Information and Feedback",
                rawContent = """
                    >>Welcome to Convocatis!<<

                    If you want such a program for your event, or you have any suggestions or problems with this program, write an email to:

                    <a href="mailto:aivarszar@gmail.com"><b>aivarszar@gmail.com</b></a>

                    ---

                    <h3>About this app</h3>

                    Convocatis is designed to help organize and read prayer and song texts at events.

                    <ul>
                      <li>✅ Simple navigation</li>
                      <li>✅ Text search and sorting</li>
                      <li>✅ Save favorite texts</li>
                      <li>✅ Multi-page and repetition support</li>
                    </ul>

                    <h3>How can you help?</h3>

                    <ul>
                      <li>💬 Send your suggestions</li>
                      <li>🐛 Report problems</li>
                      <li>⭐ Rate the app</li>
                      <li>📤 Share with friends</li>
                    </ul>

                    <p><small>Version 1.0 | Made with ❤️ in Latvia</small></p>
                """.trimIndent(),
                categoryType = 0,
                categoryCode = null,
                languageCode = "en"
            )
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
     * Refresh data from database (for re-import)
     */
    fun refreshData() {
        loadTexts()
    }

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

            // Don't show favorite icon for synthetic advertisement entries (negative RID)
            if (text.rid < 0) {
                favoriteIcon.visibility = View.GONE
                favoriteIcon.isClickable = false
            } else {
                favoriteIcon.visibility = View.VISIBLE
                favoriteIcon.isClickable = true

                // Set favorite icon (dot or star)
                val isFavorite = favoritesManager.isFavorite(text.rid)
                favoriteIcon.text = if (isFavorite) "★" else "●"
                favoriteIcon.setTextColor(
                    if (isFavorite) 0xFF000000.toInt() // Black star
                    else 0xFF999999.toInt() // Gray dot
                )

                // Click on star -> toggle favorite
                favoriteIcon.setOnClickListener {
                    onFavoriteClick(text)
                }
            }

            // Click on item -> open text
            itemView.setOnClickListener { onItemClick(text) }
        }
    }
}
