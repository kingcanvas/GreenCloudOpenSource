package greencloudclient.mixins;

import greencloudclient.com.gui.loading.ClientLoadingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public abstract class GuiMainMenuMixin {

    @Unique private boolean greencloud$transitionChecked;
    @Unique private boolean greencloud$transitionActive;
    @Unique private long greencloud$transitionStart;

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void greencloud$renderLoadingTransition(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        if (!greencloud$transitionChecked) {
            greencloud$transitionChecked = true;
            greencloud$transitionActive = ClientLoadingScreen.claimMenuTransition();
            greencloud$transitionStart = Minecraft.getSystemTime();
        }
        if (!greencloud$transitionActive) return;

        float progress = Math.min(1.0f, (Minecraft.getSystemTime() - greencloud$transitionStart) / 1100.0f);
        float eased = progress * progress * (3.0f - 2.0f * progress);
        float alpha = 1.0f - eased;
        Minecraft mc = Minecraft.getMinecraft();
        ClientLoadingScreen.renderMenuTransition(mc, mc.currentScreen.width, mc.currentScreen.height, alpha);
        if (progress >= 1.0f) greencloud$transitionActive = false;
    }
}
