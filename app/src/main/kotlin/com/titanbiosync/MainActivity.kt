package com.titanbiosync

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.MenuProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.titanbiosync.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Top-level destinations = schermate del BottomNavigation (max 5)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.dashboardFragment,
                R.id.startSessionFragment,
                R.id.gymLibraryFragment,
                R.id.progressListFragment,
                R.id.aiCoachFragment,
            )
        )

        // Aggancia la toolbar al NavController (titolo automatico + Up button quando serve)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Bottom navigation <-> navController
        binding.bottomNavigation.setupWithNavController(navController)

        // Aggiungi menu di overflow nella Toolbar per le voci rimosse dalla BottomNav
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_toolbar_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == android.R.id.home) {
                    return false
                }
                // onNavDestinationSelected naviga automaticamente se l'ID del menu combacia con quello del NavGraph
                return menuItem.onNavDestinationSelected(navController)
            }
        })

        // Nascondi toolbar e bottom nav sulla schermata di setup profilo
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isSetupScreen = destination.id == R.id.profileSetupFragment
            binding.toolbar.visibility = if (isSetupScreen) View.GONE else View.VISIBLE
            binding.bottomNavigation.visibility = if (isSetupScreen) View.GONE else View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}