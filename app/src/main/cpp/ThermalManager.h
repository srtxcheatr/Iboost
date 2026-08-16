#ifndef IBOOSTX_THERMALMANAGER_H
#define IBOOSTX_THERMALMANAGER_H

#include <string>

namespace iboostx {

// Reads real CPU temperature from Linux thermal zones under
// /sys/class/thermal/thermal_zone*/. Different OEMs label zones
// differently, so we scan all zones once, cache the index of whichever
// one looks like the CPU/SoC zone, and re-read just that value after.
//
// Android's official Thermal API (PowerManager#getCurrentThermalStatus,
// API 29+) is Java-only and is read on the Kotlin/Java side, then passed
// down through JNI if native code needs it — it is not duplicated here.
class ThermalManager {
public:
    ThermalManager();

    // Degrees Celsius, or -1000.0f if no readable thermal zone was found
    // on this device (do not display this as 0 or as a real value).
    float getCpuTemperatureCelsius();

private:
    bool findCpuZone();

    int cpuZoneIndex_ = -1;
    bool searched_ = false;
};

} // namespace iboostx

#endif // IBOOSTX_THERMALMANAGER_H
