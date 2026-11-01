# The ROM provider is instantiated by the framework, keep its name stable.
-keep class com.downhill.mobile.rom.RomFileProvider { *; }
# Activities are instantiated by the framework.
-keep class com.downhill.mobile.MainActivity { *; }
-keep class com.downhill.mobile.PadLabActivity { *; }
-keep class com.downhill.mobile.legend.LegendActivity { *; }
-keepclassmembers class * implements android.os.Parcelable { public static final ** CREATOR; }
