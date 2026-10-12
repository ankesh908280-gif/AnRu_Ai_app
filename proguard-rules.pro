# Proguard rules for ANRU AI
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.anru.ai.JarvisBridge { *; }
-keep class com.anru.ai.JarvisAccessibilityService { *; }
-keep class com.anru.ai.JarvisForegroundService { *; }
-keep class com.anru.ai.FloatingArcReactorService { *; }
