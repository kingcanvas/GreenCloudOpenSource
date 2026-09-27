package greencloudclient.com.modules.impl.combat;

import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import greencloudclient.com.settings.NumberSetting;
import greencloudclient.com.settings.ModeSetting;
import greencloudclient.com.settings.BooleanSetting;
import greencloudclient.com.utils.player.ClickTimer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

public class AutoClicker extends Module {
    
    private final NumberSetting cps = new NumberSetting("CPS", this, 9.0, 14.0, 1.0, 20.0, 0.5, true);
    private final ModeSetting mode = new ModeSetting("Mode", this, "Normal", ClickTimer.MODES);
    private final BooleanSetting onlyWeapon = new BooleanSetting("Only Weapon", this, true);
    
    private long nextDelay = 0L;
    
    private double delayAccumulator = 0.0;
    private long lastTickTime = 0L;
    
    private final ClickTimer clickTimer = new ClickTimer();
    
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
        clickTimer.reset();
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
        return clickTimer.nextDelay(cps.getValue(), cps.maxValue, mode.currentMode);
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
