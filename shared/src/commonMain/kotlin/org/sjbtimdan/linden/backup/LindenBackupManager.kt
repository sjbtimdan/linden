package org.sjbtimdan.linden.backup

import app.cash.sqldelight.async.coroutines.awaitAsList
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.sjbtimdan.linden.db.LindenDatabase
import java.io.InputStream
import java.io.OutputStream

const val BACKUP_FORMAT_VERSION = 1

class LindenBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Summary of a restored backup, shown to the user after a successful restore. */
data class RestoreResult(
    val accounts: Int,
    val categories: Int,
    val entries: Int,
)

@Serializable
data class LindenBackup(
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val accounts: List<BackupAccount> = emptyList(),
    val categories: List<BackupCategory> = emptyList(),
    val entries: List<BackupEntry> = emptyList(),
    val settings: List<BackupSetting> = emptyList(),
    val fxRates: List<BackupFxRate> = emptyList(),
)

@Serializable
data class BackupAccount(val id: Long, val name: String, val currency: String, val initialBalance: Long)

@Serializable
data class BackupCategory(val id: Long, val name: String, val type: String)

@Serializable
data class BackupEntry(
    val id: Long,
    val type: String,
    val categoryId: Long? = null,
    val description: String? = null,
    val accountId: Long,
    val amount: Long,
    val toAccountId: Long? = null,
    val toAmount: Long? = null,
    val createdAt: Long,
    val createdZone: String,
)

@Serializable
data class BackupSetting(val key: String, val value: String)

@Serializable
data class BackupFxRate(
    val baseCurrency: String,
    val quoteCurrency: String,
    val rate: Double,
    val date: String,
    val fetchedAt: Long,
)

/**
 * Serializes the whole database to a versioned JSON backup and restores such
 * backups transactionally. Ids are preserved so references between tables stay
 * intact; a failed restore rolls back and leaves the database untouched.
 */
class LindenBackupManager(private val database: LindenDatabase) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun backupTo(output: OutputStream) {
        val bytes = json.encodeToString(readBackup()).encodeToByteArray()
        output.use { it.write(bytes) }
    }

    suspend fun restoreFrom(input: InputStream): RestoreResult {
        val backup = decode(input)
        val result = RestoreResult(
            accounts = backup.accounts.size,
            categories = backup.categories.size,
            entries = backup.entries.size,
        )
        database.transaction {
            database.entryQueries.deleteAll()
            database.categoryQueries.deleteAll()
            database.accountQueries.deleteAll()
            database.settingsQueries.deleteAll()
            database.fxRateQueries.deleteAll()
            backup.accounts.forEach { account ->
                database.accountQueries.insertWithId(account.id, account.name, account.currency, account.initialBalance)
            }
            backup.categories.forEach { category ->
                database.categoryQueries.insertWithId(category.id, category.name, category.type)
            }
            backup.entries.forEach { entry ->
                database.entryQueries.insertWithId(
                    id = entry.id,
                    type = entry.type,
                    categoryId = entry.categoryId,
                    description = entry.description,
                    accountId = entry.accountId,
                    amount = entry.amount,
                    toAccountId = entry.toAccountId,
                    toAmount = entry.toAmount,
                    createdAt = entry.createdAt,
                    createdZone = entry.createdZone,
                )
            }
            backup.settings.forEach { setting ->
                database.settingsQueries.insertOrReplace(setting.key, setting.value)
            }
            backup.fxRates.forEach { rate ->
                database.fxRateQueries.insertOrReplace(
                    rate.baseCurrency,
                    rate.quoteCurrency,
                    rate.rate,
                    rate.date,
                    rate.fetchedAt,
                )
            }
        }
        return result
    }

    private fun decode(input: InputStream): LindenBackup {
        val text = input.use { it.readBytes().decodeToString() }
        val backup = try {
            json.decodeFromString<LindenBackup>(text)
        } catch (e: SerializationException) {
            throw LindenBackupException("The backup is not a valid Linden backup: ${e.message}", e)
        }
        if (backup.formatVersion != BACKUP_FORMAT_VERSION) {
            throw LindenBackupException(
                "The backup was created by an incompatible app version " +
                    "(expected format $BACKUP_FORMAT_VERSION, found ${backup.formatVersion})",
            )
        }
        if (backup.accounts.isEmpty() && backup.categories.isEmpty() && backup.entries.isEmpty()) {
            throw LindenBackupException("The backup is empty and cannot be restored")
        }
        return backup
    }

    private suspend fun readBackup(): LindenBackup = LindenBackup(
        accounts = database.accountQueries.selectAll().awaitAsList().map { account ->
            BackupAccount(account.id, account.name, account.currency, account.initialBalance)
        },
        categories = database.categoryQueries.selectAll().awaitAsList().map { category ->
            BackupCategory(category.id, category.name, category.type)
        },
        entries = database.entryQueries.selectAllRows().awaitAsList().map { entry ->
            BackupEntry(
                id = entry.id,
                type = entry.type,
                categoryId = entry.category_id,
                description = entry.description,
                accountId = entry.account_id,
                amount = entry.amount,
                toAccountId = entry.to_account_id,
                toAmount = entry.to_amount,
                createdAt = entry.created_at,
                createdZone = entry.created_zone,
            )
        },
        settings = database.settingsQueries.selectAll().awaitAsList().map { setting ->
            BackupSetting(setting.key, setting.value_)
        },
        fxRates = database.fxRateQueries.selectAll().awaitAsList().map { rate ->
            BackupFxRate(rate.baseCurrency, rate.quoteCurrency, rate.rate, rate.date, rate.fetchedAt)
        },
    )
}
