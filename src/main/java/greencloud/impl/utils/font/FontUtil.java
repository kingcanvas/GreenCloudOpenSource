package greencloud.impl.utils.font;

import greencloud.impl.logger.Log;
import greencloud.impl.logger.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.awt.Font;
import java.io.InputStream;

public class FontUtil {

    private static final Logger log = Log.get(FontUtil.class);

    public static CustomFontRenderer normal;
    public static CustomFontRenderer bold;
    public static CustomFontRenderer large;
    public static CustomFontRenderer small;

    public static void bootstrap() {
        log.info("Loading custom fonts");
        normal = loadFont("Inter-Regular.ttf", 20f);
        bold = loadFont("Inter-Bold.ttf", 20f);
        large = loadFont("Inter-ExtraBold.ttf", 24f);
        small = loadFont("Segoe UI.ttf", 18f);
        log.info("Custom fonts loaded");
    }

    private static CustomFontRenderer loadFont(String location, float size) {
        try {
            log.debug("Loading font: " + location + " @ " + size + "pt");
            InputStream is = Minecraft.getMinecraft().getResourceManager()
                    .getResource(new ResourceLocation("greencloud", "fonts/" + location))
                    .getInputStream();
            Font font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
            log.debug("Font loaded: " + location);
            return new CustomFontRenderer(font, true, true);
        } catch (Exception e) {
            log.warn("Failed to load font '" + location + "', falling back to Arial: " + e.getMessage(), e);
            return new CustomFontRenderer(new Font("Arial", Font.PLAIN, (int) size), true, true);
        }
    }

    public static class SafeFont {
        private final CustomFontRenderer renderer;

        public SafeFont(CustomFontRenderer renderer) {
            this.renderer = renderer;
        }

        private net.minecraft.client.Minecraft mc() { return net.minecraft.client.Minecraft.getMinecraft(); }

        public void drawString(String text, float x, float y, int color) {
            if (renderer != null) renderer.drawString(text, x, y, color);
            else mc().fontRendererObj.drawString(text, (int) x, (int) y, color);
        }

        public void drawStringWithShadow(String text, float x, float y, int color) {
            if (renderer != null) renderer.drawStringWithShadow(text, x, y, color);
            else mc().fontRendererObj.drawStringWithShadow(text, (int) x, (int) y, color);
        }

        public void drawCenteredString(String text, float x, float y, int color) {
            if (renderer != null) renderer.drawString(text, x - renderer.getStringWidth(text) / 2f, y, color);
            else mc().fontRendererObj.drawString(text, (int) x - mc().fontRendererObj.getStringWidth(text) / 2, (int) y, color);
        }

        public void drawWrappedString(String text, float x, float y, float width, int color) {
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            float currY = y;
            for (String word : words) {
                if (getStringWidth(line + word) > width) {
                    drawString(line.toString(), x, currY, color);
                    line = new StringBuilder(word + " ");
                    currY += getHeight() + 2;
                } else {
                    line.append(word).append(" ");
                }
            }
            drawString(line.toString(), x, currY, color);
        }

        public int getStringWidth(String text) {
            if (renderer != null) return (int) Math.ceil(renderer.getStringWidth(text));
            return mc().fontRendererObj.getStringWidth(text);
        }

        public int getHeight() {
            if (renderer != null) return renderer.getHeight();
            return mc().fontRendererObj.FONT_HEIGHT;
        }
    }

    public static SafeFont getSafeNormal() { return new SafeFont(normal); }
    public static SafeFont getSafeLarge() { return new SafeFont(large); }
    public static SafeFont getSafeSmall() { return new SafeFont(small); }
    public static SafeFont getSafeBold() { return new SafeFont(bold); }
}
