package com.convocatis.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.convocatis.app.R
import com.convocatis.app.database.entity.TextEntity

class TextReadingFragment : Fragment() {

    private lateinit var textEntity: TextEntity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            textEntity = it.getParcelable(ARG_TEXT) ?: TextEntity()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_text_reading, container, false)

        val titleView = view.findViewById<TextView>(R.id.titleText)
        val contentView = view.findViewById<TextView>(R.id.contentText)

        titleView.text = textEntity.title
        contentView.text = textEntity.text

        return view
    }

    companion object {
        private const val ARG_TEXT = "text_entity"

        fun newInstance(textEntity: TextEntity) = TextReadingFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_TEXT, textEntity)
            }
        }
    }
}
