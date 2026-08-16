package com.srtxcheats.iboostx.ui;

import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

/**
 * Real backdrop blur, where the OS actually supports it. RenderEffect
 * blur was added in Android 12 (API 31); below that Android has no public
 * live-blur API for arbitrary views, so we honestly skip it rather than
 * faking blur with a screenshot hack, and rely on the translucent glass
 * drawables to still read as "glass" without blur.
 */
public final class GlassBlurHelper {

    private GlassBlurHelper() {}

    public static void applyBlur(View view, float radius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
        }
    }

    public static void clearBlur(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(null);
        }
    }
}
