package dev.benedek.syncthingandroid.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.io.Files
import dev.benedek.syncthingandroid.model.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class ShareViewModel : ViewModel() {
	val files: MutableMap<Uri, String> = mutableStateMapOf()
	var folders: List<Folder> by mutableStateOf(emptyList())
	var selectedFolderIndex: Int by mutableIntStateOf(0)
	var subDirectory: String by mutableStateOf("")
	var copyResult: CopyResult? by mutableStateOf(null)

	var showProgressDialog by mutableStateOf(false)
		private set


	fun removeFile(key: Uri) {
		files.remove(key)
	}


	fun copyFiles(
		contentResolver: ContentResolver
	) {
		if (folders.isEmpty()) return
		val folder = folders[selectedFolderIndex]
		if (folder.path == null) return
		val directory = File(folder.path, subDirectory)

		showProgressDialog = true

		viewModelScope.launch {
			var copied = 0
			var ignored = 0
			var error = false


			withContext(Dispatchers.IO) {
				for (entry in files.entries) {
					var inputStream: InputStream? = null
					try {
						val outFile = File(directory, entry.value)
						if (outFile.isFile) {
							ignored++
							continue
						}
						inputStream = contentResolver.openInputStream(entry.key)
						if (inputStream != null)
							Files.asByteSink(outFile).writeFrom(inputStream)
						copied++
					} catch (e: FileNotFoundException) {
						Log.e(
							this@ShareViewModel.toString(), String.format(
								"Can't find input file \"%s\" to copy",
								entry.key
							), e
						)
						error = true
					} catch (e: IOException) {
						Log.e(
							this@ShareViewModel.toString(), String.format(
								"IO exception during file \"%s\" sharing",
								entry.key
							), e
						)
						error = true
					} finally {
						try {
							inputStream?.close()
						} catch (e: IOException) {
							Log.w(
								this@ShareViewModel.toString(),
								"Exception on input/output stream close",
								e
							)
						}
					}
				}
			}

			copyResult = CopyResult(copied, ignored, folder.label ?: folder.path ?: "", error)

		}
	}

	companion object {
		data class CopyResult(
			val copied: Int,
			val ignored: Int,
			val folderLabel: String,
			val hasError: Boolean
		)
	}
}
