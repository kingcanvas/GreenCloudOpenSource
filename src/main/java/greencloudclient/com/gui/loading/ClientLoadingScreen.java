package greencloudclient.com.gui.loading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;

public final class ClientLoadingScreen {

    private static final String[][] DEVELOPERS = {
            {"Founder", "kingcanvas (_kingdev.)"},
            {"Founder", "SparkyEclipseXD"},
            {"Founder", "aslam.xyz"},
            {"Lead Developer", "clilys"},
            {"Developer", "qloha"},
            {"Developer", "ozzi.one (Criminal Cat)"}
    };

    private static final int IMAGE_SIZE = 1254;
    private static final int IMAGE_CROP = 220;
    private static final int IMAGE_CROP_SIZE = 814;
    private static ResourceLocation logoTexture;
    private static boolean logoLoadAttempted;
    private static float displayedProgress;
    private static long lastAnimationTime;
    private static boolean menuTransitionClaimed;

    private ClientLoadingScreen() {}

    public static void renderLoading(Minecraft mc, Framebuffer framebuffer, int progress, String title, String message) {
        ScaledResolution resolution = new ScaledResolution(mc);
        int scale = resolution.getScaleFactor();
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();

        if (OpenGlHelper.isFramebufferEnabled()) {
            framebuffer.setFramebufferColor(0.012f, 0.01f, 0.02f, 1.0f);
            framebuffer.framebufferClear();
        } else {
            GlStateManager.clear(256);
        }

        framebuffer.bindFramebuffer(false);
        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, resolution.getScaledWidth_double(), resolution.getScaledHeight_double(), 0.0D, 100.0D, 300.0D);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -200.0F);

        if (!OpenGlHelper.isFramebufferEnabled()) {
            GlStateManager.clear(16640);
        }

        float animatedProgress = updateProgress(progress);
        String status = message == null || message.trim().isEmpty() ? title : message;
        draw(width, height, mc, animatedProgress, status, 1.0f, false);

        framebuffer.unbindFramebuffer();
        if (OpenGlHelper.isFramebufferEnabled()) {
            framebuffer.framebufferRender(width * scale, height * scale);
        }
        mc.updateDisplay();
        Thread.yield();
    }

    public static synchronized boolean claimMenuTransition() {
        if (menuTransitionClaimed) return false;
        menuTransitionClaimed = true;
        return true;
    }

    public static void renderMenuTransition(Minecraft mc, int width, int height, float alpha) {
        draw(width, height, mc, 1.0f, "Ready", alpha, true);
    }

    private static float updateProgress(int progress) {
        long now = System.nanoTime();
        float target = progress < 0 ? displayedProgress : Math.max(0.0f, Math.min(1.0f, progress / 100.0f));
        if (target < displayedProgress - 0.2f) displayedProgress = target;
        float seconds = lastAnimationTime == 0L ? 0.1f : Math.min(0.25f, (now - lastAnimationTime) / 1_000_000_000.0f);
        lastAnimationTime = now;
        displayedProgress += (target - displayedProgress) * Math.min(1.0f, seconds * 10.0f);
        if (target >= 0.995f) displayedProgress = 1.0f;
        return displayedProgress;
    }

    private static void draw(int width, int height, Minecraft mc, float progress, String status, float alpha, boolean transition) {
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        if (transition && alpha < 4.0f / 255.0f) return;
        Gui.drawRect(0, 0, width, height, withAlpha(0x05040A, alpha));

        int leftWidth = Math.max(1, (int) (width * 0.58f));
        int logoSize = Math.max(72, Math.min((int) (height * 0.52f), (int) (width * 0.36f)));
        int logoX = (leftWidth - logoSize) / 2;
        int logoY = Math.max(12, (height - logoSize) / 2 - Math.max(8, height / 32));

        drawLogo(mc, logoX, logoY, logoSize, alpha);

        int barWidth = Math.max(100, Math.min(280, (int) (leftWidth * 0.68f)));
        int barX = (leftWidth - barWidth) / 2;
        int barY = Math.min(height - 34, logoY + logoSize + Math.max(8, height / 48));
        drawProgressBar(barX, barY, barWidth, Math.max(4, height / 100), progress, alpha);

        FontRenderer font = mc.fontRendererObj;
        if (font != null) {
            drawCredits(font, width, height, leftWidth, alpha);
        }

        int separatorX = leftWidth;
        int separatorTop = Math.max(18, height / 8);
        int separatorBottom = height - separatorTop;
        Gui.drawRect(separatorX, separatorTop, separatorX + 1, separatorBottom, withAlpha(0x4B315D, alpha * 0.7f));
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void drawLogo(Minecraft mc, int x, int y, int size, float alpha) {
        ensureLogoTexture(mc);
        if (logoTexture == null) return;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0f, 1.0f, 1.0f, alpha);
        mc.getTextureManager().bindTexture(logoTexture);
        Gui.drawScaledCustomSizeModalRect(x, y, IMAGE_CROP, IMAGE_CROP, IMAGE_CROP_SIZE, IMAGE_CROP_SIZE, size, size, IMAGE_SIZE, IMAGE_SIZE);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void ensureLogoTexture(Minecraft mc) {
        if (logoLoadAttempted) return;
        logoLoadAttempted = true;
        try (InputStream stream = ClientLoadingScreen.class.getResourceAsStream("/assets/greencloudclient/textures/gui/loading_logo.png")) {
            if (stream != null) {
                DynamicTexture texture = new DynamicTexture(TextureUtil.readBufferedImage(stream));
                logoTexture = mc.getTextureManager().getDynamicTextureLocation("greencloud_loading_logo", texture);
            }
        } catch (IOException ignored) {}
    }

    private static void drawProgressBar(int x, int y, int width, int height, float progress, float alpha) {
        Gui.drawRect(x - 2, y - 2, x + width + 2, y + height + 2, withAlpha(0x261B31, alpha));
        Gui.drawRect(x, y, x + width, y + height, withAlpha(0x100D16, alpha));
        int fill = Math.max(0, Math.min(width, Math.round(width * progress)));
        int segments = Math.min(fill, 72);
        for (int i = 0; i < segments; i++) {
            int start = x + fill * i / segments;
            int end = x + fill * (i + 1) / segments;
            float mix = segments <= 1 ? 1.0f : i / (float) (segments - 1);
            Gui.drawRect(start, y, Math.max(start + 1, end), y + height, withAlpha(interpolate(0x8658FF, 0xFF62D4, mix), alpha));
        }
    }

    private static void drawCredits(FontRenderer font, int width, int height, int leftWidth, float alpha) {
        int panelX = leftWidth + Math.max(14, (width - leftWidth) / 12);
        int lineHeight = Math.max(11, font.FONT_HEIGHT + 3);
        int totalHeight = (DEVELOPERS.length + 6) * lineHeight;
        int y = Math.max(14, (height - totalHeight) / 2);

        Gui.drawRect(panelX, y, panelX + Math.max(34, (width - leftWidth) / 8), y + 1, withAlpha(0x55D98A, alpha));
        y += lineHeight;
        font.drawStringWithShadow("Green", panelX, y, withAlpha(0x55D98A, alpha));
        font.drawStringWithShadow("Cloud", panelX + font.getStringWidth("Green"), y, withAlpha(0xECE8F2, alpha));
        y += lineHeight * 2;
        font.drawStringWithShadow("Client Developers", panelX, y, withAlpha(0x9B7BFF, alpha));
        y += lineHeight + 2;

        for (String[] developer : DEVELOPERS) {
            String role = developer[0];
            String name = " - " + developer[1];
            font.drawStringWithShadow(role, panelX, y, withAlpha(0xC6A8FF, alpha));
            font.drawStringWithShadow(name, panelX + font.getStringWidth(role), y, withAlpha(0xECE8F2, alpha));
            y += lineHeight;
        }

        y += Math.max(6, lineHeight / 2);
        font.drawStringWithShadow("Actively Developing the Client", panelX, y, withAlpha(0x9B7BFF, alpha));
        y += lineHeight + 2;
        font.drawStringWithShadow("kingcanvas (_kingdev.)", panelX, y, withAlpha(0xECE8F2, alpha));
    }

    private static void drawCentered(FontRenderer font, String text, int centerX, int y, int color) {
        font.drawStringWithShadow(text, centerX - font.getStringWidth(text) / 2.0f, y, color);
    }

    private static int interpolate(int start, int end, float amount) {
        int r = Math.round(((start >> 16) & 255) + (((end >> 16) & 255) - ((start >> 16) & 255)) * amount);
        int g = Math.round(((start >> 8) & 255) + (((end >> 8) & 255) - ((start >> 8) & 255)) * amount);
        int b = Math.round((start & 255) + ((end & 255) - (start & 255)) * amount);
        return (r << 16) | (g << 8) | b;
    }

    private static int withAlpha(int color, float alpha) {
        return Math.round(Math.max(0.0f, Math.min(1.0f, alpha)) * 255.0f) << 24 | color & 0xFFFFFF;
    }
}
