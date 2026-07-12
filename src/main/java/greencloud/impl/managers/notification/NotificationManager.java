package greencloud.impl.managers.notification;

import greencloud.GreenCloud;
import greencloud.impl.managers.notification.model.Notification;
import greencloud.impl.managers.notification.render.NotificationRenderer;
import greencloud.impl.modules.render.HUD;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {

    private static NotificationManager instance;
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();
    private final NotificationRenderer renderer = new NotificationRenderer();

    private NotificationManager() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static NotificationManager getInstance() {
        if (instance == null) instance = new NotificationManager();
        return instance;
    }

    public void addNotification(String title, String message, NotificationType type, int durationMs) {
        HUD hudModule = GreenCloud.moduleManager.getModule(HUD.class);
        if (hudModule == null || !hudModule.isToggled()) return;
        notifications.add(new Notification(title, message, type, durationMs));
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        renderer.render(notifications);
    }

    public enum NotificationType {
        INFO(new Color(100, 100, 100)),
        SUCCESS(new Color(46, 204, 113)),
        WARNING(new Color(241, 196, 15)),
        ERROR(new Color(231, 76, 60));

        public final Color color;
        NotificationType(Color color) { this.color = color; }
    }
}
