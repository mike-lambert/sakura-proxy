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
# ------ orbot legCY -----------v
-dontobfuscate
# https://stackoverflow.com/questions/9651703/using-proguard-with-android-without-obfuscation
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

-keep class org.torproject.android.service.vpn.Tun2Socks {
    void logTun2Socks(java.lang.String, java.lang.String, java.lang.String);
}

-keepattributes Signature
-keepattributes Annotation
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ------ app ------v
-keep,includedescriptorclasses public class com.subgraph.orchid.** {
  public protected *;
}

-keep,includedescriptorclasses public class com.google.common.** {
  public protected *;
}

-dontwarn com.google.common.**
-dontwarn javax.**
-dontwarn org.slf4j.**
# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile
