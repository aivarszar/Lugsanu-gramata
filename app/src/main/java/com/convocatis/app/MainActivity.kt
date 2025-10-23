package com.convocatis.app

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.convocatis.app.database.entity.TextEntity
import com.convocatis.app.ui.fragments.TextReadingFragment
import com.convocatis.app.ui.fragments.TextsFragment

class MainActivity : AppCompatActivity() {

    private var currentFragment: Fragment? = null
    var onSearchTermChangedListener: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.app_name)

        if (savedInstanceState == null) {
            showTextsFragment()
        }
    }

    private fun showFragment(fragment: Fragment, title: String) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(fragment is TextReadingFragment)
    }

    fun showTextsFragment() {
        showFragment(TextsFragment(), getString(R.string.app_name))
    }

    fun showTextReadingFragment(textEntity: TextEntity) {
        val fragment = TextReadingFragment.newInstance(textEntity)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { onSearchTermChangedListener?.invoke(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { onSearchTermChangedListener?.invoke(it) }
                return true
            }
        })

        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
