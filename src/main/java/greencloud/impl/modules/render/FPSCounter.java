package greencloud.impl.modules.render;

import greencloud.impl.managers.player.PositionManager;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.modules.render.HUD;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ColorSetting;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.utils.render.GreenRender;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;

public class FPSCounter extends Module {

    private final ModeSetting colorMode = new ModeSetting("Color Mode", this, "HUD", "HUD", "Custom");
    private final ColorSetting customColor = new ColorSetting("Custom Color", this, new Color(255, 255, 255), () -> colorMode.is("Custom"));
    private final BooleanSetting background = new BooleanSetting("Background", this, true);
    private final BooleanSetting blur = new BooleanSetting("Blur", this, true, () -> background.enabled);

    private static final String DRAGGABLE_NAME = "FPS Counter";

    public FPSCounter() {
        super("FPSCounter", "Displays your current frames per second.", Category.RENDER);
        addSettings(colorMode, customColor, background, blur);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ensureDraggableRegistered();
    }

    private void ensureDraggableRegistered() {
        PositionManager pm = PositionManager.getInstance();
        if (pm.elements.stream().noneMatch(e -> e.name.equals(DRAGGABLE_NAME))) {
            PositionManager.addElement(DRAGGABLE_NAME, 5, 50, 60, 15, this::render);
        } else {
            for (PositionManager.DraggableElement el : pm.elements) {
                if (el.name.equals(DRAGGABLE_NAME)) {
                    el.renderCallback = this::render;
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public void onRender2D(RenderGameOverlayEvent.Text event) {
        if (mc.gameSettings.showDebugInfo) return;
        render();
    }

    private void render() {
        if (!isToggled()) return;

        PositionManager.DraggableElement el = null;
        for (PositionManager.DraggableElement e : PositionManager.getInstance().elements) {
            if (e.name.equals(DRAGGABLE_NAME)) {
                el = e;
                break;
            }
        }

        if (el == null) {
            ensureDraggableRegistered();
            return;
        }

        String prefix = "FPS: ";
        String valueText = String.valueOf(Minecraft.getDebugFPS());

        float prefixW = GreenRender.strWBold(prefix);
        float valueW = GreenRender.strWBold(valueText);
        float textH = GreenRender.fontHBold();

        float paddingX = 7f;
        float paddingY = 5f;

        float width = prefixW + valueW + paddingX * 2;
        float height = textH + paddingY * 2;

        el.width = (int) width;
        el.height = (int) height;

        int color = colorMode.is("HUD") ? HUD.getColor() : customColor.getColor();

        if (background.enabled) {
            if (blur.enabled) {
                GreenRender.blurRounded(el.x, el.y, width, height, 10f, 4);
            }
            GreenRender.fillRR(el.x, el.y, width, height, 4f, new Color(20, 20, 20, 150));
        }

        GreenRender.drawStringBold(prefix, el.x + paddingX, el.y + paddingY, -1);
        GreenRender.drawStringBold(valueText, el.x + paddingX + prefixW, el.y + paddingY, color);
    }
}
