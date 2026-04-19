package com.example.studbuddy

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.example.studbuddy.core.SettingsManager
import com.example.studbuddy.core.UserManager
import com.example.studbuddy.core.db.StudBuddyDatabase
import com.example.studbuddy.core.models.User
import com.example.studbuddy.core.repository.StudBuddyRepository
import com.example.studbuddy.core.repository.SyncRepository
import com.example.studbuddy.core.workers.DailyMaintenanceWorker
import com.example.studbuddy.core.workers.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class StudBuddyApp : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var settingsManager: SettingsManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        scheduleDailyMaintenance()
        scheduleSync()
        initializeUser()
    }

    private fun scheduleSync() {
        val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "data_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }

    private fun initializeUser() {
        applicationScope.launch {
            val user = userManager.userFlow.first()
            if (user.id == "guest") {
                userManager.setUser(User(
                    id = UUID.randomUUID().toString(),
                    displayName = "Guest User"
                ))
            }
        }
    }

    fun scheduleDailyMaintenance() {
        applicationScope.launch {
            val summaryTime = settingsManager.dailySummaryTime.first()
            val maintenanceWork = PeriodicWorkRequestBuilder<DailyMaintenanceWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(calculateInitialDelay(summaryTime), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(this@StudBuddyApp).enqueueUniquePeriodicWork(
                "daily_maintenance",
                ExistingPeriodicWorkPolicy.KEEP,
                maintenanceWork
            )
        }
    }

    private fun calculateInitialDelay(summaryTime: String): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        val timeParts = summaryTime.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(calendar)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return target.timeInMillis - now
    }
}
