package greencloudclient.mixins;

import greencloudclient.com.gui.loading.ForgeSplashLoadingScreen;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraftforge.fml.client.SplashProgress$3", remap = false)
public abstract class ForgeSplashRendererMixin {

    @Shadow private void setGL() {}
    @Shadow private void clearGL() {}

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    private void greencloud$renderFullLoadingScreen(CallbackInfo callbackInfo) {
        callbackInfo.cancel();
        ForgeSplashLoadingScreen.start();
        try {
            while (!ForgeSplashLoadingScreen.isDone()) {
                ForgeSplashLoadingScreen.renderFrame();
                ForgeSplashLoadingScreen.mutex().acquireUninterruptibly();
                try {
                    Display.update();
                } finally {
                    ForgeSplashLoadingScreen.mutex().release();
                }
                if (ForgeSplashLoadingScreen.isPaused()) {
                    clearGL();
                    setGL();
                }
                Display.sync(100);
            }
        } finally {
            ForgeSplashLoadingScreen.finish();
            clearGL();
        }
    }
}
