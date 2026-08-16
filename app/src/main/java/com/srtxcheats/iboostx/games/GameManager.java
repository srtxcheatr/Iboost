package com.srtxcheats.iboostx.games;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans installed apps for real games (via the OS-reported app category on
 * API 26+, since that's the only reliable signal Android gives us — there
 * is no public "is this a game" flag before that), plus lets the user add
 * any app manually. Manual additions are persisted in SharedPreferences.
 */
public class GameManager {

    private static final String PREFS = "iboostx_games";
    private static final String KEY_MANUAL = "manual_packages";

    private final Context context;
    private final PackageManager pm;

    public GameManager(Context context) {
        this.context = context.getApplicationContext();
        this.pm = this.context.getPackageManager();
    }

    /** Real installed apps the OS classifies as games. */
    public List<Game> scanInstalledGames() {
        List<Game> results = new ArrayList<>();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : apps) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue; // skip system apps
            if (pm.getLaunchIntentForPackage(app.packageName) == null) continue; // not launchable

            boolean isGame = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                isGame = app.category == ApplicationInfo.CATEGORY_GAME;
            }
            //noinspection deprecation
            isGame = isGame || (app.flags & ApplicationInfo.FLAG_IS_GAME) != 0;

            if (isGame) {
                results.add(new Game(app.packageName, String.valueOf(app.loadLabel(pm)), app.loadIcon(pm), false));
            }
        }

        // Add manually-added apps that aren't already in the list.
        Set<String> manual = getManualPackages();
        Set<String> already = new HashSet<>();
        for (Game g : results) already.add(g.packageName);

        for (String pkg : manual) {
            if (already.contains(pkg)) continue;
            try {
                ApplicationInfo app = pm.getApplicationInfo(pkg, 0);
                if (pm.getLaunchIntentForPackage(pkg) != null) {
                    results.add(new Game(pkg, String.valueOf(app.loadLabel(pm)), app.loadIcon(pm), true));
                }
            } catch (PackageManager.NameNotFoundException ignored) {
                // app was uninstalled since it was added; drop silently
            }
        }

        return results;
    }

    /** Every launchable app, for the "add a game manually" picker. */
    public List<Game> getAllLaunchableApps() {
        List<Game> results = new ArrayList<>();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo app : apps) {
            if (pm.getLaunchIntentForPackage(app.packageName) == null) continue;
            results.add(new Game(app.packageName, String.valueOf(app.loadLabel(pm)), app.loadIcon(pm), false));
        }
        return results;
    }

    public void addManualGame(String packageName) {
        Set<String> manual = new HashSet<>(getManualPackages());
        manual.add(packageName);
        prefs().edit().putStringSet(KEY_MANUAL, manual).apply();
    }

    public void removeManualGame(String packageName) {
        Set<String> manual = new HashSet<>(getManualPackages());
        manual.remove(packageName);
        prefs().edit().putStringSet(KEY_MANUAL, manual).apply();
    }

    private Set<String> getManualPackages() {
        return prefs().getStringSet(KEY_MANUAL, new HashSet<>());
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean launch(String packageName) {
        Intent intent = pm.getLaunchIntentForPackage(packageName);
        if (intent == null) return false;
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return true;
    }
}
