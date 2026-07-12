package greencloud.impl.modules.combat;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

public class BowAssist extends Module {

    public NumberSetting fov = new NumberSetting("FOV", this, 90, 1, 180, 1);
    public NumberSetting speed = new NumberSetting("Speed", this, 3, 6, 1, 20, 0.5, true);
    public NumberSetting jitter = new NumberSetting("Jitter", this, 0, 0, 3, 0.1);
    public BooleanSetting predict = new BooleanSetting("Predict", this, true);
    public BooleanSetting smooth = new BooleanSetting("Smooth", this, true);

    private EntityLivingBase target;
    private final Random random = new Random();

    private float serverYaw, serverPitch;
    private float savedYaw, savedPitch, savedPrevYaw, savedPrevPitch;
    private boolean spoofing = false;
    private boolean rotInitialized = false;

    public BowAssist() {
        super("BowAssist", "Silently aims at players when using a bow.", Category.COMBAT);
        addSettings(fov, speed, jitter, smooth, predict);
    }

    @Override
    public void onDisable() {
        if (spoofing) restoreRotations();
        target = null;
        super.onDisable();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        if (event.phase == TickEvent.Phase.START) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held == null || !(held.getItem() instanceof ItemBow) || !mc.gameSettings.keyBindUseItem.isKeyDown()) {
                if (spoofing) restoreRotations();
                rotInitialized = false;
                target = null;
                return;
            }

            target = getBestTarget();

            if (target == null) {
                if (spoofing) restoreRotations();
                rotInitialized = false;
                return;
            }

            if (!rotInitialized) {
                serverYaw = mc.thePlayer.rotationYaw;
                serverPitch = mc.thePlayer.rotationPitch;
                rotInitialized = true;
            }

            aimAtTarget();

        } else if (event.phase == TickEvent.Phase.END && spoofing) {
            restoreRotations();
        }
    }

    private void aimAtTarget() {
        double posX = target.posX - mc.thePlayer.posX;
        double posZ = target.posZ - mc.thePlayer.posZ;
        double posY = (target.posY + target.getEyeHeight()) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());

        if (predict.enabled) {
            double distance = MathHelper.sqrt_double(posX * posX + posZ * posZ);
            double timeToHit = distance / 3.0;
            posX += (target.posX - target.prevPosX) * timeToHit;
            posZ += (target.posZ - target.prevPosZ) * timeToHit;
            posY += 0.5 * 0.05 * (timeToHit * timeToHit);
        }

        double hDist = MathHelper.sqrt_double(posX * posX + posZ * posZ);
        float tYaw = (float)(Math.atan2(posZ, posX) * 180.0D / Math.PI) - 90.0F;
        float tPitch = (float)-(Math.atan2(posY, hDist) * 180.0D / Math.PI);

        if (jitter.getValue() > 0) {
            float jitterVal = (float) jitter.getValue();
            tYaw   += (random.nextFloat() - 0.5f) * jitterVal;
            tPitch += (random.nextFloat() - 0.5f) * jitterVal;
        }

        if (smooth.enabled) {
            double spd = speed.getValue() + random.nextDouble() * (speed.maxValue - speed.getValue());
            float deltaYaw = MathHelper.wrapAngleTo180_float(tYaw - serverYaw);
            float deltaPitch = MathHelper.wrapAngleTo180_float(tPitch - serverPitch);

            deltaYaw = MathHelper.clamp_float(deltaYaw, (float)-spd, (float)spd);
            deltaPitch = MathHelper.clamp_float(deltaPitch, (float)-spd, (float)spd);

            serverYaw += deltaYaw;
            serverPitch += deltaPitch;
        } else {
            serverYaw = tYaw;
            serverPitch = tPitch;
        }

        serverPitch = MathHelper.clamp_float(serverPitch, -90f, 90f);

        savedYaw = mc.thePlayer.rotationYaw;
        savedPitch = mc.thePlayer.rotationPitch;
        savedPrevYaw = mc.thePlayer.prevRotationYaw;
        savedPrevPitch = mc.thePlayer.prevRotationPitch;

        mc.thePlayer.rotationYaw = serverYaw;
        mc.thePlayer.rotationPitch = serverPitch;

        spoofing = true;
    }

    private void restoreRotations() {
        mc.thePlayer.rotationYaw = savedYaw;
        mc.thePlayer.rotationPitch = savedPitch;
        mc.thePlayer.prevRotationYaw = savedPrevYaw;
        mc.thePlayer.prevRotationPitch = savedPrevPitch;
        spoofing = false;
    }

    private EntityLivingBase getBestTarget() {
        EntityLivingBase bestTarget = null;
        double bestDist = 1000;

        for (Entity e : mc.theWorld.loadedEntityList) {
            if (!(e instanceof EntityPlayer) || e == mc.thePlayer
                    || e.isInvisible() || e instanceof EntityArmorStand) continue;

            EntityPlayer p = (EntityPlayer) e;
            if (p.getHealth() <= 0 || p.isDead) continue;
            if (!isInFOV(p, fov.getValue())) continue;

            double dist = mc.thePlayer.getDistanceToEntity(p);
            if (dist < bestDist) {
                bestDist = dist;
                bestTarget = p;
            }
        }
        return bestTarget;
    }

    private boolean isInFOV(Entity e, double fov) {
        return getAngle(e) < fov;
    }

    private float getAngle(Entity e) {
        double x = e.posX - mc.thePlayer.posX;
        double z = e.posZ - mc.thePlayer.posZ;
        float yaw = (float)(Math.atan2(z, x) * 180.0D / Math.PI) - 90.0F;
        return Math.abs(MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw));
    }
}