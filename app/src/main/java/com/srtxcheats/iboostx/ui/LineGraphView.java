package com.srtxcheats.iboostx.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Rolling line graph for live percentage-style readings (0-100). Feed it
 * real samples via {@link #addSample(float)} — it draws exactly what it's
 * given, no smoothing that would misrepresent the data.
 */
public class LineGraphView extends View {

    private static final int MAX_POINTS = 60;

    private final Deque<Float> samples = new ArrayDeque<>();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int lineColor = Color.parseColor("#4CC9FF");

    public LineGraphView(Context context) {
        super(context);
        init();
    }

    public LineGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5f);
        linePaint.setColor(lineColor);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(Color.parseColor("#1AFFFFFF"));
        gridPaint.setStrokeWidth(2f);
    }

    public void setLineColor(int color) {
        this.lineColor = color;
        linePaint.setColor(color);
        invalidate();
    }

    public void addSample(float percent) {
        if (percent < 0) return; // don't plot "unavailable" as if it were 0%
        samples.addLast(Math.max(0f, Math.min(100f, percent)));
        while (samples.size() > MAX_POINTS) samples.removeFirst();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // horizontal guide lines at 25/50/75%
        for (int i = 1; i < 4; i++) {
            float y = h * (i / 4f);
            canvas.drawLine(0, y, w, y, gridPaint);
        }

        if (samples.size() < 2) return;

        fillPaint.setShader(new LinearGradient(0, 0, 0, h,
                (lineColor & 0x00FFFFFF) | 0x55000000,
                (lineColor & 0x00FFFFFF) | 0x00000000,
                Shader.TileMode.CLAMP));

        Float[] points = samples.toArray(new Float[0]);
        float stepX = (float) w / (MAX_POINTS - 1);
        float startX = w - (points.length - 1) * stepX;

        Path line = new Path();
        Path fill = new Path();
        boolean first = true;

        for (int i = 0; i < points.length; i++) {
            float x = startX + i * stepX;
            float y = h - (points[i] / 100f) * h;
            if (first) {
                line.moveTo(x, y);
                fill.moveTo(x, h);
                fill.lineTo(x, y);
                first = false;
            } else {
                line.lineTo(x, y);
                fill.lineTo(x, y);
            }
        }
        fill.lineTo(startX + (points.length - 1) * stepX, h);
        fill.close();

        canvas.drawPath(fill, fillPaint);
        canvas.drawPath(line, linePaint);
    }
}
