package com.convocatis.app.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Fragment for reading text with two-level navigation:
 * - Main text (headers) - top level navigation
 * - Sub text (pages) - bottom level navigation
 *
 * Based on the original Java implementation with simplified parsing logic
 */
class TextReadingFragment : Fragment() {

    private lateinit var textEntity: TextEntity
    private val pageDataList = ArrayList<PageData>()
    private var savedPagePosition: Int = 0
    private val database by lazy { ConvocatisApplication.getInstance().database }

    // Views
    private lateinit var mainTextScroll: View
    private lateinit var mainText: TextView
    private lateinit var separator: View
    private lateinit var pageViewPager: ViewPager2
    private lateinit var panel1: View
    private lateinit var panel2: View
    private lateinit var mainPanel: View
    private lateinit var subPanel: View
    private lateinit var mainProgress: TextView
    private lateinit var subProgress: TextView
    private lateinit var mainProgressBar: ProgressBar
    private lateinit var subProgressBar: ProgressBar
    private lateinit var prevMainButton: Button
    private lateinit var nextMainButton: Button
    private lateinit var prevSubButton: Button
    private lateinit var nextSubButton: Button

    /**
     * Data class representing a single page with main and sub text
     */
    data class PageData(
        val mainText: String?,
        val subText: String?,
        val mainProgress: Int,
        val mainCount: Int,
        val subProgress: Int,
        val subCount: Int,
        val secondaryProgress: Int,
        val secondaryCount: Int,
        val isRepeatedSegment: Boolean = false,  // True if segment has repetition (N^)
        val headerIndex: Int = 0,  // Index among headers only (0-based)
        val headerCount: Int = 0   // Total number of unique headers
    )

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
        mainTextScroll = view.findViewById(R.id.headerScrollView)
        mainText = view.findViewById(R.id.headerTextView)
        separator = view.findViewById(R.id.headerNavigationContainer) // Using as separator
        pageViewPager = view.findViewById(R.id.pageViewPager)
        panel1 = view.findViewById(R.id.headerNavigationContainer)
        panel2 = view.findViewById(R.id.pageNavigationContainer)
        mainPanel = panel1
        subPanel = panel2
        mainProgress = view.findViewById(R.id.headerIndicator)
        subProgress = view.findViewById(R.id.pageIndicator)
        mainProgressBar = view.findViewById(R.id.headerProgressBar)
        subProgressBar = view.findViewById(R.id.pageProgressBar)
        prevMainButton = view.findViewById(R.id.prevHeaderButton)
        nextMainButton = view.findViewById(R.id.nextHeaderButton)
        prevSubButton = view.findViewById(R.id.prevPageButton)
        nextSubButton = view.findViewById(R.id.nextPageButton)

        // Set toolbar title with "Convocatis – Text Name" format
        // Android automatically handles ellipsis truncation if text is too long
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.apply {
            title = "Convocatis – ${textEntity.title}"
        }

        // Set up navigation buttons
        setupNavigationButtons()

        // Set up swipe gesture for header section
        setupHeaderSwipeGesture()

