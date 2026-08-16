package com.srtxcheats.iboostx.games;

import android.graphics.drawable.Drawable;

public class Game {
    public String packageName;
    public String label;
    public Drawable icon;
    public boolean manuallyAdded;

    public Game(String packageName, String label, Drawable icon, boolean manuallyAdded) {
        this.packageName = packageName;
        this.label = label;
        this.icon = icon;
        this.manuallyAdded = manuallyAdded;
    }
}
