package com.convocatis.app.ui.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private var pages: List<TextContentParser.Page> = emptyList()

    private lateinit var viewPager: ViewPager2
    private lateinit var pageIndicator: TextView

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
        val view = inflater.inflate(R.layout.fragment_text_reading, container, false)

        val titleView = view.findViewById<TextView>(R.id.titleText)
        viewPager = view.findViewById(R.id.viewPager)
        pageIndicator = view.findViewById(R.id.pageIndicator)

        titleView.text = textEntity.title

        // Parse content with special codes
        lifecycleScope.launch {
            pages = parser.parseToPages(textEntity)

            if (pages.isNotEmpty()) {
                setupViewPager()
            } else {
                pageIndicator.text = "Kļūda: Neizdevās ielādēt tekstu"
            }
        }

        return view
    }

    private fun setupViewPager() {
        val adapter = PageAdapter(pages)
        viewPager.adapter = adapter

        // Update page indicator on swipe
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageIndicator(position)
            }
        })

        // Set initial page indicator
        updatePageIndicator(0)
    }

    private fun updatePageIndicator(position: Int) {
        val page = pages[position]
        pageIndicator.text = getString(R.string.page_indicator, page.pageNumber, page.totalPages)
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
