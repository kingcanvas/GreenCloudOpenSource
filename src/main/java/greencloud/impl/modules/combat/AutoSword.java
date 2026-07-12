package greencloud.impl.modules.combat;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoSword extends Module {

    public AutoSword() {
        super("AutoSword", "Automatically switches to a sword when attacking.", Category.COMBAT);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!isToggled() || mc.thePlayer == null || mc.theWorld == null) return;
        if (!(event.target instanceof EntityLivingBase)) return;

        int bestSlot = findBestSwordSlot();
        if (bestSlot == -1) return;

        int currentSlot = mc.thePlayer.inventory.currentItem;
        if (currentSlot == bestSlot) return;

        mc.thePlayer.inventory.currentItem = bestSlot;
    }

    private int findBestSwordSlot() {
        int bestSlot = -1;
        float bestScore = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack == null || !(stack.getItem() instanceof ItemSword)) continue;

            float score = getSwordScore(stack);
            if (score > bestScore || (score == bestScore && (bestSlot == -1 || i < bestSlot))) {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private float getSwordScore(ItemStack stack) {
        ItemSword sword = (ItemSword) stack.getItem();
        float score = sword.getDamageVsEntity();
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25f;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack) * 0.5f;
        return score;
    }
}
