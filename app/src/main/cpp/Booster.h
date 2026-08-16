#ifndef IBOOSTX_BOOSTER_H
#define IBOOSTX_BOOSTER_H

#include <cstdint>
#include <mutex>

namespace iboostx {

// IMPORTANT — what this class honestly can and cannot do:
//
// On a non-rooted device, a third-party app CANNOT change another app's
// CPU governor, GPU clocks, or scheduler priority. Those controls require
// root or being the platform itself. Any booster app claiming to "unlock
// hidden GPU power" for other apps without root is lying.
//
// What IS real and implemented here / orchestrated from Java:
//   - Track a boost session (start time, duration, peak CPU/temp seen)
//     natively, so it survives Activity recreation.
//   - Run a lightweight thermal watchdog: if thermal status reaches
//     CRITICAL during a session, flag it so the UI can warn the user and
//     Java can auto-end the boost.
//   - Lower this app's own background overhead while boosting, via
//     thread priority (android.os.Process.setThreadPriority on the Java
//     side; this native layer just tracks whether that's been applied).
//   - Everything else the feature list asks for (immersive mode, Do Not
//     Disturb, GameManager.setGameState(), launching the chosen game) are
//     real Android APIs that require a Context/Activity, so they are
//     performed in Java/Kotlin (see Booster.java-side caller) and simply
//     reported back here for session bookkeeping.
class Booster {
public:
    static Booster& instance();

    // Begin a boost session. Returns false if one is already active.
    bool startSession();

    // End the current session. Returns false if none was active.
    bool endSession();

    bool isActive();

    // Called periodically (e.g. once per second) by the monitoring loop
    // with the latest readings, purely for bookkeeping + the watchdog.
    void reportSample(float cpuPercent, float cpuTempCelsius);

    // status matches Android's PowerManager.THERMAL_STATUS_* int constants
    // (0=NONE .. 6=SHUTDOWN), read on the Java side via the real Thermal API.
    void reportThermalStatus(int status);

    struct SessionStats {
        bool active = false;
        int64_t startTimeMillis = 0;
        float peakCpuPercent = 0.0f;
        float peakTempCelsius = -1000.0f;
        bool thermalWarningTriggered = false;
    };

    SessionStats getStats();

private:
    Booster() = default;
    std::mutex mutex_;
    SessionStats stats_;
};

} // namespace iboostx

#endif // IBOOSTX_BOOSTER_H
