package com.example.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Full snapshot of every table. Encrypted fields stay encrypted inside the file. */
data class GabbaiBackup(
    val format: String = "gabbai-cosmos-backup",
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val members: List<MemberEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val aliyot: List<AliyahEntity> = emptyList(),
    val kollelStudents: List<KollelStudentEntity> = emptyList(),
    val kollelAttendance: List<KollelAttendanceEntity> = emptyList(),
    val tzedakah: List<TzedakahEntity> = emptyList(),
    val needy: List<NeedyBeneficiaryEntity> = emptyList(),
    val events: List<SynagogueEventEntity> = emptyList()
)

object BackupManager {
    private val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(GabbaiBackup::class.java).indent("  ")

    private fun prefs(context: Context) = context.getSharedPreferences("gabbai_data", Context.MODE_PRIVATE)

    /** Seeding of demo data happens only once; after a wipe the app stays empty. */
    fun isSeeded(context: Context) = prefs(context).getBoolean("seeded", false)
    fun markSeeded(context: Context) = prefs(context).edit().putBoolean("seeded", true).apply()

    suspend fun export(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).gabbaiDao()
        val backup = GabbaiBackup(
            members = dao.getAllMembers().first(),
            transactions = dao.getAllTransactions().first(),
            aliyot = dao.getAllAliyot().first(),
            kollelStudents = dao.getAllKollelStudents().first(),
            kollelAttendance = dao.getAllAttendance().first(),
            tzedakah = dao.getAllTzedakah().first(),
            needy = dao.getAllNeedyBeneficiaries().first(),
            events = dao.getAllEvents().first()
        )
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(adapter.toJson(backup).toByteArray(Charsets.UTF_8)) }
            ?: error("cannot open file")
        backup.members.size + backup.transactions.size + backup.tzedakah.size + backup.needy.size +
            backup.aliyot.size + backup.kollelStudents.size + backup.kollelAttendance.size + backup.events.size
    }

    /** Replaces all current data with the file content. Throws if the file is not a valid backup. */
    suspend fun restore(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("cannot open file")
        val b = adapter.fromJson(text) ?: error("empty")
        require(b.format == "gabbai-cosmos-backup") { "not a Gabbai backup" }
        val db = AppDatabase.getDatabase(context)
        val dao = db.gabbaiDao()
        db.withTransaction {
            db.clearAllTables()
            b.members.forEach { dao.insertMember(it) }
            b.transactions.forEach { dao.insertTransaction(it) }
            b.aliyot.forEach { dao.insertAliyah(it) }
            b.kollelStudents.forEach { dao.insertKollelStudent(it) }
            b.kollelAttendance.forEach { dao.insertAttendance(it) }
            b.tzedakah.forEach { dao.insertTzedakah(it) }
            b.needy.forEach { dao.insertNeedyBeneficiary(it) }
            b.events.forEach { dao.insertEvent(it) }
        }
        markSeeded(context)
        b.members.size + b.transactions.size + b.tzedakah.size + b.needy.size +
            b.aliyot.size + b.kollelStudents.size + b.kollelAttendance.size + b.events.size
    }

    suspend fun wipeAll(context: Context) = withContext(Dispatchers.IO) {
        markSeeded(context)
        AppDatabase.getDatabase(context).clearAllTables()
    }
}
