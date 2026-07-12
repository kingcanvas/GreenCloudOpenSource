package greencloud.impl.modules.utility;

import greencloud.impl.managers.notification.NotificationManager;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ChestStealer extends Module {

    public ModeSetting mode = new ModeSetting("Mode", this, "Hypixel", "Hypixel", "Polar");
    public BooleanSetting instant = new BooleanSetting("Instant", this, false, () -> !mode.currentMode.equals("Polar"));
    public NumberSetting stealerDelay = new NumberSetting("Stealer Delay", this, 100, 0, 500, 10, () -> !mode.currentMode.equals("Polar") && !instant.enabled);
    public BooleanSetting autoClose = new BooleanSetting("AutoClose", this, true);

    private long nextActionTime;
    private long lastClickTime;
    private long lastDelay;
    private boolean isMouseDown;
    private int lastButton = -1;
    private Field eventButtonField;
    private Field buttonsField;
    private final List<Integer> clickOrder = new ArrayList<>();

    public ChestStealer() {
        super("ChestStealer", "Steals items from chests.", Category.UTILITY);
        this.addSettings(mode, instant, stealerDelay, autoClose);
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
                "ChestStealer",
                "Doesn't take last chest item.",
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

        if (mc.currentScreen instanceof GuiChest) {
            handleStealer();
        } else {
            clickOrder.clear();
        }
    }

    private void handleStealer() {
        GuiChest chestGui = (GuiChest) mc.currentScreen;
        ContainerChest container = (ContainerChest) chestGui.inventorySlots;
        int size = container.getLowerChestInventory().getSizeInventory();
        boolean isInstant = !mode.currentMode.equals("Polar") && instant.enabled;

        if (clickOrder.isEmpty()) {
            for (int i = 0; i < size; i++) clickOrder.add(i);
            Collections.shuffle(clickOrder);
        }

        boolean lootedAny = false;
        for (int i = 0; i < clickOrder.size(); i++) {
            if (!isInstant && isWaiting()) return;
            int slot = clickOrder.get(i);
            ItemStack stack = container.getLowerChestInventory().getStackInSlot(slot);

            if (stack != null && !InvManager.isUselessItem(stack)) {
                int remaining = 0;
                for (int j = 0; j < size; j++) {
                    ItemStack s = container.getLowerChestInventory().getStackInSlot(j);
                    if (s != null && !InvManager.isUselessItem(s)) remaining++;
                }
                if (remaining <= 1) continue;

                simulateMouseClick(0, true);
                mc.playerController.windowClick(container.windowId, slot, 0, 1, mc.thePlayer);
                clickOrder.remove(i);
                lootedAny = true;
                if (!isInstant) { resetTimer(); return; }
                i--;
            }
        }

        if (!lootedAny && autoClose.enabled && isChestEmpty(container)) {
            mc.thePlayer.closeScreen();
        }
    }

    private void simulateMouseClick(int button, boolean state) {
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

    private boolean isChestEmpty(ContainerChest container) {
        for (int i = 0; i < container.getLowerChestInventory().getSizeInventory(); i++) {
            ItemStack stack = container.getLowerChestInventory().getStackInSlot(i);
            if (stack != null && !InvManager.isUselessItem(stack)) return false;
        }
        return true;
    }

    private boolean isWaiting() { return System.currentTimeMillis() < nextActionTime; }

    private void resetTimer() {
        long d;
        if (mode.currentMode.equals("Polar")) {
            do { d = ThreadLocalRandom.current().nextLong(90, 171); } while (d == lastDelay);
        } else { d = (long) stealerDelay.value; }
        lastDelay = d;
        nextActionTime = System.currentTimeMillis() + d;
    }
}