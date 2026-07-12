package greencloud.impl.modules.movement;

import greencloud.GreenCloud;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.modules.movement.speed.*;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.Timer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Speed extends Module {

    private final ModeSetting mode;
    private final Map<String, ISubSetting> subSettings = new LinkedHashMap<>();

    public int tick;
    public int offGround;
    public int airTicks;

    private Field timerField;
    private Field speedInAirField;
    private Field isInWebField;

    public Speed() {
        super("Speed", "Moves faster than normal", Category.MOVEMENT);

        List<ISubSetting> modes = Arrays.asList(
            new NCPMode(this),
            new UNCPMode(this),
            new OldBlocksMCMode(this),
            new GrimMode(this),
            new VanillaMode(this),
            new VanillaHopMode(this),
            new LegitHopMode(this),
            new LegitAbuseMode(this),
            new MatrixMode(this),
            new TeleportAbuseMode(this),
            new TestMode(this),
            new VerusMode(this),
            new NoCheatMinusMode(this),
            new NCPRaceMode(this),
            new NCPLowMode(this),
            new GroundStrafeMode(this),
            new StrafeMode(this),
            new FixedStrafeMode(this),
            new IntaveStrafeMode(this),
            new VulcanYPortMode(this),
            new IntaveFastMode(this)
        );

        String[] modeNames = modes.stream().map(ISubSetting::getName).toArray(String[]::new);
        this.mode = new ModeSetting("Mode", this, modeNames[0], modeNames);
        addSetting(mode);

        for (ISubSetting sub : modes) {
            subSettings.put(sub.getName(), sub);
            for (Setting s : sub.getSettings()) {
                addSetting(s);
            }
        }

        try {
            timerField = ReflectionHelper.findField(Minecraft.class, "field_71428_T", "timer");
            timerField.setAccessible(true);
            speedInAirField = ReflectionHelper.findField(EntityPlayer.class, "field_71102_ce", "speedInAir");
            speedInAirField.setAccessible(true);
            isInWebField = ReflectionHelper.findField(Entity.class, "field_70134_J", "isInWeb");
            isInWebField.setAccessible(true);
        } catch (Exception e) {
            GreenCloud.logger.error("Speed reflection setup failed: " + e.getMessage());
        }
    }

    private ISubSetting activeMode() {
        return subSettings.get(mode.getMode());
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        if (mc.thePlayer == null) return;
        tick = 0;
        offGround = 0;
        airTicks = 0;
        setTimerSpeed(1.0f);
        setSpeedInAir(0.02f);
        ISubSetting active = activeMode();
        if (active != null) active.onEnable();
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        if (mc.thePlayer != null) {
            setSpeedInAir(0.02f);
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
        }
        setTimerSpeed(1.0f);
        ISubSetting active = activeMode();
        if (active != null) active.onDisable();
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.START || mc.thePlayer == null) return;
        airTicks = mc.thePlayer.onGround ? 0 : airTicks + 1;
        offGround = mc.thePlayer.onGround ? 0 : offGround + 1;
        tick++;
    }

    @SubscribeEvent
    public void onUpdate(net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent e) {
        if (e.entity != mc.thePlayer || mc.thePlayer == null) return;
        ISubSetting active = activeMode();
        if (active != null) active.onUpdate();
    }

    public void setTimerSpeed(float speed) {
        try {
            if (timerField != null) {
                Timer t = (Timer) timerField.get(mc);
                t.timerSpeed = speed;
            }
        } catch (Exception ignored) {}
    }

    public float getTimerSpeed() {
        try {
            if (timerField != null) {
                Timer t = (Timer) timerField.get(mc);
                return t.timerSpeed;
            }
        } catch (Exception ignored) {}
        return 1.0f;
    }

    public void setSpeedInAir(float speed) {
        try {
            if (speedInAirField != null) speedInAirField.setFloat(mc.thePlayer, speed);
        } catch (Exception ignored) {}
    }

    public boolean getIsInWeb() {
        try {
            if (isInWebField != null) return isInWebField.getBoolean(mc.thePlayer);
        } catch (Exception ignored) {}
        return false;
    }

    public void setMoveSpeed(double speed) {
        double forward = mc.thePlayer.movementInput.moveForward;
        double strafe = mc.thePlayer.movementInput.moveStrafe;
        float yaw = mc.thePlayer.rotationYaw;

        if (forward == 0 && strafe == 0) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
        } else {
            if (forward != 0) {
                if (strafe > 0) yaw += (forward > 0 ? -45 : 45);
                else if (strafe < 0) yaw += (forward > 0 ? 45 : -45);
                strafe = 0;
                if (forward > 0) forward = 1;
                else if (forward < 0) forward = -1;
            }
            double rad = Math.toRadians(yaw);
            mc.thePlayer.motionX = forward * speed * -Math.sin(rad) + strafe * speed * Math.cos(rad);
            mc.thePlayer.motionZ = forward * speed * Math.cos(rad) - strafe * speed * -Math.sin(rad);
        }
    }

    public double getCurrentSpeed() {
        return Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }

    public double getBaseMoveSpeed() {
        double baseSpeed = 0.2873;
        if (mc.thePlayer != null && mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            int amplifier = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            baseSpeed *= (1.0 + 0.2 * (amplifier + 1));
        }
        return baseSpeed;
    }

    public boolean isMoving() {
        return mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0;
    }

    public boolean canJump() {
        return isMoving() && mc.thePlayer.onGround;
    }

    public double getDirection() {
        float yaw = mc.thePlayer.rotationYaw;
        if (mc.thePlayer.moveForward < 0f) yaw += 180f;
        float forward = 1f;
        if (mc.thePlayer.moveForward < 0f) forward = -0.5f;
        else if (mc.thePlayer.moveForward > 0f) forward = 0.5f;
        if (mc.thePlayer.moveStrafing > 0f) yaw -= 90f * forward;
        if (mc.thePlayer.moveStrafing < 0f) yaw += 90f * forward;
        return Math.toRadians(yaw);
    }

    public boolean isOnGround(double height) {
        return !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0, -height, 0.0)).isEmpty();
    }
}
