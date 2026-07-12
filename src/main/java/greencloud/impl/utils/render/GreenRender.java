package greencloud.impl.utils.render;

import greencloud.impl.utils.font.FontUtil;
import greencloud.impl.utils.render.shaders.BlurUtil;
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

public final class GreenRender {

    public static final Minecraft mc = Minecraft.getMinecraft();

    private static int prog       = -1;
    private static int vbo        = -1;

    private static int uResolution;
    private static int uRect;
    private static int uRadius;
    private static int uColor;
    private static int uStrokeColor;
    private static int uStrokeWidth;
    private static int uGradientColor;
    private static int uGradientEnabled;
    private static int uGradientHorizontal;
    private static int uGlowColor;
    private static int uGlowWidth;
    private static int uSoftness;
    private static int uCorners;
    private static int aPos = -1;

    private static ScaledResolution cachedSR;
    private static int cachedDisplayW, cachedDisplayH;

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
                    "uniform vec2  uResolution;\n" +
                    "uniform vec4  uRect;\n" +
                    "uniform float uRadius;\n" +
                    "uniform vec4  uCorners;\n" +
                    "uniform vec4  uColor;\n" +
                    "uniform vec4  uStrokeColor;\n" +
                    "uniform float uStrokeWidth;\n" +
                    "uniform vec4  uGradientColor;\n" +
                    "uniform int   uGradientEnabled;\n" +
                    "uniform int   uGradientHorizontal;\n" +
                    "uniform vec4  uGlowColor;\n" +
                    "uniform float uGlowWidth;\n" +
                    "uniform float uSoftness;\n" +
                    "float sdf(vec2 p, vec2 b, float r) {\n" +
                    "    vec2 d = abs(p) - b + vec2(r);\n" +
                    "    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - r;\n" +
                    "}\n" +
                    "float sdfCorners(vec2 p, vec2 b, vec4 r) {\n" +
                    "    r.xy = (p.x > 0.0) ? r.xy : r.zw;\n" +
                    "    r.x  = (p.y > 0.0) ? r.x  : r.y;\n" +
                    "    vec2 d = abs(p) - b + vec2(r.x);\n" +
                    "    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - r.x;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "    vec2 center = (uRect.xy + uRect.zw) * 0.5;\n" +
                    "    vec2 halfSize = (uRect.zw - uRect.xy) * 0.5;\n" +
                    "    vec2 local = vPos - center;\n" +
                    "    float dist = (uCorners.x >= 0.0)\n" +
                    "        ? sdfCorners(local, halfSize, uCorners)\n" +
                    "        : sdf(local, halfSize, uRadius);\n" +
                    "    float soft = max(uSoftness, 0.5);\n" +
                    "    vec4 fill = uColor;\n" +
                    "    if (uGradientEnabled == 1) {\n" +
                    "        float t;\n" +
                    "        if (uGradientHorizontal == 1) t = (vPos.x - uRect.x) / (uRect.z - uRect.x);\n" +
                    "        else t = (vPos.y - uRect.y) / (uRect.w - uRect.y);\n" +
                    "        t = clamp(t, 0.0, 1.0);\n" +
                    "        fill = mix(uColor, uGradientColor, t);\n" +
                    "    }\n" +
                    "    float fillAlpha   = fill.a   * (1.0 - smoothstep(-soft, soft, dist));\n" +
                    "    float strokeAlpha = uStrokeColor.a * (1.0 - smoothstep(-soft, soft, abs(dist) - uStrokeWidth * 0.5));\n" +
                    "    float glowAlpha   = uGlowColor.a   * smoothstep(-soft, 0.0, dist) * (1.0 - smoothstep(0.0, uGlowWidth, dist));\n" +
                    "    vec4 result = vec4(0.0);\n" +
                    "    if (glowAlpha > 0.0) result = vec4(uGlowColor.rgb, glowAlpha);\n" +
                    "    if (fillAlpha > 0.0) result = vec4(mix(result.rgb, fill.rgb, fillAlpha), max(result.a, fillAlpha));\n" +
                    "    if (strokeAlpha > 0.0) result = vec4(mix(result.rgb, uStrokeColor.rgb, strokeAlpha), max(result.a, strokeAlpha));\n" +
                    "    if (result.a < 0.01) discard;\n" +
                    "    gl_FragColor = result;\n" +
                    "}\n";

