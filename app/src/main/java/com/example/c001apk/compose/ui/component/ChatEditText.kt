package com.example.c001apk.compose.ui.component

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.InputFilter
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils.setAlphaComponent
import com.example.c001apk.compose.util.EmojiTextWatcher
import com.example.c001apk.compose.util.FastDeleteAtUserKeyListener
import com.example.c001apk.compose.util.OnTextInputListener

/**
 * Created by bggRGjQaUbCoE on 2024/6/20
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun ChatEditText(
    modifier: Modifier = Modifier,
    showSendBtn: (Boolean) -> Unit,
    updateInputText: (String) -> Unit,
    clearText: Boolean,
    resetClear: () -> Unit,
    clearFocus: Boolean,
    resetFocus: () -> Unit,
    clickedEmoji: String?,
    resetEmoji: () -> Unit,
    onCloseEmojiPanel: () -> Unit,
) {

    val primary = MaterialTheme.colorScheme.primary.toArgb()
    val onSurface = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            EditText(context).apply {
                highlightColor = setAlphaComponent(primary, 128)
                setTextColor(onSurface)
                setHintTextColor(onSurfaceVariant)
                maxLines = 4
                hint = "写私信..."
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setLineSpacing(0.0f, 1.2f)
                setBackgroundColor(Color.TRANSPARENT)
                filters = arrayOf(InputFilter.LengthFilter(500))
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            onCloseEmojiPanel()
                            requestFocus()
                        }
                    }
                    return@setOnTouchListener false
                }
                addTextChangedListener(
                    EmojiTextWatcher(
                        context = context,
                        size = textSize,
                        primary = primary,
                        onAfterTextChanged = {
                            if (text.toString().trim().isBlank()) {
                                showSendBtn(false)
                            } else {
                                showSendBtn(true)
                            }
                            updateInputText(text.toString())
                        },
                    )
                )
                addTextChangedListener(
                    OnTextInputListener(
                        text = "@",
                        onTextChange = {
                            // isFromAt = true
                            // launchAtTopic()
                        }
                    )
                )
                setOnKeyListener(FastDeleteAtUserKeyListener)
            }
        },
        update = { editText ->
            editText.highlightColor = setAlphaComponent(primary, 128)
            editText.setTextColor(onSurface)
            editText.setHintTextColor(onSurfaceVariant)
            if (clearText) {
                resetClear()
                editText.text = null
            }
            if (clearFocus) {
                resetFocus()
                editText.clearFocus()
                val inputMethodManager =
                    editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(
                    editText.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            }
            clickedEmoji?.let {
                resetEmoji()
                if (it == "[c001apk]")
                    editText.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                else
                    editText.editableText.replace(
                        editText.selectionStart, editText.selectionEnd, it
                    )
            }
        }
    )

}
