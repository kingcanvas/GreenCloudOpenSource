package greencloud.impl.modules.render;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PlayerModel extends Module {

    public NumberSetting size = new NumberSetting("Size", this, 0.4, 0.1, 1.5, 0.05);

    public PlayerModel() {
        super("PlayerModel", "Changes the visual size of all players", Category.RENDER);
        addSettings(size);
    }

    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        GlStateManager.pushMatrix();

        GlStateManager.translate(event.x, event.y, event.z);

        float scale = (float) size.getValue();
        GlStateManager.scale(scale, scale, scale);

        GlStateManager.translate(-event.x, -event.y, -event.z);
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        GlStateManager.popMatrix();
    }
}