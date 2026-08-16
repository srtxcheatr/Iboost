package com.srtxcheats.iboostx.settings;

import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.srtxcheats.iboostx.BuildConfig;
import com.srtxcheats.iboostx.R;
import com.srtxcheats.iboostx.overlay.OverlayService;

public class SettingsFragment extends Fragment {

    private TextView overlayStatusText, dndStatusText, versionText, overlayPermissionButton, dndPermissionButton;
    private Switch overlaySwitch;
    private NotificationManager notificationManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationManager = (NotificationManager) requireContext().getSystemService(android.content.Context.NOTIFICATION_SERVICE);

        overlayStatusText = view.findViewById(R.id.overlayStatusText);
        overlayPermissionButton = view.findViewById(R.id.overlayPermissionButton);
        overlaySwitch = view.findViewById(R.id.overlaySwitch);
        dndStatusText = view.findViewById(R.id.dndStatusText);
        dndPermissionButton = view.findViewById(R.id.dndPermissionButton);
        versionText = view.findViewById(R.id.versionText);

        versionText.setText("iBoost X • v" + BuildConfig.VERSION_NAME);

        overlayPermissionButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + requireContext().getPackageName()));
            startActivity(intent);
        });

        dndPermissionButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));

        overlaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!OverlayService.canDrawOverlays(requireContext())) {
                    Toast.makeText(requireContext(), "Grant the overlay permission first", Toast.LENGTH_SHORT).show();
                    overlaySwitch.setChecked(false);
                    return;
                }
                requireContext().startForegroundService(new Intent(requireContext(), OverlayService.class));
            } else {
                requireContext().stopService(new Intent(requireContext(), OverlayService.class));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPermissionStatus();
    }

    private void refreshPermissionStatus() {
        boolean overlayGranted = OverlayService.canDrawOverlays(requireContext());
        overlayStatusText.setText(overlayGranted ? "Granted" : "Permission required");
        overlayPermissionButton.setText(overlayGranted ? "Granted" : "Grant");

        boolean dndGranted = notificationManager != null && notificationManager.isNotificationPolicyAccessGranted();
        dndStatusText.setText(dndGranted ? "Granted" : "Permission required");
        dndPermissionButton.setText(dndGranted ? "Granted" : "Grant");
    }
}
