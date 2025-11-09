package com.convocatis.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
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

    // Navigation buttons
    private lateinit var btnBackToParent: TextView
    private lateinit var btnResetToAll: TextView

    // Pagination UI elements
    private lateinit var alphabetScrollView: LinearLayout
    private lateinit var alphabetContainer: LinearLayout
    private lateinit var alphabetContainerRow2: LinearLayout
    private lateinit var paginationContainer: LinearLayout
    private lateinit var pageInfo: TextView
    private lateinit var btnFirst: Button
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var btnLast: Button

    private var sortAscending = true
    private var showOnlyFavorites = false
    private var searchTerm = ""
    private var currentFilter: TextTypesParser.CategoryFilter = TextTypesParser.CategoryFilter.all()
    private var allTexts: List<TextEntity> = emptyList() // Cache all texts for category filtering

    // Pagination state
    private var currentAlphabetFilter: Char? = null
    private var currentAlphabetGroup: AlphabetGroup? = null
    private var currentPage: Int = 0
    private var filteredTextsCache: List<TextEntity> = emptyList()

    /**
     * Data class for alphabet groups (single letter or combined letters like A-B)
     */
    data class AlphabetGroup(val letters: List<Char>, val count: Int) {
        fun getLabel(): String = if (letters.size == 1) letters[0].toString() else "${letters.first()}-${letters.last()}"
        fun matches(char: Char): Boolean = letters.contains(char)
    }

    // Scroll position restoration
    private var scrollPosition: Int = 0
    private var scrollOffset: Int = 0

    companion object {
        // SharedPreferences keys
        private const val PREF_LAST_FILTER_TYPE = "last_filter_type"
        private const val PREF_LAST_FILTER_CODE = "last_filter_code"
        private const val PREF_LAST_FILTER_DISPLAY = "last_filter_display"
        private const val PREF_LAST_ALPHABET_FILTER = "last_alphabet_filter"
        private const val PREF_LAST_PAGE = "last_page"

        // Pagination configuration - EASY TO MODIFY
        private const val PAGINATION_THRESHOLD = 100  // Show pagination if more than this many items
        private const val ITEMS_PER_PAGE_THRESHOLD = 20  // If letter has more than this, use pagination
        private const val ITEMS_PER_PAGE = 100  // Items per page
        private const val ALPHABET_ROW_THRESHOLD = 17  // Split alphabet into 2 rows if > this many letters

        // State keys for orientation change
        private const val STATE_ALPHABET_FILTER = "state_alphabet_filter"
        private const val STATE_CURRENT_PAGE = "state_current_page"
        private const val STATE_SCROLL_POSITION = "state_scroll_position"
        private const val STATE_SCROLL_OFFSET = "state_scroll_offset"

        /**
         * Normalize character by removing diacritics (works for all European languages)
         * Examples: ā→a, č→c, é→e, ñ→n, ö→o, ß→ss, etc.
         */
        fun normalizeChar(char: Char): Char {
            val normalized = java.text.Normalizer.normalize(char.toString(), java.text.Normalizer.Form.NFD)
            // Remove all diacritical marks (Unicode category "NonSpacingMark")
            val withoutDiacritics = normalized.replace("\\p{Mn}".toRegex(), "")
            // Handle special cases like ß → ss, œ → oe, æ → ae
            return when (withoutDiacritics.lowercase()) {
                "ß" -> 's'
                "œ" -> 'o'
                "æ" -> 'a'
                else -> withoutDiacritics.firstOrNull()?.lowercaseChar() ?: char.lowercaseChar()
            }
        }

        /**
         * Normalize string for sorting (removes diacritics from all characters)
         */
        fun normalizeString(str: String): String {
            val normalized = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
            // Remove all diacritical marks
            return normalized.replace("\\p{Mn}".toRegex(), "").lowercase()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_texts, container, false)

        favoritesManager = FavoritesManager(requireContext())
        prefs = requireContext().getSharedPreferences("convocatis_prefs", android.content.Context.MODE_PRIVATE)

        // Restore state from orientation change if available
        if (savedInstanceState != null) {
            val letterString = savedInstanceState.getString(STATE_ALPHABET_FILTER)
            currentAlphabetFilter = letterString?.firstOrNull()
            currentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE, 0)
            scrollPosition = savedInstanceState.getInt(STATE_SCROLL_POSITION, 0)
            scrollOffset = savedInstanceState.getInt(STATE_SCROLL_OFFSET, 0)
        } else {
            // Restore last filter from SharedPreferences (normal startup)
            restoreLastFilter()
            restoreLastPaginationState()
        }

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        btnBackToParent = view.findViewById(R.id.btnBackToParent)
        btnResetToAll = view.findViewById(R.id.btnResetToAll)

        alphabetScrollView = view.findViewById(R.id.alphabetScrollView)
        alphabetContainer = view.findViewById(R.id.alphabetContainer)
        alphabetContainerRow2 = view.findViewById(R.id.alphabetContainerRow2)
        paginationContainer = view.findViewById(R.id.paginationContainer)
        pageInfo = view.findViewById(R.id.pageInfo)
        btnFirst = view.findViewById(R.id.btnFirst)
        btnPrev = view.findViewById(R.id.btnPrev)
        btnNext = view.findViewById(R.id.btnNext)
        btnLast = view.findViewById(R.id.btnLast)

        // Setup navigation button listeners
        setupNavigationButtons()

        // Setup pagination button listeners
        setupPaginationButtons()

        adapter = TextsAdapter(
            favoritesManager = favoritesManager,
            onItemClick = { textEntity ->
                // Save current scroll position before opening text
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                layoutManager?.let {
                    scrollPosition = it.findFirstVisibleItemPosition()
                    val view = it.findViewByPosition(scrollPosition)
                    scrollOffset = view?.top ?: 0
                }
                savePaginationState()
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
            // Cache all texts for category filtering
            this@TextsFragment.allTexts = allTexts

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

            // Sort alphabetically using normalized strings (diacritics sorted under base letters)
            filteredTexts = if (sortAscending) {
                filteredTexts.sortedWith(compareBy(
                    { normalizeString(it.title.lowercase()) },  // Primary: normalized title
                    { it.title.lowercase() }  // Secondary: original title (for same base letter)
                )).toMutableList()
            } else {
                filteredTexts.sortedWith(compareByDescending<TextEntity>(
                    { normalizeString(it.title.lowercase()) }
                ).thenByDescending { it.title.lowercase() }).toMutableList()
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

            // Store filtered texts for pagination
            filteredTextsCache = filteredTexts

            // Apply alphabet filter and pagination
            val paginatedTexts = applyAlphabetFilterAndPagination(filteredTexts)

            adapter.submitList(paginatedTexts)

            // Update alphabet and pagination UI
            updateAlphabetFilter(filteredTexts)
            updatePaginationUI()
            updateNavigationButtons()

            // Restore scroll position after data is loaded
            recyclerView.post {
                if (scrollPosition > 0 || scrollOffset != 0) {
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                    layoutManager?.scrollToPositionWithOffset(scrollPosition, scrollOffset)
                    // Reset scroll position after restoration (only restore once)
                    scrollPosition = 0
                    scrollOffset = 0
                }
            }
        }
    }

    /**
     * Apply alphabet filter and pagination to the list
     */
    private fun applyAlphabetFilterAndPagination(texts: List<TextEntity>): List<TextEntity> {
        // Filter by alphabet group if selected (using normalized characters)
        val alphabetFiltered = if (currentAlphabetGroup != null) {
            texts.filter {
                val firstChar = it.title.firstOrNull() ?: return@filter false
                val normalizedChar = normalizeChar(firstChar).uppercaseChar()
                currentAlphabetGroup!!.matches(normalizedChar)
            }
        } else {
            texts
        }

        // Check if we need pagination for current alphabet filter
        if (alphabetFiltered.size > ITEMS_PER_PAGE_THRESHOLD) {
            // Calculate pagination
            val totalPages = (alphabetFiltered.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
            val startIndex = currentPage * ITEMS_PER_PAGE
            val endIndex = minOf(startIndex + ITEMS_PER_PAGE, alphabetFiltered.size)

            return if (startIndex < alphabetFiltered.size) {
                alphabetFiltered.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
        }

        return alphabetFiltered
    }

    /**
     * Create smart alphabet groups - combine letters with < 20 texts until group has >= 30
     */
    private fun createAlphabetGroups(texts: List<TextEntity>): List<AlphabetGroup> {
        // Count texts for each letter
        val letterCounts = mutableMapOf<Char, Int>()
        texts.forEach { text ->
            val firstChar = text.title.firstOrNull() ?: return@forEach
            val normalizedChar = normalizeChar(firstChar).uppercaseChar()
            if (normalizedChar in 'A'..'Z') {
                letterCounts[normalizedChar] = (letterCounts[normalizedChar] ?: 0) + 1
            }
        }

        // Sort letters
        val sortedLetters = letterCounts.keys.sorted()
        if (sortedLetters.isEmpty()) return emptyList()

        // Create groups
        val groups = mutableListOf<AlphabetGroup>()
        var currentGroup = mutableListOf<Char>()
        var currentCount = 0

        sortedLetters.forEach { letter ->
            val count = letterCounts[letter] ?: 0

            if (count >= 20) {
                // If letter has >= 20 texts, finalize current group first
                if (currentGroup.isNotEmpty()) {
                    groups.add(AlphabetGroup(currentGroup.toList(), currentCount))
                    currentGroup.clear()
                    currentCount = 0
                }
                // Add this letter as separate group
                groups.add(AlphabetGroup(listOf(letter), count))
            } else {
                // If letter has < 20 texts, add to current group
                currentGroup.add(letter)
                currentCount += count

                // If group has >= 30 texts, finalize it
                if (currentCount >= 30) {
                    groups.add(AlphabetGroup(currentGroup.toList(), currentCount))
                    currentGroup.clear()
                    currentCount = 0
                }
            }
        }

        // Add remaining group
        if (currentGroup.isNotEmpty()) {
            groups.add(AlphabetGroup(currentGroup.toList(), currentCount))
        }

        return groups
    }

    /**
     * Update alphabet filter UI (Latin alphabet only, with 2-row support and full-width)
     */
    private fun updateAlphabetFilter(texts: List<TextEntity>) {
        // Show alphabet filter if more than threshold items
        if (texts.size > PAGINATION_THRESHOLD) {
            alphabetScrollView.visibility = View.VISIBLE

            // Create smart alphabet groups
            val alphabetGroups = createAlphabetGroups(texts)

            // Clear both rows
            alphabetContainer.removeAllViews()
            alphabetContainerRow2.removeAllViews()
            alphabetContainerRow2.visibility = View.GONE

            // Determine if we should use full width (>=10 groups total)
            val totalButtons = alphabetGroups.size + 1  // +1 for "Visi"
            val useFullWidth = totalButtons >= 10

            // Add "Visi" button to first row
            addAlphabetButton(null, "Visi", alphabetGroups.isNotEmpty(), alphabetContainer, useFullWidth)

            // If more than ALPHABET_ROW_THRESHOLD groups, split into 2 rows
            if (alphabetGroups.size > ALPHABET_ROW_THRESHOLD) {
                val midPoint = (alphabetGroups.size + 1) / 2  // +1 for "Visi" button

                // First row: "Visi" + first half of groups
                alphabetGroups.take(midPoint).forEach { group ->
                    addAlphabetGroupButton(group, alphabetContainer, useFullWidth)
                }

                // Second row: second half of groups
                alphabetContainerRow2.visibility = View.VISIBLE
                alphabetGroups.drop(midPoint).forEach { group ->
                    addAlphabetGroupButton(group, alphabetContainerRow2, useFullWidth)
                }
            } else {
                // Single row: "Visi" + all groups
                alphabetGroups.forEach { group ->
                    addAlphabetGroupButton(group, alphabetContainer, useFullWidth)
                }
            }
        } else {
            alphabetScrollView.visibility = View.GONE
            currentAlphabetFilter = null
            currentAlphabetGroup = null
        }
    }

    /**
     * Add alphabet group button to the specified container
     */
    private fun addAlphabetGroupButton(group: AlphabetGroup, container: LinearLayout, useFullWidth: Boolean) {
        val button = TextView(requireContext()).apply {
            text = group.getLabel()
            isEnabled = true
            textSize = 16f
            minWidth = 0
            minHeight = 0
            gravity = android.view.Gravity.CENTER

            // Set padding based on mode
            if (useFullWidth) {
                setPadding(2, 8, 2, 8)  // Compact padding for full width
            } else {
                setPadding(4, 8, 4, 8)  // Slightly more padding for limited width
            }

            // Highlight if selected - link style (underlined, colored)
            val isSelected = currentAlphabetGroup == group

            // Link style - blue/purple color, underlined when selected
            setTextColor(
                if (isSelected) ContextCompat.getColor(requireContext(), R.color.purple_700)
                else ContextCompat.getColor(requireContext(), R.color.purple_500)
            )

            // Bold when selected
            setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

            // Underline when selected
            paintFlags = if (isSelected) {
                paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            } else {
                paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
            }

            setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))

            setOnClickListener {
                currentAlphabetGroup = group
                currentAlphabetFilter = null  // Legacy field, keep for compatibility
                currentPage = 0
                savePaginationState()
                loadTexts()
            }
        }

        val params = if (useFullWidth) {
            // Full width mode: use weight to distribute evenly
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f  // Equal weight for all buttons
            )
        } else {
            // Limited width mode: wrap content with max spacing
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16  // Maximum spacing when few letters
            }
        }

        container.addView(button, params)
    }

    /**
     * Add alphabet button to the specified container (compact, link-style, full-width if many letters)
     */
    private fun addAlphabetButton(letter: Char?, label: String, enabled: Boolean, container: LinearLayout, useFullWidth: Boolean) {
        val button = TextView(requireContext()).apply {
            text = label
            isEnabled = enabled
            textSize = 16f
            minWidth = 0
            minHeight = 0
            gravity = android.view.Gravity.CENTER

            // Set padding based on mode
            if (useFullWidth) {
                setPadding(2, 8, 2, 8)  // Compact padding for full width
            } else {
                setPadding(4, 8, 4, 8)  // Slightly more padding for limited width
            }

            // Highlight if selected - link style (underlined, colored)
            val isSelected = (letter == null && currentAlphabetGroup == null)

            // Link style - blue/purple color, underlined when selected
            setTextColor(
                if (isSelected) ContextCompat.getColor(requireContext(), R.color.purple_700)
                else ContextCompat.getColor(requireContext(), R.color.purple_500)
            )

            // Bold when selected
            setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

            // Underline when selected
            paintFlags = if (isSelected) {
                paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            } else {
                paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
            }

            setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))

            setOnClickListener {
                currentAlphabetFilter = null
                currentAlphabetGroup = null
                currentPage = 0
                savePaginationState()
                loadTexts()
            }
        }

        val params = if (useFullWidth) {
            // Full width mode: use weight to distribute evenly
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f  // Equal weight for all buttons
            )
        } else {
            // Limited width mode: wrap content with max spacing
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16  // Maximum spacing when few letters
            }
        }

        container.addView(button, params)
    }

    /**
     * Update pagination UI (buttons and page info)
     */
    private fun updatePaginationUI() {
        val alphabetFiltered = if (currentAlphabetGroup != null) {
            filteredTextsCache.filter {
                val firstChar = it.title.firstOrNull() ?: return@filter false
                val normalizedChar = normalizeChar(firstChar).uppercaseChar()
                currentAlphabetGroup!!.matches(normalizedChar)
            }
        } else {
            filteredTextsCache
        }

        // Calculate total pages
        val totalPages = if (alphabetFiltered.size > ITEMS_PER_PAGE_THRESHOLD) {
            (alphabetFiltered.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
        } else {
            1
        }

        // Show pagination only if more than 1 page
        if (totalPages > 1) {
            paginationContainer.visibility = View.VISIBLE

            pageInfo.text = "${currentPage + 1}/$totalPages"

            // Enable/disable buttons and set colors
            val grayColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            val blackColor = ContextCompat.getColor(requireContext(), android.R.color.black)

            btnFirst.isEnabled = currentPage > 0
            btnFirst.setTextColor(if (currentPage > 0) blackColor else grayColor)

            btnPrev.isEnabled = currentPage > 0
            btnPrev.setTextColor(if (currentPage > 0) blackColor else grayColor)

            btnNext.isEnabled = currentPage < totalPages - 1
            btnNext.setTextColor(if (currentPage < totalPages - 1) blackColor else grayColor)

            btnLast.isEnabled = currentPage < totalPages - 1
            btnLast.setTextColor(if (currentPage < totalPages - 1) blackColor else grayColor)
        } else {
            paginationContainer.visibility = View.GONE
        }
    }

    /**
     * Setup navigation button listeners
     */
    private fun setupNavigationButtons() {
        btnBackToParent.setOnClickListener {
            navigateToParentCategory()
        }

        btnResetToAll.setOnClickListener {
            resetToAllTexts()
        }

        // Initialize button states
        updateNavigationButtons()
    }

    /**
     * Navigate to parent category
     */
    private fun navigateToParentCategory() {
        when {
            // If filtered by both Type and Code, go back to Type only
            currentFilter.type != null && currentFilter.code != null -> {
                currentFilter = TextTypesParser.CategoryFilter(
                    type = currentFilter.type,
                    code = null,
                    displayText = "Type ${currentFilter.type}"
                )
            }
            // If filtered by Type only, go back to All
            currentFilter.type != null -> {
                currentFilter = TextTypesParser.CategoryFilter.all()
            }
            // Already at top level, do nothing
            else -> return
        }

        // Reset pagination when changing filter
        currentAlphabetFilter = null
        currentAlphabetGroup = null
        currentPage = 0

        saveLastFilter()
        savePaginationState()
        loadTexts()
        updateNavigationButtons()
    }

    /**
     * Reset to show all texts (clear category filter, keep alphabet/pagination)
     */
    private fun resetToAllTexts() {
        currentFilter = TextTypesParser.CategoryFilter.all()

        // Reset alphabet filter and pagination too
        currentAlphabetFilter = null
        currentAlphabetGroup = null
        currentPage = 0

        saveLastFilter()
        savePaginationState()
        loadTexts()
        updateNavigationButtons()
    }

    /**
     * Update navigation button appearance based on current filter state
     */
    private fun updateNavigationButtons() {
        // Show "Back" button only if we're in main category or subcategory
        val canGoBack = currentFilter.type != null
        btnBackToParent.visibility = if (canGoBack) View.VISIBLE else View.GONE

        // Show "Home" button only if we're in subcategory
        val isSubcategory = currentFilter.type != null && currentFilter.code != null
        btnResetToAll.visibility = if (isSubcategory) View.VISIBLE else View.GONE
    }

    /**
     * Setup pagination button listeners
     */
    private fun setupPaginationButtons() {
        btnFirst.setOnClickListener {
            currentPage = 0
            savePaginationState()
            loadTexts()
        }

        btnPrev.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                savePaginationState()
                loadTexts()
            }
        }

        btnNext.setOnClickListener {
            val alphabetFiltered = if (currentAlphabetGroup != null) {
                filteredTextsCache.filter {
                    val firstChar = it.title.firstOrNull() ?: return@filter false
                    val normalizedChar = normalizeChar(firstChar).uppercaseChar()
                    currentAlphabetGroup!!.matches(normalizedChar)
                }
            } else {
                filteredTextsCache
            }
            val totalPages = (alphabetFiltered.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE

            if (currentPage < totalPages - 1) {
                currentPage++
                savePaginationState()
                loadTexts()
            }
        }

        btnLast.setOnClickListener {
            val alphabetFiltered = if (currentAlphabetGroup != null) {
                filteredTextsCache.filter {
                    val firstChar = it.title.firstOrNull() ?: return@filter false
                    val normalizedChar = normalizeChar(firstChar).uppercaseChar()
                    currentAlphabetGroup!!.matches(normalizedChar)
                }
            } else {
                filteredTextsCache
            }
            val totalPages = (alphabetFiltered.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE

            currentPage = totalPages - 1
            savePaginationState()
            loadTexts()
        }
    }

    /**
     * Save pagination state to SharedPreferences
     */
    private fun savePaginationState() {
        prefs.edit().apply {
            currentAlphabetFilter?.let { putString(PREF_LAST_ALPHABET_FILTER, it.toString()) }
                ?: remove(PREF_LAST_ALPHABET_FILTER)
            putInt(PREF_LAST_PAGE, currentPage)
            apply()
        }
    }

    /**
     * Restore pagination state from SharedPreferences
     */
    private fun restoreLastPaginationState() {
        val letterString = prefs.getString(PREF_LAST_ALPHABET_FILTER, null)
        currentAlphabetFilter = letterString?.firstOrNull()
        currentPage = prefs.getInt(PREF_LAST_PAGE, 0)
    }

    /**
     * Save instance state for orientation changes and text reading navigation
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentAlphabetFilter?.let { outState.putString(STATE_ALPHABET_FILTER, it.toString()) }
        outState.putInt(STATE_CURRENT_PAGE, currentPage)

        // Save scroll position
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
        layoutManager?.let {
            val position = it.findFirstVisibleItemPosition()
            val view = it.findViewByPosition(position)
            val offset = view?.top ?: 0
            outState.putInt(STATE_SCROLL_POSITION, position)
            outState.putInt(STATE_SCROLL_OFFSET, offset)
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

                    <p><small>Version 1.0 Made with ❤️ in Latvia</small></p>
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
                    // Reset alphabet and pagination filters when category filter is used
                    currentAlphabetFilter = null
                    currentAlphabetGroup = null
                    currentPage = 0
                    saveLastFilter()
                    savePaginationState()
                    loadTexts()
                },
                availableTexts = allTexts
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
