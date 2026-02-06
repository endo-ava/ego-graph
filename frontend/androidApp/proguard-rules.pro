# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Compose runtime APIs
-keep,allowobfuscation,allowshrinking class androidx.compose.** {
    *;
}

# Keep Ktor reflection
-keepattributes Signature
-keepattributes InnerClasses
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep Kotlinx serialization
-keep,allowobfuscation,allowshrinking class kotlinx.serialization.** { *; }

# Keep Koin
-keep class org.koin.** { *; }
-keep class * extends org.koin.core.module.Module
-keepclassmembers class * {
    @org.koin.core.annotation.* *;
}
-dontwarn org.koin.**
