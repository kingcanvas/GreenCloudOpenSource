package greencloudclient.com.utils.font;

import greencloudclient.com.utils.AndroidUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomFontRenderer {

    private static final int PLAIN = 0, BOLD = 1, ITALIC = 2;
    private static final int PAGE_SIZE = 1024;
    private static final int GLYPH_PAD = 2;
    private static final float MIN_SCALE = 1f, MAX_SCALE = 8f;
    private static final String FORMAT_CODES = "0123456789abcdefklmnor";
    private static final int[] COLOR_CODES = new int[32];
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);

    private static ByteBuffer clearBuffer;
    private static int cachedDisplayW = -1, cachedDisplayH = -1, cachedGuiScale = -1, cachedScaleFactor = 1;

    static {
        for (int i = 0; i < 32; ++i) {
            int amplifier = (i >> 3 & 1) * 85;
            int red = (i >> 2 & 1) * 170 + amplifier;
            int green = (i >> 1 & 1) * 170 + amplifier;
            int blue = (i & 1) * 170 + amplifier;
            if (i == 6) red += 85;
            if (i >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }
            COLOR_CODES[i] = (red & 255) << 16 | (green & 255) << 8 | blue & 255;
        }
    }

    private final float size;
    private final float ascent;
    private final float lineHeight;
    private final float baselineOffset;
    private final Font[] fonts = new Font[4];
    private final Font[] fallbackFonts = new Font[4];
    private final float[][] asciiAdvances = new float[4][256];
    private final List<Map<Character, Float>> advances = new ArrayList<>();
    private final Map<Float, ScaleCache> caches = new HashMap<>();

    public static boolean isSupported() {
        return !AndroidUtil.isAndroid();
    }

    public CustomFontRenderer(Font font, float size) {
        this.size = size;
        Font base = font.deriveFont(Font.PLAIN, size);
        Font fallback = new Font(Font.SANS_SERIF, Font.PLAIN, 1).deriveFont(size);
        for (int style = 0; style < 4; style++) {
            fonts[style] = base.deriveFont(awtStyle(style));
            fallbackFonts[style] = fallback.deriveFont(awtStyle(style));
            java.util.Arrays.fill(asciiAdvances[style], Float.NaN);
            advances.add(new HashMap<>());
        }
        LineMetrics metrics = base.getLineMetrics("Ag", FRC);
        this.ascent = metrics.getAscent();
        this.lineHeight = metrics.getAscent() + metrics.getDescent();
        this.baselineOffset = size - lineHeight * 0.1f;
    }

    private static int awtStyle(int style) {
        return ((style & BOLD) != 0 ? Font.BOLD : 0) | ((style & ITALIC) != 0 ? Font.ITALIC : 0);
    }

    public float getWidth(String text) {
        if (text == null || text.isEmpty()) return 0f;
        float width = 0f, lineWidth = 0f;
        int style = PLAIN;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                int code = FORMAT_CODES.indexOf(Character.toLowerCase(text.charAt(++i)));
                if (code < 16 || code == 21) style = PLAIN;
                else if (code == 17) style |= BOLD;
                else if (code == 20) style |= ITALIC;
                continue;
            }
            if (c == '\n') {
                width = Math.max(width, lineWidth);
                lineWidth = 0f;
                continue;
            }
            lineWidth += advance(style, c);
        }
        return Math.max(width, lineWidth);
    }

    public int getStringWidth(String text) {
        return (int) Math.ceil(getWidth(text));
    }

    public float getFontHeight() {
        return lineHeight;
    }

    public int getHeight() {
        return (int) lineHeight;
    }

    public float getSize() {
        return size;
    }

    public String trimToWidth(String text, float maxWidth) {
        if (getWidth(text) <= maxWidth) return text;
        String ellipsis = "...";
        float budget = maxWidth - getWidth(ellipsis);
        int end = text.length();
        while (end > 0 && getWidth(text.substring(0, end)) > budget) end--;
        return text.substring(0, end) + ellipsis;
    }

    private float advance(int style, char c) {
        if (c == '\t') return advance(style, ' ') * 4f;
        if (c < 256) {
            float cached = asciiAdvances[style][c];
            if (!Float.isNaN(cached)) return cached;
            float adv = measure(style, c);
            asciiAdvances[style][c] = adv;
            return adv;
        }
        Map<Character, Float> map = advances.get(style);
        Float cached = map.get(c);
        if (cached != null) return cached;
        float adv = measure(style, c);
        map.put(c, adv);
        return adv;
    }

    private float measure(int style, char c) {
        Font font = fonts[style].canDisplay(c) ? fonts[style] : fallbackFonts[style];
        return (float) font.createGlyphVector(FRC, new char[]{c}).getGlyphMetrics(0).getAdvanceX();
    }

    public float drawString(String text, float x, float y, int color) {
        return drawString(text, x, y, color, false);
    }

    public float drawStringWithShadow(String text, float x, float y, int color) {
        return drawString(text, x, y, color, true);
    }

    public float drawCenteredString(String text, float x, float y, int color) {
        return drawString(text, x - getWidth(text) / 2f, y, color, false);
    }

    public float drawCenteredStringWithShadow(String text, float x, float y, int color) {
        return drawString(text, x - getWidth(text) / 2f, y, color, true);
    }

    public float drawString(String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return x;
        if ((color & 0xFC000000) == 0) color |= 0xFF000000;

        float scale = currentScale();
        ScaleCache cache = caches.computeIfAbsent(scale, ScaleCache::new);

        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        if (shadow) {
            float offset = Math.max(1f, Math.round(scale * 0.5f)) / scale;
            int shadowColor = (color & 0xFCFCFC) >> 2 | (color & 0xFF000000);
            render(cache, text, x + offset, y + offset, shadowColor, true);
        }
        float end = render(cache, text, x, y, color, false);

        if (!blend) GL11.glDisable(GL11.GL_BLEND);
        if (alpha) GL11.glEnable(GL11.GL_ALPHA_TEST);
        if (!texture) GL11.glDisable(GL11.GL_TEXTURE_2D);
        return end;
    }

    private float render(ScaleCache cache, String text, float x, float y, int color, boolean shadowPass) {
        final float scale = cache.scale;
        final int alphaBits = color & 0xFF000000;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        int current = color;
        int style = PLAIN;
        boolean underline = false, strikethrough = false;
        float startPx = x * scale;
        float penPx = startPx;
        float baselinePx = Math.round((y + baselineOffset) * scale);
        int boundPage = -1;
        boolean drawing = false;
        List<float[]> lines = null;
        List<Integer> lineColors = null;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '§' && i + 1 < text.length()) {
                int code = FORMAT_CODES.indexOf(Character.toLowerCase(text.charAt(++i)));
                if (code >= 0 && code < 16) {
                    style = PLAIN;
                    underline = strikethrough = false;
                    current = COLOR_CODES[shadowPass ? code + 16 : code] | alphaBits;
                } else if (code == 17) style |= BOLD;
                else if (code == 18) strikethrough = true;
                else if (code == 19) underline = true;
                else if (code == 20) style |= ITALIC;
                else if (code == 21) {
                    style = PLAIN;
                    underline = strikethrough = false;
                    current = color;
                }
                continue;
            }
            if (c == '\n') {
                penPx = startPx;
                baselinePx += Math.round(lineHeight * scale);
                continue;
            }

            float advancePx = advance(style, c) * scale;
            Glyph glyph = cache.glyph(style, c);
            if (glyph == null) {
                if (drawing) {
                    tessellator.draw();
                    drawing = false;
                }
                glyph = cache.rasterize(style, c);
                boundPage = -1;
            }

            if (glyph.width > 0) {
                if (glyph.page != boundPage) {
                    if (drawing) tessellator.draw();
                    GlStateManager.bindTexture(cache.pages.get(glyph.page).texture);
                    boundPage = glyph.page;
                    wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                    drawing = true;
                }
                float gx = (Math.round(penPx) + glyph.offsetX) / scale;
                float gy = (baselinePx + glyph.offsetY) / scale;
                quad(wr, gx, gy, gx + glyph.width / scale, gy + glyph.height / scale,
                        glyph.u0, glyph.v0, glyph.u1, glyph.v1, current);
            }

            if (underline || strikethrough) {
                if (lines == null) {
                    lines = new ArrayList<>();
                    lineColors = new ArrayList<>();
                }
                float thickness = Math.max(1f, Math.round(scale * size / 12f));
                float x1 = penPx / scale, x2 = (penPx + advancePx) / scale;
                if (underline) {
                    float ly = (baselinePx + Math.round(scale * size * 0.12f)) / scale;
                    lines.add(new float[]{x1, ly, x2, ly + thickness / scale});
                    lineColors.add(current);
                }
                if (strikethrough) {
                    float ly = (baselinePx - Math.round(ascent * scale * 0.3f)) / scale;
                    lines.add(new float[]{x1, ly, x2, ly + thickness / scale});
                    lineColors.add(current);
                }
            }

            penPx += advancePx;
        }
        if (drawing) tessellator.draw();

        if (lines != null) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            for (int i = 0; i < lines.size(); i++) {
                float[] l = lines.get(i);
                quad(wr, l[0], l[1], l[2], l[3], 0, 0, 0, 0, lineColors.get(i));
            }
            tessellator.draw();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
        return penPx / scale;
    }

    private static void quad(WorldRenderer wr, float x1, float y1, float x2, float y2,
                             float u0, float v0, float u1, float v1, int argb) {
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        wr.pos(x1, y1, 0).tex(u0, v0).color(r, g, b, a).endVertex();
        wr.pos(x1, y2, 0).tex(u0, v1).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, 0).tex(u1, v1).color(r, g, b, a).endVertex();
        wr.pos(x2, y1, 0).tex(u1, v0).color(r, g, b, a).endVertex();
    }

    private static float currentScale() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.displayWidth != cachedDisplayW || mc.displayHeight != cachedDisplayH || mc.gameSettings.guiScale != cachedGuiScale) {
            cachedDisplayW = mc.displayWidth;
            cachedDisplayH = mc.displayHeight;
            cachedGuiScale = mc.gameSettings.guiScale;
            cachedScaleFactor = new ScaledResolution(mc).getScaleFactor();
        }
        MATRIX.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
        float m0 = MATRIX.get(0), m1 = MATRIX.get(1);
        float scale = cachedScaleFactor * (float) Math.sqrt(m0 * m0 + m1 * m1);
        scale = Math.round(scale * 2f) / 2f;
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    public void destroy() {
        for (ScaleCache cache : caches.values()) {
            for (Page page : cache.pages) GL11.glDeleteTextures(page.texture);
        }
        caches.clear();
    }

    private static final class Glyph {
        int page;
        float u0, v0, u1, v1;
        int offsetX, offsetY, width, height;
    }

    private static final class Page {
        final int texture;
        int cursorX = 1, cursorY = 1, rowHeight = 0;

        Page() {
            texture = GL11.glGenTextures();
            GlStateManager.bindTexture(texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            if (clearBuffer == null) clearBuffer = BufferUtils.createByteBuffer(PAGE_SIZE * PAGE_SIZE * 4);
            clearBuffer.clear();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, PAGE_SIZE, PAGE_SIZE, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, clearBuffer);
        }

        boolean fits(int w, int h) {
            if (cursorX + w + 1 > PAGE_SIZE) {
                cursorX = 1;
                cursorY += rowHeight + 1;
                rowHeight = 0;
            }
            return cursorY + h + 1 <= PAGE_SIZE;
        }
    }

    private final class ScaleCache {
        final float scale;
        final Font[] scaledFonts = new Font[4];
        final Font[] scaledFallbacks = new Font[4];
        final List<Glyph[]> ascii = new ArrayList<>();
        final List<Map<Character, Glyph>> other = new ArrayList<>();
        final List<Page> pages = new ArrayList<>();

        ScaleCache(float scale) {
            this.scale = scale;
            for (int style = 0; style < 4; style++) {
                scaledFonts[style] = fonts[style].deriveFont(size * scale);
                scaledFallbacks[style] = fallbackFonts[style].deriveFont(size * scale);
                ascii.add(new Glyph[256]);
                other.add(new HashMap<>());
            }
        }

        Glyph glyph(int style, char c) {
            return c < 256 ? ascii.get(style)[c] : other.get(style).get(c);
        }

        Glyph rasterize(int style, char c) {
            Glyph glyph = new Glyph();
            Font font = scaledFonts[style].canDisplay(c) ? scaledFonts[style] : scaledFallbacks[style];
            GlyphVector vector = font.createGlyphVector(FRC, new char[]{c});
            Rectangle bounds = vector.getPixelBounds(FRC, 0, 0);

            if (!Character.isWhitespace(c) && bounds.width > 0 && bounds.height > 0) {
                int w = bounds.width + GLYPH_PAD * 2;
                int h = bounds.height + GLYPH_PAD * 2;
                BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g.setColor(java.awt.Color.WHITE);
                g.drawGlyphVector(vector, GLYPH_PAD - bounds.x, GLYPH_PAD - bounds.y);
                g.dispose();

                Page page = pages.isEmpty() ? null : pages.get(pages.size() - 1);
                if (page == null || !page.fits(w, h)) {
                    page = new Page();
                    pages.add(page);
                    page.fits(w, h);
                }

                int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
                ByteBuffer buffer = BufferUtils.createByteBuffer(w * h * 4);
                for (int pixel : pixels) {
                    buffer.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) (pixel >>> 24));
                }
                buffer.flip();
                GlStateManager.bindTexture(page.texture);
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, page.cursorX, page.cursorY, w, h,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

                glyph.page = pages.size() - 1;
                glyph.u0 = page.cursorX / (float) PAGE_SIZE;
                glyph.v0 = page.cursorY / (float) PAGE_SIZE;
                glyph.u1 = (page.cursorX + w) / (float) PAGE_SIZE;
                glyph.v1 = (page.cursorY + h) / (float) PAGE_SIZE;
                glyph.offsetX = bounds.x - GLYPH_PAD;
                glyph.offsetY = bounds.y - GLYPH_PAD;
                glyph.width = w;
                glyph.height = h;

                page.cursorX += w + 1;
                page.rowHeight = Math.max(page.rowHeight, h);
            }

            if (c < 256) ascii.get(style)[c] = glyph;
            else other.get(style).put(c, glyph);
            return glyph;
        }
    }
}
