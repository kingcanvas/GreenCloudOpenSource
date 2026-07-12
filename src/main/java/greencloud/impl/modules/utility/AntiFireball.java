package greencloud.impl.modules.utility;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.utils.TimerUtil;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AntiFireball extends Module {

    private final NumberSetting range = new NumberSetting("Range", this, 5.5, 2.0, 8.0, 0.1);
    private final NumberSetting speed = new NumberSetting("Aim Speed", this, 12.0, 1.0, 15.0, 0.5);
    private final NumberSetting cps   = new NumberSetting("CPS", this, 12.0, 1.0, 20.0, 1.0);

    private EntityFireball target;
    private final TimerUtil attackTimer = new TimerUtil();

    private float serverYaw, serverPitch;
    private float savedYaw, savedPitch, savedPrevYaw, savedPrevPitch;
    private boolean spoofing = false;
    private boolean rotInitialized = false;

    public AntiFireball() {
        super("AntiFireball", "Automatically hits fireballs back 360 degrees silently.", Category.COMBAT);
        addSettings(range, speed, cps);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        target = null;
        spoofing = false;
        rotInitialized = false;
        attackTimer.reset();
        if (mc.thePlayer != null) {
            serverYaw = mc.thePlayer.rotationYaw;
            serverPitch = mc.thePlayer.rotationPitch;
        }
    }

    @Override
    public void onDisable() {
        if (spoofing) restoreRotations();
        target = null;
        super.onDisable();
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (event.phase == TickEvent.Phase.START) {

            target = getBestFireball();

            if (target == null) {
                if (spoofing) restoreRotations();
                rotInitialized = false;

                serverYaw = lerpAngle(serverYaw, mc.thePlayer.rotationYaw, 0.2f);
                serverPitch = lerp(serverPitch, mc.thePlayer.rotationPitch, 0.2f);
                return;
            }

            if (!rotInitialized) {
                serverYaw = mc.thePlayer.rotationYaw;
                serverPitch = mc.thePlayer.rotationPitch;
                rotInitialized = true;
            }

            float[] rots = getRotations(target.posX, target.posY + (target.height / 2.0), target.posZ);

            float yawDiff = wrapDeg(rots[0] - serverYaw);
            float pitDiff = rots[1] - serverPitch;

            float speedVal = (float) speed.getValue();
            float fraction = 0.15f + (speedVal / 15.0f) * 0.3f;

            float moveYaw = yawDiff * fraction;
            float movePitch = pitDiff * fraction;

            float maxTurn = speedVal * 6.0f;
            moveYaw = MathHelper.clamp_float(moveYaw, -maxTurn, maxTurn);
            movePitch = MathHelper.clamp_float(movePitch, -maxTurn, maxTurn);

            serverYaw += applyGCD(moveYaw);
            serverPitch = MathHelper.clamp_float(serverPitch + applyGCD(movePitch), -90f, 90f);

            savedYaw = mc.thePlayer.rotationYaw;
            savedPitch = mc.thePlayer.rotationPitch;
            savedPrevYaw = mc.thePlayer.prevRotationYaw;
            savedPrevPitch = mc.thePlayer.prevRotationPitch;

            mc.thePlayer.rotationYaw = serverYaw;
            mc.thePlayer.rotationPitch = serverPitch;
            mc.thePlayer.prevRotationYaw = serverYaw;
            mc.thePlayer.prevRotationPitch = serverPitch;

            spoofing = true;

            long delay = (long) (1000.0 / cps.getValue());
            if (attackTimer.hasReached(delay)) {
                if (Math.abs(yawDiff) < 35 && Math.abs(pitDiff) < 35) {
                    mc.thePlayer.swingItem();
                    mc.playerController.attackEntity(mc.thePlayer, target);
                    attackTimer.reset();
                }
            }

        } else if (event.phase == TickEvent.Phase.END) {
            if (spoofing) {
                mc.thePlayer.rotationYawHead = serverYaw;
                mc.thePlayer.renderYawOffset = serverYaw;

                restoreRotations();
            }
        }
    }

    private EntityFireball getBestFireball() {
        EntityFireball best = null;
        double bestDist = Double.MAX_VALUE;
        double rSq = range.getValue() * range.getValue();

        for (net.minecraft.entity.Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityFireball) {
                EntityFireball fireball = (EntityFireball) entity;

                if (fireball.isDead) continue;

                double d = mc.thePlayer.getDistanceSqToEntity(fireball);

                if (d < rSq && d < bestDist && mc.thePlayer.canEntityBeSeen(fireball)) {
                    bestDist = d;
                    best = fireball;
                }
            }
        }
        return best;
    }

    private void restoreRotations() {
        mc.thePlayer.rotationYaw = savedYaw;
        mc.thePlayer.rotationPitch = savedPitch;
        mc.thePlayer.prevRotationYaw = savedPrevYaw;
        mc.thePlayer.prevRotationPitch = savedPrevPitch;
        spoofing = false;
    }

    private float[] getRotations(double x, double y, double z) {
        double dx = x - mc.thePlayer.posX;
        double dy = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = z - mc.thePlayer.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        return new float[]{
                (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f,
                (float) -Math.toDegrees(Math.atan2(dy, dist))
        };
    }

    private float applyGCD(float value) {
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 1.2F;
        return (gcd > 0) ? Math.round(value / gcd) * gcd : value;
    }

    private float wrapDeg(float d) {
        d %= 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    private float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private float lerpAngle(float from, float to, float t) {
        return from + wrapDeg(to - from) * t;
    }
}