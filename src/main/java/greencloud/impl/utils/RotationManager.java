package greencloud.impl.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class RotationManager {

    private static final RotationManager INSTANCE = new RotationManager();
    private final Minecraft mc = Minecraft.getMinecraft();

    private Rotation targetRotation = null;
    private Rotation currentRotation = null;
    private Rotation previousRotation = null;

    private float rotationSpeed = 15.0f;
    private boolean active = false;
    private int aimTicks = 0;

    private float savedRenderYawOffset, savedRotationYawHead, savedRotationPitch;
    private float savedPrevRenderYawOffset, savedPrevRotationYawHead, savedPrevRotationPitch;

    private float decelVelYaw = 0f;
    private float decelVelPitch = 0f;
    private float decelAccumYaw = 0f;
    private float decelAccumPitch = 0f;
    private boolean decelerating = false;
    private long decelLastMs = 0L;

    private RotationManager() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static RotationManager getInstance() {
        return INSTANCE;
    }

    public void setTargetEntity(Entity entity) {
        if (entity == null || mc.thePlayer == null) return;
        Rotation rotation = RotationUtil.getRotations(entity);
        if (rotation != null) setTargetRotation(rotation);
    }

    public void setTargetRotation(Rotation rotation) {
        if (rotation == null) return;
        if (currentRotation == null && mc.thePlayer != null) {
            currentRotation = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }
        this.targetRotation = rotation;
        this.active = true;
    }

    public void update() {
        if (!active || mc.thePlayer == null) {
            aimTicks = 0;
            return;
        }

        if (currentRotation == null) {
            currentRotation = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }

        previousRotation = currentRotation.copy();
        aimTicks++;

        if (targetRotation != null) {
            float gcd = RotationUtil.getGCD();

            float targetYaw = targetRotation.getYaw();
            float targetPitch = targetRotation.getPitch();
            float serverYaw = currentRotation.getYaw();
            float serverPitch = currentRotation.getPitch();

            float yawDiff = RotationUtil.wrapDeg(targetYaw - serverYaw);
            float pitchDiff = targetPitch - serverPitch;

            float maxPerTick = rotationSpeed * 8.0f;

            float easeIn = MathHelper.clamp_float((float) aimTicks / 4.0f, 0.15f, 1.0f);
            float distance = Math.max(Math.abs(yawDiff), Math.abs(pitchDiff));
            float easeOut = distance > 15.0f ? 1.0f : MathHelper.clamp_float(distance / 15.0f, 0.1f, 1.0f);

            float speedMultiplier = easeIn * easeOut;

            float moveYaw = MathHelper.clamp_float(yawDiff * speedMultiplier, -maxPerTick, maxPerTick);
            float movePitch = MathHelper.clamp_float(pitchDiff * speedMultiplier, -maxPerTick, maxPerTick);

            int dx = Math.round(moveYaw / gcd);
            int dy = Math.round(movePitch / gcd);

            if (dx == 0 && Math.abs(yawDiff) > gcd) dx = yawDiff > 0 ? 1 : -1;
            if (dy == 0 && Math.abs(pitchDiff) > gcd) dy = pitchDiff > 0 ? 1 : -1;

            serverYaw += dx * gcd;
            serverPitch = MathHelper.clamp_float(serverPitch + dy * gcd, -90f, 90f);

            currentRotation = new Rotation(serverYaw, serverPitch);

            if (Math.abs(yawDiff) < 1.0 && Math.abs(pitchDiff) < 1.0) {
                targetRotation = null;
            }
        }
    }

    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (active && currentRotation != null && event.entityPlayer == mc.thePlayer) {
            savedRenderYawOffset = event.entityPlayer.renderYawOffset;
            savedRotationYawHead = event.entityPlayer.rotationYawHead;
            savedRotationPitch = event.entityPlayer.rotationPitch;
            savedPrevRenderYawOffset = event.entityPlayer.prevRenderYawOffset;
            savedPrevRotationYawHead = event.entityPlayer.prevRotationYawHead;
            savedPrevRotationPitch = event.entityPlayer.prevRotationPitch;

            event.entityPlayer.renderYawOffset = currentRotation.getYaw();
            event.entityPlayer.rotationYawHead = currentRotation.getYaw();
            event.entityPlayer.rotationPitch = currentRotation.getPitch();

            if (previousRotation != null) {
                event.entityPlayer.prevRenderYawOffset = previousRotation.getYaw();
                event.entityPlayer.prevRotationYawHead = previousRotation.getYaw();
                event.entityPlayer.prevRotationPitch = previousRotation.getPitch();
            } else {
                event.entityPlayer.prevRenderYawOffset = currentRotation.getYaw();
                event.entityPlayer.prevRotationYawHead = currentRotation.getYaw();
                event.entityPlayer.prevRotationPitch = currentRotation.getPitch();
            }
        }
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (active && currentRotation != null && event.entityPlayer == mc.thePlayer) {
            event.entityPlayer.renderYawOffset = savedRenderYawOffset;
            event.entityPlayer.rotationYawHead = savedRotationYawHead;
            event.entityPlayer.rotationPitch = savedRotationPitch;
            event.entityPlayer.prevRenderYawOffset = savedPrevRenderYawOffset;
            event.entityPlayer.prevRotationYawHead = savedPrevRotationYawHead;
            event.entityPlayer.prevRotationPitch = savedPrevRotationPitch;
        }
    }

    public void beginDeceleration(float velYaw, float velPitch) {
        if (Math.abs(velYaw) < 0.01f && Math.abs(velPitch) < 0.01f) return;
        decelVelYaw = velYaw;
        decelVelPitch = velPitch;
        decelAccumYaw = 0f;
        decelAccumPitch = 0f;
        decelLastMs = 0L;
        decelerating = true;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!decelerating || mc.thePlayer == null) return;

        long now = System.currentTimeMillis();
        float deltaMs = (decelLastMs == 0L) ? 16.67f : Math.min((float)(now - decelLastMs), 50f);
        decelLastMs = now;
        float dt = deltaMs / 16.67f;

        float decay = (float) Math.pow(0.80, dt);
        decelVelYaw *= decay;
        decelVelPitch *= decay;

        if (Math.abs(decelVelYaw) < 0.003f && Math.abs(decelVelPitch) < 0.003f) {
            decelerating = false;
            decelVelYaw = decelVelPitch = 0f;
            decelAccumYaw = decelAccumPitch = 0f;
            return;
        }

        decelAccumYaw += decelVelYaw;
        decelAccumPitch += decelVelPitch;

        float yawStep = floorToGCD(decelAccumYaw);
        float pitchStep = floorToGCD(decelAccumPitch);

        decelAccumYaw -= yawStep;
        decelAccumPitch -= pitchStep;

        mc.thePlayer.rotationYaw += yawStep;
        mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + pitchStep, -90f, 90f);
    }

    private float floorToGCD(float value) {
        float sens = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float step = sens * sens * sens * 8.0F * 0.15F;
        float sign = value < 0 ? -1f : 1f;
        int steps = (int)(Math.abs(value) / step);
        return sign * steps * step;
    }

    public Rotation getCurrentRotation() { return currentRotation; }
    public boolean isActive() { return active && currentRotation != null; }

    public void reset() {
        this.targetRotation = null;
        this.currentRotation = null;
        this.previousRotation = null;
        this.active = false;
        this.aimTicks = 0;
    }

    public float getRotationSpeed() { return rotationSpeed; }
    public void setRotationSpeed(float speed) { this.rotationSpeed = Math.max(1.0f, speed); }
}