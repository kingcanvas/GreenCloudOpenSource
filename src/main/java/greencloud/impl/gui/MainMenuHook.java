package greencloud.impl.gui;

import greencloud.impl.gui.clickgui.AltManager.GuiAltManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MainMenuHook {

    private static final int ALT_MANAGER_BUTTON_ID = 42069;

    private boolean blurPromptShown = false;

    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiMultiplayer) {
            int x = 6;
            int y = 6;

            int btnWidth = 100;
            int btnHeight = 20;

            event.buttonList.add(new GuiButton(ALT_MANAGER_BUTTON_ID, x, y, btnWidth, btnHeight, "Alt Manager"));
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.gui instanceof GuiMultiplayer) {
            if (event.button.id == ALT_MANAGER_BUTTON_ID) {
                Minecraft.getMinecraft().displayGuiScreen(new GuiAltManager(event.gui));
            }
        }
    }
}