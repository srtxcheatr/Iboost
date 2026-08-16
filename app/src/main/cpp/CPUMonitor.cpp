#include "CPUMonitor.h"

#include <cstdio>
#include <cstring>
#include <cerrno>
#include <unistd.h>
#include <android/log.h>

#define LOG_TAG "iBoostX/CPUMonitor"
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace iboostx {

CPUMonitor::CPUMonitor() {
    // Take a baseline sample immediately so the very first delta call
    // has something to compare against.
    lastSample_ = readProcStat();
}

CPUMonitor::CpuTimes CPUMonitor::readProcStat() {
    CpuTimes result;

    FILE* fp = fopen("/proc/stat", "r");
    if (!fp) {
        LOGW("Could not open /proc/stat: %s", strerror(errno));
        return result; // valid stays false
    }

    // Aggregate line format:
    // cpu  user nice system idle iowait irq softirq steal guest guest_nice
    unsigned long long user = 0, nice = 0, system = 0, idle = 0;
    unsigned long long iowait = 0, irq = 0, softirq = 0, steal = 0;
    char label[16] = {0};

    int matched = fscanf(fp, "%15s %llu %llu %llu %llu %llu %llu %llu %llu",
                          label, &user, &nice, &system, &idle,
                          &iowait, &irq, &softirq, &steal);
    fclose(fp);

    if (matched < 8) {
        LOGW("Unexpected /proc/stat format (matched=%d)", matched);
        return result;
    }

    uint64_t idleAll = idle + iowait;
    uint64_t totalAll = user + nice + system + idle + iowait + irq + softirq + steal;

    result.idle = idleAll;
    result.total = totalAll;
    result.valid = true;
    return result;
}

float CPUMonitor::getCpuUsagePercent() {
    std::lock_guard<std::mutex> lock(mutex_);

    CpuTimes current = readProcStat();
    if (!current.valid || !lastSample_.valid) {
        lastSample_ = current;
        return current.valid ? 0.0f : -1.0f;
    }

    uint64_t totalDelta = current.total - lastSample_.total;
    uint64_t idleDelta = current.idle - lastSample_.idle;

    lastSample_ = current;

    if (totalDelta == 0) {
        return 0.0f; // no time elapsed between samples, avoid div-by-zero
    }

    float usage = (1.0f - (static_cast<float>(idleDelta) / static_cast<float>(totalDelta))) * 100.0f;

    if (usage < 0.0f) usage = 0.0f;
    if (usage > 100.0f) usage = 100.0f;
    return usage;
}

int CPUMonitor::getCoreCount() {
    if (coreCount_ > 0) {
        return coreCount_;
    }
    long cores = sysconf(_SC_NPROCESSORS_CONF);
    coreCount_ = cores > 0 ? static_cast<int>(cores) : 1;
    return coreCount_;
}

} // namespace iboostx
