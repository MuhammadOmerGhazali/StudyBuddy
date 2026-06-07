package com.example.studbuddy.courses.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.setupWithNavController
import com.example.studbuddy.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CourseDetailFragment : Fragment() {

    private val sharedViewModel: CourseDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navHostFragment = childFragmentManager
            .findFragmentById(R.id.courseNavHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Pass arguments (courseId) to the child graph and shared ViewModel
        val courseId = arguments?.getString("courseId")
        sharedViewModel.setCourseId(courseId)

        val bundle = Bundle().apply {
            putString("courseId", courseId)
        }
        navController.setGraph(R.navigation.course_nav_graph, bundle)

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.courseBottomNavigation)
        bottomNav.setupWithNavController(navController)

        // Dynamically update the host toolbar title based on child destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            (activity as? AppCompatActivity)?.supportActionBar?.title = destination.label
        }
    }
}
