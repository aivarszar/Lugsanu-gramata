package com.convocatis.app.ui.fragments

import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
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
import kotlin.math.abs

class TextReadingFragment : Fragment() {

    private lateinit var textEntity: TextEntity
    private lateinit var parser: TextContentParser
    private var sections: List<TextContentParser.HeaderSection> = emptyList()
    private var sectionsWithHeaders: List<TextContentParser.HeaderSection> = emptyList()
    private var currentSectionIndex = 0

    // Views
    private lateinit var titleView: TextView
    private lateinit var headerTextView: TextView
    private lateinit var headerNavigationContainer: View
    private lateinit var prevHeaderButton: Button
    private lateinit var nextHeaderButton: Button
    private lateinit var headerIndicator: TextView
    private lateinit var headerProgressBar: ProgressBar
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
        titleView = view.findViewById(R.id.titleText)
        headerTextView = view.findViewById(R.id.headerTextView)
        headerNavigationContainer = view.findViewById(R.id.headerNavigationContainer)
        prevHeaderButton = view.findViewById(R.id.prevHeaderButton)
        nextHeaderButton = view.findViewById(R.id.nextHeaderButton)
        headerIndicator = view.findViewById(R.id.headerIndicator)
        headerProgressBar = view.findViewById(R.id.headerProgressBar)
        pageViewPager = view.findViewById(R.id.pageViewPager)
        pageProgressContainer = view.findViewById(R.id.pageProgressContainer)
        pageNavigationContainer = view.findViewById(R.id.pageNavigationContainer)
        pageIndicator = view.findViewById(R.id.pageIndicator)
        pageProgressBar = view.findViewById(R.id.pageProgressBar)
        prevPageButton = view.findViewById(R.id.prevPageButton)
        nextPageButton = view.findViewById(R.id.nextPageButton)

        // Limit header section to maximum 50% of screen height
        val headerScrollView = view.findViewById<View>(R.id.headerScrollView)
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val maxHeaderHeight = (screenHeight * 0.5).toInt()

        // Measure the header and limit it if it exceeds max height
        headerScrollView.post {
            if (headerScrollView.height > maxHeaderHeight) {
                headerScrollView.layoutParams = headerScrollView.layoutParams.apply {
                    height = maxHeaderHeight
                }
            }
        }

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

