package greencloud.impl.modules.utility;

import greencloud.impl.managers.notification.NotificationManager;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;


public class InvManager extends Module {

    public ModeSetting mode = new ModeSetting("Mode", this, "Basic", "Basic", "Hypixel", "Polar");
    public BooleanSetting instant = new BooleanSetting("Instant", this, false, () -> !mode.currentMode.equals("Polar"));
    public BooleanSetting cleaner = new BooleanSetting("Cleaner", this, true);
    public BooleanSetting manager = new BooleanSetting("Manager", this, true);
    public BooleanSetting autoArmor = new BooleanSetting("AutoArmor", this, true);

    public NumberSetting swordSlot = new NumberSetting("Sword Slot", this, 1, 1, 9, 1, () -> manager.enabled);
    public NumberSetting pickSlot = new NumberSetting("Pickaxe Slot", this, 9, 1, 9, 1, () -> manager.enabled);
    public NumberSetting axeSlot = new NumberSetting("Axe Slot", this, 8, 1, 9, 1, () -> manager.enabled);
    public NumberSetting shovelSlot = new NumberSetting("Shovel Slot", this, 4, 1, 9, 1, () -> manager.enabled);
    public NumberSetting blockSlot = new NumberSetting("Block Slot", this, 5, 1, 9, 1, () -> manager.enabled);
    public NumberSetting gappleSlot = new NumberSetting("Gapple Slot", this, 6, 1, 9, 1, () -> manager.enabled);

    private long nextActionTime;
    private long lastClickTime;
    private long lastDelay;
    private boolean isMouseDown;
    private int lastButton = -1;
    private Field eventButtonField;
    private Field buttonsField;

    public InvManager() {
        super("InvManager", "Manages your inventory.", Category.UTILITY);
        this.addSettings(mode, instant, cleaner, manager, autoArmor, swordSlot, pickSlot, axeSlot, shovelSlot, blockSlot, gappleSlot);
        try {
            eventButtonField = net.minecraft.client.gui.GuiScreen.class.getDeclaredField("eventButton");
            eventButtonField.setAccessible(true);
            buttonsField = Mouse.class.getDeclaredField("buttons");
            buttonsField.setAccessible(true);
        } catch (Exception ignored) {}
    }

    @Override
    public void onEnable() {
        NotificationManager.getInstance().addNotification(
                "InvManager",
                "Doesn't throw out some items that are the same.",
                NotificationManager.NotificationType.WARNING,
                2500
        );
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || event.phase != TickEvent.Phase.START) return;

        if (isMouseDown && System.currentTimeMillis() - lastClickTime > 50) {
            simulateMouseClick(lastButton, false);
        }

