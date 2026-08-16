#include "ThermalManager.h"

#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <cctype>
#include <android/log.h>

#define LOG_TAG "iBoostX/ThermalManager"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace iboostx {

namespace {

// Names commonly used across OEMs (Qualcomm, MediaTek, Exynos, Tensor)
// for a zone that tracks CPU / SoC package temperature. Not exhaustive —
// this is a best-effort heuristic, exactly like every other booster app
// has to do, because there is no single standard zone name.
const char* kCpuZoneHints[] = {
    "cpu", "soc", "tsens_tz_sensor", "tsens", "apq", "msm_therm",
    "cpu-0-0-usr", "cpu0", "cpuss", "mtktscpu", "exynos-therm", "tj-therm"
};

bool nameLooksLikeCpu(const std::string& type) {
    std::string lower = type;
    for (auto& c : lower) c = static_cast<char>(tolower(static_cast<unsigned char>(c)));
    for (const char* hint : kCpuZoneHints) {
        if (lower.find(hint) != std::string::npos) return true;
    }
    return false;
}

bool readZoneType(int index, std::string& out) {
    char path[128];
    snprintf(path, sizeof(path), "/sys/class/thermal/thermal_zone%d/type", index);
    FILE* fp = fopen(path, "r");
    if (!fp) return false;
    char buf[128] = {0};
    bool ok = fgets(buf, sizeof(buf), fp) != nullptr;
    fclose(fp);
    if (!ok) return false;
    // strip trailing newline
    size_t len = strlen(buf);
    while (len > 0 && (buf[len - 1] == '\n' || buf[len - 1] == '\r')) buf[--len] = '\0';
    out = buf;
    return true;
}

bool readZoneTempRaw(int index, long& out) {
    char path[128];
    snprintf(path, sizeof(path), "/sys/class/thermal/thermal_zone%d/temp", index);
    FILE* fp = fopen(path, "r");
    if (!fp) return false;
    long value = 0;
    int matched = fscanf(fp, "%ld", &value);
    fclose(fp);
    if (matched != 1) return false;
    out = value;
    return true;
}

// Thermal zones report in different units per-OEM: millidegree (45000),
// decidegree (450), or plain celsius (45). Normalize with a sanity check
// rather than assuming one format for every device.
float normalizeToCelsius(long raw) {
    float celsius;
    long magnitude = raw < 0 ? -raw : raw;
    if (magnitude >= 1000) {
        celsius = raw / 1000.0f;
    } else if (magnitude >= 200) {
        celsius = raw / 10.0f;
    } else {
        celsius = static_cast<float>(raw);
    }
    return celsius;
}

bool isPlausibleDeviceTemp(float celsius) {
    // Generous bounds for a phone SoC; anything outside this is almost
    // certainly a misread zone, not a real reading.
    return celsius > -40.0f && celsius < 150.0f;
}

} // namespace

ThermalManager::ThermalManager() = default;

bool ThermalManager::findCpuZone() {
    searched_ = true;

    // Zone count isn't published anywhere fixed; probe sequentially until
    // a zone fails to open, which is how every /sys/class thermal reader
    // on Android does this.
    int bestFallback = -1;

    for (int i = 0; i < 40; i++) {
        std::string type;
        if (!readZoneType(i, type)) {
            if (i == 0) continue; // some devices start at a gap, keep trying a few
            break;
        }

        long raw = 0;
        if (!readZoneTempRaw(i, raw)) continue;
        float celsius = normalizeToCelsius(raw);
        if (!isPlausibleDeviceTemp(celsius)) continue;

        if (bestFallback < 0) bestFallback = i;

        if (nameLooksLikeCpu(type)) {
            LOGI("Selected thermal_zone%d ('%s') as CPU zone", i, type.c_str());
            cpuZoneIndex_ = i;
            return true;
        }
    }

    if (bestFallback >= 0) {
        LOGW("No zone name matched known CPU hints, falling back to thermal_zone%d", bestFallback);
        cpuZoneIndex_ = bestFallback;
        return true;
    }

    LOGW("No readable thermal zone found on this device");
    cpuZoneIndex_ = -1;
    return false;
}

float ThermalManager::getCpuTemperatureCelsius() {
    if (!searched_) {
        findCpuZone();
    }

    if (cpuZoneIndex_ < 0) {
        return -1000.0f; // sentinel: "not available", never shown as a real reading
    }

    long raw = 0;
    if (!readZoneTempRaw(cpuZoneIndex_, raw)) {
        // Zone disappeared or became unreadable (permissions can change
        // after an OTA); re-search next call instead of reporting stale data.
        searched_ = false;
        return -1000.0f;
    }

    float celsius = normalizeToCelsius(raw);
    if (!isPlausibleDeviceTemp(celsius)) {
        return -1000.0f;
    }
    return celsius;
}

} // namespace iboostx
