package com.naman.focusos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.naman.focusos.data.model.RecurrenceRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurrenceRuleDao {
    @Query("SELECT * FROM recurrence_rules WHERE id = :id LIMIT 1")
    suspend fun getRuleById(id: Long): RecurrenceRuleEntity?

    @Query("SELECT * FROM recurrence_rules WHERE id = :id LIMIT 1")
    fun getRuleByIdSync(id: Long): RecurrenceRuleEntity?

    @Query("SELECT * FROM recurrence_rules")
    suspend fun getAllRules(): List<RecurrenceRuleEntity>

    @Query("SELECT * FROM recurrence_rules")
    fun getAllRulesSync(): List<RecurrenceRuleEntity>

    @Query("SELECT * FROM recurrence_rules")
    fun getAllRulesFlow(): Flow<List<RecurrenceRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RecurrenceRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<RecurrenceRuleEntity>): List<Long>

    @Update
    suspend fun updateRule(rule: RecurrenceRuleEntity)

    @Delete
    suspend fun deleteRule(rule: RecurrenceRuleEntity)

    @Query("DELETE FROM recurrence_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("DELETE FROM recurrence_rules")
    suspend fun deleteAllRules()
}
