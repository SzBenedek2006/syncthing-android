package dev.benedek.syncthingandroid.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * It stores 2 fields: useDynamicColor and useDarkMode,
 * both of them are class Boolean type.
 */
public class ThemeControls {
    @NonNull
    private static final Boolean useDynamicColor = false;
    @Nullable
    private static final Boolean useDarkMode = null; // null = auto

    @Nullable
    public static Boolean getUseDarkMode() {
        return useDarkMode;
    }
    @NonNull
    public static Boolean getUseDynamicColor() {
        return useDynamicColor;
    }
}
