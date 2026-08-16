package com.srtxcheats.iboostx.core;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Locale;

/**
 * Single source of truth for every stat shown on the dashboard. Every
 * field is either read directly from an Android system API or from the
 * native layer (which itself reads /proc or /sys). If a value truly
 * isn't available on a given device, the corresponding field is left at
 * its "unavailable" sentinel and the UI shows "N/A" — never a made-up
 * number.
 */
public class DeviceMonitor {

    public static final float UNAVAILABLE = -1000f;

    public static class Snapshot {
        public float cpuUsagePercent = -1f;
        public int cpuCoreCount = -1;
        public float cpuTempCelsius = UNAVAILABLE;

        public float gpuUsagePercent = -1f; // -1 = not exposed on this device

        public long ramUsedBytes = -1;
        public long ramTotalBytes = -1;

        public int batteryPercent = -1;
        public boolean batteryCharging = false;
        public float batteryVoltage = -1f;
        public float batteryTempCelsius = UNAVAILABLE;

        public int thermalStatus = -1; // maps to PowerManager.THERMAL_STATUS_*
        public String thermalLabel = "UNKNOWN";
    }

    private final Context appContext;
    private final ActivityManager activityManager;
    private final PowerManager powerManager;

    public DeviceMonitor(Context context) {
        this.appContext = context.getApplicationContext();
        this.activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        this.powerManager = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        NativeBridge.ensureLoaded();
    }

    public Snapshot readSnapshot() {
        Snapshot s = new Snapshot();

        // ---- CPU (native, real /proc/stat delta) ----
        s.cpuUsagePercent = NativeBridge.nativeGetCpuUsage();
        s.cpuCoreCount = NativeBridge.nativeGetCoreCount();
        s.cpuTempCelsius = NativeBridge.nativeGetCpuTemperature();

        // ---- GPU (best-effort; most devices simply don't expose this without root) ----
        s.gpuUsagePercent = readGpuUsageBestEffort();

        // ---- RAM (real, via ActivityManager.MemoryInfo) ----
        if (activityManager != null) {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(mi);
            s.ramTotalBytes = mi.totalMem;
            s.ramUsedBytes = mi.totalMem - mi.availMem;
        }

        // ---- Battery (real, via sticky ACTION_BATTERY_CHANGED intent) ----
        Intent batteryIntent = appContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            s.batteryPercent = (level >= 0 && scale > 0) ? Math.round(level * 100f / scale) : -1;

            int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            s.batteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;

            int tempTenths = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
            s.batteryTempCelsius = tempTenths == Integer.MIN_VALUE ? UNAVAILABLE : tempTenths / 10f;

            int millivolts = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            s.batteryVoltage = millivolts > 0 ? millivolts / 1000f : -1f;
        }

        // Feed the real battery reading down into native so Booster.cpp's
        // watchdog can reason about it without re-crossing JNI itself.
        NativeBridge.nativeUpdateBattery(s.batteryPercent, s.batteryTempCelsius, s.batteryVoltage, s.batteryCharging);

        // ---- Thermal status (real Android Thermal API, API 29+) ----
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            s.thermalStatus = powerManager.getCurrentThermalStatus();
            s.thermalLabel = thermalStatusToLabel(s.thermalStatus);
            NativeBridge.nativeReportThermalStatus(s.thermalStatus);
        } else {
            s.thermalStatus = -1;
            s.thermalLabel = "UNSUPPORTED";
        }

        NativeBridge.nativeReportSample(s.cpuUsagePercent, s.cpuTempCelsius);

        return s;
    }

    /**
     * Maps Android's fine-grained THERMAL_STATUS_* (NONE..SHUTDOWN, 0-6) onto
     * the simpler NORMAL / WARM / HOT / CRITICAL labels this app displays.
     */
    public static String thermalStatusToLabel(int status) {
        switch (status) {
            case PowerManager.THERMAL_STATUS_NONE:
            case PowerManager.THERMAL_STATUS_LIGHT:
                return "NORMAL";
            case PowerManager.THERMAL_STATUS_MODERATE:
                return "WARM";
            case PowerManager.THERMAL_STATUS_SEVERE:
                return "HOT";
            case PowerManager.THERMAL_STATUS_CRITICAL:
            case PowerManager.THERMAL_STATUS_EMERGENCY:
            case PowerManager.THERMAL_STATUS_SHUTDOWN:
                return "CRITICAL";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Stock Android exposes no public GPU-usage API. A handful of chipsets
     * expose a readable sysfs busy/load node; if none of those paths exist
     * or are readable on this device (the common case, especially on
     * production non-rooted builds), this honestly returns -1 and the UI
     * shows "N/A" rather than a guess.
     */
    private float readGpuUsageBestEffort() {
        String[] candidatePaths = {
                "/sys/class/kgsl/kgsl-3d0/gpubusy",       // Adreno
                "/sys/kernel/gpu/gpu_busy",                 // some Mali/Mediatek builds
                "/sys/class/devfreq/gpufreq/gpu_busy_pct" // some Mali builds
        };
        for (String path : candidatePaths) {
            String raw = readFirstLine(path);
            if (raw == null) continue;
            try {
                String[] parts = raw.trim().split("\\s+");
                if (parts.length >= 2) {
                    // "busy total" style, e.g. kgsl gpubusy
                    long busy = Long.parseLong(parts[0]);
                    long total = Long.parseLong(parts[1]);
                    if (total > 0) return Math.min(100f, (busy * 100f) / total);
                } else if (parts.length == 1) {
                    float pct = Float.parseFloat(parts[0].replace("%", ""));
                    return Math.min(100f, Math.max(0f, pct));
                }
            } catch (NumberFormatException ignored) {
                // fall through and try the next candidate
            }
        }
        return -1f;
    }

    private String readFirstLine(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        double gb = bytes / (1024.0 * 1024.0 * 1024.0);
        return String.format(Locale.US, "%.1f GB", gb);
    }
}
