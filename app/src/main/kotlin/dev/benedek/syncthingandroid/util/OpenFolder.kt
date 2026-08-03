package dev.benedek.syncthingandroid.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import dev.benedek.syncthingandroid.R

fun openFolder(context: Context, path: Uri) {
	val openFileManager = context.getString(R.string.open_file_manager)

	val intent = Intent(Intent.ACTION_VIEW)
	intent.setDataAndType(path, "resource/folder")
	intent.putExtra("org.openintents.extra.ABSOLUTE_PATH", path.path)
	intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
	if (intent.resolveActivity(context.packageManager) != null) {
		context.startActivity(intent)
	} else {
		Log.v(
			"FolderListItem",
			"openFolder: Fallback to application chooser to open folder."
		)
		intent.setDataAndType(path, "application/*")
		val chooserIntent = Intent.createChooser(intent, openFileManager)
		if (chooserIntent != null) {
			context.startActivity(chooserIntent)
		} else {
			Toast.makeText(context, R.string.toast_no_file_manager, Toast.LENGTH_SHORT).show()
		}
	}
}