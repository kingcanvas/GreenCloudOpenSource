package greencloudclient.mixins;

import greencloudclient.com.GreenCloud;
import greencloudclient.com.modules.impl.combat.Reach;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererReachMixin {
    @Inject(method = "getMouseOver", at = @At("RETURN"))
    private void greencloud$applyReach(float partialTicks, CallbackInfo callbackInfo) {
        if (GreenCloud.moduleManager == null) return;
        Reach reach = GreenCloud.moduleManager.getModule(Reach.class);
        if (reach != null && reach.isToggled()) reach.updateMouseOver(partialTicks);
    }
}
