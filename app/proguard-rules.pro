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

# ============================================
# Supabase 관련 ProGuard 규칙
# ============================================

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.sweetapps.pocketchord.**$$serializer { *; }
-keepclassmembers class com.sweetapps.pocketchord.** {
    *** Companion;
}
-keepclasseswithmembers class com.sweetapps.pocketchord.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Supabase 데이터 모델 클래스 보호
-keep class com.sweetapps.pocketchord.data.supabase.model.** { *; }
-keepclassmembers class com.sweetapps.pocketchord.data.supabase.model.** {
    <fields>;
    <init>(...);
}

# Ktor (Supabase가 사용하는 HTTP 클라이언트)
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# ============================================
# 기존 규칙 (있다면 유지)
# ============================================
