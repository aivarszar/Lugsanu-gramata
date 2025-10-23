package com.convocatis.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.convocatis.app.ConvocatisApplication
import com.convocatis.app.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val profile = ConvocatisApplication.getInstance().getProfile()
        view.findViewById<TextView>(R.id.emailText).text = "Email: ${profile.email}"
        view.findViewById<TextView>(R.id.nameText).text = "Name: ${profile.name ?: "Not set"}"

        return view
    }
}
