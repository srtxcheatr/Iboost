package com.srtxcheats.iboostx.ui;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Fullscreen OpenGL ES 3.0 shader background: a slowly drifting blue/purple
 * "nebula" glow behind the glass UI, satisfying the "advanced visual
 * effects via OpenGL ES 3.0" requirement without needing a full 3D scene.
 * It's a single textured-quad fragment shader — cheap enough to run
 * continuously behind a dashboard without competing with the very CPU
 * usage this app is trying to measure honestly.
 */
public class NebulaBackgroundView extends GLSurfaceView {

    public NebulaBackgroundView(Context context) {
        super(context);
        init();
    }

    public NebulaBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(3);
        setRenderer(new NebulaRenderer());
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setPreserveEGLContextOnPause(true);
    }

    private static class NebulaRenderer implements Renderer {

        private static final String VERTEX_SHADER =
                "#version 300 es\n" +
                "layout(location = 0) in vec2 aPosition;\n" +
                "out vec2 vUv;\n" +
                "void main() {\n" +
                "    vUv = aPosition * 0.5 + 0.5;\n" +
                "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
                "}\n";

        private static final String FRAGMENT_SHADER =
                "#version 300 es\n" +
                "precision mediump float;\n" +
                "in vec2 vUv;\n" +
                "out vec4 fragColor;\n" +
                "uniform float uTime;\n" +
                "uniform vec2 uResolution;\n" +
                "\n" +
                "float glow(vec2 uv, vec2 center, float radius) {\n" +
                "    float d = distance(uv, center);\n" +
                "    return smoothstep(radius, 0.0, d);\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = vUv;\n" +
                "    float aspect = uResolution.x / max(uResolution.y, 1.0);\n" +
                "    vec2 p = vec2((uv.x - 0.5) * aspect, uv.y - 0.5);\n" +
                "\n" +
                "    vec3 base = vec3(0.02, 0.024, 0.06);\n" +
                "    vec3 blue = vec3(0.30, 0.79, 1.0);\n" +
                "    vec3 purple = vec3(0.64, 0.42, 1.0);\n" +
                "\n" +
                "    vec2 c1 = vec2(sin(uTime * 0.08) * 0.35, cos(uTime * 0.05) * 0.25 + 0.25);\n" +
                "    vec2 c2 = vec2(cos(uTime * 0.06) * 0.4 - 0.1, sin(uTime * 0.07) * 0.3 - 0.2);\n" +
                "\n" +
                "    float g1 = glow(p, c1, 0.6);\n" +
                "    float g2 = glow(p, c2, 0.55);\n" +
                "\n" +
                "    vec3 color = base + blue * g1 * 0.55 + purple * g2 * 0.5;\n" +
                "    color = mix(color, base, smoothstep(0.0, 1.4, length(p)) * 0.35);\n" +
                "\n" +
                "    fragColor = vec4(color, 1.0);\n" +
                "}\n";

        private int program;
        private int uTimeHandle;
        private int uResolutionHandle;
        private FloatBuffer quadBuffer;
        private float width = 1f, height = 1f;
        private long startTimeNanos;

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            startTimeNanos = System.nanoTime();

            float[] quad = {
                    -1f, -1f,
                    1f, -1f,
                    -1f, 1f,
                    1f, 1f
            };
            quadBuffer = ByteBuffer.allocateDirect(quad.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            quadBuffer.put(quad).position(0);

            int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

            program = GLES30.glCreateProgram();
            GLES30.glAttachShader(program, vertexShader);
            GLES30.glAttachShader(program, fragmentShader);
            GLES30.glLinkProgram(program);

            uTimeHandle = GLES30.glGetUniformLocation(program, "uTime");
            uResolutionHandle = GLES30.glGetUniformLocation(program, "uResolution");
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int w, int h) {
            GLES30.glViewport(0, 0, w, h);
            width = w;
            height = h;
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES30.glClearColor(0.02f, 0.024f, 0.06f, 1f);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

            GLES30.glUseProgram(program);

            float elapsed = (System.nanoTime() - startTimeNanos) / 1_000_000_000f;
            GLES30.glUniform1f(uTimeHandle, elapsed);
            GLES30.glUniform2f(uResolutionHandle, width, height);

            GLES30.glEnableVertexAttribArray(0);
            GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, quadBuffer);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
            GLES30.glDisableVertexAttribArray(0);
        }

        private int compileShader(int type, String source) {
            int shader = GLES30.glCreateShader(type);
            GLES30.glShaderSource(shader, source);
            GLES30.glCompileShader(shader);
            return shader;
        }
    }
}
