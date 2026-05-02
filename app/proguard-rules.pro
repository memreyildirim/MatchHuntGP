# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Crashlytics symbol/line bilgilerini koru
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Firebase modelleri (Firestore data binding icin field/constructor reflection)
-keep class com.emreyildirim.matchhuntv1.data.model.** { *; }

# Release build'inde Log.d / Log.v / Log.i cagrilarini tamamen kaldir.
# Log.w ve Log.e production'da kalmaya devam eder (Crashlytics icin onemli).
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
    public static boolean isLoggable(java.lang.String, int);
}

# println / System.out kullanimlarini da release'de kaldir.
-assumenosideeffects class java.io.PrintStream {
    public void println(%);
    public void println(**);
    public void print(%);
    public void print(**);
}
