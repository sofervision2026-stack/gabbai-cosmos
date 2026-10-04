package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GabbaiDao {
    // TRANSACTIONS
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY dateMillis DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("SELECT * FROM transactions ORDER BY id DESC LIMIT 1")
    suspend fun getLastTransaction(): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    // MEMBERS
    @Query("SELECT * FROM members ORDER BY role ASC, fullName ASC")
    fun getAllMembers(): Flow<List<MemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteMember(id: Long)

    // ALIYOT / PESUKIM
    @Query("SELECT * FROM aliyot ORDER BY dateMillis DESC")
    fun getAllAliyot(): Flow<List<AliyahEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAliyah(aliyah: AliyahEntity): Long

    @Query("UPDATE aliyot SET status = :status WHERE id = :id")
    suspend fun updateAliyahStatus(id: Long, status: TransactionStatus)

    @Query("DELETE FROM aliyot WHERE id = :id")
    suspend fun deleteAliyah(id: Long)

    // KOLLEL
    @Query("SELECT * FROM kollel_students ORDER BY fullName ASC")
    fun getAllKollelStudents(): Flow<List<KollelStudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKollelStudent(student: KollelStudentEntity): Long

    @Update
    suspend fun updateKollelStudent(student: KollelStudentEntity)

    @Query("SELECT * FROM kollel_attendance ORDER BY dateMillis DESC")
    fun getAllAttendance(): Flow<List<KollelAttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: KollelAttendanceEntity): Long

    // TZEDAKAH
    @Query("SELECT * FROM tzedakah_funds ORDER BY dateMillis DESC")
    fun getAllTzedakah(): Flow<List<TzedakahEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTzedakah(tzedakah: TzedakahEntity): Long

    @Query("DELETE FROM tzedakah_funds WHERE id = :id")
    suspend fun deleteTzedakah(id: Long)

    // NEEDY BENEFICIARIES (נזקקים)
    @Query("SELECT * FROM needy_beneficiaries ORDER BY dateRegisteredMillis DESC")
    fun getAllNeedyBeneficiaries(): Flow<List<NeedyBeneficiaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNeedyBeneficiary(beneficiary: NeedyBeneficiaryEntity): Long

    @Update
    suspend fun updateNeedyBeneficiary(beneficiary: NeedyBeneficiaryEntity)

    @Query("DELETE FROM needy_beneficiaries WHERE id = :id")
    suspend fun deleteNeedyBeneficiary(id: Long)

    // EVENTS
    @Query("SELECT * FROM synagogue_events ORDER BY dateMillis DESC")
    fun getAllEvents(): Flow<List<SynagogueEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SynagogueEventEntity): Long

    @Update
    suspend fun updateEvent(event: SynagogueEventEntity)

    @Query("DELETE FROM synagogue_events WHERE id = :id")
    suspend fun deleteEvent(id: Long)
}
