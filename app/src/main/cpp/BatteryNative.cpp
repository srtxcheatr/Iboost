#include "BatteryNative.h"
#include <mutex>

namespace iboostx {

namespace {
std::mutex g_mutex;
}

BatteryNative& BatteryNative::instance() {
    static BatteryNative inst;
    return inst;
}

void BatteryNative::updateSnapshot(const BatterySnapshot& snapshot) {
    std::lock_guard<std::mutex> lock(g_mutex);
    snapshot_ = snapshot;
}

BatterySnapshot BatteryNative::getSnapshot() {
    std::lock_guard<std::mutex> lock(g_mutex);
    return snapshot_;
}

} // namespace iboostx
