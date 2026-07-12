package greencloud.impl.modules.combat;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.Random;

public class WTap extends Module {

    private final NumberSetting chance = new NumberSetting("Chance", this, 100, 0, 100, 5);
    private final NumberSetting releaseDelay = new NumberSetting("Release Delay (ms)", this, 120, 10, 500, 10);
    private final NumberSetting repressDelay = new NumberSetting("Re-press Delay (ms)", this, 500, 50, 1000, 50);

    private long lastWTapTime = 0;
    private long wTapStartTime = -1;
    private boolean isWTapping = false;
    private final Random random = new Random();

    public WTap() {
        super("WTap", "Automatically Sprint-Resets.", Category.COMBAT);
        this.addSettings(chance, releaseDelay, repressDelay);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!isToggled() || mc.thePlayer == null) return;
        if (!(event.target instanceof EntityLivingBase)) return;

        if (mc.thePlayer.isSprinting()) {

            if (System.currentTimeMillis() - lastWTapTime < repressDelay.getValue()) {
                return;
            }


            if (random.nextInt(100) > chance.getValue()) {
                return;
            }

            startWTap();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (!isToggled() || mc.thePlayer == null) return;

        if (isWTapping) {
            long timeSinceStart = System.currentTimeMillis() - wTapStartTime;

            if (timeSinceStart >= releaseDelay.getValue()) {
                finishWTap();
            } else {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            }
        }
    }

    private void startWTap() {
        isWTapping = true;
        wTapStartTime = System.currentTimeMillis();

        if (mc.gameSettings.keyBindForward.isKeyDown()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
        }
    }

    private void finishWTap() {
        isWTapping = false;
        lastWTapTime = System.currentTimeMillis();

        if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        isWTapping = false;
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
        }
    }
}