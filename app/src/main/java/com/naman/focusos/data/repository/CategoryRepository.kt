package com.naman.focusos.data.repository

import android.content.Context
import com.naman.focusos.data.db.FocusDatabase
import com.naman.focusos.data.model.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CategoryRepository(private val context: Context) {

    private val db = FocusDatabase.getInstance(context)
    private val categoryDao = db.categoryDao()

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun getCategoryById(id: Long): CategoryEntity? = withContext(Dispatchers.IO) {
        categoryDao.getCategoryById(id)
    }

    suspend fun insertCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category)
    }
}
