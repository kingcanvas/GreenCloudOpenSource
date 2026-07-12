package greencloud.impl.managers.notification.render;

import greencloud.GreenCloud;
import greencloud.impl.managers.notification.NotificationManager;
import greencloud.impl.managers.notification.model.Notification;
import greencloud.impl.managers.notification.util.NotificationUtil;
import greencloud.impl.modules.render.HUD;
import greencloud.impl.utils.font.FontUtil;
import greencloud.impl.utils.render.RenderUtil;
import greencloud.impl.utils.render.GreenRender;
import greencloud.impl.utils.render.shaders.BlurUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class NotificationRenderer {

    private static final float WIDTH        = 160f;
    private static final float HEIGHT       = 32f;
    private static final float CORNER       = 6f;
    private static final float SLIDE_IN_MS  = 500f;
    private static final float GAP          = 6f;
    private static final float MARGIN       = 15f;
    private static final float SPRING_K     = 160f;
    private static final float SPRING_DAMP  = 22f;

    private final Minecraft mc = Minecraft.getMinecraft();
    private long lastFrameNanos = 0;

    public void render(List<Notification> notifications) {
        if (FontUtil.normal == null) return;

        notifications.removeIf(Notification::isExpired);
        if (notifications.isEmpty()) return;

        long now = System.nanoTime();
        float dt = lastFrameNanos == 0 ? 0.016f : (now - lastFrameNanos) / 1_000_000_000f;
        dt = NotificationUtil.clamp(dt, 0.001f, 0.05f);
        lastFrameNanos = now;

        ScaledResolution sr = new ScaledResolution(mc);
        float totalHeight = MARGIN;

        HUD hud = GreenCloud.moduleManager.getModule(HUD.class);
        int themeColor = hud != null ? hud.getHudColor() : new Color(180, 40, 40).getRGB();

        List<Notification> snapshot = new ArrayList<>(notifications);
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            Notification notif = snapshot.get(i);

            float targetY = sr.getScaledHeight() - totalHeight - HEIGHT;
            if (notif.currentY == -1) {
                notif.currentY = sr.getScaledHeight();
                notif.velocityY = 0f;
            }

            float accel = (targetY - notif.currentY) * SPRING_K - notif.velocityY * SPRING_DAMP;
            notif.velocityY += accel * dt;
            notif.currentY  += notif.velocityY * dt;
            float y = notif.currentY;

            float slideIn  = NotificationUtil.clamp(notif.getSlideInProgress(SLIDE_IN_MS), 0f, 1f);
            float slideOut = NotificationUtil.clamp(notif.getSlideOutRaw(), 0f, 1f);
            float slideAmount = slideOut > 0f
                    ? 1f - NotificationUtil.easeInCubic(slideOut)
                    : NotificationUtil.easeOutBack(slideIn);

            float slideOffset = (1f - slideAmount) * (WIDTH + 20f);
            if (slideOffset >= WIDTH + 20f) {
                totalHeight += HEIGHT + GAP;
                continue;
            }

            float momentumOffset = slideAmount > 0.98f
                    ? NotificationUtil.clamp(-notif.velocityY * 0.08f, 0f, 14f)
                    : 0f;

            float x = sr.getScaledWidth() - WIDTH - 10f + slideOffset + momentumOffset;
            float progress = NotificationUtil.clamp(notif.getAnimationProgress(), 0f, 1f);

            Color accentColor = notif.type == NotificationManager.NotificationType.INFO
                    ? new Color(themeColor) : notif.type.color;

            GlStateManager.pushMatrix();

            GreenRender.glowRR(x, y, WIDTH, HEIGHT, CORNER, 10f,
                    new Color(0, 0, 0, (int) (80 * progress)));

            BlurUtil.blurRegionRounded(x, y, WIDTH, HEIGHT, 14f, (int) CORNER);

            RenderUtil.drawRoundedRect(x, y, x + WIDTH, y + HEIGHT, CORNER,
                    new Color(15, 15, 15, (int) (210 * progress)).getRGB());

            float barWidth = WIDTH * notif.getTimeProgress();
            if (barWidth > 1f) {
                float barH = 2f;
                RenderUtil.drawRoundedRect(
                        x, y + HEIGHT - barH,
                        x + barWidth, y + HEIGHT,
                        barH / 2f,
                        RenderUtil.applyOpacity(accentColor.getRGB(), progress * 0.85f));
            }

            FontUtil.normal.drawStringWithShadow(notif.title, x + 6f, y + 5f,
                    RenderUtil.applyOpacity(-1, progress));

            String[] parts = NotificationUtil.parseMessageParts(notif.message);
            float currentX = x + 6f;
            for (String part : parts) {
                if (part.isEmpty()) continue;
                int textColor = NotificationUtil.shouldHighlight(part)
                        ? RenderUtil.applyOpacity(accentColor.getRGB(), progress)
                        : RenderUtil.applyOpacity(new Color(200, 200, 200).getRGB(), progress);
                if (currentX + FontUtil.small.getStringWidth(part) < x + WIDTH - 6f) {
                    FontUtil.small.drawString(part, currentX, y + 18f, textColor);
                    currentX += FontUtil.small.getStringWidth(part);
                }
            }

            GlStateManager.popMatrix();
            totalHeight += HEIGHT + GAP;
        }
    }
}
