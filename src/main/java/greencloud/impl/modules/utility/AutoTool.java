package greencloud.impl.modules.utility;

import greencloud.GreenCloud;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.settings.BooleanSetting;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;

@SuppressWarnings("all")
public class AutoTool extends Module {

    public ModeSetting mode = new ModeSetting("Mode", this, "Lite", "Lite");
    public NumberSetting delay = new NumberSetting("Swap Delay", this, 100, 0, 1000, 50);
    public BooleanSetting requireSneak = new BooleanSetting("Require Sneak", this, false);
    public ModeSetting filterMode = new ModeSetting("Filter Mode", this, "Off", "Off", "Blacklist");
    public BooleanSetting wool = new BooleanSetting("Wool", this, false, () -> filterMode.currentMode.equals("Blacklist"));
    public BooleanSetting wood = new BooleanSetting("Wood", this, false, () -> filterMode.currentMode.equals("Blacklist"));
    public BooleanSetting stone = new BooleanSetting("Stone", this, false, () -> filterMode.currentMode.equals("Blacklist"));
    public BooleanSetting enderchest = new BooleanSetting("Enderchest", this, true, () -> filterMode.currentMode.equals("Blacklist"));
    private int originalSlot = -1;
    private int bestSlot = -1;
    private boolean isMining = false;
    private long lastMineTime = 0;

    private Field curBlockDamageMP;

    public AutoTool() {
        super("AutoTool", "Switchs to the best tool to break the block.", Category.UTILITY);
        this.addSettings(mode, delay, requireSneak, filterMode, wool, wood, stone, enderchest);
        setupReflection();
    }

    private void setupReflection() {
        try {
            curBlockDamageMP = PlayerControllerMP.class.getDeclaredField("curBlockDamageMP");
            curBlockDamageMP.setAccessible(true);
        } catch (Exception e) {
            try {
                curBlockDamageMP = PlayerControllerMP.class.getDeclaredField("field_78770_f");
                curBlockDamageMP.setAccessible(true);
            } catch (Exception ex) {
                GreenCloud.logger.error("AutoTool HUD broken, reflection failed.");
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.theWorld == null) return;
        boolean attemptingToMine = mc.gameSettings.keyBindAttack.isKeyDown()
                && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK
                && (!requireSneak.enabled || mc.thePlayer.isSneaking());
        if (attemptingToMine) {
            lastMineTime = System.currentTimeMillis();
            BlockPos pos = mc.objectMouseOver.getBlockPos();
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            if (!isBlockAllowed(block)) return;
            bestSlot = getBestTool(block);
            if (bestSlot != -1 && bestSlot != mc.thePlayer.inventory.currentItem) {
                if (!isMining) {
                    originalSlot = mc.thePlayer.inventory.currentItem;
                    isMining = true;
                }
                if (mode.currentMode.equals("Lite")) {
                    mc.thePlayer.inventory.currentItem = bestSlot;
                }
            }
        } else {
            if (isMining) {
                if (System.currentTimeMillis() - lastMineTime >= delay.value) {
                    resetTool();
                }
            }
        }
    }

    private boolean isBlockAllowed(Block block) {
        if (filterMode.currentMode.equals("Off")) return true;
        if (wool.enabled && block == Blocks.wool) return false;
        if (wood.enabled && (block == Blocks.planks || block == Blocks.log || block == Blocks.log2 || block == Blocks.wooden_slab || block == Blocks.double_wooden_slab)) return false;
        if (stone.enabled && (block == Blocks.stone || block == Blocks.cobblestone || block == Blocks.mossy_cobblestone || block == Blocks.stonebrick)) return false;
        if (enderchest.enabled && block == Blocks.ender_chest) return false;
        return true;
    }

    private void resetTool() {
        if (mode.currentMode.equals("Lite")) {
            if (originalSlot != -1) {
                mc.thePlayer.inventory.currentItem = originalSlot;
            }
        }
        isMining = false;
        originalSlot = -1;
        bestSlot = -1;
    }

    private int getBestTool(Block block) {
        int bestSlot = -1;
        float maxStr = 1.0f;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack == null) continue;
            float str = stack.getStrVsBlock(block);
            if (str > maxStr) {
                maxStr = str;
                bestSlot = i;
            }
        }
        return bestSlot;
    }
}