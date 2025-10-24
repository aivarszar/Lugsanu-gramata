package com.convocatis.app

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.convocatis.app.database.entity.TextEntity
import com.convocatis.app.ui.fragments.TextReadingFragment
import com.convocatis.app.ui.fragments.TextsFragment

class MainActivity : AppCompatActivity() {

    private var currentFragment: Fragment? = null
    var onSearchTermChangedListener: ((String) -> Unit)? = null

    private var sortMenuItem: MenuItem? = null
    private var favoritesMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.app_name)

        // Listen for back stack changes
        supportFragmentManager.addOnBackStackChangedListener {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            currentFragment = fragment
            supportActionBar?.setDisplayHomeAsUpEnabled(fragment is TextReadingFragment)
            invalidateOptionsMenu()
        }

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

        // Refresh menu to show/hide search based on fragment type
        invalidateOptionsMenu()
    }

    fun showTextsFragment() {
        showFragment(TextsFragment(), getString(R.string.app_name))
    }

    fun showTextReadingFragment(textEntity: TextEntity) {
        val fragment = TextReadingFragment.newInstance(textEntity)
        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Refresh menu to hide search in reading view
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // Hide search, sort, category filter, and favorites filter for TextReadingFragment
        val isReadingFragment = currentFragment is TextReadingFragment
        menu?.findItem(R.id.action_search)?.isVisible = !isReadingFragment
        menu?.findItem(R.id.action_category_filter)?.isVisible = !isReadingFragment
        menu?.findItem(R.id.action_sort_toggle)?.isVisible = !isReadingFragment
        menu?.findItem(R.id.action_filter_favorites)?.isVisible = !isReadingFragment

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

        // Store references to menu items for updating icons
        sortMenuItem = menu?.findItem(R.id.action_sort_toggle)
        favoritesMenuItem = menu?.findItem(R.id.action_filter_favorites)

        // Update icons based on current state
        updateMenuIcons()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_category_filter -> {
                val textsFragment = currentFragment as? TextsFragment
                textsFragment?.showCategoryFilterDialog()
                true
            }
            R.id.action_sort_toggle -> {
                val textsFragment = currentFragment as? TextsFragment
                textsFragment?.toggleSort()
                updateMenuIcons()
                true
            }
            R.id.action_filter_favorites -> {
                val textsFragment = currentFragment as? TextsFragment
                textsFragment?.toggleFavoritesFilter()
                updateMenuIcons()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateMenuIcons() {
        val textsFragment = currentFragment as? TextsFragment
        if (textsFragment != null) {
            // Update sort icon (arrow up for A-Z, arrow down for Z-A)
            val sortIcon = if (textsFragment.getSortAscending()) {
                android.R.drawable.arrow_up_float
            } else {
                android.R.drawable.arrow_down_float
            }
            sortMenuItem?.setIcon(sortIcon)

            // Update favorites filter icon (checkbox on/off)
            val favoritesIcon = if (textsFragment.getShowOnlyFavorites()) {
                android.R.drawable.checkbox_on_background
            } else {
                android.R.drawable.checkbox_off_background
            }
            favoritesMenuItem?.setIcon(favoritesIcon)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
