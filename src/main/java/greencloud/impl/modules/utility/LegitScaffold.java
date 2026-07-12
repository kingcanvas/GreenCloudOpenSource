package greencloud.impl.modules.utility;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.utils.TimerUtil;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.ThreadLocalRandom;

public class LegitScaffold extends Module {

    private final NumberSetting minDelay;
    private final NumberSetting maxDelay;
    private final BooleanSetting onlyBlocks;
    public final BooleanSetting holdShift;

    private final TimerUtil shiftCooldown = new TimerUtil();

    private long currentDelay = 0;
    private boolean isOnSolidGround = true;

    public LegitScaffold() {
        super("Legit Scaffold", "Helps you bridge Legitly", Category.UTILITY);
        minDelay = new NumberSetting("Min Delay", this, 50, 0, 500, 10);
        maxDelay = new NumberSetting("Max Delay", this, 100, 0, 500, 10);
        onlyBlocks = new BooleanSetting("Only Blocks", this, true);
        holdShift = new BooleanSetting("Hold Shift", this, false);

        addSettings(minDelay, maxDelay, onlyBlocks, holdShift);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        shiftCooldown.reset();
        isOnSolidGround = true;
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            boolean physicalState = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), physicalState);
        }
        super.onDisable();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.theWorld == null) return;

        if (onlyBlocks.enabled && !isHoldingBlock()) {
            return;
        }

        boolean active = false;

        if (holdShift.enabled) {
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                active = true;
            }
        } else {
            if (mc.gameSettings.keyBindBack.isKeyDown()) {
                active = true;
            }
        }

        if (active) {
            BlockPos posUnder = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);

            if (mc.theWorld.isAirBlock(posUnder)) {
                if (isOnSolidGround) {
                    isOnSolidGround = false;
                    if (shiftCooldown.hasReached(currentDelay)) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
                    }
                } else {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
                }
            } else {
                if (!isOnSolidGround) {
                    isOnSolidGround = true;
                    shiftCooldown.reset();
                    updateDelay();
                }
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            }
        } else {
            if (!isOnSolidGround) {
                isOnSolidGround = true;
                shiftCooldown.reset();
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            }
        }
    }

    private void updateDelay() {
        double min = minDelay.getValue();
        double max = Math.max(min, maxDelay.getValue());

        if (min >= max) {
            this.currentDelay = (long) min;
        } else {
            this.currentDelay = ThreadLocalRandom.current().nextLong((long)min, (long)max);
        }
    }

    private boolean isHoldingBlock() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        return stack != null && stack.getItem() instanceof ItemBlock;
    }
}