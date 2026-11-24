package com.example.android_echo_music.ui.common

import android.content.DialogInterface
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import coil.load
import com.example.android_echo_music.R
import com.example.android_echo_music.databinding.DialogTextInputBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 加载歌曲封面（[C] 维护）。
 *
 * - [uri] 为空时清空图片，由调用方显示音符占位图标；
 * - 加载失败时退化为占位图，避免列表里出现空白方块。
 */
fun ImageView.loadCoverArt(uri: String?) {
    if (uri.isNullOrBlank()) {
        setImageDrawable(null)
        return
    }
    load(uri) {
        crossfade(true)
        placeholder(R.drawable.bg_cover_placeholder)
        error(R.drawable.ic_music_note)
    }
}

/**
 * 统一的「输入名称」对话框：新建 / 重命名歌单共用。
 *
 * 名称为空时在输入框内提示，不关闭对话框；确认后回调已 trim 的名称。
 */
fun Fragment.showNameInputDialog(
    @StringRes titleRes: Int,
    @StringRes hintRes: Int,
    initialName: String = "",
    onConfirm: (String) -> Unit
) {
    val dialogBinding = DialogTextInputBinding.inflate(layoutInflater)
    dialogBinding.nameInputLayout.hint = getString(hintRes)
    dialogBinding.nameInput.setText(initialName)
    dialogBinding.nameInput.setSelection(initialName.length)

    val dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(titleRes)
        .setView(dialogBinding.root)
        .setNegativeButton(R.string.action_cancel, null)
        .setPositiveButton(R.string.action_confirm, null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val name = dialogBinding.nameInput.text?.toString().orEmpty().trim()
            if (name.isEmpty()) {
                dialogBinding.nameInputLayout.error = getString(R.string.error_name_empty)
                return@setOnClickListener
            }
            dialogBinding.nameInputLayout.error = null
            dialog.dismiss()
            onConfirm(name)
        }
    }
    dialog.show()
}