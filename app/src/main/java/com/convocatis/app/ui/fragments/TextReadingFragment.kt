package com.convocatis.app.ui.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.convocatis.app.ConvocatisApplication
import com.convocatis.app.R
import com.convocatis.app.database.entity.TextEntity
import com.convocatis.app.utils.TextContentParser
import kotlinx.coroutines.launch

class TextReadingFragment : Fragment() {

    private lateinit var textEntity: TextEntity
    private lateinit var parser: TextContentParser
    private var sections: List<TextContentParser.HeaderSection> = emptyList()
    private var sectionsWithHeaders: List<TextContentParser.HeaderSection> = emptyList()
    private var currentSectionIndex = 0

    // Views
    private lateinit var headerTextView: TextView
    private lateinit var headerNavigationContainer: View
    private lateinit var prevHeaderButton: Button
    private lateinit var nextHeaderButton: Button
    private lateinit var headerIndicator: TextView
    private lateinit var pageViewPager: ViewPager2
    private lateinit var pageProgressContainer: View
    private lateinit var pageNavigationContainer: View
    private lateinit var pageIndicator: TextView
    private lateinit var pageProgressBar: ProgressBar
    private lateinit var prevPageButton: Button
    private lateinit var nextPageButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            textEntity = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_TEXT, TextEntity::class.java) ?: getDefaultEntity()
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable(ARG_TEXT) as? TextEntity ?: getDefaultEntity()
            }
        }

        val database = ConvocatisApplication.getInstance().database
        parser = TextContentParser(database.textDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_text_reading_two_level, container, false)

        // Initialize views
        val titleView = view.findViewById<TextView>(R.id.titleText)
        headerTextView = view.findViewById(R.id.headerTextView)
        headerNavigationContainer = view.findViewById(R.id.headerNavigationContainer)
        prevHeaderButton = view.findViewById(R.id.prevHeaderButton)
        nextHeaderButton = view.findViewById(R.id.nextHeaderButton)
        headerIndicator = view.findViewById(R.id.headerIndicator)
        pageViewPager = view.findViewById(R.id.pageViewPager)
        pageProgressContainer = view.findViewById(R.id.pageProgressContainer)
        pageNavigationContainer = view.findViewById(R.id.pageNavigationContainer)
        pageIndicator = view.findViewById(R.id.pageIndicator)
        pageProgressBar = view.findViewById(R.id.pageProgressBar)
        prevPageButton = view.findViewById(R.id.prevPageButton)
        nextPageButton = view.findViewById(R.id.nextPageButton)

        titleView.text = textEntity.title

        // Set up header navigation
        prevHeaderButton.setOnClickListener {
            if (currentSectionIndex > 0) {
                currentSectionIndex--
                updateHeaderDisplay()
                loadPagesForCurrentSection()
            }
        }

        nextHeaderButton.setOnClickListener {
            if (currentSectionIndex < sections.size - 1) {
                currentSectionIndex++
                updateHeaderDisplay()
                loadPagesForCurrentSection()
            }
        }

        // Set up page navigation
        prevPageButton.setOnClickListener {
            val currentItem = pageViewPager.currentItem
            if (currentItem > 0) {
                pageViewPager.currentItem = currentItem - 1
            } else if (currentSectionIndex > 0) {
                // At first page, go to previous section
                currentSectionIndex--
                updateHeaderDisplay()
                loadPagesForCurrentSection(startAtEnd = true)
            }
        }

        nextPageButton.setOnClickListener {
            val currentSection = sections[currentSectionIndex]
            val currentItem = pageViewPager.currentItem
            if (currentItem < currentSection.pages.size - 1) {
                pageViewPager.currentItem = currentItem + 1
            } else if (currentSectionIndex < sections.size - 1) {
                // At last page, go to next section
                currentSectionIndex++
                updateHeaderDisplay()
                loadPagesForCurrentSection()
            }
        }

        // Parse content
        lifecycleScope.launch {
            sections = parser.parseToSections(textEntity)

            // Filter sections that have headers (for header navigation counter)
            sectionsWithHeaders = sections.filter { it.headerText != null }

            if (sections.isNotEmpty()) {
                val hasHeaders = sectionsWithHeaders.isNotEmpty()

                // Show/hide header navigation based on whether there are headers
                if (hasHeaders) {
                    headerNavigationContainer.visibility = View.VISIBLE
                    updateHeaderDisplay()
                } else {
                    headerNavigationContainer.visibility = View.GONE
                    headerTextView.visibility = View.GONE
                }

                loadPagesForCurrentSection()
            }
        }

        return view
    }

    /**
     * Update header display with current section's header text
     */
    private fun updateHeaderDisplay() {
        val section = sections[currentSectionIndex]

        if (section.headerText != null) {
            headerTextView.visibility = View.VISIBLE
            // Just show plain text header, no HTML formatting
            headerTextView.text = section.headerText
        } else {
            headerTextView.visibility = View.GONE
        }

        // Update header navigation counter - only count sections with headers
        if (sectionsWithHeaders.isNotEmpty()) {
            val headerSectionIndex = sectionsWithHeaders.indexOf(section)
            if (headerSectionIndex >= 0) {
                headerIndicator.text = "${headerSectionIndex + 1}/${sectionsWithHeaders.size}"
            } else {
                // Current section has no header, don't show counter
                headerIndicator.text = ""
            }
        }

        // Hide << button if at first section, >> if at last
        prevHeaderButton.visibility = if (currentSectionIndex > 0) View.VISIBLE else View.INVISIBLE
        nextHeaderButton.visibility = if (currentSectionIndex < sections.size - 1) View.VISIBLE else View.INVISIBLE
    }

    /**
     * Load pages for the current section into ViewPager
     */
    private fun loadPagesForCurrentSection(startAtEnd: Boolean = false) {
        val section = sections[currentSectionIndex]
        val adapter = PageAdapter(section.pages)
        pageViewPager.adapter = adapter

        // Set up page change listener with auto-advance
        var isUserScrolling = false
        pageViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                isUserScrolling = state == ViewPager2.SCROLL_STATE_DRAGGING
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageIndicator(position)

                // Auto-advance to next section if at last page and user swiped forward
                if (isUserScrolling && position == section.pages.size - 1 && currentSectionIndex < sections.size - 1) {
                    // Small delay to allow current swipe to complete
                    pageViewPager.postDelayed({
                        if (pageViewPager.currentItem == section.pages.size - 1) {
                            // Still at last page, advance to next section
                            currentSectionIndex++
                            updateHeaderDisplay()
                            loadPagesForCurrentSection()
                        }
                    }, 300)
                }
            }
        })

        // Navigate to first or last page
        if (startAtEnd && section.pages.isNotEmpty()) {
            pageViewPager.setCurrentItem(section.pages.size - 1, false)
        } else {
            pageViewPager.setCurrentItem(0, false)
            updatePageIndicator(0)
        }
    }

    /**
     * Update page indicator and navigation
     */
    private fun updatePageIndicator(position: Int) {
        val section = sections[currentSectionIndex]
        val pages = section.pages

        if (pages.isEmpty()) return

        val page = pages[position]

        // Only show page counter if this is a repetition (has repetitionIndex)
        // Don't show counters for regular pages separated by |
        val isRepetition = page.repetitionIndex != null && page.totalRepetitions != null

        if (isRepetition) {
            pageProgressContainer.visibility = View.VISIBLE

            // Show repetition counter (e.g., "3/10" for 3rd repetition out of 10)
            pageIndicator.text = "${page.repetitionIndex}/${page.totalRepetitions}"
            val progress = if (page.totalRepetitions!! > 0) {
                (page.repetitionIndex!! * 100) / page.totalRepetitions!!
            } else 100
            pageProgressBar.progress = progress
        } else {
            pageProgressContainer.visibility = View.GONE
        }

        // Show navigation arrows if there are multiple pages (regardless of repetition)
        if (pages.size > 1) {
            pageNavigationContainer.visibility = View.VISIBLE
        } else {
            pageNavigationContainer.visibility = View.GONE
        }

        // Determine if we're at absolute first or last across all sections
        val isAbsoluteFirst = currentSectionIndex == 0 && position == 0
        val isAbsoluteLast = currentSectionIndex == sections.size - 1 && position == pages.size - 1

        // Hide < button if at first page of first section, > if at last page of last section
        prevPageButton.visibility = if (isAbsoluteFirst) View.INVISIBLE else View.VISIBLE
        nextPageButton.visibility = if (isAbsoluteLast) View.INVISIBLE else View.VISIBLE
    }

    private fun getDefaultEntity() = TextEntity(
        rid = 0,
        title = "",
        rawContent = "",
        languageCode = "lv"
    )

    companion object {
        private const val ARG_TEXT = "text_entity"

        fun newInstance(textEntity: TextEntity) = TextReadingFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_TEXT, textEntity)
            }
        }
    }
}

/**
 * ViewPager2 adapter for page navigation
 */
class PageAdapter(private val pages: List<TextContentParser.Page>) :
    RecyclerView.Adapter<PageAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount() = pages.size

    class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val contentView: TextView = view.findViewById(R.id.pageContent)

        fun bind(page: TextContentParser.Page) {
            // Render HTML content
            contentView.text = HtmlCompat.fromHtml(
                page.content,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }
    }
}
