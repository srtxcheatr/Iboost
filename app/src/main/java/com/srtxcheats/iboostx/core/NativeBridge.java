package com.srtxcheats.iboostx.core;

/**
 * Thin Java-side declarations for the native (C++) monitoring and boost
 * engine. Every method here crosses into JNI.cpp — nothing in this class
 * computes or fakes a value itself.
 */
public final class NativeBridge {

    private static volatile boolean sLoaded = false;

    public static synchronized void ensureLoaded() {
        if (sLoaded) return;
        System.loadLibrary("iboostx_native");
        sLoaded = true;
    }

    private NativeBridge() {}

    // ---- CPU ----
    public static native float nativeGetCpuUsage();
    public static native int nativeGetCoreCount();

    // ---- Thermal ----
    public static native float nativeGetCpuTemperature();

    // ---- Battery ----
    public static native void nativeUpdateBattery(int percent, float temperatureCelsius, float voltage, boolean charging);

    // ---- Booster ----
    public static native boolean nativeStartBoost();
    public static native boolean nativeEndBoost();
    public static native boolean nativeIsBoostActive();
    public static native void nativeReportSample(float cpuPercent, float cpuTemp);
    public static native void nativeReportThermalStatus(int status);
    public static native float nativeGetBoostPeakCpu();
    public static native float nativeGetBoostPeakTemp();
    public static native boolean nativeGetBoostThermalWarning();
}
