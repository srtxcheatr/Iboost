#include "Booster.h"
#include <ctime>

namespace iboostx {

namespace {
int64_t nowMillis() {
    struct timespec ts{};
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return static_cast<int64_t>(ts.tv_sec) * 1000LL + ts.tv_nsec / 1000000LL;
}

// A thermal_status of >= 3 corresponds to Android's THERMAL_STATUS_SEVERE
// (see PowerManager.THERMAL_STATUS_*), passed down from Java. We treat
// SEVERE and above as "back off the boost".
constexpr int kThermalWarningThreshold = 3;
} // namespace

Booster& Booster::instance() {
    static Booster inst;
    return inst;
}

bool Booster::startSession() {
    std::lock_guard<std::mutex> lock(mutex_);
    if (stats_.active) return false;
    stats_ = SessionStats{};
    stats_.active = true;
    stats_.startTimeMillis = nowMillis();
    return true;
}

bool Booster::endSession() {
    std::lock_guard<std::mutex> lock(mutex_);
    if (!stats_.active) return false;
    stats_.active = false;
    return true;
}

bool Booster::isActive() {
    std::lock_guard<std::mutex> lock(mutex_);
    return stats_.active;
}

void Booster::reportSample(float cpuPercent, float cpuTempCelsius) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (!stats_.active) return;

    if (cpuPercent > stats_.peakCpuPercent) {
        stats_.peakCpuPercent = cpuPercent;
    }
    if (cpuTempCelsius > -900.0f && cpuTempCelsius > stats_.peakTempCelsius) {
        stats_.peakTempCelsius = cpuTempCelsius;
    }
}

void Booster::reportThermalStatus(int status) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (!stats_.active) return;
    if (status >= kThermalWarningThreshold) {
        stats_.thermalWarningTriggered = true;
    }
}

Booster::SessionStats Booster::getStats() {
    std::lock_guard<std::mutex> lock(mutex_);
    return stats_;
}

} // namespace iboostx
