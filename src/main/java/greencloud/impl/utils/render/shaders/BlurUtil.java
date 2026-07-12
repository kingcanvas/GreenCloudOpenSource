package greencloud.impl.utils.render.shaders;

import greencloud.impl.logger.Log;
import greencloud.impl.logger.Logger;
import greencloud.impl.utils.AndroidUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.lang.reflect.Method;
import java.nio.FloatBuffer;

public class BlurUtil {

    private static final Logger log = Log.get(BlurUtil.class);

    public static boolean enabled = !AndroidUtil.isAndroid();

    private static final int KERNEL = 16;

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static int blurProg = -1;
    private static int uTexture, uTexelSize, uDirection, uRadius, uWeights;

    private static int compProg = -1;
    private static int cTexture, cTexelSize, cDirection, cRadius, cWeights;
    private static int cRectMin, cRectMax, cCornerRadius;

    private static Framebuffer snapshotBuffer;
    private static Framebuffer swapBuffer;
    private static int lastWidth, lastHeight;

    private static Framebuffer halfSnapBuffer;
    private static Framebuffer halfSwapBuffer;
    private static int lastHalfW, lastHalfH;

    private static boolean optifineChecked = false;
    private static Method isFastRenderMethod = null;

    private static float lastCachedRadius = -1;
    private static FloatBuffer cachedWeightBuf = null;

    private static boolean halfSnapFresh = false;

    private static final String VERT =
            "#version 120\n" +
                    "varying vec2 vUV;\n" +
                    "varying vec2 vPos;\n" +
                    "void main() {\n" +
                    "    gl_Position = vec4(gl_Vertex.xy, 0.0, 1.0);\n" +
                    "    vUV = gl_MultiTexCoord0.xy;\n" +
                    "    vPos = gl_Vertex.xy;\n" +
                    "}\n";

    private static final String FRAG_BLUR =
            "#version 120\n" +
                    "varying vec2 vUV;\n" +
                    "uniform sampler2D textureIn;\n" +
                    "uniform vec2 texelSize;\n" +
                    "uniform vec2 direction;\n" +
                    "uniform float radius;\n" +
                    "uniform float weights[16];\n" +
                    "void main() {\n" +
                    "    vec3 color = texture2D(textureIn, vUV).rgb * weights[0];\n" +
                    "    for (int i = 1; i < 16; i++) {\n" +
                    "        float fi = float(i);\n" +
                    "        float w = weights[i] * step(fi, radius);\n" +
                    "        vec2 delta = fi * texelSize * direction;\n" +
                    "        color += texture2D(textureIn, vUV + delta).rgb * w;\n" +
                    "        color += texture2D(textureIn, vUV - delta).rgb * w;\n" +
                    "    }\n" +
                    "    gl_FragColor = vec4(color, 1.0);\n" +
                    "}\n";

    private static final String FRAG_COMP =
            "#version 120\n" +
                    "varying vec2 vUV;\n" +
                    "varying vec2 vPos;\n" +
                    "uniform sampler2D textureIn;\n" +
                    "uniform vec2 texelSize;\n" +
                    "uniform vec2 direction;\n" +
                    "uniform float radius;\n" +
                    "uniform float weights[16];\n" +
                    "uniform vec2 rectMin;\n" +
                    "uniform vec2 rectMax;\n" +
                    "uniform float cornerRadius;\n" +
                    "float roundedRectSDF(vec2 p, vec2 b, float r) {\n" +
                    "    vec2 d = abs(p) - b + vec2(r);\n" +
                    "    return length(max(d, 0.0)) - r;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "    vec2 center = (rectMin + rectMax) * 0.5;\n" +
                    "    vec2 halfSize = (rectMax - rectMin) * 0.5;\n" +
                    "    float dist = roundedRectSDF(vPos - center, halfSize, cornerRadius);\n" +
                    "    if (dist > 0.0) discard;\n" +
                    "    vec3 color = texture2D(textureIn, vUV).rgb * weights[0];\n" +
                    "    for (int i = 1; i < 16; i++) {\n" +
                    "        float fi = float(i);\n" +
                    "        float w = weights[i] * step(fi, radius);\n" +
                    "        vec2 delta = fi * texelSize * direction;\n" +
                    "        color += texture2D(textureIn, vUV + delta).rgb * w;\n" +
                    "        color += texture2D(textureIn, vUV - delta).rgb * w;\n" +
                    "    }\n" +
                    "    gl_FragColor = vec4(color, 1.0);\n" +
                    "}\n";

