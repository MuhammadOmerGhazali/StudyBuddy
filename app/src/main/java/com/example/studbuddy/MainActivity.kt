package com.example.studbuddy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.NavOptions
import androidx.navigation.ui.NavigationUI
import com.example.studbuddy.core.notifications.NotificationHelper
import com.example.studbuddy.core.notifications.NotificationType
import com.google.android.material.navigation.NavigationView
import com.google.android.material.imageview.ShapeableImageView
import coil.load
import coil.transform.CircleCropTransformation
import android.widget.ImageView
import android.widget.TextView
import android.net.Uri
import com.example.studbuddy.core.SettingsManager
import com.example.studbuddy.core.UserManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var userManager: UserManager

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: androidx.navigation.NavController

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notifications disabled. You might miss class reminders.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.navigationView)
        
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Destinations where we want the hamburger icon instead of back button
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment, R.id.semesterFragment, R.id.coursesFragment, R.id.attendanceFragment,
                R.id.timetableFragment, R.id.assignmentsFragment, R.id.examsFragment,
                R.id.gpaFragment, R.id.settingsFragment
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        
        // Custom listener to ensure clicking "Courses" or "Semesters" in sidebar
        // always resets the view to the default/active context (clearing arguments).
        navView.setNavigationItemSelectedListener { item ->
            val handled = when (item.itemId) {
                R.id.coursesFragment -> {
                    // Navigate without arguments to show active semester
                    navController.navigate(R.id.coursesFragment, null, NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setRestoreState(true)
                        .setPopUpTo(navController.graph.startDestinationId, false, true)
                        .build())
                    true
                }
                R.id.semesterFragment -> {
                    navController.navigate(R.id.semesterFragment, null, NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setRestoreState(true)
                        .setPopUpTo(navController.graph.startDestinationId, false, true)
                        .build())
                    true
                }
                else -> {
                    NavigationUI.onNavDestinationSelected(item, navController)
                }
            }
            
            if (handled) {
                // Ensure the item is checked in the UI
                navView.setCheckedItem(item.itemId)
                drawerLayout.closeDrawers()
            }
            handled
        }

        // We still call this but it's partially overridden by our listener above
        // This is mainly to keep sync for back-press or direct navigate() calls
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            // This ensures sidebar selection stays in sync even if navigation happened elsewhere
            navView.setCheckedItem(destination.id)

            val isSemesterDrivenCourses = destination.id == R.id.coursesFragment && arguments?.getString("semesterId") != null
            val topLevelDestinations = mutableSetOf(
                R.id.homeFragment,
                R.id.semesterFragment,
                R.id.coursesFragment,
                R.id.attendanceFragment,
                R.id.timetableFragment,
                R.id.assignmentsFragment,
                R.id.examsFragment,
                R.id.gpaFragment,
                R.id.settingsFragment
            )

            if (isSemesterDrivenCourses || destination.id == R.id.profileFragment) {
                topLevelDestinations.remove(R.id.coursesFragment)
                topLevelDestinations.remove(R.id.profileFragment)
            }

            appBarConfiguration = AppBarConfiguration(topLevelDestinations, drawerLayout)
            setupActionBarWithNavController(navController, appBarConfiguration)
        }

        val headerView = navView.getHeaderView(0)
        val imgProfile = headerView.findViewById<ShapeableImageView>(R.id.imgHeaderProfile)
        val txtName = headerView.findViewById<TextView>(R.id.txtHeaderName)
        val txtEmail = headerView.findViewById<TextView>(R.id.txtHeaderEmail)

        lifecycleScope.launch {
            userManager.userFlow.collect { user ->
                txtName.text = user.displayName
                txtEmail.text = user.email ?: "Guest Mode"
                imgProfile.load(user.profileImageUri) {
                    crossfade(true)
                    placeholder(R.drawable.ic_person_24)
                    error(R.drawable.ic_person_24)
                    transformations(CircleCropTransformation())
                }
            }
        }

        lifecycleScope.launch {
            settingsManager.themeMode.collect { mode ->
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }

        NotificationHelper.createChannels(this)
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.navHostFragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
