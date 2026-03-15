package dev.benedek.syncthingandroid.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatEditText

/**
 * Apparently EditText blocks touch event propagation to the parent even
 * when disabled/not clickable/not focusable. Therefore we have to manually
 * check whether we are enabled and either ignore the event or process it normally. <br></br>
 * <br></br>
 * This class also blocks the default EditText behaviour of textMultiLine flag enforcing replacement
 * of the IME action button with the new line character. This allows rendering soft wraps on single
 * line input.
 */
class EnhancedEditText : AppCompatEditText {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        if (event.action == MotionEvent.ACTION_UP) super.performClick()
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return if (isEnabled)
            super.performClick()
        else
            false
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val conn = super.onCreateInputConnection(outAttrs)
        outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION.inv()
        return conn
    }
}