        if (mc.currentScreen instanceof GuiInventory || (mode.currentMode.equals("Basic") && mc.currentScreen == null)) {
            if (autoArmor.enabled) handleArmorLogic();
            if (cleaner.enabled) handleCleanerLogic();
            if (manager.enabled) handleManagerLogic();
        }
    }

    private void handleArmorLogic() {
        boolean isInstant = !mode.currentMode.equals("Polar") && instant.enabled;
        for (int armorType = 0; armorType < 4; armorType++) {
            if (!isInstant && isWaiting()) return;
            int bestArmorSlot = findBestArmor(armorType);
            if (bestArmorSlot != -1) {
                ItemStack equipped = mc.thePlayer.inventory.armorInventory[3 - armorType];
                ItemStack newArmor = mc.thePlayer.inventoryContainer.getSlot(bestArmorSlot).getStack();

                if (equipped == null || getScore(newArmor) > getScore(equipped)) {
                    simulateMouseClick(0, true);
                    mc.playerController.windowClick(0, equipped != null ? 5 + armorType : bestArmorSlot, 0, equipped != null ? 4 : 1, mc.thePlayer);
                    if (!isInstant) { resetTimer(); return; }
                }
            }
        }
    }

    private void handleCleanerLogic() {
        boolean isInstant = !mode.currentMode.equals("Polar") && instant.enabled;
        for (int i = 9; i < 45; i++) {
            if (!isInstant && isWaiting()) return;
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && (isUselessItem(stack) || isRedundant(stack, i))) {
                simulateMouseClick(0, true);
                mc.playerController.windowClick(0, i, 1, 4, mc.thePlayer);
                if (!isInstant) { resetTimer(); return; }
            }
        }
    }

    private void handleManagerLogic() {
        boolean isInstant = !mode.currentMode.equals("Polar") && instant.enabled;

        int[] hotbarMap = {
                findBestTool(ItemSword.class),
                findBestTool(ItemPickaxe.class),
                findBestTool(ItemAxe.class),
                findBestTool(ItemSpade.class),
                findBestBlock(),
                findBestGapple()
        };

        NumberSetting[] slots = {swordSlot, pickSlot, axeSlot, shovelSlot, blockSlot, gappleSlot};

        for (int i = 0; i < hotbarMap.length; i++) {
            if (!isInstant && isWaiting()) return;
            int source = hotbarMap[i];
            int target = (int) slots[i].value - 1;

            if (source != -1 && source != target + 36) {
                ItemStack currentInTarget = mc.thePlayer.inventoryContainer.getSlot(target + 36).getStack();
                if (i == 4 && currentInTarget != null && currentInTarget.getItem() instanceof ItemBlock && currentInTarget.stackSize == 64) continue;

                simulateMouseClick(0, true);
                mc.playerController.windowClick(0, source, target, 2, mc.thePlayer);
                if (!isInstant) { resetTimer(); return; }
            }
        }
    }

    public void simulateMouseClick(int button, boolean state) {
        if (mc.currentScreen == null || button == -1) return;
        try {
            if (eventButtonField != null) eventButtonField.setInt(mc.currentScreen, state ? button : -1);
            if (buttonsField != null) {
                ByteBuffer buttons = (ByteBuffer) buttonsField.get(null);
                if (button < buttons.capacity()) buttons.put(button, (byte) (state ? 1 : 0));
            }
            isMouseDown = state;
            lastButton = button;
            if (state) lastClickTime = System.currentTimeMillis();
        } catch (Exception ignored) {}
    }

    private boolean isRedundant(ItemStack stack, int slot) {
        Item item = stack.getItem();
        Class<?> type = null;
        if (item instanceof ItemSword) type = ItemSword.class;
        else if (item instanceof ItemPickaxe) type = ItemPickaxe.class;
        else if (item instanceof ItemAxe) type = ItemAxe.class;
        else if (item instanceof ItemSpade) type = ItemSpade.class;
        else if (item instanceof ItemArmor) type = ItemArmor.class;
        if (type == null) return false;

        float bestScore = -1;
        int bestSlot = -1;
        for (int i = 9; i < 45; i++) {
            ItemStack s = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (s == null) continue;
            if (type == ItemArmor.class) {
                if (!(s.getItem() instanceof ItemArmor)) continue;
                if (((ItemArmor) item).armorType != ((ItemArmor) s.getItem()).armorType) continue;
            } else if (!type.isInstance(s.getItem())) continue;
            float sc = getScore(s);
            if (sc > bestScore || (sc == bestScore && i < bestSlot)) {
                bestScore = sc;
                bestSlot = i;
            }
        }
        return slot != bestSlot;
    }

    public static boolean isUselessItem(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ItemSword || item instanceof ItemTool || item instanceof ItemArmor || item instanceof ItemPotion || item instanceof ItemFood || item instanceof ItemBlock) return false;
        if (item instanceof ItemBow || item instanceof ItemEgg || item instanceof ItemSnowball || item == Items.arrow) return false;
        return Arrays.asList(367, 352, 287, 288, 318, 289, 375, 376, 353, 338, 30, 295, 296, 361, 362, 81, 111, 31, 37, 38, 175, 6, 39, 40, 280, 281, 374, 339, 340, 345, 331, 351, 54, 131, 70, 72, 69, 77, 143, 66, 50, 321, 323, 330, 394).contains(Item.getIdFromItem(item));
    }

    private int findBestTool(Class<?> type) {
        int slot = -1; float score = -1;
        for (int i = 9; i < 45; i++) {
            ItemStack s = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (s != null && type.isInstance(s.getItem())) {
                float cur = getScore(s);
                if (cur > score) { score = cur; slot = i; }
            }
        }
        return slot;
    }

    private int findBestBlock() {
        int slot = -1; int size = -1;
        for (int i = 9; i < 45; i++) {
            ItemStack s = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (s != null && s.getItem() instanceof ItemBlock && s.stackSize > size) {
                size = s.stackSize; slot = i;
            }
        }
        return slot;
    }

    private int findBestGapple() {
        for (int i = 9; i < 45; i++) {
            ItemStack s = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (s != null && s.getItem() == Items.golden_apple) return i;
        }
        return -1;
    }

    private int findBestArmor(int type) {
        int slot = -1; float score = -1;
        for (int i = 9; i < 45; i++) {
            ItemStack s = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (s != null && s.getItem() instanceof ItemArmor && ((ItemArmor) s.getItem()).armorType == type) {
                float cur = getScore(s);
                if (cur > score) { score = cur; slot = i; }
            }
        }
        return slot;
    }

    private float getScore(ItemStack stack) {
        if (stack == null) return 0;
        Item item = stack.getItem();
        float score = 0;
        if (item instanceof ItemSword) score += ((ItemSword) item).getDamageVsEntity() + EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25f;
        else if (item instanceof ItemArmor) score += ((ItemArmor) item).damageReduceAmount + EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) * 1.25f;
        return score + EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) * 0.1f;
    }

    private boolean isWaiting() { return System.currentTimeMillis() < nextActionTime; }

    public void resetTimer() {
        long d;
        do { d = ThreadLocalRandom.current().nextLong(90, 171); } while (d == lastDelay);
        lastDelay = d;
        nextActionTime = System.currentTimeMillis() + d;
    }
}