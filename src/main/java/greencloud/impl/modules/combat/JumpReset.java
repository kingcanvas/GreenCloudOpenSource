package greencloud.impl.modules.combat;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.NumberSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

public class JumpReset extends Module {

    private final NumberSetting chance   = new NumberSetting("Chance",   this, 100, 0, 100, 1);
    private final NumberSetting accuracy = new NumberSetting("Accuracy", this, 100, 0, 100, 1);

    private final Random random = new Random();
    private int jumpTimer = -1;

    public JumpReset() {
        super("JumpReset", "Jumps when hit to reduce knockback.", Category.COMBAT);
        addSettings(chance, accuracy);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        jumpTimer = -1;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        jumpTimer = -1;
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null) return;

        if (mc.thePlayer.isDead || mc.thePlayer.capabilities.isFlying || mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || mc.gameSettings.keyBindJump.isKeyDown()) {
            jumpTimer = -1;
            return;
        }

        if (mc.thePlayer.hurtTime == mc.thePlayer.maxHurtTime && mc.thePlayer.maxHurtTime > 0) {
            if (random.nextInt(100) < chance.getValue()) {
                int maxDelay = (int)((1.0 - (accuracy.getValue() / 100.0)) * 4.0);
                jumpTimer = maxDelay > 0 ? random.nextInt(maxDelay + 1) : 0;
            }
        }

        if (jumpTimer >= 0) {

            if (mc.thePlayer.hurtTime == 0) {
                jumpTimer = -1;
                return;
            }

            if (jumpTimer > 0) {
                jumpTimer--;
            } else {
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump();
                }
                jumpTimer = -1;
            }
        }
    }
}