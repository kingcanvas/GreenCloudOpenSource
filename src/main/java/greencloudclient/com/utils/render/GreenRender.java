package greencloudclient.com.utils.render;

import greencloudclient.com.utils.AndroidUtil;
import greencloudclient.com.utils.font.CustomFontRenderer;
import greencloudclient.com.utils.font.FontUtil;
import greencloudclient.com.utils.render.shaders.BlurUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

public final class GreenRender {

    public static final Minecraft mc = Minecraft.getMinecraft();

    private static final int GRADIENT_NONE = 0, GRADIENT_HORIZONTAL = 1, GRADIENT_VERTICAL = 2;

    private static int prog = -1;
    private static int vbo = -1;
    private static int aPos = -1;
    private static boolean shaderFailed;

    private static int uRect, uCorners, uColor, uGradientColor, uGradientMode;
    private static int uStrokeColor, uStrokeWidth, uGlowColor, uGlowWidth;

    private static final FloatBuffer quadBuffer = BufferUtils.createFloatBuffer(8);
    private static final Deque<int[]> scissorStack = new ArrayDeque<>();

    private static ScaledResolution cachedSR;
    private static int cachedDisplayW, cachedDisplayH, cachedGuiScale = -1;

    private static final String VERT =
            "#version 120\n" +
            "attribute vec2 aPos;\n" +
            "varying vec2 vPos;\n" +
            "void main() {\n" +
            "    vPos = aPos;\n" +
            "    gl_Position = gl_ModelViewProjectionMatrix * vec4(aPos, 0.0, 1.0);\n" +
            "}\n";

    private static final String FRAG =
            "#version 120\n" +
            "varying vec2 vPos;\n" +
            "uniform vec4  uRect;\n" +
            "uniform vec4  uCorners;\n" +
            "uniform vec4  uColor;\n" +
            "uniform vec4  uGradientColor;\n" +
            "uniform int   uGradientMode;\n" +
            "uniform vec4  uStrokeColor;\n" +
            "uniform float uStrokeWidth;\n" +
            "uniform vec4  uGlowColor;\n" +
            "uniform float uGlowWidth;\n" +
            "float roundedBox(vec2 p, vec2 b, vec4 r) {\n" +
            "    r.xy = (p.x > 0.0) ? r.xy : r.zw;\n" +
            "    r.x  = (p.y > 0.0) ? r.x  : r.y;\n" +
            "    float rad = min(r.x, min(b.x, b.y));\n" +
            "    vec2 q = abs(p) - b + rad;\n" +
            "    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rad;\n" +
            "}\n" +
            "vec4 over(vec4 top, vec4 bottom) {\n" +
            "    float a = top.a + bottom.a * (1.0 - top.a);\n" +
            "    if (a <= 0.0) return vec4(0.0);\n" +
            "    return vec4((top.rgb * top.a + bottom.rgb * bottom.a * (1.0 - top.a)) / a, a);\n" +
            "}\n" +
            "void main() {\n" +
            "    vec2 halfSize = (uRect.zw - uRect.xy) * 0.5;\n" +
            "    vec2 p = vPos - (uRect.xy + uRect.zw) * 0.5;\n" +
            "    float d = roundedBox(p, halfSize, uCorners);\n" +
            "    float aa = max(fwidth(d), 0.0001);\n" +
            "    vec4 fill = uColor;\n" +
            "    if (uGradientMode == 1) fill = mix(uColor, uGradientColor, clamp((vPos.x - uRect.x) / (uRect.z - uRect.x), 0.0, 1.0));\n" +
            "    else if (uGradientMode == 2) fill = mix(uColor, uGradientColor, clamp((vPos.y - uRect.y) / (uRect.w - uRect.y), 0.0, 1.0));\n" +
            "    float fillCov = clamp(0.5 - d / aa, 0.0, 1.0);\n" +
            "    float strokeCov = 0.0;\n" +
            "    if (uStrokeWidth > 0.0) strokeCov = clamp(0.5 - (abs(d + uStrokeWidth * 0.5) - uStrokeWidth * 0.5) / aa, 0.0, 1.0);\n" +
            "    float glowA = 0.0;\n" +
            "    if (uGlowWidth > 0.0) glowA = uGlowColor.a * smoothstep(-uGlowWidth, 0.0, d) * (1.0 - smoothstep(0.0, uGlowWidth, d));\n" +
            "    vec4 result = vec4(uGlowColor.rgb, glowA);\n" +
            "    result = over(vec4(fill.rgb, fill.a * fillCov), result);\n" +
            "    result = over(vec4(uStrokeColor.rgb, uStrokeColor.a * strokeCov), result);\n" +
            "    if (result.a <= 0.002) discard;\n" +
            "    gl_FragColor = result;\n" +
            "}\n";

