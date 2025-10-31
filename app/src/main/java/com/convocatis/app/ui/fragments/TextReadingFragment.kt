package com.convocatis.app.ui.fragments

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.abs

class TextReadingFragment : Fragment() {

    private lateinit var textEntity: TextEntity
    private lateinit var parser: TextContentParser
    private var sections: List<TextContentParser.HeaderSection> = emptyList()
    private var sectionsWithHeaders: List<TextContentParser.HeaderSection> = emptyList()
    private var savedPagePosition: Int = 0

    // Flat list of all pages with their header information
    private data class PageWithHeader(
        val page: TextContentParser.Page,
        val headerText: String?,
        val sectionIndex: Int
    )
    private var allPages: List<PageWithHeader> = emptyList()

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

        // Restore saved page position
        savedPagePosition = savedInstanceState?.getInt(KEY_CURRENT_PAGE, 0) ?: 0
        Log.d(TAG, "Restoring state: saved page = $savedPagePosition")

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

        titleView.text = textEntity.title

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

        // Set up header navigation - navigate to previous/next header
        prevHeaderButton.setOnClickListener {
            val currentPos = pageViewPager.currentItem
            if (currentPos < allPages.size) {
                val currentHeader = allPages[currentPos].headerText

                // Find previous page with different header (going backwards)
                for (i in currentPos - 1 downTo 0) {
                    val pageHeader = allPages[i].headerText
                    if (pageHeader != currentHeader && pageHeader != null) {
                        pageViewPager.currentItem = i
                        break
                    }
                }
            }
        }

        nextHeaderButton.setOnClickListener {
            val currentPos = pageViewPager.currentItem
            if (currentPos < allPages.size) {
                val currentHeader = allPages[currentPos].headerText

                // Find next page with different header (going forwards)
                for (i in currentPos + 1 until allPages.size) {
                    val pageHeader = allPages[i].headerText
                    if (pageHeader != currentHeader && pageHeader != null) {
                        pageViewPager.currentItem = i
                        break
                    }
                }
            }
        }

        // Set up page navigation - simple next/previous in flat list
        prevPageButton.setOnClickListener {
            val currentItem = pageViewPager.currentItem
            if (currentItem > 0) {
                pageViewPager.currentItem = currentItem - 1
            }
        }

        nextPageButton.setOnClickListener {
            val currentItem = pageViewPager.currentItem
            if (currentItem < allPages.size - 1) {
                pageViewPager.currentItem = currentItem + 1
            }
        }

        // Parse content
        lifecycleScope.launch {
            sections = parser.parseToSections(textEntity)

            // Filter sections that have headers (for header navigation counter)
            sectionsWithHeaders = sections.filter { it.headerText != null }

            // Flatten all sections into a single list of pages with header info
            allPages = sections.flatMapIndexed { sectionIndex, section ->
                section.pages.map { page ->
                    PageWithHeader(
                        page = page,
                        headerText = section.headerText,
                        sectionIndex = sectionIndex
                    )
                }
            }

            if (allPages.isNotEmpty()) {
                // Load all pages into ViewPager
                loadAllPages()

                // Set up header swipe gesture AFTER views are visible
                setupHeaderSwipeGesture()
            }
        }

