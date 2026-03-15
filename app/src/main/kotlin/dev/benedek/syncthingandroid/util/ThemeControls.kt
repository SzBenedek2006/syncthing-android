package dev.benedek.syncthingandroid.util

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
}
