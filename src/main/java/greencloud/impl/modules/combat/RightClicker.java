package greencloud.impl.modules.combat;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.*;
import greencloud.impl.utils.TimerUtil;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.Random;

public class RightClicker extends Module {

    public NumberSetting cps = new NumberSetting("CPS", this, 8.0, 12.0, 1.0, 20.0, 1.0, true);
    public NumberSetting blockPlaceDelay = new NumberSetting("Block Delay", this, 1.0, 0.0, 20.0, 1.0);
    public ModeSetting randomization = new ModeSetting("Randomization", this, "Normal", "Normal", "Extra");
    public BooleanSetting jitter = new BooleanSetting("Jitter", this, false);
    public NumberSetting jitterStrength = new NumberSetting("Jitter Strength", this, 0.5, 0.1, 3.0, 0.1, () -> this.jitter.enabled);

    private final TimerUtil timer = new TimerUtil();
    private final Random random = new Random();
    private long nextClickDelay;
    private int continuousClicks;
    private Field rightClickDelayTimerField;

    public RightClicker() {
        super("RightClicker", "Right Clicks.", Category.COMBAT);
        this.addSettings(cps, blockPlaceDelay, randomization, jitter, jitterStrength);
        try {
            rightClickDelayTimerField = mc.getClass().getDeclaredField("field_71467_ac");
        } catch (NoSuchFieldException e) {
            try {
                rightClickDelayTimerField = mc.getClass().getDeclaredField("rightClickDelayTimer");
            } catch (NoSuchFieldException ex) {
                throw new RuntimeException("Could not find rightClickDelayTimer field", ex);
            }
        }
        rightClickDelayTimerField.setAccessible(true);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        continuousClicks = 0;
        timer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        continuousClicks = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null || mc.currentScreen != null) return;
        if (event.phase != TickEvent.Phase.START) return;

        if (Mouse.isButtonDown(1)) {
            while (timer.hasReached(nextClickDelay)) {
                performClick();
                updateDelay();
                timer.subtract(nextClickDelay);
            }
        } else {
            nextClickDelay = 0;
            continuousClicks = 0;
            timer.reset();
        }
    }

    private void performClick() {
        int key = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(key, true);
        KeyBinding.onTick(key);
        KeyBinding.setKeyBindState(key, false);

        try {
            rightClickDelayTimerField.setInt(mc, 0);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        greencloud.impl.modules.render.CPSCounter.registerClick(true);

        if (jitter.enabled && jitterStrength.getValue() > 0) {
            float strength = (float) jitterStrength.getValue();
            mc.thePlayer.rotationYaw += (random.nextFloat() - 0.5f) * strength;
            mc.thePlayer.rotationPitch += (random.nextFloat() - 0.5f) * strength;
        }

        continuousClicks++;
    }

    private void updateDelay() {
        double min = cps.getValue();
        double max = cps.maxValue;
        double baseCPS = min + (random.nextDouble() * (max - min));

        if (randomization.is("Extra")) {
            double mean = (min + max) / 2.0;
            double stdDev = (max - min) / 4.0;
            baseCPS = mean + random.nextGaussian() * stdDev;
            baseCPS = MathHelper.clamp_double(baseCPS, min * 0.85, max * 1.15);

            double speed = Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ);
            if (speed > 0.15) baseCPS *= 1.0 + (random.nextDouble() * 0.1);

            if (continuousClicks > 10) {
                double fatigue = Math.min(continuousClicks - 10, 40) / 40.0 * 0.1;
                baseCPS *= 1.0 - fatigue;
            }

            if (random.nextDouble() < 0.03) baseCPS *= 1.2 + (random.nextDouble() * 0.3);
            if (random.nextDouble() < 0.05) baseCPS *= 0.7 + (random.nextDouble() * 0.2);

            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                baseCPS *= 0.85 + (random.nextDouble() * 0.1);
            }
        }

        baseCPS = MathHelper.clamp_double(baseCPS, 1, 20);
        nextClickDelay = (long) (1000.0 / baseCPS);

        if (blockPlaceDelay.getValue() > 0) {
            nextClickDelay += blockPlaceDelay.getValue();
        }

        nextClickDelay += (long) (random.nextGaussian() * 10);
        nextClickDelay = Math.max(nextClickDelay, 1);
    }
}
