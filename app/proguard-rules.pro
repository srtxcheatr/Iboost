# Keep native bridge methods - JNI resolves these by exact name/signature
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.srtxcheats.iboostx.core.NativeBridge { *; }
-keep class com.srtxcheats.iboostx.games.Game { *; }
