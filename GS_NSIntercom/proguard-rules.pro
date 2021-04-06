# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
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
-keep public class com.astroluj.** { public protected *; }
-keep public interface com.astroluj.** { public protected *; }
-keep public class org.freedesktop.gstreamer.** { public protected *; }
-keep public interface org.freedesktop.gstreamer.** { public protected *; }

-dontwarn org.**
-dontwarn junit.**

# 줄번호 유지
-renamesourcefileattribute astroluj.intercom
-keepattributes SourceFile, LineNumberTable, Signature, Exceptions, *Annotation*, InnerClasses, EnclosingMethod
# variable names
-keepparameternames