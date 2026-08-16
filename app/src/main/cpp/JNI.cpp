#include <jni.h>
#include "CPUMonitor.h"
#include "ThermalManager.h"
#include "BatteryNative.h"
#include "Booster.h"

using namespace iboostx;

namespace {
// One process-wide monitor instance. CPU usage is delta-based, so it must
// persist across calls rather than being recreated each time.
CPUMonitor g_cpuMonitor;
ThermalManager g_thermalManager;
} // namespace

extern "C" {

// ---- CPU ----

JNIEXPORT jfloat JNICALL
Java_com_srtxcheats_iboostx_core_NativeBridge_nativeGetCpuUsage(JNIEnv*, jobject) {
    return g_cpuMonitor.getCpuUsagePercent();
}

JNIEXPORT jint JNICALL
Java_com_srtxcheats_iboostx_core_NativeBridge_nativeGetCoreCount(JNIEnv*, jobject) {
    return g_cpuMonitor.getCoreCount();
}

// ---- Thermal ----

JNIEXPORT jfloat JNICALL
Java_com_srtxcheats_iboostx_core_NativeBridge_nativeGetCpuTemperature(JNIEnv*, jobject) {
    return g_thermalManager.getCpuTemperatureCelsius();
}

// ---- Battery (Java reads real values from BatteryManager, pushes them here) ----

JNIEXPORT void JNICALL
Java_com_srtxcheats_iboostx_core_NativeBridge_nativeUpdateBattery(
        JNIEnv*, jobject, jint percent, jfloat temperatureCelsius, jfloat voltage, jboolean charging) {
    BatterySnapshot snap;
    snap.percent = percent;
    snap.temperatureCelsius = temperatureCelsius;
    snap.voltage = voltage;
    snap.charging = charging == JNI_TRUE;
    snap.valid = true;
    BatteryNative::instance().updateSnapshot(snap);
}

// ---- Booster ----

JNIEXPORT jboolean JNICALL
Java_com_srtxcheats_iboostx_core_NativeBridge_nativeStartBoost(JNIEnv*, jobject) {
    return Booster::instance().startSession() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_srtxcheats_iboostx_core_NativeBridge_nativeEndBoost(JNIEnv*, jobject) {
    return Booster::instance().endSession() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_srtxcheats_iboostx_core_NativeBridge_nativeIsBoostActive(JNIEnv*, jobject) {
    return Booster::instance().isActive() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_srtxcheats_iboostx_core_NativeBridge_nativeReportSample(
        JNIEnv*, jobject, jfloat cpuPercent, jfloat cpuTemp) {
    Booster::instance().reportSample(cpuPercent, cpuTemp);
}

JNIEXPORT void JNICALL
Java_com_srtxcheats_iboostx_core_NativeBridge_nativeReportThermalStatus(
        JNIEnv*, jobject, jint status) {
    Booster::instance().reportThermalStatus(status);
}

JNIEXPORT jfloat JNICALL
Java_com_srtxcheats_iboostx_core_NativeBridge_nativeGetBoostPeakCpu(JNIEnv*, jobject) {
    return Booster::instance().getStats().peakCpuPercent;
}

JNIEXPORT jfloat JNICALL
Java_com_srtxcheats_iboostx_core_NativeBridge_nativeGetBoostPeakTemp(JNIEnv*, jobject) {
    return Booster::instance().getStats().peakTempCelsius;
}

JNIEXPORT jboolean JNICALL
Java_com_srtxcheats_iboostx_core_NativeBridge_nativeGetBoostThermalWarning(JNIEnv*, jobject) {
    return Booster::instance().getStats().thermalWarningTriggered ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
