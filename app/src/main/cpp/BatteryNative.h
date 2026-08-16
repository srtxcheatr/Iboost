#ifndef IBOOSTX_BATTERYNATIVE_H
#define IBOOSTX_BATTERYNATIVE_H

namespace iboostx {

// Real battery data comes from android.os.BatteryManager on the Java side
// (that's the only supported source on Android — there is no /proc or
// /sys path guaranteed to exist across OEMs the way there is for CPU/
// thermal). Java reads it and pushes a snapshot down through JNI so the
// rest of the native layer (e.g. Booster) can reason about it without
// re-crossing the JNI boundary on every check.
struct BatterySnapshot {
    int percent = -1;         // 0-100, -1 = unknown
    float temperatureCelsius = -1000.0f; // sentinel = unavailable
    float voltage = -1.0f;    // volts, -1 = unknown
    bool charging = false;
    bool valid = false;
};

class BatteryNative {
public:
    static BatteryNative& instance();

    void updateSnapshot(const BatterySnapshot& snapshot);
    BatterySnapshot getSnapshot();

private:
    BatteryNative() = default;
    BatterySnapshot snapshot_;
};

} // namespace iboostx

#endif // IBOOSTX_BATTERYNATIVE_H
