package com.convocatis.app

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.convocatis.app.ui.fragments.GroupsFragment
import com.convocatis.app.ui.fragments.LoginFragment
import com.convocatis.app.ui.fragments.NotificationsFragment
import com.convocatis.app.ui.fragments.ProfileFragment
import com.convocatis.app.ui.fragments.TextReadingFragment
import com.convocatis.app.ui.fragments.TextsFragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var menuLayout: View
    private var currentFragment: Fragment? = null

    var onSearchTermChangedListener: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_drawer)

        setupNavigationDrawer()
        setupCustomMenu()

        // Check if user is logged in
        val profile = ConvocatisApplication.getInstance().getProfile()
        if (savedInstanceState == null) {
            if (profile.email.isNullOrEmpty()) {
                showLoginFragment()
            } else {
                showTextsFragment()
            }
        }
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        toggle = ActionBarDrawerToggle(
            this, drawerLayout,
            R.string.app_name, R.string.app_name
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun setupCustomMenu() {
        menuLayout = findViewById(R.id.menu_layout)

        findViewById<View>(R.id.menu_texts).setOnClickListener {
            showTextsFragment()
            drawerLayout.closeDrawers()
        }

        findViewById<View>(R.id.menu_notifs).setOnClickListener {
            showNotificationsFragment()
            drawerLayout.closeDrawers()
        }

        findViewById<View>(R.id.menu_groups).setOnClickListener {
            val profile = ConvocatisApplication.getInstance().getProfile()
            if (!profile.nick.isNullOrEmpty()) {
                showGroupsFragment()
                drawerLayout.closeDrawers()
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_texts -> showTextsFragment()
            R.id.nav_notifications -> showNotificationsFragment()
            R.id.nav_groups -> showGroupsFragment()
            R.id.nav_profile -> showProfileFragment()
            R.id.nav_logout -> showLoginFragment(true)
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showFragment(fragment: Fragment, title: String, showMenu: Boolean = true) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()

        supportActionBar?.title = title
        menuLayout.visibility = if (showMenu) View.VISIBLE else View.GONE

        // Enable/disable drawer based on fragment
        if (fragment is LoginFragment || fragment is TextReadingFragment) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
    }

    fun showTextsFragment() {
        showFragment(TextsFragment(), "Texts", true)
    }

    fun showNotificationsFragment() {
        showFragment(NotificationsFragment(), "Notifications", true)
    }

    fun showGroupsFragment() {
        showFragment(GroupsFragment(), "Groups", true)
    }

    fun showProfileFragment() {
        showFragment(ProfileFragment(), "Profile", false)
    }

    fun showLoginFragment(doLogout: Boolean = false) {
        if (doLogout) {
            ConvocatisApplication.getInstance().clearProfile()
        }
        showFragment(LoginFragment(), "Convocatis", false)
    }

    fun showTextReadingFragment(textEntity: com.convocatis.app.database.entity.TextEntity) {
        val fragment = TextReadingFragment.newInstance(textEntity)
        showFragment(fragment, "Reading", false)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
