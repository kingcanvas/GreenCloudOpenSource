package greencloud.impl.modules.utility;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

public class MLG extends Module {

    public NumberSetting minFallDist = new NumberSetting("Min Fall Dist", this, 4.0, 3.0, 20.0, 0.5);
    public NumberSetting aimSpeed = new NumberSetting("Aim Speed", this, 10, 1, 10, 1);
    public NumberSetting aimHeight = new NumberSetting("Aim Height", this, 3.0, 1.0, 10.0, 0.5);
    public NumberSetting successRate = new NumberSetting("Success Rate", this, 100, 1, 100, 1);

    private boolean hasRolledChance = false;
    private boolean chancePassed = false;
    private boolean placed = false;
    private boolean rotInit = false;
    private float serverPitch = 0f;
    private final Random random = new Random();

    private float savedPitch, savedPrevPitch;
    private boolean spoofing = false;

    public MLG() {
        super("MLG", "Uses water bucket/cobweb to prevent fall damage.", Category.UTILITY);
        addSettings(minFallDist, aimSpeed, aimHeight, successRate);
    }

    @Override
    public void onEnable() {
        resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (spoofing) restoreRotations();
        resetState();
        super.onDisable();
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (event.phase == TickEvent.Phase.START) {
            handleMLG();
        } else if (event.phase == TickEvent.Phase.END) {
            if (spoofing) {
                restoreRotations();
            }
        }
    }

    private void handleMLG() {
        if (mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying || mc.thePlayer.isInWater()) {
            resetState();
            return;
        }

        if (mc.thePlayer.motionY >= 0) return;
        if (mc.thePlayer.fallDistance <= minFallDist.getValue()) return;

        if (!hasRolledChance) {
            chancePassed = (random.nextInt(100) + 1) <= successRate.getValue();
            hasRolledChance = true;
        }

        if (!chancePassed) return;

        int mlgSlot = getMLGItemSlot();
        if (mlgSlot == -1) return;

        double dist = getDistanceToGround();
        boolean isVoid = (dist == -1);

        if (!rotInit) {
            serverPitch = mc.thePlayer.rotationPitch;
            rotInit = true;
        }

        float targetPitch = 90f;
        float speed = (float) aimSpeed.getValue() * 9.0f;
        float delta = targetPitch - serverPitch;

        if (Math.abs(delta) > speed) {
            serverPitch += Math.signum(delta) * speed;
        } else {
            serverPitch = targetPitch;
        }

        savedPitch = mc.thePlayer.rotationPitch;
        savedPrevPitch = mc.thePlayer.prevRotationPitch;

        mc.thePlayer.rotationPitch = serverPitch;
        mc.thePlayer.prevRotationPitch = serverPitch;
        spoofing = true;

        if (!placed && serverPitch >= 85f && (isVoid || (dist <= aimHeight.getValue() && dist + mc.thePlayer.getEyeHeight() <= 4.5))) {
            mc.thePlayer.inventory.currentItem = mlgSlot;
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(mlgSlot);
            if (stack == null) return;

            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, stack);
            mc.thePlayer.swingItem();
            placed = true;
        }
    }

    private void restoreRotations() {
        mc.thePlayer.rotationPitch = savedPitch;
        mc.thePlayer.prevRotationPitch = savedPrevPitch;
        spoofing = false;
    }

    private void resetState() {
        hasRolledChance = false;
        chancePassed = false;
        placed = false;
        rotInit = false;
        serverPitch = 0f;
    }

    private double getDistanceToGround() {
        for (int i = 0; i < 256; i++) {
            BlockPos pos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - i, mc.thePlayer.posZ);
            Block block = mc.theWorld.getBlockState(pos).getBlock();

            if (pos.getY() < 0) return -1;

            if (!(block instanceof BlockAir)
                    && !(block instanceof BlockLiquid)
                    && block.getMaterial() != Material.plants) {
                return mc.thePlayer.posY - pos.getY();
            }
        }
        return -1;
    }

    private int getMLGItemSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null) {
                if (stack.getItem() == Items.water_bucket) return i;
                if (stack.getItem() == Item.getItemFromBlock(Blocks.web)) return i;
                if (stack.getItem() == Item.getItemFromBlock(Blocks.slime_block)) return i;
            }
        }
        return -1;
    }
}