        // Parse text in background
        lifecycleScope.launch {
            parseText()

            if (pageDataList.isEmpty()) {
                // No pages - go back
                activity?.onBackPressed()
                return@launch
            }

            // Setup ViewPager
            val adapter = PageAdapter(pageDataList)
            pageViewPager.adapter = adapter

            // Set up page change listener
            pageViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateUI(position)
                }
            })

            // Restore or set initial position
            val startPosition = if (savedPagePosition in 0 until pageDataList.size) {
                savedPagePosition
            } else {
                0
            }
            pageViewPager.setCurrentItem(startPosition, false)
            updateUI(startPosition)

            // Show panels if multiple pages
            if (pageDataList.size > 1) {
                panel1.visibility = View.VISIBLE
                panel2.visibility = View.VISIBLE
            }
        }

        return view
    }

    /**
     * Set up swipe gesture for header navigation
     * Simple manual detection - swipe left/right on header to navigate between headers
     */
    private fun setupHeaderSwipeGesture() {
        var startX = 0f
        var startY = 0f
        var isSwiping = false

        mainTextScroll.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    isSwiping = false
                    false // Let ScrollView handle it
                }
                MotionEvent.ACTION_MOVE -> {
                    val diffX = abs(event.x - startX)
                    val diffY = abs(event.y - startY)

                    // If horizontal movement is dominant and significant
                    if (diffX > 30 && diffX > diffY) {
                        isSwiping = true
                    }
                    false // Let ScrollView handle scrolling
                }
                MotionEvent.ACTION_UP -> {
                    if (isSwiping) {
                        val diffX = event.x - startX

                        // Swipe threshold: 100 pixels
                        if (abs(diffX) > 100) {
                            if (diffX > 0) {
                                // Swipe right - previous header
                                navigateToPreviousHeader()
                            } else {
                                // Swipe left - next header
                                navigateToNextHeader()
                            }
                            true // Consume event
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
    }

    /**
     * Navigate to next header's first page
     */
    private fun navigateToNextHeader() {
        val currentItem = pageViewPager.currentItem
        if (currentItem >= pageDataList.size) return

        val currentHeaderIndex = pageDataList[currentItem].headerIndex

        // Find first page with next headerIndex
        for (i in currentItem + 1 until pageDataList.size) {
            val page = pageDataList[i]
            if (page.headerIndex > currentHeaderIndex && page.mainText != null) {
                pageViewPager.currentItem = i
                break
            }
        }
    }

    /**
     * Navigate to previous header's first page
     */
    private fun navigateToPreviousHeader() {
        val currentItem = pageViewPager.currentItem
        if (currentItem >= pageDataList.size) return

        val currentHeaderIndex = pageDataList[currentItem].headerIndex

        // Find the previous headerIndex
        var targetHeaderIndex = -1
        for (i in currentItem - 1 downTo 0) {
            val page = pageDataList[i]
            if (page.headerIndex < currentHeaderIndex && page.mainText != null) {
                targetHeaderIndex = page.headerIndex
                break
            }
        }

        // If found, go to the FIRST page with this headerIndex
        if (targetHeaderIndex >= 0) {
            for (i in 0 until pageDataList.size) {
                val page = pageDataList[i]
                if (page.headerIndex == targetHeaderIndex && page.mainText != null) {
                    pageViewPager.currentItem = i
                    break
                }
            }
        }
    }

    /**
     * Set up navigation button click listeners
     */
    private fun setupNavigationButtons() {
        // Next main button - go to next header
        nextMainButton.setOnClickListener {
            navigateToNextHeader()
        }

        // Previous main button - go to previous header's first page
        prevMainButton.setOnClickListener {
            navigateToPreviousHeader()
        }

        // Next sub button - go to next page
        nextSubButton.setOnClickListener {
            val currentItem = pageViewPager.currentItem
            if (currentItem < pageDataList.size - 1) {
                pageViewPager.currentItem = currentItem + 1
            }
        }

        // Previous sub button - go to previous page
        prevSubButton.setOnClickListener {
            val currentItem = pageViewPager.currentItem
            if (currentItem > 0) {
                pageViewPager.currentItem = currentItem - 1
            }
        }
    }

    /**
     * Parse text content into pages
     * Based on the original Java parseText() logic
     */
    private suspend fun parseText() = withContext(Dispatchers.Default) {
        pageDataList.clear()

        val text = textEntity.rawContent
        var currentMainText: String? = null
        var currentRepetitionCount = 1
        val currentSubtexts = ArrayList<String>()

        var currentIndex = 0

        while (true) {
            val mainTextIndex = text.indexOf(">>", currentIndex)
            val subTextIndex = text.indexOf("|", currentIndex)

            if (mainTextIndex == -1 && subTextIndex == -1) {
                break
            }

            if (mainTextIndex != -1 && (subTextIndex == -1 || subTextIndex > mainTextIndex)) {
                // Found main text (header)

                // Process previous main/sub texts
                if (currentMainText != null || currentSubtexts.isNotEmpty()) {
                    processCurrentTexts(currentMainText, currentSubtexts, currentRepetitionCount)
                    currentSubtexts.clear()
                }

                val endMainTextIndex = text.indexOf("<<", mainTextIndex).takeIf { it != -1 } ?: text.length
                currentIndex = endMainTextIndex + 2

                currentMainText = text.substring(mainTextIndex + 2, endMainTextIndex)

                // Check for repetition (e.g., "3^Header text")
                val repetitionSignIndex = currentMainText.indexOf("^")
                currentRepetitionCount = 1

                if (repetitionSignIndex > -1) {
                    try {
                        val repetitionString = currentMainText.substring(0, repetitionSignIndex)
                        currentRepetitionCount = repetitionString.toInt().coerceIn(1, 1000)
                        currentMainText = currentMainText.substring(repetitionSignIndex + 1)
                    } catch (e: NumberFormatException) {
                        // Ignore if not a valid number
                    }
                }

                // Check for content after >> << (reference or text without |)
                if (currentIndex < text.length) {
                    when {
                        // Reference (e.g., ">>Header<<%50")
                        text[currentIndex] == '%' -> {
                            val referenceEndIndex = text.indexOfAny(charArrayOf('|', '>'), currentIndex + 1)
                                .takeIf { it != -1 } ?: text.length
                            val referenceText = text.substring(currentIndex, referenceEndIndex).trim()
                            currentIndex = referenceEndIndex

                            // Resolve reference and add as sub text
                            val resolvedText = resolveReference(referenceText)
                            if (resolvedText != null) {
                                currentSubtexts.add(resolvedText)
                            }
                        }
                        // Text without | (e.g., ">>Header<<\nPIRMAIS NOSLĒPUMS")
                        text[currentIndex] != '|' && text[currentIndex] != '>' -> {
                            // Skip whitespace
                            while (currentIndex < text.length && text[currentIndex].isWhitespace()) {
                                currentIndex++
                            }

                            // Read until next >> or |
                            val contentEndIndex1 = text.indexOf(">>", currentIndex).takeIf { it != -1 } ?: Int.MAX_VALUE
                            val contentEndIndex2 = text.indexOf("|", currentIndex).takeIf { it != -1 } ?: Int.MAX_VALUE
                            val contentEndIndex = minOf(contentEndIndex1, contentEndIndex2, text.length)

                            if (contentEndIndex > currentIndex) {
                                val contentText = text.substring(currentIndex, contentEndIndex).trim()
                                if (contentText.isNotEmpty()) {
                                    currentSubtexts.add(contentText)
                                }
                                currentIndex = contentEndIndex
                            }
                        }
                    }
                }
            } else {
                // Found sub text (page)
                val endSubTextIndex1 = text.indexOf(">>", subTextIndex + 1).takeIf { it != -1 } ?: Int.MAX_VALUE
                val endSubTextIndex2 = text.indexOf("|", subTextIndex + 1).takeIf { it != -1 } ?: Int.MAX_VALUE
                val endSubTextIndex = minOf(endSubTextIndex1, endSubTextIndex2, text.length)

                currentIndex = endSubTextIndex

                var subText = text.substring(subTextIndex + 1, endSubTextIndex)

                // Check for repetition (e.g., "10^Page content")
                val repetitionSignIndex = subText.indexOf("^")
                var repetitionCount = 1

                if (repetitionSignIndex > -1) {
                    try {
                        val repetitionString = subText.substring(0, repetitionSignIndex)
                        repetitionCount = repetitionString.toInt().coerceIn(1, 1000)
                        subText = subText.substring(repetitionSignIndex + 1)
                    } catch (e: NumberFormatException) {
                        // Ignore if not a valid number
                    }
                }

                // Check if sub text is a reference (e.g., "%50")
                val finalSubText = if (subText.trim().startsWith("%")) {
                    resolveReference(subText.trim()) ?: subText
                } else {
                    subText
                }

                // Add repeated sub texts
                repeat(repetitionCount) {
                    currentSubtexts.add(finalSubText)
                }
            }

            if (currentIndex >= text.length) {
                break
            }
        }

        // Process remaining texts
        if (currentMainText != null || currentSubtexts.isNotEmpty()) {
            processCurrentTexts(currentMainText, currentSubtexts, currentRepetitionCount)
        }

        // If no pages were created, add the raw text as a single page
        if (pageDataList.isEmpty()) {
            addPageData(null, text, 0, 1, 0, 1, 0, 1)
        }

        // Recalculate segment progress for proper counting
        recalculateSegmentProgress()
    }

    /**
     * Recalculate segment progress to group consecutive pages
     * Grouping rules:
     * - Group consecutive non-repeated pages (isRepeatedSegment=false) with same mainText
     * - Group consecutive repeated pages (isRepeatedSegment=true) with same subText and mainText
     * - Header change starts a new group
     * Also calculates header indices for top navigation
     */
    private fun recalculateSegmentProgress() {
        if (pageDataList.isEmpty()) return

        // Group consecutive pages by header and repetition status
        val segments = mutableListOf<MutableList<Int>>()
        var currentSegment = mutableListOf<Int>()
        var prevMainText: String? = null
        var prevSubText: String? = null
        var prevIsRepeated: Boolean? = null

        pageDataList.forEachIndexed { index, page ->
            val mainText = page.mainText
            val subText = page.subText
            val isRepeated = page.isRepeatedSegment

            // Start new segment if:
            // 1. mainText (header) changed
            // 2. isRepeated status changed
            // 3. If isRepeated=true, subText changed (repeated content changed)
            val shouldStartNewSegment = if (prevMainText != null) {
                // Header changed
                mainText != prevMainText ||
                // Repetition status changed
                isRepeated != prevIsRepeated ||
                // For repeated segments, subText must be same
                (isRepeated && subText != prevSubText)
            } else {
                false
            }

            if (shouldStartNewSegment) {
                if (currentSegment.isNotEmpty()) {
                    segments.add(currentSegment)
                    currentSegment = mutableListOf()
                }
            }

            currentSegment.add(index)
            prevMainText = mainText
            prevSubText = subText
            prevIsRepeated = isRepeated
        }

        // Add final segment
        if (currentSegment.isNotEmpty()) {
            segments.add(currentSegment)
        }

        // Calculate header indices - find unique headers in order
        val headersInOrder = mutableListOf<String>()
        val headerToIndex = mutableMapOf<String, Int>()

        pageDataList.forEach { page ->
            val header = page.mainText
            if (header != null && !headerToIndex.containsKey(header)) {
                headerToIndex[header] = headersInOrder.size
                headersInOrder.add(header)
            }
        }

        val totalHeaders = headersInOrder.size

        // Update pages with new segment-based progress and header info
        segments.forEachIndexed { segmentIndex, pageIndices ->
            pageIndices.forEachIndexed { positionInSegment, pageIndex ->
                val oldPage = pageDataList[pageIndex]
                val headerIdx = if (oldPage.mainText != null) {
                    headerToIndex[oldPage.mainText] ?: 0
                } else {
                    0
                }

                pageDataList[pageIndex] = oldPage.copy(
                    mainProgress = segmentIndex,
                    mainCount = segments.size,
                    subProgress = positionInSegment,
                    subCount = pageIndices.size,
                    headerIndex = headerIdx,
                    headerCount = totalHeaders
                )
            }
        }
    }

    /**
     * Resolve reference to another text (e.g., "%50" -> text with RID=50)
     */
    private suspend fun resolveReference(reference: String): String? {
        return try {
            // Extract RID from reference (e.g., "%50" -> 50)
            val ridString = reference.trim().removePrefix("%")
            val rid = ridString.toLongOrNull() ?: return null

            // Load text from database
            val referencedText = withContext(Dispatchers.IO) {
                database.textDao().getTextByRid(rid)
            }

            // Return raw content
            referencedText?.rawContent
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving reference: $reference", e)
            null
        }
    }

    /**
     * Process current main text and sub texts into pages
     */
    private fun processCurrentTexts(
        mainText: String?,
        subTexts: List<String>,
        repetitionCount: Int
    ) {
        if (subTexts.isEmpty()) {
            // Only main text, repeat it
            val isRepeated = repetitionCount > 1
            repeat(repetitionCount) { i ->
                addPageData(mainText, null, i, repetitionCount, 0, 1, i, repetitionCount, isRepeated)
            }
        } else {
            val actualRepetitionCount = if (mainText == null) 1 else repetitionCount

            repeat(actualRepetitionCount) { c ->
                var subRepetition = 0
                var subCount = 0
                var prevSubText: String? = null

                for (i in subTexts.indices) {
                    val subText = subTexts[i]

                    if (prevSubText == null || prevSubText != subText) {
                        subRepetition = 0
                        prevSubText = subText

                        // Count consecutive identical sub texts
                        subCount = 0
                        for (j in i until subTexts.size) {
                            if (subTexts[j] == subText) {
                                subCount++
                            } else {
                                break
                            }
                        }
                    } else {
                        subRepetition++
                    }

                    // Mark as repeated if subCount > 1
                    val isRepeated = subCount > 1

                    addPageData(
                        mainText,
                        subText,
                        c,
                        actualRepetitionCount,
                        subRepetition,
                        subCount,
                        c * subTexts.size + i,
                        actualRepetitionCount * subTexts.size,
                        isRepeated
                    )
                }
            }
        }
    }

    /**
     * Add a page to the data list
     */
    private fun addPageData(
        mainText: String?,
        subText: String?,
        mainProgress: Int,
        mainCount: Int,
        subProgress: Int,
        subCount: Int,
        secondaryProgress: Int,
        secondaryCount: Int,
        isRepeated: Boolean = false
    ) {
        pageDataList.add(
            PageData(
                mainText = mainText,
                subText = subText,
                mainProgress = mainProgress,
                mainCount = mainCount,
                subProgress = subProgress,
                subCount = subCount,
                secondaryProgress = secondaryProgress,
                secondaryCount = secondaryCount,
                isRepeatedSegment = isRepeated
            )
        )
    }

    /**
     * Update UI based on current page position
     */
    private fun updateUI(position: Int) {
        if (position >= pageDataList.size) return

        val data = pageDataList[position]

        // Update main text panel (headers navigation)
        if (data.mainText == null || data.headerCount <= 1) {
            // Hide main panel if no header or only 1 header total
            mainPanel.visibility = View.GONE
        } else {
            mainPanel.visibility = View.VISIBLE
            mainProgress.text = "${data.headerIndex + 1}/${data.headerCount}"
            mainProgressBar.max = data.headerCount - 1
            mainProgressBar.progress = data.headerIndex

            // Show/hide main navigation buttons based on header position
            val isFirstHeader = data.headerIndex == 0
            val isLastHeader = data.headerIndex == data.headerCount - 1

            prevMainButton.visibility = if (isFirstHeader) View.INVISIBLE else View.VISIBLE
            nextMainButton.visibility = if (isLastHeader) View.INVISIBLE else View.VISIBLE
        }

        // Update sub text panel (pages navigation)
        if (data.subText == null) {
            // Hide sub panel if no sub text
            subPanel.visibility = View.GONE
        } else {
            // Determine if we need to show the panel
            val hasMultiplePagesInSegment = data.subCount > 1
            val hasMultiplePagesTotal = pageDataList.size > 1

            if (!hasMultiplePagesInSegment && !hasMultiplePagesTotal) {
                // Only 1 page total - hide everything
                subPanel.visibility = View.GONE
            } else {
                subPanel.visibility = View.VISIBLE

                // Show/hide progress indicator and bar based on segment page count
                if (hasMultiplePagesInSegment) {
                    subProgress.visibility = View.VISIBLE
                    subProgressBar.visibility = View.VISIBLE
                    subProgress.text = "${data.subProgress + 1}/${data.subCount}"
                    subProgressBar.max = data.subCount - 1
                    subProgressBar.progress = data.subProgress

                    // Change color based on whether segment has repetition
                    // Green for non-repeated segments (single pages)
                    // Blue for repeated segments (N^ notation)
                    val progressColor = if (data.isRepeatedSegment) {
                        android.graphics.Color.parseColor("#2196F3") // Blue for repeated (N^)
                    } else {
                        android.graphics.Color.parseColor("#4CAF50") // Green for single pages
                    }
                    subProgressBar.progressTintList = android.content.res.ColorStateList.valueOf(progressColor)
                    subProgress.setTextColor(progressColor)
                } else {
                    // Hide progress indicator/bar if only 1 page in segment (1/1 case)
                    subProgress.visibility = View.GONE
                    subProgressBar.visibility = View.GONE
                }

                // Show/hide navigation buttons based on global position
                val isFirstPage = position == 0
                val isLastPage = position == pageDataList.size - 1

                prevSubButton.visibility = if (isFirstPage) View.INVISIBLE else View.VISIBLE
                nextSubButton.visibility = if (isLastPage) View.INVISIBLE else View.VISIBLE
            }
        }

        // Update main text and separator visibility
        separator.visibility = View.VISIBLE
        pageViewPager.visibility = View.VISIBLE
        mainTextScroll.visibility = View.VISIBLE

        if (data.mainText.isNullOrEmpty() || data.subText.isNullOrEmpty()) {
            separator.visibility = View.GONE
        }

        if (data.mainText.isNullOrEmpty()) {
            mainTextScroll.visibility = View.GONE
            mainText.visibility = View.GONE
        } else if (data.subText.isNullOrEmpty()) {
            pageViewPager.visibility = View.GONE
        }

        // Set main text and make it visible
        if (!data.mainText.isNullOrEmpty()) {
            mainText.text = data.mainText
            mainText.visibility = View.VISIBLE
        } else {
            mainText.visibility = View.GONE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::pageViewPager.isInitialized) {
            outState.putInt(KEY_CURRENT_PAGE, pageViewPager.currentItem)
            Log.d(TAG, "Saving state: current page = ${pageViewPager.currentItem}")
        }
    }

    private fun getDefaultEntity() = TextEntity(
        rid = 0,
        title = "",
        rawContent = "",
        languageCode = "lv"
    )

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
 * ViewPager2 adapter for page content
 */
class PageAdapter(private val pages: List<TextReadingFragment.PageData>) :
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

        fun bind(page: TextReadingFragment.PageData) {
            val context = itemView.context

            // Use sub text as content
            val content = page.subText ?: page.mainText ?: ""

            // Use Glide-based ImageGetter for loading images
            val imageGetter = com.convocatis.app.utils.GlideImageGetter(
                context = context,
                textView = contentView,
                maxWidth = 800,
                maxHeight = 600
            )

            // Convert line breaks (\n) to HTML <br> tags for proper rendering
            val contentWithBreaks = content.replace("\n", "<br>")

            // Render HTML content with image support
            val spanned = HtmlCompat.fromHtml(
                contentWithBreaks,
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

        private fun makeLinkClickable(spanned: android.text.Spanned) {
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
