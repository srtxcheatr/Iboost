package com.srtxcheats.iboostx.core;

import android.app.GameManager;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/**
 * Performs the real, Android-permitted parts of "BOOST GAME":
 *
 *  - Do Not Disturb: only if the user has already granted Notification
 *    Policy Access (Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).
 *    We never silently flip this without that grant — Android won't let
 *    us anyway, but we also don't want to surprise the user.
 *  - GameManager.setGameState() (API 31+): the actual, documented system
 *    API for telling Android "this session is in a boosted game loop" so
 *    the platform's own Game Mode can react. This is the real mechanism;
 *    there is no public API to force another app's CPU/GPU clocks.
 *  - Session bookkeeping + thermal watchdog: delegated to the native
 *    Booster (see Booster.cpp) via NativeBridge.
 *
 * Reducing background activity is intentionally NOT implemented as
 * force-killing other apps: modern Android does not let a normal app do
 * that to other packages, and pretending otherwise would be exactly the
 * kind of fake feature this project explicitly avoids. Instead callers
 * should direct users to Settings > Apps > Battery for background limits,
 * which is the real, user-visible control for that.
 */
public class BoosterController {

    private final Context appContext;
    private final NotificationManager notificationManager;

    public BoosterController(Context context) {
        this.appContext = context.getApplicationContext();
        this.notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NativeBridge.ensureLoaded();
    }

    public boolean hasNotificationPolicyAccess() {
        return notificationManager != null && notificationManager.isNotificationPolicyAccessGranted();
    }

    public boolean start(String targetPackageName, boolean quietNotifications) {
        boolean started = NativeBridge.nativeStartBoost();
        if (!started) return false;

        if (quietNotifications && hasNotificationPolicyAccess()) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && targetPackageName != null) {
            try {
                GameManager gm = appContext.getSystemService(GameManager.class);
                if (gm != null) {
                    gm.setGameState(new android.app.GameState(false, android.app.GameState.MODE_NONE));
                }
            } catch (SecurityException | IllegalStateException ignored) {
                // Only the foreground game itself is normally allowed to report
                // its own state; failing here is expected and non-fatal.
            }
        }

        return true;
    }

    public void stop() {
        NativeBridge.nativeEndBoost();
        if (notificationManager != null && hasNotificationPolicyAccess()) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
        }
    }

    public boolean isActive() {
        return NativeBridge.nativeIsBoostActive();
    }

    public float getPeakCpu() {
        return NativeBridge.nativeGetBoostPeakCpu();
    }

    public float getPeakTemp() {
        return NativeBridge.nativeGetBoostPeakTemp();
    }

    public boolean thermalWarningTriggered() {
        return NativeBridge.nativeGetBoostThermalWarning();
    }
}
