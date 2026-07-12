package greencloud.impl.modules.utility;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;

public class FastPlace extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", this, 0.0, 0.0, 4.0, 1.0);
    private final BooleanSetting blocksOnly = new BooleanSetting("Blocks Only", this, true);

    private Field rightClickDelayField;

    public FastPlace() {
        super("FastPlace", "Removes right click delay.", Category.UTILITY);
        addSettings(delay, blocksOnly);
        try {
            rightClickDelayField = net.minecraft.client.Minecraft.class.getDeclaredField("rightClickDelayTimer");
        } catch (NoSuchFieldException e1) {
            try {
                rightClickDelayField = net.minecraft.client.Minecraft.class.getDeclaredField("field_71467_ac");
            } catch (NoSuchFieldException e2) {
                e2.printStackTrace();
            }
        }
        if (rightClickDelayField != null) rightClickDelayField.setAccessible(true);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || event.phase != TickEvent.Phase.END) return;

        if (blocksOnly.enabled) {
            if (mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock)) {
                return;
            }
        }

        if (rightClickDelayField == null) return;

        try {
            if (mc.gameSettings.keyBindAttack.isKeyDown()) return;

            int currentDelay = rightClickDelayField.getInt(mc);
            int targetDelay = (int) delay.getValue();
            if (currentDelay > targetDelay) {
                rightClickDelayField.setInt(mc, targetDelay);
            }
        } catch (Exception ignored) {}
    }
}