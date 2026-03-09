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
    public static final Boolean useDynamicColor = true;
    @Nullable
    public static final Boolean useDarkMode = null; // null = auto

    @NonNull
    public static final Boolean showDividers = false;

    public static final int dividerThickness = 1;

    public static final boolean blurEnabled = true;
    public static final int blurRadius = 12;
}
