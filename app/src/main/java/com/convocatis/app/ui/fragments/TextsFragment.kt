package com.convocatis.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.convocatis.app.MainActivity
import com.convocatis.app.R
import com.convocatis.app.database.entity.TextEntity

class TextsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TextsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_texts, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = TextsAdapter { textEntity ->
            (activity as? MainActivity)?.showTextReadingFragment(textEntity)
        }
        recyclerView.adapter = adapter

        // Load texts from database
        loadTexts()

        return view
    }

    private fun loadTexts() {
        // TODO: Load from database using ViewModel
        // For now, show sample data
        val currentTime = System.currentTimeMillis()
        val sampleTexts = listOf(
            TextEntity(
                title = "Sample Prayer 1",
                text = "This is a sample prayer text...",
                createdAt = currentTime,
                updatedAt = currentTime
            ),
            TextEntity(
                title = "Sample Prayer 2",
                text = "Another sample prayer text...",
                createdAt = currentTime,
                updatedAt = currentTime
            ),
            TextEntity(
                title = "Sample Hymn",
                text = "A sample hymn text...",
                createdAt = currentTime,
                updatedAt = currentTime
            )
        )
        adapter.submitList(sampleTexts)
    }
}

class TextsAdapter(private val onItemClick: (TextEntity) -> Unit) :
    RecyclerView.Adapter<TextsAdapter.ViewHolder>() {

    private var texts = listOf<TextEntity>()

    fun submitList(newTexts: List<TextEntity>) {
        texts = newTexts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(texts[position])
    }

    override fun getItemCount() = texts.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView: TextView = view.findViewById(android.R.id.text1)
        private val subtitleView: TextView = view.findViewById(android.R.id.text2)

        fun bind(text: TextEntity) {
            titleView.text = text.title
            subtitleView.text = text.text.take(100) + "..."
            itemView.setOnClickListener { onItemClick(text) }
        }
    }
}
