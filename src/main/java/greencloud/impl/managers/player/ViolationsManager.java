package greencloud.impl.managers.player;

import greencloud.impl.logger.Log;
import greencloud.impl.logger.Logger;
import greencloud.impl.managers.notification.NotificationManager;
import greencloud.impl.managers.notification.NotificationManager.NotificationType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ViolationsManager {

    private static final Logger log = Log.get(ViolationsManager.class);

    private static ViolationsManager instance;
    private final Minecraft mc = Minecraft.getMinecraft();

    private int violationCount = 0;
    private long lastViolationTime = 0;
    private boolean enabled = true;

    private double lastX, lastY, lastZ;
    private double lastDeltaX, lastDeltaY, lastDeltaZ;
    private boolean hasLastPos = false;

    private static final double SETBACK_THRESHOLD = 1.5;
    private static final long VIOLATION_COOLDOWN = 1000;

    private ViolationsManager() {
        log.info("ViolationsManager initializing");
        try {
            MinecraftForge.EVENT_BUS.register(this);
            log.debug("ViolationsManager registered on event bus");
        } catch (Exception e) {
            log.error("Failed to register ViolationsManager on event bus", e);
        }
    }

    public static ViolationsManager getInstance() {
        if (instance == null) {
            instance = new ViolationsManager();
        }
        return instance;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) {
            hasLastPos = false;
            return;
        }

        try {
            EntityPlayerSP player = mc.thePlayer;

            if (hasLastPos) {
                double deltaX = player.posX - lastX;
                double deltaY = player.posY - lastY;
                double deltaZ = player.posZ - lastZ;

                double currentSpeed = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
                double lastSpeed = Math.sqrt(lastDeltaX * lastDeltaX + lastDeltaY * lastDeltaY + lastDeltaZ * lastDeltaZ);

                if (currentSpeed > SETBACK_THRESHOLD && lastSpeed < currentSpeed * 0.5) {
                    if (!player.capabilities.isFlying && !(deltaY < -0.5 && player.fallDistance > 3)) {
                        onSetbackDetected(currentSpeed);
                    }
                }

                double directionChange = (deltaX * lastDeltaX) + (deltaY * lastDeltaY) + (deltaZ * lastDeltaZ);
                if (directionChange < -0.5 && currentSpeed > 1.0) {
                    onSetbackDetected(currentSpeed);
                }

                lastDeltaX = deltaX;
                lastDeltaY = deltaY;
                lastDeltaZ = deltaZ;
            }

            lastX = player.posX;
            lastY = player.posY;
            lastZ = player.posZ;
            hasLastPos = true;
        } catch (Exception e) {
            log.error("Exception in ViolationsManager tick", e);
        }
    }

    private void onSetbackDetected(double distance) {
        if (!enabled) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastViolationTime < VIOLATION_COOLDOWN) {
            return;
        }

        violationCount++;
        lastViolationTime = currentTime;

        String message = String.format("Setback detected %.1fm", distance);
        log.warn("Setback detected: " + String.format("%.1f", distance) + " blocks (total violations: " + violationCount + ")");

        NotificationManager.getInstance().addNotification(
                "Warning",
                message,
                NotificationType.WARNING,
                3000
        );
    }

    public void reset() {
        log.debug("ViolationsManager state reset");
        violationCount = 0;
        lastViolationTime = 0;
        hasLastPos = false;
        lastDeltaX = 0;
        lastDeltaY = 0;
        lastDeltaZ = 0;
    }

    public int getViolationCount() { return violationCount; }
    public long getLastViolationTime() { return lastViolationTime; }
    public boolean isEnabled() { return enabled; }

    public long getTimeSinceLastViolation() {
        if (lastViolationTime == 0) return -1;
        return System.currentTimeMillis() - lastViolationTime;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setViolationCount(int count) { this.violationCount = Math.max(0, count); }
    public void incrementViolationCount() { this.violationCount++; }
    public void decrementViolationCount() { this.violationCount = Math.max(0, this.violationCount - 1); }
}
