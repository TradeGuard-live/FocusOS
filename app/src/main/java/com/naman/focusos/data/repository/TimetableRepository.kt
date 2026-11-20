package com.naman.focusos.data.repository

import android.content.Context
import com.naman.focusos.data.db.FocusDatabase
import com.naman.focusos.data.model.TimetableEntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TimetableRepository(private val context: Context) {

    private val db = FocusDatabase.getInstance(context)
    private val timetableDao = db.timetableDao()

    fun getAllEntries(): Flow<List<TimetableEntryEntity>> = timetableDao.getAllEntries()
    fun getEnabledEntries(): Flow<List<TimetableEntryEntity>> = timetableDao.getEnabledEntries()

    suspend fun insertEntry(entry: TimetableEntryEntity): Long = withContext(Dispatchers.IO) {
        timetableDao.insertEntry(entry)
    }

    suspend fun updateEntry(entry: TimetableEntryEntity) = withContext(Dispatchers.IO) {
        timetableDao.updateEntry(entry)
    }

    suspend fun deleteEntry(entry: TimetableEntryEntity) = withContext(Dispatchers.IO) {
        timetableDao.deleteEntry(entry)
    }

    suspend fun toggleEntryEnabled(entry: TimetableEntryEntity) = withContext(Dispatchers.IO) {
        timetableDao.updateEntry(entry.copy(isEnabled = !entry.isEnabled))
    }
}
