package greencloud.impl.modules.render;

import greencloud.impl.managers.player.PositionManager;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.modules.render.HUD;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ColorSetting;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.utils.render.GreenRender;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class CPSCounter extends Module {

    private final BooleanSetting leftClick = new BooleanSetting("Left Click", this, true);
    private final BooleanSetting rightClick = new BooleanSetting("Right Click", this, true);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", this, "HUD", "HUD", "Custom");
    private final ColorSetting customColor = new ColorSetting("Custom Color", this, new Color(255, 255, 255), () -> colorMode.is("Custom"));
    private final BooleanSetting background = new BooleanSetting("Background", this, true);
    private final BooleanSetting blur = new BooleanSetting("Blur", this, true, () -> background.enabled);

    private static final List<Long> leftClicks = new ArrayList<>();
    private static final List<Long> rightClicks = new ArrayList<>();

    private boolean wasLeftDown = false;
    private boolean wasRightDown = false;

    private static final String DRAGGABLE_NAME = "CPS HUD";

    public CPSCounter() {
        super("CPSCounter", "Displays your clicks per second.", Category.RENDER);
        addSettings(leftClick, rightClick, colorMode, customColor, background, blur);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ensureDraggableRegistered();
    }

    private void ensureDraggableRegistered() {
        PositionManager pm = PositionManager.getInstance();
        if (pm.elements.stream().noneMatch(e -> e.name.equals(DRAGGABLE_NAME))) {
            PositionManager.addElement(DRAGGABLE_NAME, 5, 70, 80, 15, this::render);
        } else {
            for (PositionManager.DraggableElement el : pm.elements) {
                if (el.name.equals(DRAGGABLE_NAME)) {
                    el.renderCallback = this::render;
                    break;
                }
            }
        }
    }

    public static void registerClick(boolean right) {
        if (right) {
            rightClicks.add(System.currentTimeMillis());
        } else {
            leftClicks.add(System.currentTimeMillis());
        }
    }

    @SubscribeEvent
    public void onRender2D(RenderGameOverlayEvent.Text event) {
        if (mc.gameSettings.showDebugInfo) return;

        boolean isLeftDown = Mouse.isButtonDown(0);
        boolean isRightDown = Mouse.isButtonDown(1);

        if (isLeftDown && !wasLeftDown) registerClick(false);
        if (isRightDown && !wasRightDown) registerClick(true);

        wasLeftDown = isLeftDown;
        wasRightDown = isRightDown;

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

        long now = System.currentTimeMillis();
        leftClicks.removeIf(t -> now - t > 1000);
        rightClicks.removeIf(t -> now - t > 1000);

        int lCps = leftClicks.size();
        int rCps = rightClicks.size();

        StringBuilder sb = new StringBuilder();
        if (leftClick.enabled) sb.append("L: ").append(lCps);
        if (leftClick.enabled && rightClick.enabled) sb.append("  ");
        if (rightClick.enabled) sb.append("R: ").append(rCps);

        String text = sb.toString();
        if (text.isEmpty()) return;

        float textH = GreenRender.fontHBold();
        float paddingX = 8f;
        float paddingY = 5f;

        float totalW = 0;
        if (leftClick.enabled) {
            totalW += GreenRender.strWBold("L: ");
            totalW += GreenRender.strWBold(String.valueOf(lCps));
        }
        if (leftClick.enabled && rightClick.enabled) {
            totalW += GreenRender.strWBold("  ");
        }
        if (rightClick.enabled) {
            totalW += GreenRender.strWBold("R: ");
            totalW += GreenRender.strWBold(String.valueOf(rCps));
        }

        float width = totalW + paddingX * 2;
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

        float curX = el.x + paddingX;
        if (leftClick.enabled) {
            GreenRender.drawStringBold("L: ", curX, el.y + paddingY, -1);
            curX += GreenRender.strWBold("L: ");
            GreenRender.drawStringBold(String.valueOf(lCps), curX, el.y + paddingY, color);
            curX += GreenRender.strWBold(String.valueOf(lCps));
        }
        if (leftClick.enabled && rightClick.enabled) {
            curX += GreenRender.strWBold("  ");
        }
        if (rightClick.enabled) {
            GreenRender.drawStringBold("R: ", curX, el.y + paddingY, -1);
            curX += GreenRender.strWBold("R: ");
            GreenRender.drawStringBold(String.valueOf(rCps), curX, el.y + paddingY, color);
        }
    }
}
