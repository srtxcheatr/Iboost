#ifndef IBOOSTX_CPUMONITOR_H
#define IBOOSTX_CPUMONITOR_H

#include <cstdint>
#include <mutex>

namespace iboostx {

// Reads real CPU utilization from /proc/stat (aggregate "cpu" line).
// Usage percentage is computed as a delta between two consecutive reads,
// so the first call after construction always returns 0 until a second
// sample is taken. Nothing here is simulated or randomized.
class CPUMonitor {
public:
    CPUMonitor();

    // Returns overall CPU usage in the range [0.0, 100.0].
    // Returns -1.0f if /proc/stat could not be read (e.g. SELinux restricted).
    float getCpuUsagePercent();

    // Number of logical cores, read once from /proc/cpuinfo / sysconf.
    int getCoreCount();

private:
    struct CpuTimes {
        uint64_t idle = 0;
        uint64_t total = 0;
        bool valid = false;
    };

    CpuTimes readProcStat();

    std::mutex mutex_;
    CpuTimes lastSample_;
    int coreCount_ = -1;
};

} // namespace iboostx

#endif // IBOOSTX_CPUMONITOR_H
