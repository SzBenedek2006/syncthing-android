-dontobfuscate

-keepclassmembers class dev.benedek.syncthingandroid.model.** {
    <init>(...);
}

-keep class dev.benedek.syncthingandroid.model.** { *; }
-keep class dev.benedek.syncthingandroid.service.RestApi$* { *; }

-keep class com.google.common.reflect.TypeToken { *; }
-keep class com.google.common.reflect.TypeParameter { *; }
