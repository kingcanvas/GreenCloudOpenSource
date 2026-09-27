package greencloudclient.com.modules.impl.combat;

import greencloudclient.com.GreenCloud;
import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import greencloudclient.com.settings.BooleanSetting;
import greencloudclient.com.settings.ModeSetting;
import greencloudclient.com.settings.NumberSetting;
import greencloudclient.com.utils.player.ClickTimer;
import greencloudclient.com.utils.rotation.MovementCorrection;
import greencloudclient.com.utils.rotation.Noise;
import greencloudclient.com.utils.rotation.RotationEngine;
import greencloudclient.com.utils.rotation.TargetTracker;
import greencloudclient.com.utils.rotation.TrackingError;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

public class KillAura extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Natural", "Natural", "Linear");

    private final NumberSetting cps = new NumberSetting("CPS", this, 9.0, 13.0, 1.0, 20.0, 0.5, true);
    private final ModeSetting clickMode = new ModeSetting("Randomization", this, "Extra", ClickTimer.MODES);
    private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", this, 4.5, 6.0, 1.0, 10.0, 0.1, true);
    private final NumberSetting range = new NumberSetting("Range", this, 3.0, 1.0, 6.0, 0.05);
    private final NumberSetting aimRange = new NumberSetting("Aim Range", this, 4.5, 1.0, 8.0, 0.1);
    private final NumberSetting fov = new NumberSetting("FOV", this, 180.0, 30.0, 360.0, 10.0);
    private final BooleanSetting teams = new BooleanSetting("Teams", this, true);

    private final NumberSetting reactionTime = new NumberSetting("Reaction Time", this, 150, 0, 500, 10, () -> mode.is("Natural"));
    private final NumberSetting interpolation = new NumberSetting("Interpolation", this, 0.5, 0.0, 1.0, 0.05, () -> mode.is("Natural"));
    private final NumberSetting trackingError = new NumberSetting("Tracking Error", this, 0.6, 0.0, 1.0, 0.05, () -> mode.is("Natural"));
    private final NumberSetting overshoot = new NumberSetting("Overshoot", this, 0.5, 0.0, 1.0, 0.05, () -> mode.is("Natural"));
    private final NumberSetting jitter = new NumberSetting("Natural Jitter", this, 0.6, 0.0, 3.0, 0.05, () -> mode.is("Natural"));
    private final NumberSetting jitterSpeed = new NumberSetting("Jitter Speed", this, 1.5, 0.2, 5.0, 0.1,
            () -> mode.is("Natural") && jitter.getValue() > 0);

    private final RotationEngine rotator = new RotationEngine();
    private final RotationEngine.Profile profile = new RotationEngine.Profile();
    private final RotationEngine.Profile returnProfile = new RotationEngine.Profile();
    private final TargetTracker tracker = new TargetTracker();
    private final TrackingError aimError = new TrackingError();
    private final MovementCorrection correction = new MovementCorrection();
    private final Noise noise = new Noise();
    private final ClickTimer clickTimer = new ClickTimer();
    private final Random random = new Random();

    private EntityLivingBase target;
    private EntityLivingBase aimedTarget;
    private MovementInput originalKAInput;

    private boolean aiming, spoofing;
    private boolean disengaging;
    private int disengageTicks;
    private static final int MAX_DISENGAGE_TICKS = 40;
    private float prevYaw, prevPitch;
    private float lastCameraYaw, lastCameraPitch;
    private float savedYaw, savedPitch;
    private float sRYO, sRYH, sRP, sPRYO, sPRYH, sPRP;

    private long nextDelay;
    private double delayAccumulator;
    private long lastTickTime;

    private double aimHeight = 0.72, aimHeightTarget = 0.72;
    private int aimHeightTimer;

    public KillAura() {
        super("KillAura", Category.COMBAT);
        addSettings(mode, cps, clickMode, rotationSpeed, range, aimRange, fov, teams,
                reactionTime, interpolation, trackingError, overshoot, jitter, jitterSpeed);
    }

    @Override
    public String[] getBindAliases() {
        return new String[] {
                "KillAura", "Aura"
        };
    }

    @Override
    public void onEnable() {
        boolean resuming = disengaging;
        disengaging = false;
        if (!resuming) {
            super.onEnable();
            aiming = spoofing = false;
        }
        target = null;
        aimHeightTimer = 0;
        lastTickTime = System.currentTimeMillis();
        delayAccumulator = 0.0;
        clickTimer.reset();
        nextDelay = nextClickDelay();
        if (mc.thePlayer != null) {
            if (!resuming) {
                rotator.reset(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                tracker.clear();
                correction.reset();
            }
            lastCameraYaw = mc.thePlayer.rotationYaw;
            lastCameraPitch = mc.thePlayer.rotationPitch;

            MovementInput cur = mc.thePlayer.movementInput;
            originalKAInput = (cur instanceof KAMovementInput) ? ((KAMovementInput) cur).parent : cur;
            mc.thePlayer.movementInput = new KAMovementInput(originalKAInput);
        }
    }

    @Override
    public void onDisable() {
        target = null;
        if (aiming && mc.thePlayer != null) {
            disengaging = true;
            disengageTicks = 0;
            return;
        }
        finishDisable();
    }

    private void finishDisable() {
        disengaging = false;
        restoreRotation();
        aiming = false;
        if (mc.thePlayer != null && originalKAInput != null) {
            mc.thePlayer.movementInput = originalKAInput;
            originalKAInput = null;
        }
        target = null;
        super.onDisable();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            if (disengaging) finishDisable();
            return;
        }

        if (event.phase == TickEvent.Phase.END) {
            restoreRotation();
            if (disengaging && !aiming) finishDisable();
            return;
        }
        if (mc.currentScreen != null) return;
        if (disengaging && ++disengageTicks > MAX_DISENGAGE_TICKS) {
            finishDisable();
            return;
        }

        updateRotation();
        if (aiming) applySpoof();
        if (!disengaging) handleClicks();
    }

    private void updateRotation() {
        float realYaw = mc.thePlayer.rotationYaw;
        float realPitch = mc.thePlayer.rotationPitch;
        float cameraVelYaw = MathHelper.wrapAngleTo180_float(realYaw - lastCameraYaw);
        float cameraVelPitch = realPitch - lastCameraPitch;
        lastCameraYaw = realYaw;
        lastCameraPitch = realPitch;

        if (!aiming) rotator.reset(realYaw, realPitch);
        prevYaw = rotator.yaw;
        prevPitch = rotator.pitch;

        target = disengaging ? null : findTarget();
        boolean natural = mode.is("Natural");

        if (target != null) {
            tracker.track(target);
            if (natural) {
                if (tracker.isReacting((float) reactionTime.getValue())) return;
                if (!aiming || target != aimedTarget) {
                    aimError.reset();
                    rotator.overshootNext((float) overshoot.getValue());
                }
                if (!naturalStep(target)) return;
            } else {
                float[] want = linearAim(target);
                if (want == null) return;
                float min = degreesPerTick(rotationSpeed.getValue()), max = degreesPerTick(rotationSpeed.maxValue);
                rotator.stepLinear(want[0], want[1], min + random.nextFloat() * (max - min));
            }
            aiming = true;
            aimedTarget = target;
            return;
        }

        tracker.clear();
        aimedTarget = null;
        if (!aiming) return;

        float gcd = RotationEngine.gcd();
        if (rotator.distanceTo(realYaw, realPitch) < gcd * 1.5f) {
            aiming = false;
            return;
        }
        returnProfile.speedMin = returnProfile.speedMax = (float) Math.max(6.0, rotationSpeed.maxValue);
        returnProfile.reactionMs = 60f;
        returnProfile.smoothing = natural ? (float) interpolation.getValue() : 0.3f;
        returnProfile.error = 0f;
        returnProfile.tremor = 0f;
        rotator.stepHuman(realYaw, realPitch, cameraVelYaw, cameraVelPitch, gcd, 6f, returnProfile);
    }

    private static float degreesPerTick(double speed) {
        return (float) (3.0 * Math.pow(60.0, (speed - 1.0) / 9.0));
    }

    private boolean naturalStep(EntityLivingBase entity) {
        float error = (float) trackingError.getValue();
        double t = noise.seconds();

        float lead = (float) (1.0 - error * (0.55 + noise.fractal(t * 0.3, 503L) * 0.35));
        double[] seen = tracker.perceive((float) reactionTime.getValue(), lead);
        if (seen == null) return false;

        if (--aimHeightTimer <= 0) {
            aimHeightTarget = 0.5 + random.nextDouble() * 0.38;
            aimHeightTimer = 12 + random.nextInt(18);
        }
        aimHeight += (aimHeightTarget - aimHeight) * 0.12;

        double ax = seen[0], ay = seen[1] + entity.height * aimHeight, az = seen[2];
        float[] now = rotationsTo(ax, ay, az);
        if (now == null) return false;

        double selfVX = mc.thePlayer.posX - mc.thePlayer.lastTickPosX;
        double selfVZ = mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ;
        float[] next = rotationsTo(ax + seen[3] - selfVX, ay, az + seen[4] - selfVZ);
        float velYaw = next == null ? 0f : MathHelper.wrapAngleTo180_float(next[0] - now[0]);
        float velPitch = next == null ? 0f : next[1] - now[1];

        double dx = ax - mc.thePlayer.posX;
        double dy = ay - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = az - mc.thePlayer.posZ;
        double dist = Math.max(0.5, Math.sqrt(dx * dx + dy * dy + dz * dz));
        float size = (float) Math.toDegrees(2.0 * Math.atan2(entity.width * 0.5, dist));
        float tolerance = size * (0.08f + 0.35f * error);

        float lateral = (float) (Math.toRadians(velYaw) * dist);
        float selfSpeed = (float) Math.sqrt(selfVX * selfVX + selfVZ * selfVZ);
        float[] off = aimError.update(lateral, selfSpeed, (float) dist, entity.width, entity.height, error);

        profile.speedMin = (float) rotationSpeed.getValue();
        profile.speedMax = (float) rotationSpeed.maxValue;
        profile.reactionMs = (float) reactionTime.getValue();
        profile.smoothing = (float) interpolation.getValue();
        profile.error = error;
        profile.tremor = (float) jitter.getValue();
        profile.tremorSpeed = (float) jitterSpeed.getValue();

        rotator.stepHuman(now[0] + off[0], MathHelper.clamp_float(now[1] + off[1], -90f, 90f),
                velYaw, velPitch, tolerance, size, profile);
        return true;
    }

    private float[] linearAim(EntityLivingBase entity) {
        AxisAlignedBB bb = entity.getEntityBoundingBox();
        double eyeX = mc.thePlayer.posX;
        double eyeY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        double eyeZ = mc.thePlayer.posZ;
        double inset = 0.05;
        double x = MathHelper.clamp_double(eyeX, bb.minX + inset, bb.maxX - inset);
        double y = MathHelper.clamp_double(eyeY, bb.minY + inset, bb.maxY - inset);
        double z = MathHelper.clamp_double(eyeZ, bb.minZ + inset, bb.maxZ - inset);
        return rotationsTo(x, y, z);
    }

    private float[] rotationsTo(double x, double y, double z) {
        double dx = x - mc.thePlayer.posX;
        double dy = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = z - mc.thePlayer.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 1.0E-4) return null;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new float[]{yaw, pitch};
    }

    private void applySpoof() {
        if (!spoofing) {
            savedYaw = mc.thePlayer.rotationYaw;
            savedPitch = mc.thePlayer.rotationPitch;
        }
        mc.thePlayer.rotationYaw = rotator.yaw;
        mc.thePlayer.rotationPitch = rotator.pitch;
        mc.thePlayer.prevRotationYaw = prevYaw;
        mc.thePlayer.prevRotationPitch = prevPitch;
        spoofing = true;
    }

    private void restoreRotation() {
        if (!spoofing || mc.thePlayer == null) return;
        mc.thePlayer.rotationYaw = savedYaw;
        mc.thePlayer.rotationPitch = savedPitch;
        mc.thePlayer.prevRotationYaw = savedYaw;
        mc.thePlayer.prevRotationPitch = savedPitch;
        spoofing = false;
    }

    private void handleClicks() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastTickTime;
        lastTickTime = now;
        if (elapsed > 200) elapsed = 50;
        delayAccumulator += elapsed;

        int clicksThisTick = 0;
        while (delayAccumulator >= nextDelay && clicksThisTick < 2) {
            if (aiming && target != null && isLookingAtTarget()) {
                mc.objectMouseOver = new MovingObjectPosition(target);
                AutoClicker ac = getAutoClicker();
                if (ac != null) ac.click();
                clicksThisTick++;
            }
            delayAccumulator -= nextDelay;
            nextDelay = nextClickDelay();
        }
    }

    private boolean isLookingAtTarget() {
        double reach = range.getValue();
        if (mc.thePlayer.getDistanceToEntity(target) > reach + 1.0) return false;

        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        float cosYaw = MathHelper.cos(-rotator.yaw * 0.017453292F - (float) Math.PI);
        float sinYaw = MathHelper.sin(-rotator.yaw * 0.017453292F - (float) Math.PI);
        float cosPitch = -MathHelper.cos(-rotator.pitch * 0.017453292F);
        float sinPitch = MathHelper.sin(-rotator.pitch * 0.017453292F);
        Vec3 end = eyes.addVector(sinYaw * cosPitch * reach, sinPitch * reach, cosYaw * cosPitch * reach);

        float border = target.getCollisionBorderSize();
        AxisAlignedBB aabb = target.getEntityBoundingBox().expand(border, border, border);
        return aabb.isVecInside(eyes) || aabb.calculateIntercept(eyes, end) != null;
    }

    private long nextClickDelay() {
        return clickTimer.nextDelay(cps.getValue(), cps.maxValue, clickMode.currentMode);
    }

    private AutoClicker getAutoClicker() {
        return GreenCloud.moduleManager == null ? null
                : (AutoClicker) GreenCloud.moduleManager.getModule(AutoClicker.class);
    }

    private EntityLivingBase findTarget() {
        double maxDist = Math.max(range.getValue(), aimRange.getValue());

        if (target != null && isValidTarget(target, maxDist)) return target;

        EntityLivingBase best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) continue;
            EntityLivingBase living = (EntityLivingBase) entity;
            if (!isValidTarget(living, maxDist)) continue;
            double d = mc.thePlayer.getDistanceToEntity(living);
            if (d < bestDist) {
                bestDist = d;
                best = living;
            }
        }
        return best;
    }

    private boolean isValidTarget(EntityLivingBase entity, double maxDist) {
        if (entity == mc.thePlayer || !(entity instanceof EntityPlayer)) return false;
        if (entity.isDead || entity.getHealth() <= 0) return false;
        if (teams.enabled && isTeam((EntityPlayer) entity)) return false;
        if (mc.thePlayer.getDistanceToEntity(entity) > maxDist) return false;
        return fov.getValue() >= 360 || getAngleToEntity(entity) <= fov.getValue() / 2.0;
    }

    private boolean isTeam(EntityPlayer entity) {
        String targetName = entity.getDisplayName().getFormattedText().replace("§r", "");
        String clientName = mc.thePlayer.getDisplayName().getFormattedText().replace("§r", "");
        if (targetName.startsWith("§") && clientName.startsWith("§")) {
            return targetName.charAt(1) == clientName.charAt(1);
        }
        return false;
    }

    private double getAngleToEntity(Entity entity) {
        double dx = entity.posX - mc.thePlayer.posX;
        double dz = entity.posZ - mc.thePlayer.posZ;
        double yawToE = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        float cameraYaw = spoofing ? savedYaw : mc.thePlayer.rotationYaw;
        return Math.abs(MathHelper.wrapAngleTo180_double(yawToE - cameraYaw));
    }

    public EntityLivingBase getTarget() { return target; }

    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!aiming || event.entityPlayer != mc.thePlayer) return;
        EntityPlayer p = event.entityPlayer;
        sRYO = p.renderYawOffset;
        sRYH = p.rotationYawHead;
        sRP = p.rotationPitch;
        sPRYO = p.prevRenderYawOffset;
        sPRYH = p.prevRotationYawHead;
        sPRP = p.prevRotationPitch;

        p.renderYawOffset = rotator.yaw;
        p.rotationYawHead = rotator.yaw;
        p.rotationPitch = rotator.pitch;
        p.prevRenderYawOffset = prevYaw;
        p.prevRotationYawHead = prevYaw;
        p.prevRotationPitch = prevPitch;
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (!aiming || event.entityPlayer != mc.thePlayer) return;
        EntityPlayer p = event.entityPlayer;
        p.renderYawOffset = sRYO;
        p.rotationYawHead = sRYH;
        p.rotationPitch = sRP;
        p.prevRenderYawOffset = sPRYO;
        p.prevRotationYawHead = sPRYH;
        p.prevRotationPitch = sPRP;
    }

    private class KAMovementInput extends MovementInput {
        final MovementInput parent;
        KAMovementInput(MovementInput parent) { this.parent = parent; }

        @Override
        public void updatePlayerMoveState() {
            parent.updatePlayerMoveState();
            this.jump  = parent.jump;
            this.sneak = parent.sneak;
            this.moveForward = parent.moveForward;
            this.moveStrafe  = parent.moveStrafe;

            if (!spoofing) {
                correction.reset();
                return;
            }

            correction.apply(this, rotator.yaw, savedYaw);
        }
    }
}
