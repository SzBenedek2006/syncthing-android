package dev.benedek.syncthingandroid.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * {@link ThemeControls} stores the global state of theming controls.
 * It stores 2 fields: {@link #useDynamicColor} and {@link #useDarkMode},
 * both of them are Boolean type.
 */
public class ThemeControls {
    @NonNull
    public static final Boolean useDynamicColor = false;
    @Nullable
    public static final Boolean useDarkMode = null; // null = auto
}