    public static void captureScene() {
        if (!enabled || isFastRenderActive()) return;
        ensureBuffers();
        int dw = mc.displayWidth, dh = mc.displayHeight;
        int hw = dw / 2, hh = dh / 2;
        if (hw < 1 || hh < 1) return;
        ensureHalfBuffers(hw, hh);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mc.getFramebuffer().framebufferObject);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, halfSnapBuffer.framebufferObject);
        GL30.glBlitFramebuffer(0, 0, dw, dh, 0, 0, hw, hh, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mc.getFramebuffer().framebufferObject);
        halfSnapFresh = true;
    }

    public static void snapshot() {
        if (!enabled) return;
        ensureBuffers();
        int w = mc.displayWidth, h = mc.displayHeight;
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mc.getFramebuffer().framebufferObject);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, snapshotBuffer.framebufferObject);
        GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mc.getFramebuffer().framebufferObject);
    }

    public static boolean isFastRenderActive() {
        if (!optifineChecked) {
            optifineChecked = true;
            try {
                Class<?> cfg = null;
                try {
                    cfg = Class.forName("Config");
                } catch (ClassNotFoundException e) {
                    cfg = Class.forName("net.optifine.Config");
                }
                isFastRenderMethod = cfg.getMethod("isFastRender");
            } catch (Throwable ignored) {
                isFastRenderMethod = null;
            }
        }
        if (isFastRenderMethod == null) return false;
        try {
            return (boolean) isFastRenderMethod.invoke(null);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static FloatBuffer getOrBuildWeights(float blurRadius) {
        if (cachedWeightBuf != null && blurRadius == lastCachedRadius) {
            cachedWeightBuf.rewind();
            return cachedWeightBuf;
        }
        lastCachedRadius = blurRadius;
        float[] weights = buildWeights(Math.max(blurRadius / 3f, 0.01f));
        if (cachedWeightBuf == null) {
            cachedWeightBuf = BufferUtils.createFloatBuffer(KERNEL);
        }
        cachedWeightBuf.clear();
        cachedWeightBuf.put(weights).flip();
        return cachedWeightBuf;
    }

    public static void blurRegion(float rx, float ry, float rw, float rh, float radius) {
        blurRegionRounded(rx, ry, rw, rh, radius, 0);
    }

    public static void blurRegionRounded(float rx, float ry, float rw, float rh, float blurRadius, int cornerRadiusPx) {
        if (!enabled) return;
        if (isFastRenderActive()) return;
        if (!ensurePrograms()) return;
        ensureBuffers();

        blurRadius = Math.min(blurRadius, 20f);

        int dw = mc.displayWidth, dh = mc.displayHeight;
        int hw = dw / 2, hh = dh / 2;
        if (hw < 1 || hh < 1) return;

        ensureHalfBuffers(hw, hh);

        boolean scissor = GL11.glGetBoolean(GL11.GL_SCISSOR_TEST);
        if (scissor) GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (!halfSnapFresh) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mc.getFramebuffer().framebufferObject);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, halfSnapBuffer.framebufferObject);
            GL30.glBlitFramebuffer(0, 0, dw, dh, 0, 0, hw, hh, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mc.getFramebuffer().framebufferObject);
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.getScaleFactor();

        int px0 = (int)(rx * sf);
        int py0 = (int)(dh - (ry + rh) * sf);
        int px1 = (int)(px0 + rw * sf);
        int py1 = (int)(py0 + rh * sf);
        if (px1 <= px0 || py1 <= py0) return;

        float nx0 = (px0 / (float)dw) * 2f - 1f;
        float ny0 = (py0 / (float)dh) * 2f - 1f;
        float nx1 = (px1 / (float)dw) * 2f - 1f;
        float ny1 = (py1 / (float)dh) * 2f - 1f;

        float texW = (float) halfSwapBuffer.framebufferTextureWidth;
        float texH = (float) halfSwapBuffer.framebufferTextureHeight;
        float u0 = (px0 / 2f) / texW;
        float u1 = (px1 / 2f) / texW;
        float v0 = (py0 / 2f) / texH;
        float v1 = (py1 / 2f) / texH;

        float crNDC = (cornerRadiusPx * sf * 2f) / (float) dh;
        crNDC = Math.min(crNDC, Math.min((nx1 - nx0) / 2f, (ny1 - ny0) / 2f));

        float halfBlurRadius = Math.min(blurRadius * 0.5f, (float)(KERNEL - 1));
        FloatBuffer weightBuf = getOrBuildWeights(halfBlurRadius);

        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);  GL11.glPushMatrix(); GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, halfSnapBuffer.framebufferObject);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, halfSwapBuffer.framebufferObject);
        GL30.glBlitFramebuffer(0, 0, hw, hh, 0, 0, hw, hh, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

        GL20.glUseProgram(blurProg);
        GL20.glUniform1i(uTexture,   0);
        GL20.glUniform1f(uRadius,    halfBlurRadius);
        GL20.glUniform1(uWeights,    weightBuf);
        GL20.glUniform2f(uTexelSize, 1f / hw, 1f / hh);
        GL20.glUniform2f(uDirection, 1f, 0f);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, halfSwapBuffer.framebufferObject);
        GL11.glViewport(0, 0, hw, hh);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, halfSnapBuffer.framebufferTexture);
        drawNDCQuad(nx0, ny0, nx1, ny1, u0, v0, u1, v1);

        weightBuf.rewind();

        if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);

        GL20.glUseProgram(compProg);
        GL20.glUniform1i(cTexture,      0);
        GL20.glUniform1f(cRadius,       halfBlurRadius);
        GL20.glUniform1(cWeights,       weightBuf);
        GL20.glUniform2f(cTexelSize,    1f / hw, 1f / hh);
        GL20.glUniform2f(cDirection,    0f, 1f);
        GL20.glUniform2f(cRectMin,      nx0, ny0);
        GL20.glUniform2f(cRectMax,      nx1, ny1);
        GL20.glUniform1f(cCornerRadius, crNDC);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mc.getFramebuffer().framebufferObject);
        GL11.glViewport(0, 0, dw, dh);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, halfSwapBuffer.framebufferTexture);
        drawNDCQuad(nx0, ny0, nx1, ny1, u0, v0, u1, v1);

        GL20.glUseProgram(0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);  GL11.glPopMatrix();

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
    }

    private static void drawNDCQuad(float x0, float y0, float x1, float y1,
                                    float u0, float v0, float u1, float v1) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x0, y0, 0).tex(u0, v0).endVertex();
        wr.pos(x1, y0, 0).tex(u1, v0).endVertex();
        wr.pos(x1, y1, 0).tex(u1, v1).endVertex();
        wr.pos(x0, y1, 0).tex(u0, v1).endVertex();
        tess.draw();
    }

    private static float[] buildWeights(float sigma) {
        float[] w = new float[KERNEL];
        float sum = 0;
        for (int i = 0; i < KERNEL; i++) {
            w[i] = (float) Math.exp(-(i * i) / (2.0 * sigma * sigma));
            sum += (i == 0 ? 1 : 2) * w[i];
        }
        for (int i = 0; i < KERNEL; i++) w[i] /= sum;
        return w;
    }

    private static boolean ensurePrograms() {
        if (blurProg != -1 && compProg != -1) return true;
        try {
            if (blurProg == -1) {
                int v = compile(GL20.GL_VERTEX_SHADER,   VERT);
                int f = compile(GL20.GL_FRAGMENT_SHADER, FRAG_BLUR);
                blurProg = GL20.glCreateProgram();
                GL20.glAttachShader(blurProg, v); GL20.glAttachShader(blurProg, f);
                GL20.glLinkProgram(blurProg);
                GL20.glDeleteShader(v); GL20.glDeleteShader(f);
                uTexture = GL20.glGetUniformLocation(blurProg, "textureIn");
                uTexelSize = GL20.glGetUniformLocation(blurProg, "texelSize");
                uDirection = GL20.glGetUniformLocation(blurProg, "direction");
                uRadius = GL20.glGetUniformLocation(blurProg, "radius");
                uWeights = GL20.glGetUniformLocation(blurProg, "weights[0]");
                if (uWeights == -1) uWeights = GL20.glGetUniformLocation(blurProg, "weights");
            }
            if (compProg == -1) {
                int v = compile(GL20.GL_VERTEX_SHADER,   VERT);
                int f = compile(GL20.GL_FRAGMENT_SHADER, FRAG_COMP);
                compProg = GL20.glCreateProgram();
                GL20.glAttachShader(compProg, v); GL20.glAttachShader(compProg, f);
                GL20.glLinkProgram(compProg);
                GL20.glDeleteShader(v); GL20.glDeleteShader(f);
                cTexture = GL20.glGetUniformLocation(compProg, "textureIn");
                cTexelSize = GL20.glGetUniformLocation(compProg, "texelSize");
                cDirection = GL20.glGetUniformLocation(compProg, "direction");
                cRadius = GL20.glGetUniformLocation(compProg, "radius");
                cWeights = GL20.glGetUniformLocation(compProg, "weights[0]");
                if (cWeights == -1) cWeights = GL20.glGetUniformLocation(compProg, "weights");
                cRectMin = GL20.glGetUniformLocation(compProg, "rectMin");
                cRectMax = GL20.glGetUniformLocation(compProg, "rectMax");
                cCornerRadius = GL20.glGetUniformLocation(compProg, "cornerRadius");
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to compile/link blur shader programs", e);
            blurProg = compProg = -1;
            return false;
        }
    }

    private static int compile(int type, String src) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        String infoLog = GL20.glGetShaderInfoLog(id, 512);
        if (infoLog != null && !infoLog.trim().isEmpty()) {
            log.warn("Blur shader compile log: " + infoLog.trim());
        }
        return id;
    }

    private static void ensureBuffers() {
        if (swapBuffer != null && lastWidth == mc.displayWidth && lastHeight == mc.displayHeight) return;
        log.debug("Blur framebuffers resized to " + mc.displayWidth + "x" + mc.displayHeight);
        destroyFullBuffers();
        lastWidth = mc.displayWidth;
        lastHeight = mc.displayHeight;
        snapshotBuffer = new Framebuffer(lastWidth, lastHeight, true);
        snapshotBuffer.setFramebufferFilter(GL11.GL_LINEAR);
        swapBuffer = new Framebuffer(lastWidth, lastHeight, true);
        swapBuffer.setFramebufferFilter(GL11.GL_LINEAR);
    }

    private static void ensureHalfBuffers(int hw, int hh) {
        if (halfSnapBuffer != null && lastHalfW == hw && lastHalfH == hh) return;
        destroyHalfBuffers();
        lastHalfW = hw;
        lastHalfH = hh;
        halfSnapBuffer = new Framebuffer(hw, hh, false);
        halfSnapBuffer.setFramebufferFilter(GL11.GL_LINEAR);
        halfSwapBuffer = new Framebuffer(hw, hh, false);
        halfSwapBuffer.setFramebufferFilter(GL11.GL_LINEAR);
    }

    private static void destroyFullBuffers() {
        if (snapshotBuffer != null) { snapshotBuffer.deleteFramebuffer(); snapshotBuffer = null; }
        if (swapBuffer != null) { swapBuffer.deleteFramebuffer(); swapBuffer = null; }
    }

    private static void destroyHalfBuffers() {
        if (halfSnapBuffer != null) { halfSnapBuffer.deleteFramebuffer(); halfSnapBuffer = null; }
        if (halfSwapBuffer != null) { halfSwapBuffer.deleteFramebuffer(); halfSwapBuffer = null; }
    }

    public static void destroy() {
        if (blurProg != -1) { GL20.glDeleteProgram(blurProg); blurProg = -1; }
        if (compProg != -1) { GL20.glDeleteProgram(compProg); compProg = -1; }
        destroyFullBuffers();
        destroyHalfBuffers();
    }
}