        // Set up swipe gesture for header navigation
        val headerGestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (abs(diffX) > abs(diffY) && abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // Swipe right - go to previous section
                        val headerSectionIndex = sectionsWithHeaders.indexOf(sections[currentSectionIndex])
                        if (headerSectionIndex > 0) {
                            currentSectionIndex = sections.indexOf(sectionsWithHeaders[headerSectionIndex - 1])
                            updateHeaderDisplay()
                            loadPagesForCurrentSection()
                        }
                    } else {
                        // Swipe left - go to next section
                        val headerSectionIndex = sectionsWithHeaders.indexOf(sections[currentSectionIndex])
                        if (headerSectionIndex >= 0 && headerSectionIndex < sectionsWithHeaders.size - 1) {
                            currentSectionIndex = sections.indexOf(sectionsWithHeaders[headerSectionIndex + 1])
                            updateHeaderDisplay()
                            loadPagesForCurrentSection()
                        }
                    }
                    return true
                }
                return false
            }
        })

        headerTextView.setOnTouchListener { v, event ->
            headerGestureDetector.onTouchEvent(event)
            false
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
            // Hide title when header is shown
            titleView.visibility = View.GONE

            headerTextView.visibility = View.VISIBLE
            // Just show plain text header, no HTML formatting
            headerTextView.text = section.headerText

            // Show header navigation when in a section with header
            headerNavigationContainer.visibility = View.VISIBLE

            // Update header navigation counter - only count sections with headers
            val headerSectionIndex = sectionsWithHeaders.indexOf(section)
            if (headerSectionIndex >= 0) {
                headerIndicator.text = "${headerSectionIndex + 1}/${sectionsWithHeaders.size}"

                // Update header progress bar
                val progress = if (sectionsWithHeaders.size > 0) {
                    ((headerSectionIndex + 1) * 100) / sectionsWithHeaders.size
                } else 100
                headerProgressBar.progress = progress

                // Hide < button if at first header section, > if at last header section
                val isFirstHeaderSection = headerSectionIndex == 0
                val isLastHeaderSection = headerSectionIndex == sectionsWithHeaders.size - 1

                prevHeaderButton.visibility = if (isFirstHeaderSection) View.INVISIBLE else View.VISIBLE
                nextHeaderButton.visibility = if (isLastHeaderSection) View.INVISIBLE else View.VISIBLE
            }
        } else {
            // Show title when no header
            titleView.visibility = View.VISIBLE

            headerTextView.visibility = View.GONE
            // Hide header navigation when not in a header section
            headerNavigationContainer.visibility = View.GONE
        }
    }

    /**
     * Load pages for the current section into ViewPager
     */
    private fun loadPagesForCurrentSection(startAtEnd: Boolean = false) {
        val section = sections[currentSectionIndex]
        val adapter = PageAdapter(section.pages)
        pageViewPager.adapter = adapter

        // Set up page change listener
        pageViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageIndicator(position)
            }
        })

        // Set up gesture detector for swipe at boundaries
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false

                val diffX = e2.x - e1.x
                val currentPos = pageViewPager.currentItem

                // Check if swiping left (forward) at last page
                if (diffX < -100 && currentPos == section.pages.size - 1 && currentSectionIndex < sections.size - 1) {
                    android.util.Log.d("TextReading", "Swipe forward from last page, advancing section")
                    currentSectionIndex++
                    updateHeaderDisplay()
                    loadPagesForCurrentSection(startAtEnd = false)
                    return true
                }

                // Check if swiping right (backward) at first page
                if (diffX > 100 && currentPos == 0 && currentSectionIndex > 0) {
                    android.util.Log.d("TextReading", "Swipe backward from first page, going to previous section")
                    currentSectionIndex--
                    updateHeaderDisplay()
                    loadPagesForCurrentSection(startAtEnd = true)
                    return true
                }

                return false
            }
        })

        // Attach touch listener to ViewPager2's RecyclerView child
        val recyclerView = pageViewPager.getChildAt(0) as? RecyclerView
        recyclerView?.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            false  // Don't consume, let ViewPager2 handle it too
        }

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

        // Show navigation bar if there are multiple pages
        if (pages.size > 1) {
            pageNavigationContainer.visibility = View.VISIBLE

            // Only show counter and progress if this is a repetition
            val isRepetition = page.repetitionIndex != null && page.totalRepetitions != null

            if (isRepetition) {
                pageIndicator.visibility = View.VISIBLE
                pageProgressBar.visibility = View.VISIBLE

                // Show repetition counter (e.g., "3/10")
                pageIndicator.text = "${page.repetitionIndex}/${page.totalRepetitions}"
                val progress = if (page.totalRepetitions!! > 0) {
                    (page.repetitionIndex!! * 100) / page.totalRepetitions!!
                } else 100
                pageProgressBar.progress = progress
            } else {
                // No repetition - hide counter and progress bar
                pageIndicator.visibility = View.GONE
                pageProgressBar.visibility = View.GONE
            }

            // Determine if we're at absolute first or last across all sections
            val isAbsoluteFirst = currentSectionIndex == 0 && position == 0
            val isAbsoluteLast = currentSectionIndex == sections.size - 1 && position == pages.size - 1

            // Hide arrows at absolute boundaries
            prevPageButton.visibility = if (isAbsoluteFirst) View.INVISIBLE else View.VISIBLE
            nextPageButton.visibility = if (isAbsoluteLast) View.INVISIBLE else View.VISIBLE
        } else {
            // Only one page - hide navigation bar completely
            pageNavigationContainer.visibility = View.GONE
        }
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