    private GreenRender() {}

    private static boolean ensureShader() {
        if (prog != -1) return true;
        try {
            int v = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(v, VERT);
            GL20.glCompileShader(v);

            int f = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(f, FRAG);
            GL20.glCompileShader(f);

            prog = GL20.glCreateProgram();
            GL20.glAttachShader(prog, v);
            GL20.glAttachShader(prog, f);
            GL20.glLinkProgram(prog);
            GL20.glDeleteShader(v);
            GL20.glDeleteShader(f);

            uResolution        = GL20.glGetUniformLocation(prog, "uResolution");
            uRect              = GL20.glGetUniformLocation(prog, "uRect");
            uRadius            = GL20.glGetUniformLocation(prog, "uRadius");
            uColor             = GL20.glGetUniformLocation(prog, "uColor");
            uStrokeColor       = GL20.glGetUniformLocation(prog, "uStrokeColor");
            uStrokeWidth       = GL20.glGetUniformLocation(prog, "uStrokeWidth");
            uGradientColor     = GL20.glGetUniformLocation(prog, "uGradientColor");
            uGradientEnabled   = GL20.glGetUniformLocation(prog, "uGradientEnabled");
            uGradientHorizontal= GL20.glGetUniformLocation(prog, "uGradientHorizontal");
            uGlowColor         = GL20.glGetUniformLocation(prog, "uGlowColor");
            uGlowWidth         = GL20.glGetUniformLocation(prog, "uGlowWidth");
            uSoftness          = GL20.glGetUniformLocation(prog, "uSoftness");
            uCorners           = GL20.glGetUniformLocation(prog, "uCorners");

            vbo = GL15.glGenBuffers();
            aPos = GL20.glGetAttribLocation(prog, "aPos");
            return true;
        } catch (Exception e) {
            prog = -1;
            return false;
        }
    }

    private static final FloatBuffer quadBuffer = BufferUtils.createFloatBuffer(8);

    private static ScaledResolution getSR() {
        int dw = mc.displayWidth, dh = mc.displayHeight;
        if (cachedSR == null || cachedDisplayW != dw || cachedDisplayH != dh) {
            cachedSR = new ScaledResolution(mc);
            cachedDisplayW = dw;
            cachedDisplayH = dh;
        }
        return cachedSR;
    }

    private static void drawQuad(float x, float y, float w, float h) {
        float x2 = x + w, y2 = y + h;
        quadBuffer.clear();
        quadBuffer.put(x).put(y).put(x2).put(y).put(x2).put(y2).put(x).put(y2).flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quadBuffer, GL15.GL_DYNAMIC_DRAW);

        GL20.glEnableVertexAttribArray(aPos);
        GL20.glVertexAttribPointer(aPos, 2, GL11.GL_FLOAT, false, 0, 0);

        GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);

        GL20.glDisableVertexAttribArray(aPos);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private static void setDefaults(float x, float y, float w, float h, float radius) {
        ScaledResolution sr = getSR();
        GL20.glUniform2f(uResolution, sr.getScaledWidth(), sr.getScaledHeight());
        GL20.glUniform4f(uRect, x, y, x + w, y + h);
        GL20.glUniform1f(uRadius, radius);
        GL20.glUniform1f(uSoftness, 1.0f);
        GL20.glUniform4f(uCorners, -1f, -1f, -1f, -1f);
        GL20.glUniform1f(uStrokeWidth, 0f);
        GL20.glUniform4f(uStrokeColor, 0f, 0f, 0f, 0f);
        GL20.glUniform1i(uGradientEnabled, 0);
        GL20.glUniform1i(uGradientHorizontal, 0);
        GL20.glUniform4f(uGradientColor, 0f, 0f, 0f, 0f);
        GL20.glUniform4f(uGlowColor, 0f, 0f, 0f, 0f);
        GL20.glUniform1f(uGlowWidth, 0f);
    }

    private static void applyColor(int loc, Color c) {
        GL20.glUniform4f(loc, c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, c.getAlpha()/255f);
    }

    private static void render(float x, float y, float w, float h) {
        ScaledResolution sr = getSR();

        boolean depthWasOn = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean alphaWasOn = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean cullWasOn = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean texWasOn = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean blendWasOn = GL11.glIsEnabled(GL11.GL_BLEND);

        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glLoadIdentity();
        GL11.glOrtho(0, sr.getScaledWidth(), sr.getScaledHeight(), 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix(); GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        drawQuad(x - 1, y - 1, w + 2, h + 2);

        GL20.glUseProgram(0);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);  GL11.glPopMatrix();

        if (depthWasOn) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (alphaWasOn) GL11.glEnable(GL11.GL_ALPHA_TEST); else GL11.glDisable(GL11.GL_ALPHA_TEST);
        if (cullWasOn) GL11.glEnable(GL11.GL_CULL_FACE); else GL11.glDisable(GL11.GL_CULL_FACE);
        if (texWasOn) GL11.glEnable(GL11.GL_TEXTURE_2D); else GL11.glDisable(GL11.GL_TEXTURE_2D);
        if (blendWasOn) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public static void fillRR(float x, float y, float w, float h, float radius, Color color) {
        if (!ensureShader()) return;
        GL20.glUseProgram(prog);
        setDefaults(x, y, w, h, radius);
        applyColor(uColor, color);
        render(x, y, w, h);
    }

    public static void fillRR(float x, float y, float w, float h, float radius, int argb) {
        fillRR(x, y, w, h, radius, new Color(argb, true));
    }

    public static void fillRRCorners(float x, float y, float w, float h,
                                     float tl, float tr, float br, float bl, Color color) {
        if (!ensureShader()) return;
        GL20.glUseProgram(prog);
        setDefaults(x, y, w, h, 0);
        applyColor(uColor, color);
        GL20.glUniform4f(uCorners, br, tr, bl, tl);
        render(x, y, w, h);
    }

    public static void fillRRCorners(float x, float y, float w, float h,
                                     float tl, float tr, float br, float bl, int argb) {
        fillRRCorners(x, y, w, h, tl, tr, br, bl, new Color(argb, true));
    }

    public static void fillRRCornersHard(float x, float y, float w, float h,
                                         float tl, float tr, float br, float bl, Color color) {
        if (!ensureShader()) return;
        GL20.glUseProgram(prog);
        setDefaults(x, y, w, h, 0);
        GL20.glUniform1f(uSoftness, 0.5f);
        applyColor(uColor, color);
        GL20.glUniform4f(uCorners, br, tr, bl, tl);
        render(x, y, w, h);
    }

    public static void fillRRCornersHard(float x, float y, float w, float h,
                                         float tl, float tr, float br, float bl, int argb) {
        fillRRCornersHard(x, y, w, h, tl, tr, br, bl, new Color(argb, true));
    }

    public static void strokeRR(float x, float y, float w, float h, float radius, float strokeWidth, Color fill, Color stroke) {
        if (!ensureShader()) return;
        GL20.glUseProgram(prog);
        setDefaults(x, y, w, h, radius);
        applyColor(uColor, fill);
        applyColor(uStrokeColor, stroke);
        GL20.glUniform1f(uStrokeWidth, strokeWidth);
        render(x, y, w, h);
    }

    public static void strokeRR(float x, float y, float w, float h, float radius, float strokeWidth, int fillARGB, int strokeARGB) {
        strokeRR(x, y, w, h, radius, strokeWidth, new Color(fillARGB, true), new Color(strokeARGB, true));
    }

    public static void outlineRR(float x, float y, float w, float h, float radius, float strokeWidth, Color stroke) {
        strokeRR(x, y, w, h, radius, strokeWidth, new Color(0, 0, 0, 0), stroke);
    }

    public static void outlineRR(float x, float y, float w, float h, float radius, float strokeWidth, int strokeARGB) {
        outlineRR(x, y, w, h, radius, strokeWidth, new Color(strokeARGB, true));
    }

    public static void fillRRGradientH(float x, float y, float w, float h, float radius, Color left, Color right) {
        if (!ensureShader()) return;
        GL20.glUseProgram(prog);
        setDefaults(x, y, w, h, radius);
        applyColor(uColor, left);
        applyColor(uGradientColor, right);
        GL20.glUniform1i(uGradientEnabled, 1);
        GL20.glUniform1i(uGradientHorizontal, 1);
        render(x, y, w, h);
    }

    public static void fillRRGradientH(float x, float y, float w, float h, float radius, int leftARGB, int rightARGB) {
        fillRRGradientH(x, y, w, h, radius, new Color(leftARGB, true), new Color(rightARGB, true));
    }

    public static void fillRRGradientV(float x, float y, float w, float h, float radius, Color top, Color bottom) {
        if (!ensureShader()) return;
        GL20.glUseProgram(prog);
        setDefaults(x, y, w, h, radius);
        applyColor(uColor, top);
        applyColor(uGradientColor, bottom);
        GL20.glUniform1i(uGradientEnabled, 1);
        GL20.glUniform1i(uGradientHorizontal, 0);
        render(x, y, w, h);
    }

    public static void fillRRGradientV(float x, float y, float w, float h, float radius, int topARGB, int bottomARGB) {
        fillRRGradientV(x, y, w, h, radius, new Color(topARGB, true), new Color(bottomARGB, true));
    }

    public static void fillRRCornersGradientV(float x, float y, float w, float h,
                                              float tl, float tr, float br, float bl,
                                              Color top, Color bottom) {
        if (!ensureShader()) return;
        GL20.glUseProgram(prog);
        setDefaults(x, y, w, h, 0);
        applyColor(uColor, top);
        applyColor(uGradientColor, bottom);
        GL20.glUniform1i(uGradientEnabled, 1);
        GL20.glUniform1i(uGradientHorizontal, 0);
        GL20.glUniform4f(uCorners, br, tr, bl, tl);
        render(x, y, w, h);
    }

    public static void fillRRCornersGradientV(float x, float y, float w, float h,
                                              float tl, float tr, float br, float bl,
                                              int topARGB, int bottomARGB) {
        fillRRCornersGradientV(x, y, w, h, tl, tr, br, bl,
                new Color(topARGB, true), new Color(bottomARGB, true));
    }

    public static void glowRR(float x, float y, float w, float h, float radius, float glowWidth, Color glowColor) {
        if (!ensureShader()) return;
        GL20.glUseProgram(prog);
        setDefaults(x, y, w, h, radius);
        applyColor(uColor, new Color(0, 0, 0, 0));
        applyColor(uGlowColor, glowColor);
        GL20.glUniform1f(uGlowWidth, glowWidth);
        GL20.glUniform1f(uSoftness, glowWidth);
        render(x - glowWidth, y - glowWidth, w + glowWidth * 2, h + glowWidth * 2);
    }

    public static void glowRR(float x, float y, float w, float h, float radius, float glowWidth, int argb) {
        glowRR(x, y, w, h, radius, glowWidth, new Color(argb, true));
    }

    public static void fillRect(float x, float y, float w, float h, Color color) {
        fillRR(x, y, w, h, 0, color);
    }

    public static void fillRect(float x, float y, float w, float h, int argb) {
        fillRR(x, y, w, h, 0, argb);
    }

    public static void fillGradientH(float x, float y, float w, float h, Color left, Color right) {
        fillRRGradientH(x, y, w, h, 0, left, right);
    }

    public static void fillGradientV(float x, float y, float w, float h, Color top, Color bottom) {
        fillRRGradientV(x, y, w, h, 0, top, bottom);
    }

    public static void fillCircle(float cx, float cy, float radius, Color color) {
        fillRR(cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    public static void fillCircle(float cx, float cy, float radius, int argb) {
        fillCircle(cx, cy, radius, new Color(argb, true));
    }

    public static void strokeCircle(float cx, float cy, float radius, float strokeWidth, Color fill, Color stroke) {
        strokeRR(cx - radius, cy - radius, radius * 2, radius * 2, radius, strokeWidth, fill, stroke);
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float thickness, Color color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = -dy / len * thickness * 0.5f;
        float ny =  dx / len * thickness * 0.5f;
        if (!ensureShader()) return;
        GL20.glUseProgram(prog);
        float minX = Math.min(x1, x2) - thickness;
        float minY = Math.min(y1, y2) - thickness;
        float maxX = Math.max(x1, x2) + thickness;
        float maxY = Math.max(y1, y2) + thickness;
        setDefaults(minX, minY, maxX - minX, maxY - minY, thickness / 2f);
        applyColor(uColor, color);
        render(minX, minY, maxX - minX, maxY - minY);
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float thickness, int argb) {
        drawLine(x1, y1, x2, y2, thickness, new Color(argb, true));
    }

    public static void blur(float x, float y, float w, float h, float strength) {
        BlurUtil.blurRegion(x, y, w, h, strength);
    }

    public static void blurRounded(float x, float y, float w, float h, float strength, int cornerRadius) {
        BlurUtil.blurRegionRounded(x, y, w, h, strength, cornerRadius);
    }

    public static void pushScissor(float x, float y, float w, float h) {
        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
                (int)(x * sf),
                (int)((sr.getScaledHeight() - y - h) * sf),
                (int)(w * sf),
                (int)(h * sf)
        );
    }

    public static void popScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void drawString(String text, float x, float y, Color color) {
        prepText();
        if (FontUtil.normal != null) FontUtil.normal.drawStringWithShadow(text, x, y, color.getRGB());
        else mc.fontRendererObj.drawStringWithShadow(text, (int)x, (int)y, color.getRGB());
    }

    public static void drawStringSmall(String text, float x, float y, Color color) {
        prepText();
        if (FontUtil.small != null) FontUtil.small.drawStringWithShadow(text, x, y, color.getRGB());
        else mc.fontRendererObj.drawStringWithShadow(text, (int)x, (int)y, color.getRGB());
    }

    public static void drawStringBold(String text, float x, float y, Color color) {
        prepText();
        if (FontUtil.bold != null) FontUtil.bold.drawStringWithShadow(text, x, y, color.getRGB());
        else mc.fontRendererObj.drawStringWithShadow(text, (int)x, (int)y, color.getRGB());
    }

    public static void drawString(String text, float x, float y, int argb) { drawString(text, x, y, new Color(argb, true)); }
    public static void drawStringSmall(String text, float x, float y, int argb) { drawStringSmall(text, x, y, new Color(argb, true)); }
    public static void drawStringBold(String text, float x, float y, int argb) { drawStringBold(text, x, y, new Color(argb, true)); }

    public static float strW(String t)      { return FontUtil.normal != null ? FontUtil.normal.getStringWidth(t) : mc.fontRendererObj.getStringWidth(t); }
    public static float strWSmall(String t) { return FontUtil.small  != null ? FontUtil.small.getStringWidth(t)  : mc.fontRendererObj.getStringWidth(t); }
    public static float strWBold(String t)  { return FontUtil.bold   != null ? FontUtil.bold.getStringWidth(t)   : mc.fontRendererObj.getStringWidth(t); }
    public static float fontH()      { return FontUtil.normal != null ? FontUtil.normal.getHeight() : mc.fontRendererObj.FONT_HEIGHT; }
    public static float fontHSmall() { return FontUtil.small  != null ? FontUtil.small.getHeight()  : mc.fontRendererObj.FONT_HEIGHT; }
    public static float fontHBold()  { return FontUtil.bold   != null ? FontUtil.bold.getHeight()   : mc.fontRendererObj.FONT_HEIGHT; }

    public static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
                (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }

    public static int lerpARGB(int a, int b, float t) {
        return lerp(new Color(a, true), new Color(b, true), t).getRGB();
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    public static Color withAlpha(Color c, float alpha) {
        return withAlpha(c, (int)(alpha * 255));
    }

    public static int withAlphaARGB(int argb, float alpha) {
        return withAlpha(new Color(argb, true), alpha).getRGB();
    }

    public static float smooth(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    public static float lerpFloat(float a, float b, float t) {
        return a + (b - a) * Math.max(0f, Math.min(1f, t));
    }

    private static void prepText() {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public static void destroy() {
        if (prog != -1) { GL20.glDeleteProgram(prog); prog = -1; }
        if (vbo  != -1) { GL15.glDeleteBuffers(vbo);  vbo  = -1; }
    }
}