package com.srtxcheats.iboostx.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.srtxcheats.iboostx.R;
import com.srtxcheats.iboostx.core.BoosterController;
import com.srtxcheats.iboostx.core.DeviceMonitor;

public class HomeFragment extends Fragment {

    private static final long POLL_INTERVAL_MS = 1500;

    private DeviceMonitor deviceMonitor;
    private BoosterController boosterController;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean polling = false;

    private TextView cpuValue, cpuCores, ramValue, ramSub, batteryValue, batterySub, gpuValue;
    private TextView thermalStatusText, thermalTempValue;
    private View thermalDot, boostButton;

    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) return;
            DeviceMonitor.Snapshot s = deviceMonitor.readSnapshot();
            renderSnapshot(s);
            if (polling) handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        deviceMonitor = new DeviceMonitor(requireContext());
        boosterController = new BoosterController(requireContext());

        cpuValue = view.findViewById(R.id.cpuValue);
        cpuCores = view.findViewById(R.id.cpuCores);
        ramValue = view.findViewById(R.id.ramValue);
        ramSub = view.findViewById(R.id.ramSub);
        batteryValue = view.findViewById(R.id.batteryValue);
        batterySub = view.findViewById(R.id.batterySub);
        gpuValue = view.findViewById(R.id.gpuValue);
        thermalStatusText = view.findViewById(R.id.thermalStatusText);
        thermalTempValue = view.findViewById(R.id.thermalTempValue);
        thermalDot = view.findViewById(R.id.thermalDot);
        boostButton = view.findViewById(R.id.boostButton);

        boostButton.setOnClickListener(v -> toggleBoost());
        updateBoostButtonUi();
    }

    private void toggleBoost() {
        if (boosterController.isActive()) {
            boosterController.stop();
        } else {
            boosterController.start(null, /* quietNotifications= */ true);
        }
        updateBoostButtonUi();
    }

    private void updateBoostButtonUi() {
        TextView label = (TextView) boostButton;
        if (boosterController.isActive()) {
            label.setText(R.string.end_boost);
        } else {
            label.setText(R.string.boost_game);
        }
    }

    private void renderSnapshot(DeviceMonitor.Snapshot s) {
        cpuValue.setText(s.cpuUsagePercent >= 0 ? Math.round(s.cpuUsagePercent) + "%" : "N/A");
        cpuCores.setText(s.cpuCoreCount > 0 ? s.cpuCoreCount + " cores" : "");

        if (s.ramTotalBytes > 0) {
            ramValue.setText(DeviceMonitor.formatBytes(s.ramUsedBytes) + " / " + DeviceMonitor.formatBytes(s.ramTotalBytes));
            int pct = (int) (100 * s.ramUsedBytes / Math.max(1, s.ramTotalBytes));
            ramSub.setText(pct + "% used");
        } else {
            ramValue.setText("N/A");
        }

        batteryValue.setText(s.batteryPercent >= 0 ? s.batteryPercent + "%" : "N/A");
        String chargeState = s.batteryCharging ? "Charging" : "On battery";
        String tempStr = s.batteryTempCelsius > -900 ? String.format(java.util.Locale.US, "%.1f°C", s.batteryTempCelsius) : "N/A";
        batterySub.setText(chargeState + " • " + tempStr);

        gpuValue.setText(s.gpuUsagePercent >= 0 ? Math.round(s.gpuUsagePercent) + "%" : "N/A");

        thermalStatusText.setText(s.thermalLabel);
        thermalDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorForThermal(s.thermalLabel)));
        thermalTempValue.setText(s.cpuTempCelsius > -900 ? String.format(java.util.Locale.US, "%.1f°C", s.cpuTempCelsius) : "N/A");

        updateBoostButtonUi();
    }

    private int colorForThermal(String label) {
        switch (label) {
            case "NORMAL": return Color.parseColor("#4CFFB3");
            case "WARM": return Color.parseColor("#FFD24C");
            case "HOT": return Color.parseColor("#FF9A4C");
            case "CRITICAL": return Color.parseColor("#FF4C4C");
            default: return Color.parseColor("#8899AA");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        polling = true;
        handler.post(pollTask);
    }

    @Override
    public void onPause() {
        super.onPause();
        polling = false;
        handler.removeCallbacks(pollTask);
    }
}
