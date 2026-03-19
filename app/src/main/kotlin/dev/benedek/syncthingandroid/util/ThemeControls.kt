package dev.benedek.syncthingandroid.util

import android.content.res.Configuration.UI_MODE_NIGHT_YES

/**
 * [ThemeControls] stores the global state of theming controls.
 * It stores 2 fields: [.useDynamicColor] and [.useDarkMode],
 * both of them are Boolean type.
 */
object ThemeControls {
    const val useDynamicColor: Boolean = true
    val useDarkMode: Boolean? = null // null = auto

    val showDividers: Boolean = false

    val dividerThickness: Int = 1

    val blurEnabled: Boolean = true
    val blurRadius: Int = 12

    const val previewDarkTheme = true

    /**
     * A compile-time knowable if-statement would be perfectly sane here, but Kotlin *somehow*
     * isn't smart enough to evaluate an if statement at compile time
     * (it can evaluate it, but doesn't accept it as const for some reason)
     * so instead of: if (darkTheme) UI_MODE_NIGHT_YES else UI_MODE_NIGHT_NO
     * I have to use a math operations, which are compile-time resolvable.
     *
     * Luckily UI_MODE_NIGHT_YES has an int value 32 and UI_MODE_NIGHT_NO 16,
     * so the operation should be mathematically easy. Now how do I get the int value from a Boolean?
     * Just cast it to Int, right? right? surely it works? **NO?** why?
     * Maybe it has a reason so complicated I wouldn't understand.
     *
     * Anyway, Bool.compareTo saved the day.
     */

    const val uiMode = UI_MODE_NIGHT_YES * (1 + previewDarkTheme.compareTo(false))
}
