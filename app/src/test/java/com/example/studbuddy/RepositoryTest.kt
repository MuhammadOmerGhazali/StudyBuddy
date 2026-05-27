package com.example.studbuddy

import com.example.studbuddy.core.db.*
import com.example.studbuddy.core.models.Semester
import com.example.studbuddy.core.repository.StudBuddyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class RepositoryTest {

    @Mock
    private lateinit var db: StudBuddyDatabase
    @Mock
    private lateinit var semesterDao: SemesterDao

    private lateinit var repository: StudBuddyRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(db.semesterDao()).thenReturn(semesterDao)
        repository = StudBuddyRepository(db)
    }

    @Test
    fun testGetSemester() = runBlocking {
        val semester = Semester(name = "Test Sem", startDate = 0, endDate = 0)
        `when`(semesterDao.getActiveSemester()).thenReturn(semester)

        val result = repository.getActiveSemester()
        assertEquals("Test Sem", result?.name)
    }

    @Test
    fun testGetSemesterFlow() = runBlocking {
        val semester = Semester(name = "Flow Sem", startDate = 0, endDate = 0)
        `when`(semesterDao.getActiveSemesterFlow()).thenReturn(flowOf(semester))

        val result = repository.getActiveSemesterFlow().first()
        assertEquals("Flow Sem", result?.name)
    }
}