    private GreenRender() {}

    private static boolean ensureShader() {
        if (prog != -1) return true;
        if (shaderFailed || AndroidUtil.isAndroid()) return false;
        int v = -1, f = -1;
        try {
            v = compile(GL20.GL_VERTEX_SHADER, VERT);
            f = compile(GL20.GL_FRAGMENT_SHADER, FRAG);
            if (v == -1 || f == -1) return fail();

            prog = GL20.glCreateProgram();
            GL20.glAttachShader(prog, v);
            GL20.glAttachShader(prog, f);
            GL20.glLinkProgram(prog);
            if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) return fail();

            uRect          = GL20.glGetUniformLocation(prog, "uRect");
            uCorners       = GL20.glGetUniformLocation(prog, "uCorners");
            uColor         = GL20.glGetUniformLocation(prog, "uColor");
            uGradientColor = GL20.glGetUniformLocation(prog, "uGradientColor");
            uGradientMode  = GL20.glGetUniformLocation(prog, "uGradientMode");
            uStrokeColor   = GL20.glGetUniformLocation(prog, "uStrokeColor");
            uStrokeWidth   = GL20.glGetUniformLocation(prog, "uStrokeWidth");
            uGlowColor     = GL20.glGetUniformLocation(prog, "uGlowColor");
            uGlowWidth     = GL20.glGetUniformLocation(prog, "uGlowWidth");
            aPos           = GL20.glGetAttribLocation(prog, "aPos");

            vbo = GL15.glGenBuffers();
            if (vbo <= 0 || aPos < 0) return fail();
            return true;
        } catch (Throwable t) {
            return fail();
        } finally {
            if (v > 0) GL20.glDeleteShader(v);
            if (f > 0) GL20.glDeleteShader(f);
        }
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GL20.glDeleteShader(shader);
            return -1;
        }
        return shader;
    }

    private static boolean fail() {
        destroy();
        shaderFailed = true;
        return false;
    }

    private static void shape(float x, float y, float w, float h, float tl, float tr, float br, float bl,
                              int fill, int gradient, int gradientMode,
                              int stroke, float strokeWidth, int glow, float glowWidth) {
        if (w <= 0 || h <= 0) return;
        if (!ensureShader()) {
            fallbackShape(x, y, w, h, tl, tr, br, bl, fill, gradient, gradientMode, stroke, strokeWidth);
            return;
        }

        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        GL20.glUseProgram(prog);
        GL20.glUniform4f(uRect, x, y, x + w, y + h);
        GL20.glUniform4f(uCorners, br, tr, bl, tl);
        uniformColor(uColor, fill);
        uniformColor(uGradientColor, gradient);
        GL20.glUniform1i(uGradientMode, gradientMode);
        uniformColor(uStrokeColor, stroke);
        GL20.glUniform1f(uStrokeWidth, strokeWidth);
        uniformColor(uGlowColor, glow);
        GL20.glUniform1f(uGlowWidth, glowWidth);

        float pad = 1f + glowWidth;
        float x1 = x - pad, y1 = y - pad, x2 = x + w + pad, y2 = y + h + pad;
        quadBuffer.clear();
        quadBuffer.put(x1).put(y1).put(x2).put(y1).put(x2).put(y2).put(x1).put(y2).flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quadBuffer, GL15.GL_STREAM_DRAW);
        GL20.glEnableVertexAttribArray(aPos);
        GL20.glVertexAttribPointer(aPos, 2, GL11.GL_FLOAT, false, 0, 0);
        GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);
        GL20.glDisableVertexAttribArray(aPos);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL20.glUseProgram(0);

        if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST);
        if (alpha) GL11.glEnable(GL11.GL_ALPHA_TEST);
        if (cull) GL11.glEnable(GL11.GL_CULL_FACE);
        if (!blend) GL11.glDisable(GL11.GL_BLEND);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void uniformColor(int location, int argb) {
        GL20.glUniform4f(location, (argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f);
    }

    private static void fallbackShape(float x, float y, float w, float h, float tl, float tr, float br, float bl,
                                      int fill, int gradient, int gradientMode, int stroke, float strokeWidth) {
        float max = Math.min(w, h) / 2f;
        tl = Math.min(tl, max); tr = Math.min(tr, max); br = Math.min(br, max); bl = Math.min(bl, max);
        float[] outer = roundedPath(x, y, w, h, tl, tr, br, bl);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        if ((fill >>> 24) > 0 || (gradientMode != GRADIENT_NONE && (gradient >>> 24) > 0)) {
            wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            vertex(wr, x + w / 2f, y + h / 2f, fallbackColor(x, y, w, h, x + w / 2f, y + h / 2f, fill, gradient, gradientMode));
            for (int i = 0; i <= outer.length - 2; i += 2) {
                vertex(wr, outer[i], outer[i + 1], fallbackColor(x, y, w, h, outer[i], outer[i + 1], fill, gradient, gradientMode));
            }
            vertex(wr, outer[0], outer[1], fallbackColor(x, y, w, h, outer[0], outer[1], fill, gradient, gradientMode));
            tessellator.draw();
        }

        if (strokeWidth > 0 && (stroke >>> 24) > 0) {
            float s = Math.min(strokeWidth, max);
            float[] inner = roundedPath(x + s, y + s, w - s * 2, h - s * 2,
                    Math.max(0, tl - s), Math.max(0, tr - s), Math.max(0, br - s), Math.max(0, bl - s));
            wr.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= outer.length; i += 2) {
                int j = i % outer.length;
                vertex(wr, outer[j], outer[j + 1], stroke);
                vertex(wr, inner[j], inner[j + 1], stroke);
            }
            tessellator.draw();
        }

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static int fallbackColor(float x, float y, float w, float h, float px, float py, int fill, int gradient, int mode) {
        if (mode == GRADIENT_HORIZONTAL) return lerpARGB(fill, gradient, (px - x) / w);
        if (mode == GRADIENT_VERTICAL) return lerpARGB(fill, gradient, (py - y) / h);
        return fill;
    }

    private static float[] roundedPath(float x, float y, float w, float h, float tl, float tr, float br, float bl) {
        final int seg = 8;
        float[] out = new float[(seg + 1) * 4 * 2];
        int i = 0;
        float[][] corners = {
                {x + tl, y + tl, tl, 180},
                {x + w - tr, y + tr, tr, 270},
                {x + w - br, y + h - br, br, 0},
                {x + bl, y + h - bl, bl, 90}
        };
        for (float[] c : corners) {
            for (int s = 0; s <= seg; s++) {
                double a = Math.toRadians(c[3] + 90.0 * s / seg);
                out[i++] = c[0] + (float) Math.cos(a) * c[2];
                out[i++] = c[1] + (float) Math.sin(a) * c[2];
            }
        }
        return out;
    }

    private static void vertex(WorldRenderer wr, float x, float y, int argb) {
        wr.pos(x, y, 0).color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24).endVertex();
    }

    public static void fillRR(float x, float y, float w, float h, float radius, int argb) {
        shape(x, y, w, h, radius, radius, radius, radius, argb, 0, GRADIENT_NONE, 0, 0, 0, 0);
    }

    public static void fillRR(float x, float y, float w, float h, float radius, Color color) {
        fillRR(x, y, w, h, radius, color.getRGB());
    }

    public static void fillRRCorners(float x, float y, float w, float h,
                                     float tl, float tr, float br, float bl, int argb) {
        shape(x, y, w, h, tl, tr, br, bl, argb, 0, GRADIENT_NONE, 0, 0, 0, 0);
    }

    public static void fillRRCorners(float x, float y, float w, float h,
                                     float tl, float tr, float br, float bl, Color color) {
        fillRRCorners(x, y, w, h, tl, tr, br, bl, color.getRGB());
    }

    public static void fillRRCornersHard(float x, float y, float w, float h,
                                         float tl, float tr, float br, float bl, int argb) {
        fillRRCorners(x, y, w, h, tl, tr, br, bl, argb);
    }

    public static void fillRRCornersHard(float x, float y, float w, float h,
                                         float tl, float tr, float br, float bl, Color color) {
        fillRRCorners(x, y, w, h, tl, tr, br, bl, color.getRGB());
    }

    public static void strokeRR(float x, float y, float w, float h, float radius, float strokeWidth, int fillARGB, int strokeARGB) {
        shape(x, y, w, h, radius, radius, radius, radius, fillARGB, 0, GRADIENT_NONE, strokeARGB, strokeWidth, 0, 0);
    }

    public static void strokeRR(float x, float y, float w, float h, float radius, float strokeWidth, Color fill, Color stroke) {
        strokeRR(x, y, w, h, radius, strokeWidth, fill.getRGB(), stroke.getRGB());
    }

    public static void outlineRR(float x, float y, float w, float h, float radius, float strokeWidth, int strokeARGB) {
        strokeRR(x, y, w, h, radius, strokeWidth, 0, strokeARGB);
    }

    public static void outlineRR(float x, float y, float w, float h, float radius, float strokeWidth, Color stroke) {
        outlineRR(x, y, w, h, radius, strokeWidth, stroke.getRGB());
    }

    public static void fillRRGradientH(float x, float y, float w, float h, float radius, int leftARGB, int rightARGB) {
        shape(x, y, w, h, radius, radius, radius, radius, leftARGB, rightARGB, GRADIENT_HORIZONTAL, 0, 0, 0, 0);
    }

    public static void fillRRGradientH(float x, float y, float w, float h, float radius, Color left, Color right) {
        fillRRGradientH(x, y, w, h, radius, left.getRGB(), right.getRGB());
    }

    public static void fillRRGradientV(float x, float y, float w, float h, float radius, int topARGB, int bottomARGB) {
        shape(x, y, w, h, radius, radius, radius, radius, topARGB, bottomARGB, GRADIENT_VERTICAL, 0, 0, 0, 0);
    }

    public static void fillRRGradientV(float x, float y, float w, float h, float radius, Color top, Color bottom) {
        fillRRGradientV(x, y, w, h, radius, top.getRGB(), bottom.getRGB());
    }

    public static void fillRRCornersGradientV(float x, float y, float w, float h,
                                              float tl, float tr, float br, float bl,
                                              int topARGB, int bottomARGB) {
        shape(x, y, w, h, tl, tr, br, bl, topARGB, bottomARGB, GRADIENT_VERTICAL, 0, 0, 0, 0);
    }

    public static void fillRRCornersGradientV(float x, float y, float w, float h,
                                              float tl, float tr, float br, float bl,
                                              Color top, Color bottom) {
        fillRRCornersGradientV(x, y, w, h, tl, tr, br, bl, top.getRGB(), bottom.getRGB());
    }

    public static void glowRR(float x, float y, float w, float h, float radius, float glowWidth, int argb) {
        if (glowWidth <= 0 || !ensureShader()) return;
        shape(x, y, w, h, radius, radius, radius, radius, 0, 0, GRADIENT_NONE, 0, 0, argb, glowWidth);
    }

    public static void glowRR(float x, float y, float w, float h, float radius, float glowWidth, Color glowColor) {
        glowRR(x, y, w, h, radius, glowWidth, glowColor.getRGB());
    }

    public static void fillRect(float x, float y, float w, float h, int argb) {
        fillRR(x, y, w, h, 0, argb);
    }

    public static void fillRect(float x, float y, float w, float h, Color color) {
        fillRR(x, y, w, h, 0, color.getRGB());
    }

    public static void fillGradientH(float x, float y, float w, float h, Color left, Color right) {
        fillRRGradientH(x, y, w, h, 0, left.getRGB(), right.getRGB());
    }

    public static void fillGradientV(float x, float y, float w, float h, Color top, Color bottom) {
        fillRRGradientV(x, y, w, h, 0, top.getRGB(), bottom.getRGB());
    }

    public static void fillCircle(float cx, float cy, float radius, int argb) {
        fillRR(cx - radius, cy - radius, radius * 2, radius * 2, radius, argb);
    }

    public static void fillCircle(float cx, float cy, float radius, Color color) {
        fillCircle(cx, cy, radius, color.getRGB());
    }

    public static void strokeCircle(float cx, float cy, float radius, float strokeWidth, Color fill, Color stroke) {
        strokeRR(cx - radius, cy - radius, radius * 2, radius * 2, radius, strokeWidth, fill.getRGB(), stroke.getRGB());
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float thickness, int argb) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 0 || thickness <= 0) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x1, y1, 0);
        GlStateManager.rotate((float) Math.toDegrees(Math.atan2(dy, dx)), 0, 0, 1);
        float r = thickness / 2f;
        fillRR(-r, -r, len + thickness, thickness, r, argb);
        GlStateManager.popMatrix();
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float thickness, Color color) {
        drawLine(x1, y1, x2, y2, thickness, color.getRGB());
    }

    public static void blur(float x, float y, float w, float h, float strength) {
        BlurUtil.blurRegion(x, y, w, h, strength);
    }

    public static void blurRounded(float x, float y, float w, float h, float strength, int cornerRadius) {
        BlurUtil.blurRegionRounded(x, y, w, h, strength, cornerRadius);
    }

    public static void pushScissor(float x, float y, float w, float h) {
        int sf = getSR().getScaleFactor();
        int x1 = (int) Math.floor(x * sf);
        int y1 = (int) Math.floor(y * sf);
        int x2 = (int) Math.ceil((x + Math.max(0, w)) * sf);
        int y2 = (int) Math.ceil((y + Math.max(0, h)) * sf);
        int[] parent = scissorStack.peek();
        if (parent != null) {
            x1 = Math.max(x1, parent[0]);
            y1 = Math.max(y1, parent[1]);
            x2 = Math.min(x2, parent[2]);
            y2 = Math.min(y2, parent[3]);
        }
        int[] rect = {x1, y1, Math.max(x1, x2), Math.max(y1, y2)};
        scissorStack.push(rect);
        applyScissor(rect);
    }

    public static void popScissor() {
        scissorStack.poll();
        int[] parent = scissorStack.peek();
        if (parent == null) GL11.glDisable(GL11.GL_SCISSOR_TEST);
        else applyScissor(parent);
    }

    private static void applyScissor(int[] r) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(r[0], mc.displayHeight - r[3], r[2] - r[0], r[3] - r[1]);
    }

    private static ScaledResolution getSR() {
        int dw = mc.displayWidth, dh = mc.displayHeight, gs = mc.gameSettings.guiScale;
        if (cachedSR == null || cachedDisplayW != dw || cachedDisplayH != dh || cachedGuiScale != gs) {
            cachedSR = new ScaledResolution(mc);
            cachedDisplayW = dw;
            cachedDisplayH = dh;
            cachedGuiScale = gs;
        }
        return cachedSR;
    }

    public static void drawString(String text, float x, float y, int argb)      { text(FontUtil.normal, text, x, y, argb); }
    public static void drawStringSmall(String text, float x, float y, int argb) { text(FontUtil.small, text, x, y, argb); }
    public static void drawStringBold(String text, float x, float y, int argb)  { text(FontUtil.bold, text, x, y, argb); }

    public static void drawString(String text, float x, float y, Color color)      { drawString(text, x, y, color.getRGB()); }
    public static void drawStringSmall(String text, float x, float y, Color color) { drawStringSmall(text, x, y, color.getRGB()); }
    public static void drawStringBold(String text, float x, float y, Color color)  { drawStringBold(text, x, y, color.getRGB()); }

    public static void drawCenteredString(String text, float cx, float y, int argb) { drawString(text, cx - strW(text) / 2f, y, argb); }
    public static void drawCenteredStringBold(String text, float cx, float y, int argb) { drawStringBold(text, cx - strWBold(text) / 2f, y, argb); }

    private static void text(CustomFontRenderer font, String text, float x, float y, int argb) {
        if (font != null) {
            font.drawStringWithShadow(text, x, y, argb);
            return;
        }
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc.fontRendererObj.drawStringWithShadow(text, Math.round(x), Math.round(y), argb);
    }

    public static float strW(String t)      { return FontUtil.normal != null ? FontUtil.normal.getWidth(t) : mc.fontRendererObj.getStringWidth(t); }
    public static float strWSmall(String t) { return FontUtil.small  != null ? FontUtil.small.getWidth(t)  : mc.fontRendererObj.getStringWidth(t); }
    public static float strWBold(String t)  { return FontUtil.bold   != null ? FontUtil.bold.getWidth(t)   : mc.fontRendererObj.getStringWidth(t); }
    public static float fontH()      { return FontUtil.normal != null ? FontUtil.normal.getFontHeight() : mc.fontRendererObj.FONT_HEIGHT; }
    public static float fontHSmall() { return FontUtil.small  != null ? FontUtil.small.getFontHeight()  : mc.fontRendererObj.FONT_HEIGHT; }
    public static float fontHBold()  { return FontUtil.bold   != null ? FontUtil.bold.getFontHeight()   : mc.fontRendererObj.FONT_HEIGHT; }

    public static int lerpARGB(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return (int) (aa + (ba - aa) * t) << 24
                | (int) (ar + (br - ar) * t) << 16
                | (int) (ag + (bg - ag) * t) << 8
                | (int) (ab + (bb - ab) * t);
    }

    public static Color lerp(Color a, Color b, float t) {
        return new Color(lerpARGB(a.getRGB(), b.getRGB(), t), true);
    }

    private static int setAlpha(int argb, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | (argb & 0xFFFFFF);
    }

    public static int withAlphaARGB(int argb, float alpha) {
        return setAlpha(argb, (int) (alpha * 255));
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(setAlpha(c.getRGB(), alpha), true);
    }

    public static Color withAlpha(Color c, float alpha) {
        return withAlpha(c, (int) (alpha * 255));
    }

    public static float smooth(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    public static float lerpFloat(float a, float b, float t) {
        return a + (b - a) * Math.max(0f, Math.min(1f, t));
    }

    public static void destroy() {
        if (prog > 0) GL20.glDeleteProgram(prog);
        if (vbo > 0) GL15.glDeleteBuffers(vbo);
        prog = -1;
        vbo = -1;
        aPos = -1;
    }
}
