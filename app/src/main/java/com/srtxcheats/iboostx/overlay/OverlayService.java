package com.srtxcheats.iboostx.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.srtxcheats.iboostx.R;
import com.srtxcheats.iboostx.core.DeviceMonitor;
import com.srtxcheats.iboostx.ui.MainActivity;

import java.util.Locale;

/**
 * Draggable floating HUD showing real CPU/RAM/battery/temperature while a
 * game is focused. FPS is intentionally reported as N/A: Android gives a
 * normal app no public way to read another app's frame rate without root
 * or ADB-level shell access (that's how tools like GameBench/PerfDog do
 * it) — showing a made-up number there would violate the "never fake a
 * reading" rule this project is built around.
 */
public class OverlayService extends Service {

    private static final String CHANNEL_ID = "iboostx_overlay";
    private static final int NOTIFICATION_ID = 1001;
    private static final long UPDATE_INTERVAL_MS = 1500;

    private WindowManager windowManager;
    private View hudView;
    private DeviceMonitor deviceMonitor;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView hudCpu, hudRam, hudBattery, hudTemp;

    private final Runnable updateTask = new Runnable() {
        @Override
        public void run() {
            updateHud();
            handler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        deviceMonitor = new DeviceMonitor(this);
        startForegroundWithNotification();
        addHudView();
        handler.post(updateTask);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Gaming HUD", NotificationManager.IMPORTANCE_MIN);
            channel.setDescription("Shows the floating performance overlay while boosting.");
            nm.createNotificationChannel(channel);
        }

        Intent tapIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, tapIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("iBoost X overlay active")
                .setContentText("Tap to open iBoost X")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void addHudView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        hudView = LayoutInflater.from(this).inflate(R.layout.overlay_hud, null);

        hudCpu = hudView.findViewById(R.id.hudCpu);
        hudRam = hudView.findViewById(R.id.hudRam);
        hudBattery = hudView.findViewById(R.id.hudBattery);
        hudTemp = hudView.findViewById(R.id.hudTemp);

        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 24;
        params.y = 150;

        makeDraggable(hudView, params);
        windowManager.addView(hudView, params);
    }

    /** Lets the user drag the HUD anywhere on screen; no other touch handling needed. */
    private void makeDraggable(View view, WindowManager.LayoutParams params) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(view, params);
                        return true;
                }
                return false;
            }
        });
    }

    private void updateHud() {
        DeviceMonitor.Snapshot s = deviceMonitor.readSnapshot();

        hudCpu.setText(String.format(Locale.US, "CPU: %s",
                s.cpuUsagePercent >= 0 ? Math.round(s.cpuUsagePercent) + "%" : "N/A"));

        if (s.ramTotalBytes > 0) {
            int pct = (int) (100 * s.ramUsedBytes / Math.max(1, s.ramTotalBytes));
            hudRam.setText(String.format(Locale.US, "RAM: %d%%", pct));
        } else {
            hudRam.setText("RAM: N/A");
        }

        hudBattery.setText(String.format(Locale.US, "BATT: %s",
                s.batteryPercent >= 0 ? s.batteryPercent + "%" : "N/A"));

        hudTemp.setText(String.format(Locale.US, "TEMP: %s",
                s.cpuTempCelsius > -900 ? String.format(Locale.US, "%.0f°C", s.cpuTempCelsius) : "N/A"));
    }

    public static boolean canDrawOverlays(android.content.Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(context);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTask);
        if (hudView != null && windowManager != null) {
            windowManager.removeView(hudView);
        }
    }
}
