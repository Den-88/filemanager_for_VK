package com.example.filemanager_for_vk

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

private const val PERMISSION_REQUEST_CODE = 100
private var myFileOrDirList = mutableListOf<ElementList>()
@SuppressLint("StaticFieldLeak")
private lateinit var listView: ListView
var current_directory : File = Environment.getExternalStorageDirectory().absoluteFile
var sort_type: Int = 1
data class ElementList(val fileOrDirName: String, val fileSize: Long, val itsFile: Boolean, val fileDate: Long, val imageId: Int, val ext: String, val status: Int)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FileHashDatabase(this)
        listView = findViewById(R.id.listView)
        checkPermission()
        val butPrem = findViewById<Button>(R.id.button)
        butPrem.setOnClickListener {
            checkPermission()
        }

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (current_directory != Environment.getExternalStorageDirectory().absoluteFile) {
                    current_directory = current_directory.parentFile as File
                    myFileOrDirList.clear()
                    getFiles(current_directory)
                }
                else {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_HOME)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)

                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sort_menu, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        myFileOrDirList.clear()
        getFiles(current_directory)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (current_directory != Environment.getExternalStorageDirectory().absoluteFile) {
                    current_directory = current_directory.parentFile as File
                    myFileOrDirList.clear()
                    getFiles(current_directory)
                }
                return true
            }
        }
        when (item.itemId) {
            R.id.sortByNameUp -> sort_type = 1
            R.id.sortByNameDw -> sort_type = 2
            R.id.sortBySizeUp -> sort_type = 3
            R.id.sortBySizeDw -> sort_type = 4
            R.id.sortByDateUp -> sort_type = 5
            R.id.sortByDateDw -> sort_type = 6
            R.id.sortByExtUp -> sort_type = 7
            R.id.sortByExtDw -> sort_type = 8
        }
        myFileOrDirList.clear()
        getFiles(current_directory)
        return true
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.fromParts("package", this.packageName, null))
                startActivity(intent)
            } else {
                val layPrem = findViewById<LinearLayout>(R.id.layoutSetPrem)
                layPrem.visibility = View.GONE
                getFiles(current_directory)
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            else {
                val layPrem = findViewById<LinearLayout>(R.id.layoutSetPrem)
                layPrem.visibility = View.GONE
                getFiles(current_directory)
            }
        }
    }

    private fun getFiles(storageDirectory: File) {
        val adapter = object : ArrayAdapter<ElementList>(this, R.layout.custom_list, myFileOrDirList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var convertMyView = convertView
                if (convertMyView == null) {
                    convertMyView = layoutInflater.inflate(R.layout.custom_list, parent, false)
                }
                val titleText = convertMyView?.findViewById<TextView>(R.id.title)
                val statusText = convertMyView?.findViewById<TextView>(R.id.status)
                val imageView = convertMyView?.findViewById<ImageView>(R.id.icon)
                val subtitleText = convertMyView?.findViewById<TextView>(R.id.description)
                val date = convertMyView?.findViewById<TextView>(R.id.date)
                val element = getItem(position)
                titleText!!.text = element?.fileOrDirName
                when (element?.status) {
                    1 -> { statusText!!.text = getString(R.string.status_new)
                        statusText.setTextColor(Color.BLUE) }
                    2 -> { statusText!!.text = getString(R.string.status_dif)
                        statusText.setTextColor(Color.RED) }
                    3 -> { statusText!!.text = getString(R.string.status_not_dif)
                        statusText.setTextColor(Color.GRAY) }
                    else -> statusText!!.text = getString(R.string.none_string)
                }
                imageView!!.setImageResource(element?.imageId!!)
                if (element.itsFile) {
                    subtitleText!!.text = getString(R.string.kb, element.fileSize.toString())
                    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US)
                    date!!.text = formatter.format(element.fileDate)
                }
                else {
                    subtitleText!!.text = getString(R.string.none_string)
                    date!!.text = getString(R.string.none_string)
                }
                return convertMyView!!
            }
        }
        getFile(storageDirectory, adapter)
        if (current_directory == Environment.getExternalStorageDirectory().absoluteFile)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        else
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        listView.adapter = adapter
        when (sort_type) {
            1 -> myFileOrDirList.sortBy { it.fileOrDirName }
            2 -> myFileOrDirList.sortByDescending { it.fileOrDirName }
            3 -> myFileOrDirList.sortBy { it.fileSize }
            4 -> myFileOrDirList.sortByDescending { it.fileSize }
            5 -> myFileOrDirList.sortBy { it.fileDate }
            6 -> myFileOrDirList.sortByDescending { it.fileDate }
            7 -> myFileOrDirList.sortBy { it.ext }
            8 -> myFileOrDirList.sortByDescending { it.ext }
        }
        adapter.notifyDataSetChanged()
        listView.isClickable = true
        listView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            val o: ElementList = listView.getItemAtPosition(position) as ElementList
            val str: String = o.fileOrDirName
            val newDirOrFile = File(storageDirectory.absolutePath + "/" + str)
            if (newDirOrFile.isDirectory) {
                current_directory = File(storageDirectory.absolutePath + "/" + str)
                myFileOrDirList.clear()
                getFiles(current_directory)
            }
            else {
                val myMime: MimeTypeMap = MimeTypeMap.getSingleton()
                val newIntent = Intent(Intent.ACTION_VIEW)
                val mimeType: String = myMime.getMimeTypeFromExtension(newDirOrFile.extension).toString()
                newIntent.setDataAndType(
                    FileProvider.getUriForFile(applicationContext, applicationContext.packageName.toString() +
                            ".provider", newDirOrFile), mimeType)
                newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                newIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                try {
                    startActivity(newIntent)
                } catch (e: ActivityNotFoundException) {
                    val chooserIntent = Intent.createChooser(newIntent, getString(R.string.choose_app))
                    startActivity(chooserIntent)
                }
            }
        }
        listView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            val o: ElementList = listView.getItemAtPosition(position) as ElementList
            val str: String = o.fileOrDirName
            val dirOrFile = File(storageDirectory.absolutePath + "/" + str)
            if (dirOrFile.isFile) {
                val uri = FileProvider.getUriForFile(this, getString(R.string.app_provider), dirOrFile)
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                shareIntent.type = contentResolver.getType(uri)
                shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text))
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
            }
            return@OnItemLongClickListener true
        }
    }

    private fun getFile(directory: File, adapter: ArrayAdapter<ElementList>) {
        val listFiles: Array<File>? = directory.listFiles()
        if (!listFiles.isNullOrEmpty()) {
            val runnable = Runnable {
                val newFileOrDirList = FileHashDatabase(this).checkFileHashes(listFiles)
                runOnUiThread {
                    myFileOrDirList.clear()
                    myFileOrDirList.addAll(newFileOrDirList)
                    when (sort_type) {
                        1 -> myFileOrDirList.sortBy { it.fileOrDirName }
                        2 -> myFileOrDirList.sortByDescending { it.fileOrDirName }
                        3 -> myFileOrDirList.sortBy { it.fileSize }
                        4 -> myFileOrDirList.sortByDescending { it.fileSize }
                        5 -> myFileOrDirList.sortBy { it.fileDate }
                        6 -> myFileOrDirList.sortByDescending { it.fileDate }
                        7 -> myFileOrDirList.sortBy { it.ext }
                        8 -> myFileOrDirList.sortByDescending { it.ext }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
            val thread = Thread(runnable)
            thread.start()
            for (file in listFiles) {
                if (file.isDirectory) {
                    myFileOrDirList.add(ElementList(file.name, 0, false, 0, R.drawable.folder, "", 0))
                } else {
                    when (file.extension) {
                        "mp3", "wav" -> myFileOrDirList.add(ElementList(file.name, file.length()/ 1024, true, file.lastModified(), R.drawable.music, file.extension, 0))
                        "mp4", "avi", "mkv" -> myFileOrDirList.add(ElementList(file.name, file.length()/ 1024, true, file.lastModified(), R.drawable.video, file.extension, 0))
                        "jpeg", "jpg", "png" -> myFileOrDirList.add(ElementList(file.name, file.length()/ 1024, true, file.lastModified(), R.drawable.image, file.extension, 0))
                        "txt", "doc", "pdf" -> myFileOrDirList.add(ElementList(file.name, file.length()/ 1024, true, file.lastModified(), R.drawable.text, file.extension, 0))
                        else -> myFileOrDirList.add(ElementList(file.name, file.length()/ 1024, true, file.lastModified(), R.drawable.file, file.extension, 0))
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val layPrem = findViewById<LinearLayout>(R.id.layoutSetPrem)
                layPrem.visibility = View.GONE
                getFiles(current_directory)
            } else {
                Toast.makeText(this, getString(R.string.not_acsess), Toast.LENGTH_SHORT).show()
            }
        }
    }
}



