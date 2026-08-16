package com.srtxcheats.iboostx.core;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

/**
 * IMPORTANT: this is a per-game *profile/config* store, exactly as the
 * spec asked for — it does NOT and cannot modify real hardware touch
 * sensitivity. Android gives apps no API to change another app's touch
 * input scaling without root or an Accessibility-service input filter
 * that remaps raw touch events (a much bigger, more invasive feature
 * than "Suzuki Setup" describes). What this class does is save the
 * numbers the user picked so the wizard can recall/display/export them,
 * e.g. as a personal reference for manually adjusting in-game sliders.
 */
public class SensitivityProfile {

    public String gameId = "";
    public String gameName = "";
    public int xSensitivity = 100; // 0-400 (%)
    public int ySensitivity = 100; // 0-400 (%)
    public String controlLayout = "default";

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("gameId", gameId);
        o.put("gameName", gameName);
        o.put("xSensitivity", xSensitivity);
        o.put("ySensitivity", ySensitivity);
        o.put("controlLayout", controlLayout);
        return o;
    }

    public static SensitivityProfile fromJson(JSONObject o) throws JSONException {
        SensitivityProfile p = new SensitivityProfile();
        p.gameId = o.optString("gameId", "");
        p.gameName = o.optString("gameName", "");
        p.xSensitivity = o.optInt("xSensitivity", 100);
        p.ySensitivity = o.optInt("ySensitivity", 100);
        p.controlLayout = o.optString("controlLayout", "default");
        return p;
    }

    private static File fileFor(Context context, String gameId) {
        File dir = new File(context.getFilesDir(), "profiles");
        if (!dir.exists()) dir.mkdirs();
        String safeName = gameId.replaceAll("[^a-zA-Z0-9._-]", "_");
        return new File(dir, safeName + "_settings.json");
    }

    public void save(Context context) throws IOException, JSONException {
        File f = fileFor(context, gameId);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(toJson().toString(2).getBytes());
        }
    }

    public static SensitivityProfile load(Context context, String gameId) {
        File f = fileFor(context, gameId);
        if (!f.exists()) return null;
        StringBuilder sb = new StringBuilder();
        try (FileReader reader = new FileReader(f)) {
            char[] buf = new char[1024];
            int read;
            while ((read = reader.read(buf)) != -1) {
                sb.append(buf, 0, read);
            }
            return fromJson(new JSONObject(sb.toString()));
        } catch (Exception e) {
            return null;
        }
    }
}
