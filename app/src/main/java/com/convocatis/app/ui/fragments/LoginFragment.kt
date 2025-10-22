package com.convocatis.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.convocatis.app.ConvocatisApplication
import com.convocatis.app.MainActivity
import com.convocatis.app.R
import com.convocatis.app.database.entity.ProfileEntity

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        val emailInput = view.findViewById<EditText>(R.id.emailInput)
        val loginButton = view.findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            if (email.isNotEmpty()) {
                // Simple login - just save email
                val profile = ProfileEntity(email = email, loggedIn = true)
                ConvocatisApplication.getInstance().saveProfile(profile)

                Toast.makeText(requireContext(), "Logged in successfully", Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.showTextsFragment()
            } else {
                Toast.makeText(requireContext(), "Please enter email", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
