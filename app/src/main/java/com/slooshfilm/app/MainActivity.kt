package com.slooshfilm.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.core.view.updatePadding
import android.content.res.Configuration
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavOptions
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.databinding.ActivityMainBinding
import com.slooshfilm.app.navigation.KeepStateNavigator

class MainActivity : AppCompatActivity() {

    internal lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // We don't need to do anything here, just ask for permission.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val localStorage = LocalStorage(this)
        // Apply the selected theme.
        applyCurrentTheme(localStorage)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        navController.navigatorProvider.addNavigator(
            KeepStateNavigator(this, navHostFragment.childFragmentManager, R.id.nav_host_fragment)
        )

        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        navGraph.setStartDestination(R.id.navigation_home)
        navController.graph = navGraph

        val bottomNavView: BottomNavigationView = binding.bottomNavView
        bottomNavView.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_SELECTED
        setupCustomBottomNav(bottomNavView, navController)

        // Apply insets to the container and DO NOT consume them
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = systemBars.left, right = systemBars.right)

            val navInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val extra = (12 * resources.displayMetrics.density).toInt()
            val shift = (navInset - extra).coerceAtLeast(0)
            binding.bottomNavCardContainer.translationY = -shift.toFloat()
            
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavView) { v, insets ->
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, 0)
            insets
        }

        // Setup BlurView
        val radius = 16f
        val decorView = window.decorView
        val windowBackground = decorView.background

        binding.blurView.setupWith(binding.blurTarget)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
            .setOverlayColor(ContextCompat.getColor(this, com.slooshfilm.app.R.color.scrim_color))
            .setBlurAutoUpdate(true)

        binding.bottomNavCardContainer.clipToOutline = true

        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (isNight) {
            val themeMode = localStorage.getThemeMode()
            if (themeMode == LocalStorage.THEME_MODE_AMOLED) {
                binding.bottomNavCardContainer.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.app_background_amoled)
                )
            } else {
                binding.bottomNavCardContainer.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.bottom_nav_card_dark)
                )
            }
        } else {
            binding.bottomNavCardContainer.setCardBackgroundColor(Color.TRANSPARENT)
        }

        val tintList = ContextCompat.getColorStateList(this, com.slooshfilm.app.R.color.bottom_nav_item_color)
        binding.bottomNavView.itemIconTintList = tintList
        binding.bottomNavView.itemTextColor = tintList

        askForNotificationPermission()

        intent.getStringExtra("UPDATE_URL")?.let { url ->
            try {
                com.slooshfilm.app.utils.AppUpdater(this).downloadAndInstall(url)
            } catch (_: Exception) {}
        }
    }

    private fun setupCustomBottomNav(bottomNav: BottomNavigationView, navController: NavController) {
        // This listener handles the visual state of the bottom navigation bar (which item is selected).
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.menu.forEach { item ->
                if (destination.hierarchy.any { it.id == item.itemId }) {
                    item.isChecked = true
                }
            }
        }

        // This listener handles the navigation actions when a menu item is tapped.
        bottomNav.setOnItemSelectedListener { item ->
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)

                .setEnterAnim(R.anim.fade_in)
                .setExitAnim(R.anim.fade_out)
                .setPopEnterAnim(R.anim.fade_in)
                .setPopExitAnim(R.anim.fade_out)
                .build()

            if (navController.currentDestination?.id == item.itemId) {
                return@setOnItemSelectedListener true
            }

            try {
                navController.navigate(item.itemId, null, navOptions)
                true
            } catch (_: Exception) {
                false
            }
        }

        bottomNav.setOnItemReselectedListener { }
    }

    private fun applyCurrentTheme(localStorage: LocalStorage) {
        val themeMode = localStorage.getThemeMode()

        if (themeMode == LocalStorage.THEME_MODE_AMOLED) {
            setTheme(R.style.Theme_Sloosh_Amoled)
        } else {
            AppCompatDelegate.setDefaultNightMode(themeMode)
        }
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
