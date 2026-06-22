package com.example.busschedule

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.busschedule.data.AppDatabase
import com.example.busschedule.data.BusSchedule
import com.example.busschedule.data.BusScheduleDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BusScheduleDaoTest {

    private lateinit var busScheduleDao: BusScheduleDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Menggunakan in-memory database agar data terhapus setelah proses test selesai
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        busScheduleDao = db.busScheduleDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndReadSchedule() = runBlocking {
        // 1. Buat data dummy
        val schedule = BusSchedule(id = 1, stopName = "Halte UI", arrivalTimeInMillis = 10000)

        // 2. Eksekusi fungsi insert
        busScheduleDao.insert(schedule)

        // 3. Eksekusi fungsi read dan pastikan data cocok
        val allSchedules = busScheduleDao.getAll().first()
        assertEquals(allSchedules[0].stopName, schedule.stopName)
    }
}