        return view
    }

    /**
     * Set up swipe gesture for header navigation
     * Called after pages are loaded and views are visible
     */
    private fun setupHeaderSwipeGesture() {
        val headerGestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onDown(e: MotionEvent): Boolean {
                return true  // Must return true to receive subsequent events
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (abs(diffX) > abs(diffY) && abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    val currentPos = pageViewPager.currentItem
                    val currentHeader = if (currentPos < allPages.size) allPages[currentPos].headerText else null

                    if (diffX > 0) {
                        // Swipe right - go to previous header
                        for (i in currentPos - 1 downTo 0) {
                            val pageHeader = allPages[i].headerText
                            if (pageHeader != currentHeader && pageHeader != null) {
                                pageViewPager.currentItem = i
                                return true
                            }
                        }
                    } else {
                        // Swipe left - go to next header
                        for (i in currentPos + 1 until allPages.size) {
                            val pageHeader = allPages[i].headerText
                            if (pageHeader != currentHeader && pageHeader != null) {
                                pageViewPager.currentItem = i
                                return true
                            }
                        }
                    }
                }
                return false
            }
        })

        // Attach swipe to header area
        val headerTouchListener = View.OnTouchListener { _, event ->
            headerGestureDetector.onTouchEvent(event)
            false  // Don't consume, allow scrolling
        }

        // Apply to progress bar and indicator
        headerProgressBar.isClickable = true
        headerProgressBar.setOnTouchListener(headerTouchListener)

        headerIndicator.isClickable = true
        headerIndicator.setOnTouchListener(headerTouchListener)

        // Apply to navigation container
        headerNavigationContainer.isClickable = true
        headerNavigationContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        headerNavigationContainer.setOnTouchListener(headerTouchListener)

        // Apply to ScrollView and its children
        val headerScrollView = requireView().findViewById<View>(R.id.headerScrollView)
        headerScrollView.isClickable = true
        headerScrollView.setOnTouchListener(headerTouchListener)

        titleView.isClickable = true
        titleView.setOnTouchListener(headerTouchListener)

        headerTextView.isClickable = true
        headerTextView.setOnTouchListener(headerTouchListener)
    }

    /**
     * Update header display based on current page position
     */
    private fun updateHeaderDisplay(position: Int) {
        if (position >= allPages.size) return

        val currentPageWithHeader = allPages[position]
        val currentHeader = currentPageWithHeader.headerText

        if (currentHeader != null) {
            // Hide title when header is shown
            titleView.visibility = View.GONE

            headerTextView.visibility = View.VISIBLE
            headerTextView.text = currentHeader

            // Show header navigation when in a section with header
            headerNavigationContainer.visibility = View.VISIBLE

            // Find which header section we're in (count unique headers up to this point)
            val uniqueHeadersBefore = allPages.take(position + 1)
                .mapNotNull { it.headerText }
                .distinct()
            val currentHeaderIndex = uniqueHeadersBefore.indexOf(currentHeader)
            val totalHeaders = sectionsWithHeaders.size

            // Hide progress bar and indicator if only one header
            if (totalHeaders <= 1) {
                headerProgressBar.visibility = View.GONE
                headerIndicator.visibility = View.GONE
                prevHeaderButton.visibility = View.INVISIBLE
                nextHeaderButton.visibility = View.INVISIBLE
            } else {
                headerProgressBar.visibility = View.VISIBLE
                headerIndicator.visibility = View.VISIBLE

                headerIndicator.text = "${currentHeaderIndex + 1}/$totalHeaders"

                // Update header progress bar
                val progress = ((currentHeaderIndex + 1) * 100) / totalHeaders
                headerProgressBar.progress = progress

                // Check if there's a previous/next header available
                val hasPrevHeader = allPages.take(position).any { it.headerText != null && it.headerText != currentHeader }
                val hasNextHeader = allPages.drop(position + 1).any { it.headerText != null && it.headerText != currentHeader }

                prevHeaderButton.visibility = if (hasPrevHeader) View.VISIBLE else View.INVISIBLE
                nextHeaderButton.visibility = if (hasNextHeader) View.VISIBLE else View.INVISIBLE
            }
        } else {
            // Show title when no header
            titleView.visibility = View.VISIBLE

            headerTextView.visibility = View.GONE
            headerNavigationContainer.visibility = View.GONE
        }
    }

    /**
     * Load all pages into ViewPager (flat list from all sections)
     */
    private fun loadAllPages() {
        // Extract just the Page objects for the adapter
        val pages = allPages.map { it.page }
        val adapter = PageAdapter(pages)
        pageViewPager.adapter = adapter

        // Set up page change listener to update header display
        pageViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateHeaderDisplay(position)
                updatePageIndicator(position)
            }
        })

        // Restore saved position or start at first page
        val startPosition = if (savedPagePosition > 0 && savedPagePosition < allPages.size) {
            Log.d(TAG, "Restoring page position to $savedPagePosition")
            savedPagePosition
        } else {
            0
        }
        pageViewPager.setCurrentItem(startPosition, false)
        updateHeaderDisplay(startPosition)
        updatePageIndicator(startPosition)
    }

    /**
     * Update page indicator and navigation
     */
    private fun updatePageIndicator(position: Int) {
        if (position >= allPages.size) return

        val pageWithHeader = allPages[position]
        val page = pageWithHeader.page

        // Show navigation bar if there are multiple pages
        if (allPages.size > 1) {
            pageNavigationContainer.visibility = View.VISIBLE

            // Only show counter and progress if this is a repetition
            val isRepetition = page.repetitionIndex != null && page.totalRepetitions != null

            if (isRepetition) {
                pageIndicator.visibility = View.VISIBLE
                pageProgressBar.visibility = View.VISIBLE

                // Show repetition counter (e.g., "3/10")
                val repIndex = page.repetitionIndex ?: 1
                val repTotal = page.totalRepetitions ?: 1
                pageIndicator.text = "$repIndex/$repTotal"
                val progress = if (repTotal > 0) {
                    (repIndex * 100) / repTotal
                } else 100
                pageProgressBar.progress = progress
            } else {
                // No repetition - hide counter and progress bar
                pageIndicator.visibility = View.GONE
                pageProgressBar.visibility = View.GONE
            }

            // Hide arrows at absolute boundaries
            val isAbsoluteFirst = position == 0
            val isAbsoluteLast = position == allPages.size - 1

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::pageViewPager.isInitialized) {
            outState.putInt(KEY_CURRENT_PAGE, pageViewPager.currentItem)
            Log.d(TAG, "Saving state: current page = ${pageViewPager.currentItem}")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Fragment resumed - textEntity: ${textEntity.title}")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Fragment paused - textEntity: ${textEntity.title}")
    }

    companion object {
        private const val TAG = "TextReadingFragment"
        private const val ARG_TEXT = "text_entity"
        private const val KEY_CURRENT_PAGE = "current_page"

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
            val context = itemView.context

            // Use Glide-based ImageGetter for loading images
            val imageGetter = com.convocatis.app.utils.GlideImageGetter(
                context = context,
                textView = contentView,
                maxWidth = 800,
                maxHeight = 600
            )

            // Render HTML content with image support
            val spanned = HtmlCompat.fromHtml(
                page.content,
                HtmlCompat.FROM_HTML_MODE_LEGACY,
                imageGetter,
                null
            )

            contentView.text = spanned

            // Make links clickable
            contentView.movementMethod = LinkMovementMethod.getInstance()

            // Custom link click handler to open in browser/external app
            makeLinkClickable(spanned)
        }

        private fun makeLinkClickable(spanned: Spanned) {
            val urlSpans = spanned.getSpans(0, spanned.length, URLSpan::class.java)

            for (span in urlSpans) {
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                val flags = spanned.getSpanFlags(span)

                val clickable = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val url = span.url
                        val context = widget.context

                        try {
                            // Open URL in default browser/app
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("PageAdapter", "Error opening URL: $url", e)
                        }
                    }
                }

                (spanned as? android.text.SpannableString)?.setSpan(clickable, start, end, flags)
                (spanned as? android.text.SpannableStringBuilder)?.setSpan(clickable, start, end, flags)
            }
        }
    }
}
