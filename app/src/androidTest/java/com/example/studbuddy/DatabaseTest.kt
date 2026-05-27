package com.example.studbuddy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.studbuddy.core.db.StudBuddyDatabase
import com.example.studbuddy.core.models.Semester
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: StudBuddyDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StudBuddyDatabase::class.java).build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeSemesterAndRead() = runBlocking {
        val semester = Semester(name = "Fall 2026", startDate = 1000L, endDate = 2000L)
        db.semesterDao().insert(semester)
        val result = db.semesterDao().getAll()
        assertEquals(semester.name, result.firstOrNull()?.name)
    }
}
