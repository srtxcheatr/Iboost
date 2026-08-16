package com.srtxcheats.iboostx.ui;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.srtxcheats.iboostx.R;
import com.srtxcheats.iboostx.core.DeviceMonitor;

public class PerformanceFragment extends Fragment {

    private static final long POLL_INTERVAL_MS = 1000;

    private DeviceMonitor deviceMonitor;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean polling = false;

    private TextView perfCpuValue, perfTempValue, perfCoreCount, perfRam, perfThermalApi;
    private LineGraphView cpuGraph, tempGraph;

    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) return;
            DeviceMonitor.Snapshot s = deviceMonitor.readSnapshot();
            render(s);
            if (polling) handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_performance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        deviceMonitor = new DeviceMonitor(requireContext());

        perfCpuValue = view.findViewById(R.id.perfCpuValue);
        perfTempValue = view.findViewById(R.id.perfTempValue);
        perfCoreCount = view.findViewById(R.id.perfCoreCount);
        perfRam = view.findViewById(R.id.perfRam);
        perfThermalApi = view.findViewById(R.id.perfThermalApi);
        cpuGraph = view.findViewById(R.id.cpuGraph);
        tempGraph = view.findViewById(R.id.tempGraph);
    }

    private void render(DeviceMonitor.Snapshot s) {
        perfCpuValue.setText(s.cpuUsagePercent >= 0 ? Math.round(s.cpuUsagePercent) + "%" : "N/A");
        cpuGraph.addSample(s.cpuUsagePercent);

        if (s.cpuTempCelsius > -900) {
            perfTempValue.setText(String.format(java.util.Locale.US, "%.1f°C", s.cpuTempCelsius));
            // scale temp onto a 0-100 style graph axis (0-100°C is a reasonable phone SoC band)
            tempGraph.addSample(Math.max(0, Math.min(100, s.cpuTempCelsius)));
        } else {
            perfTempValue.setText("N/A");
        }

        perfCoreCount.setText("Cores: " + (s.cpuCoreCount > 0 ? s.cpuCoreCount : "N/A"));
        perfRam.setText("RAM: " + DeviceMonitor.formatBytes(s.ramUsedBytes) + " used / " + DeviceMonitor.formatBytes(s.ramTotalBytes) + " total");

        String thermalApiNote = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? "Thermal API: " + s.thermalLabel + " (status " + s.thermalStatus + ")"
                : "Thermal API: unsupported below Android 10";
        perfThermalApi.setText(thermalApiNote);
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
