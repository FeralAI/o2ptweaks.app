package dev.adrian.thortools.utils

import android.content.Context
import dev.adrian.thortools.AppSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

@Serializable
data class RecoveryImageRecord(
    val fileName: String,
    val slot: String,
    val partition: String,
    val patched: Boolean,
    val size: Long,
    val sha256: String,
    val buildIdentity: String,
    val sourceSha256: String = "",
) {
    fun matches(
        expectedSlot: String,
        expectedPartition: String,
        expectedPatched: Boolean,
        expectedBuildIdentity: String,
    ): Boolean =
        expectedBuildIdentity.isNotBlank() &&
            buildIdentity == expectedBuildIdentity &&
            slot == expectedSlot &&
            partition == expectedPartition &&
            patched == expectedPatched
}

data class RecoveryImageInput(
    val fileName: String,
    val slot: String,
    val partition: String,
    val patched: Boolean,
    val path: String,
    val buildIdentity: String,
    val sourceSha256: String = "",
)

@Serializable
private data class RecoveryManifestData(
    val records: List<RecoveryImageRecord> = emptyList(),
)

object RecoveryManifestStore {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun recordLocalImages(context: Context, inputs: List<RecoveryImageInput>): Boolean {
        if (inputs.isEmpty() || inputs.any { it.buildIdentity.isBlank() || (it.patched && it.sourceSha256.isBlank()) }) return false
        var invalid = false
        val records = inputs.mapNotNull { input ->
            val file = File(input.path)
            val hash = hashFile(input.path)
            if (!file.isFile || file.length() <= 0L || hash == null) {
                invalid = true
                null
            } else {
                RecoveryImageRecord(
                    fileName = input.fileName,
                    slot = input.slot,
                    partition = input.partition,
                    patched = input.patched,
                    size = file.length(),
                    sha256 = hash,
                    buildIdentity = input.buildIdentity,
                    sourceSha256 = input.sourceSha256,
                )
            }
        }
        if (invalid || records.size != inputs.size) return false
        val names = records.map { it.fileName }.toSet()
        return write(
            context,
            read(context).filterNot { it.fileName in names } + records,
        )
    }

    fun removePatchedRecords(context: Context, fileNames: Set<String>): Boolean {
        if (fileNames.isEmpty()) return true
        return write(context, read(context).filterNot { it.patched && it.fileName in fileNames })
    }

    fun verifiedStockSource(
        context: Context,
        slot: String,
        partition: String,
        localPath: String,
        downloadPath: String,
        buildIdentity: String,
    ): String? {
        val record = find(context, slot, partition, patched = false) ?: return null
        if (!record.matches(slot, partition, expectedPatched = false, buildIdentity)) return null
        if (verifyLocal(record, localPath)) return localPath
        return RootUtils.sha256FileRoot(context, downloadPath)
            ?.takeIf { it == record.sha256 }
            ?.let { downloadPath }
    }

    fun verifiedStockHash(
        context: Context,
        slot: String,
        partition: String,
        localPath: String,
        buildIdentity: String,
    ): String? {
        val record = find(context, slot, partition, patched = false) ?: return null
        if (!record.matches(slot, partition, expectedPatched = false, buildIdentity)) return null
        return record.sha256.takeIf { verifyLocal(record, localPath) }
    }

    fun hasVerifiedStockImage(
        context: Context,
        slot: String,
        partition: String,
        localPath: String,
        downloadPath: String,
        buildIdentity: String,
    ): Boolean = verifiedStockSource(
        context,
        slot,
        partition,
        localPath,
        downloadPath,
        buildIdentity,
    ) != null

    fun hasVerifiedPatchedImage(
        context: Context,
        slot: String,
        partition: String,
        localPath: String,
        stockPath: String,
        buildIdentity: String,
    ): Boolean {
        val patched = find(context, slot, partition, patched = true) ?: return false
        val stockHash = verifiedStockHash(
            context,
            slot,
            partition,
            stockPath,
            buildIdentity,
        ) ?: return false
        return patched.matches(slot, partition, expectedPatched = true, buildIdentity) &&
            patched.sourceSha256 == stockHash &&
            verifyLocal(patched, localPath)
    }

    fun records(context: Context): List<RecoveryImageRecord> = read(context)

    private fun find(context: Context, slot: String, partition: String, patched: Boolean): RecoveryImageRecord? =
        read(context).firstOrNull { record ->
            record.slot == slot && record.partition == partition && record.patched == patched
        }

    private fun verifyLocal(record: RecoveryImageRecord, path: String): Boolean {
        val file = File(path)
        return file.isFile && file.length() == record.size && hashFile(path) == record.sha256
    }

    private fun read(context: Context): List<RecoveryImageRecord> =
        AppSettings.getSharedPrefs(context)
            .getString(AppSettings.RECOVERY_MANIFEST_KEY, null)
            ?.let { encoded ->
                runCatching { json.decodeFromString<RecoveryManifestData>(encoded).records }
                    .getOrDefault(emptyList())
            }
            ?: emptyList()

    private fun write(context: Context, records: List<RecoveryImageRecord>): Boolean =
        runCatching {
            AppSettings.getSharedPrefs(context).edit()
                .putString(AppSettings.RECOVERY_MANIFEST_KEY, json.encodeToString(RecoveryManifestData(records)))
                .commit()
        }.getOrDefault(false)

    fun hashFile(path: String): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        File(path).inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            var read = input.read(buffer)
            while (read > 0) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }.getOrNull()
}
