package greencloudclient.com.modules.impl.combat;

import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import greencloudclient.com.settings.NumberSetting;
import greencloudclient.com.settings.ModeSetting;
import greencloudclient.com.settings.BooleanSetting;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.Random;

public class AutoClicker extends Module {
    
    private final NumberSetting cps = new NumberSetting("CPS", this, 9.0, 14.0, 1.0, 20.0, 0.5, true);
    private final ModeSetting mode = new ModeSetting("Mode", this, "Normal", "Normal", "Extra", "Extra+");
    private final BooleanSetting onlyWeapon = new BooleanSetting("Only Weapon", this, true);
    
    private final Random random = new Random();
    private long nextDelay = 0L;
    
    private double delayAccumulator = 0.0;
    private long lastTickTime = 0L;
    
    private double speedFactor = 1.0;
    private int clicksInCurrentStreak = 0;
    private int streakTarget = 10;
    
    private java.lang.reflect.Field leftClickCounterField;
    private java.lang.reflect.Method clickMouseMethod;
    
    public AutoClicker() {
        super("AutoClicker", Category.COMBAT);
        addSettings(cps, mode, onlyWeapon);
        
        try {
            try {
                leftClickCounterField = net.minecraft.client.Minecraft.class.getDeclaredField("leftClickCounter");
            } catch (NoSuchFieldException e) {
                leftClickCounterField = net.minecraft.client.Minecraft.class.getDeclaredField("field_71429_W");
            }
            leftClickCounterField.setAccessible(true);
            
            try {
                clickMouseMethod = net.minecraft.client.Minecraft.class.getDeclaredMethod("clickMouse");
            } catch (NoSuchMethodException e) {
                clickMouseMethod = net.minecraft.client.Minecraft.class.getDeclaredMethod("func_147116_af");
            }
            clickMouseMethod.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        lastTickTime = System.currentTimeMillis();
        delayAccumulator = 0.0;
        clicksInCurrentStreak = 0;
        streakTarget = 5 + random.nextInt(10);
        nextDelay = calculateDelay();
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        
        long now = System.currentTimeMillis();
        long elapsed = now - lastTickTime;
        lastTickTime = now;
        
        if (elapsed > 200) elapsed = 50;
        
        if (!Mouse.isButtonDown(0)) {
            delayAccumulator = 0.0;
            return;
        }
        
        if (Mouse.isButtonDown(1)) {
            delayAccumulator = 0.0;
            return;
        }
        
        if (mc.playerController != null && mc.playerController.getIsHittingBlock()) return;
        if (onlyWeapon.enabled && !isHoldingWeapon()) return;
        
        delayAccumulator += elapsed;
        
        int clicksThisTick = 0;
        while (delayAccumulator >= nextDelay && clicksThisTick < 2) {
            click();
            clicksThisTick++;
            delayAccumulator -= nextDelay;
            nextDelay = calculateDelay();
        }
    }
    
    public long calculateDelay() {
        double min = cps.getValue();
        double max = cps.maxValue;
        double targetCPS = min + random.nextDouble() * (max - min);
        
        if (mode.is("Extra") || mode.is("Extra+")) {
            double outlierRoll = random.nextDouble();
            double outlierChance = mode.is("Extra+") ? 0.08 : 0.04;
            if (outlierRoll < outlierChance) {
                return 10 + random.nextInt(25);
            } else if (outlierRoll < outlierChance + 0.03) {
                return 220 + random.nextInt(150);
            }
            
            if (clicksInCurrentStreak >= streakTarget) {
                clicksInCurrentStreak = 0;
                streakTarget = 5 + random.nextInt(10);
                
                double roll = random.nextDouble();
                if (mode.is("Extra+")) {
                    if (roll < 0.30) {
                        speedFactor = 0.50 + random.nextDouble() * 0.30;
                    } else if (roll < 0.60) {
                        speedFactor = 1.10 + random.nextDouble() * 0.20;
                    } else {
                        speedFactor = 0.80 + random.nextDouble() * 0.40;
                    }
                } else {
                    if (roll < 0.20) {
                        speedFactor = 0.65 + random.nextDouble() * 0.20;
                    } else if (roll < 0.40) {
                        speedFactor = 1.05 + random.nextDouble() * 0.15;
                    } else {
                        speedFactor = 0.85 + random.nextDouble() * 0.20;
                    }
                }
            }
            
            clicksInCurrentStreak++;
            targetCPS *= speedFactor;
        }
        
        targetCPS = Math.max(2.0, Math.min(targetCPS, max + 3.0));
        
        long delay = (long) (1000.0 / targetCPS);
        
        double gaussianMod = mode.is("Extra+") ? 6.0 : (mode.is("Extra") ? 3.0 : 0.0);
        if (gaussianMod > 0) {
            delay += (long) (random.nextGaussian() * gaussianMod);
        }
        
        return Math.max(10, delay);
    }
    
    public void click() {
        try {
            if (leftClickCounterField != null) leftClickCounterField.set(mc, 0);
            if (clickMouseMethod != null) clickMouseMethod.invoke(mc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean isHoldingWeapon() {
        if (mc.thePlayer.getHeldItem() == null) return false;
        net.minecraft.item.Item item = mc.thePlayer.getHeldItem().getItem();
        return item instanceof ItemSword || item instanceof ItemAxe;
    }
}
