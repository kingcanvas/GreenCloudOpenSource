package greencloudclient.mixins;

import greencloudclient.com.gui.loading.ClientLoadingScreen;
import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.MinecraftError;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingScreenRenderer.class)
public abstract class LoadingScreenRendererMixin {

    @Shadow private Minecraft mc;
    @Shadow private String message;
    @Shadow private String currentlyDisplayedText;
    @Shadow private boolean loadingSuccess;
    @Shadow private Framebuffer framebuffer;
    @Unique private long greencloud$lastRender;

    @Inject(method = "setLoadingProgress", at = @At("HEAD"), cancellable = true)
    private void greencloud$renderLoadingScreen(int progress, CallbackInfo callbackInfo) {
        callbackInfo.cancel();
        if (!((MinecraftAccessor) mc).greencloud$isRunning()) {
            if (!loadingSuccess) throw new MinecraftError();
            return;
        }

        long now = Minecraft.getSystemTime();
        if (now - greencloud$lastRender < 50L) return;
        greencloud$lastRender = now;
        ClientLoadingScreen.renderLoading(mc, framebuffer, progress, currentlyDisplayedText, message);
    }
}
