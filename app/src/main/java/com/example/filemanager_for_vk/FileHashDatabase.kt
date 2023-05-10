package com.example.filemanager_for_vk

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

@Entity(tableName = "file_hashes", primaryKeys = ["path"])
data class FileHash(
    val path: String,
    val hash: String
)

@Dao
interface FileHashDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(fileHash: FileHash)

    @Query("SELECT * FROM file_hashes")
    fun getAll(): List<FileHash>

    @Query("SELECT hash FROM file_hashes WHERE path = :filePath")
    fun getHash(filePath: String): String
}

@Database(entities = [FileHash::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileHashDao(): FileHashDao
}

@Suppress("SENSELESS_COMPARISON")
class FileHashDatabase (context: Context) {
    private val db: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "file_hashes.db"
    ).build()

    fun checkFileHashes(file_list: Array<File>?) : MutableList<ElementList> {
        val checkedFileOrDirList = mutableListOf<ElementList>()
        if (!file_list.isNullOrEmpty())
            for (file in file_list) {
                if (!file.isDirectory) {
                    db.fileHashDao().apply {
                        val filePath = file.absolutePath
                        val fileHash = getFileHash(file)
                        val status: Int = when {
                            getHash(filePath) == null -> 1
                            getHash(filePath) != fileHash -> 2
                            else -> 3
                        }
                        when (file.extension) {
                            "mp3", "wav" -> checkedFileOrDirList.add(ElementList(file.name, file.length()/ 1024, true, file.lastModified(), R.drawable.music, file.extension, status))
                            "mp4", "avi", "mkv" -> checkedFileOrDirList.add(ElementList(file.name, file.length()/ 1024, true, file.lastModified(), R.drawable.video, file.extension, status))
                            "jpeg", "jpg", "png" -> checkedFileOrDirList.add(ElementList(file.name, file.length()/ 1024, true, file.lastModified(), R.drawable.image, file.extension, status))
                            "txt", "doc", "pdf" -> checkedFileOrDirList.add(ElementList(file.name, file.length()/ 1024, true, file.lastModified(), R.drawable.text, file.extension, status))
                            else -> checkedFileOrDirList.add(ElementList(file.name, file.length()/ 1024, true, file.lastModified(), R.drawable.file, file.extension, status))
                        }
                        insert(FileHash(filePath, fileHash))
                    }
                }
                else
                    checkedFileOrDirList.add(ElementList(file.name, 0, false, 0, R.drawable.folder, "", 0))
            }
    return checkedFileOrDirList
    }

    private fun getFileHash(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        val inputStream = FileInputStream(file)
        val dataBytes = ByteArray(1024)
        var bytesRead: Int
        while (inputStream.read(dataBytes, 0, 1024).also { bytesRead = it } != -1) {
            md.update(dataBytes, 0, bytesRead)
        }
        val hashBytes = md.digest()
        val sb = StringBuilder()
        for (hashByte in hashBytes) {
            sb.append(((hashByte.toInt() and 0xff) + 0x100).toString(16).substring(1))
        }
        return sb.toString()
    }
}
