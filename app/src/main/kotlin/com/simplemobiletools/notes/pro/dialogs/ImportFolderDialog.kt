package com.simplemobiletools.notes.pro.dialogs

import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PROTECTION_NONE
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.notes.pro.R
import com.simplemobiletools.notes.pro.activities.SimpleActivity
import com.simplemobiletools.notes.pro.extensions.notesDB
import com.simplemobiletools.notes.pro.extensions.parseChecklistItems
import com.simplemobiletools.notes.pro.helpers.NotesHelper
import com.simplemobiletools.notes.pro.models.Note
import com.simplemobiletools.notes.pro.models.NoteType
import kotlinx.android.synthetic.main.dialog_import_folder.view.open_file_filename
import kotlinx.android.synthetic.main.dialog_import_folder.view.open_file_type
import java.io.File

class ImportFolderDialog(val activity: SimpleActivity, val path: String, val callback: () -> Unit) : AlertDialog.Builder(activity) {
    private var dialog: AlertDialog? = null

    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_import_folder, null) as ViewGroup).apply {
            open_file_filename.setText(activity.humanizePath(path))
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.import_folder) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val updateFilesOnEdit = view.open_file_type.checkedRadioButtonId == R.id.open_file_update_file
                        ensureBackgroundThread {
                            saveFolder(updateFilesOnEdit)
                        }
                    }
                }
            }
    }

    private fun saveFolder(updateFilesOnEdit: Boolean) {
        val folder = File(path)
        folder.listFiles { file ->
            val filename = file.path.getFilenameFromPath()
            when {
                file.isDirectory -> false
                filename.isMediaFile() -> false
                file.length() > 1000 * 1000 -> false
                activity.notesDB.getNoteIdWithTitle(filename) != null -> false
                else -> true
            }
        }?.forEach {
            val storePath = if (updateFilesOnEdit) it.absolutePath else ""
            val title = it.absolutePath.getFilenameFromPath()
            val value = if (updateFilesOnEdit) "" else it.readText()
            val fileText = it.readText().trim()
            val checklistItems = fileText.parseChecklistItems()
            if (checklistItems != null) {
                saveNote(title.substringBeforeLast('.'), fileText, NoteType.TYPE_CHECKLIST, "")
            } else {
                if (updateFilesOnEdit) {
                    activity.handleSAFDialog(path) {
                        saveNote(title, value, NoteType.TYPE_TEXT, storePath)
                    }
                } else {
                    saveNote(title, value, NoteType.TYPE_TEXT, storePath)
                }
            }
        }

        activity.runOnUiThread {
            callback()
            dialog?.dismiss()
        }
    }

    private fun saveNote(title: String, value: String, type: NoteType, path: String) {
        val note = Note(null, title, value, type, path, PROTECTION_NONE, "")
        NotesHelper(activity).insertOrUpdateNote(note)
    }
}
