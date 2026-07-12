package greencloud.impl.gui.clickgui.Clickguis;

import greencloud.GreenCloud;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.modules.render.HUD;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ColorSetting;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.settings.Setting;
import greencloud.impl.settings.StringSetting;
import greencloud.impl.utils.font.FontUtil;
import greencloud.impl.utils.render.RenderUtil;
import greencloud.impl.utils.render.shaders.BlurUtil;
import greencloud.impl.modules.render.ClickGUIModule;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class KingCanvasGUI extends GuiScreen {
    private Category selectedCategory = Category.COMBAT;
    private Module expandedModule = null;
    private Module bindingModule = null;
    private NumberSetting draggingSlider = null;
    private ColorSetting expandedColorSetting = null;
    private final Color bgColor = new Color(20, 20, 20, 220);
    private final Color itemColor = new Color(30, 30, 30, 230);
    private final Color headerColor = new Color(18, 18, 18, 240);
    private final Color subtleTextColor = new Color(140, 140, 140);
    private final Color textColor = new Color(220, 220, 220);
    private int startX, startY, width, height;
    private int categoryWidth, mainPanelWidth;
    private float scroll, targetScroll;

    @Override
    public void initGui() {
        this.width = 450;
        this.height = 300;
        ScaledResolution sr = new ScaledResolution(mc);
        this.startX = (sr.getScaledWidth() / 2) - (width / 2);
        this.startY = (sr.getScaledHeight() / 2) - (height / 2);
        this.categoryWidth = 100;
        this.mainPanelWidth = this.width - this.categoryWidth;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            if (wheel > 0) targetScroll += 20;
            else targetScroll -= 20;
        }
    }

    private Color getAccentColor() {
        return new Color(HUD.getColor(), true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Color accentColor = getAccentColor();

        ScaledResolution sr = new ScaledResolution(mc);
        int scaleFactor = sr.getScaleFactor();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(startX * scaleFactor, mc.displayHeight - (startY + height) * scaleFactor, width * scaleFactor, height * scaleFactor);

        ClickGUIModule clickGui = GreenCloud.moduleManager.getModule(ClickGUIModule.class);
        if (clickGui != null && clickGui.blur.enabled && !BlurUtil.isFastRenderActive()) {
            BlurUtil.blurRegionRounded(startX, startY, width, height, (float) clickGui.blurStrength.value, 8);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        RenderUtil.drawRoundedRect(startX, startY, startX + width, startY + height, 8f, bgColor.getRGB());
        RenderUtil.drawRoundedRect(startX, startY, startX + width, startY + 30 + 8, 8f, headerColor.getRGB());
        Gui.drawRect(startX, startY + 30, startX + width, startY + 30 + 8, headerColor.getRGB());

        FontUtil.getSafeNormal().drawStringWithShadow("GreenCloud", startX + 10, startY + 10, accentColor.getRGB());
        FontUtil.getSafeNormal().drawStringWithShadow(GreenCloud.VERSION, startX + 10 + FontUtil.getSafeNormal().getStringWidth("GreenCloud") + 5, startY + 11, subtleTextColor.getRGB());

        int categoryY = startY + 40;
        for (Category category : Category.values()) {
            boolean isSelected = category == selectedCategory;
            if (isSelected) {
                Gui.drawRect(startX, categoryY, startX + categoryWidth, categoryY + 20, new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 25).getRGB());
                Gui.drawRect(startX, categoryY, startX + 2, categoryY + 20, accentColor.getRGB());
            }
            FontUtil.getSafeNormal().drawString(category.name(), startX + 15, categoryY + 6, isSelected ? textColor.getRGB() : subtleTextColor.getRGB());
            categoryY += 20;
        }

        Gui.drawRect(startX + categoryWidth, startY + 30, startX + categoryWidth + 1, startY + height, new Color(40, 40, 40).getRGB());

        float contentHeight = calculateContentHeight();
        float maxScroll = Math.max(0, contentHeight - (height - 30));
        targetScroll = Math.max(-maxScroll, Math.min(0, targetScroll));
        scroll += (targetScroll - scroll) * 0.2f;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((startX + categoryWidth) * scaleFactor, mc.displayHeight - (startY + height) * scaleFactor, mainPanelWidth * scaleFactor, (height - 30) * scaleFactor);

        float currentY = startY + 30 + scroll;

        drawModulePanel(startX + categoryWidth, currentY, mouseX, mouseY);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        float btnX = sr.getScaledWidth() - 135;
        float btnY = sr.getScaledHeight() - 35;
        float btnW = 120;
        float btnH = 20;
        boolean btnHov = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        RenderUtil.drawRoundedRect(btnX, btnY, btnX + btnW, btnY + btnH, 4f, btnHov ? new Color(60, 60, 60).getRGB() : itemColor.getRGB());
        FontUtil.getSafeNormal().drawString("Edit HUD", btnX + 10, btnY + 6, textColor.getRGB());
    }

    private void drawModulePanel(int x, float y, int mouseX, int mouseY) {
        Color accentColor = getAccentColor();

        y += 10;
        for (Module module : GreenCloud.moduleManager.getModulesInCategory(selectedCategory)) {
            int moduleRowY = (int) y;
            Color rowColor = module.isToggled() ? new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 50) : itemColor;
            Gui.drawRect(x, moduleRowY, x + mainPanelWidth, moduleRowY + 20, rowColor.getRGB());
            FontUtil.getSafeNormal().drawStringWithShadow(module.getName(), x + 10, moduleRowY + 6, module.isToggled() ? accentColor.getRGB() : textColor.getRGB());
            String categoryTag = "[" + module.getCategory().name() + "]";
            int tagWidth = FontUtil.getSafeNormal().getStringWidth(categoryTag);
            FontUtil.getSafeNormal().drawStringWithShadow(categoryTag, x + mainPanelWidth - 10 - tagWidth, moduleRowY + 6, subtleTextColor.getRGB());

            if (module == expandedModule) {
                int settingPanelHeight = 0;
                for (Setting s : module.getSettings()) {
                    if (s.isVisible()) {
                        settingPanelHeight += (s instanceof NumberSetting) ? 25 : 20;
                        if (s == expandedColorSetting) settingPanelHeight += 65;
                    }
                }
                settingPanelHeight += 20;
                Gui.drawRect(x, moduleRowY + 20, x + mainPanelWidth, moduleRowY + 20 + settingPanelHeight, itemColor.darker().getRGB());

                for (Setting setting : module.getSettings()) {
                    if (setting.isVisible()) {
                        y += 20;
                        int settingRowY = (int) y;
                        if (setting instanceof BooleanSetting) {
                            BooleanSetting bool = (BooleanSetting) setting;
                            FontUtil.getSafeNormal().drawStringWithShadow(setting.name, x + 15, settingRowY + 6, textColor.getRGB());
                            drawToggleButton(x + mainPanelWidth - 25, settingRowY + 6, bool.enabled);
                        } else if (setting instanceof ModeSetting) {
                            ModeSetting mode = (ModeSetting) setting;
                            FontUtil.getSafeNormal().drawStringWithShadow(setting.name + ": " + mode.currentMode, x + 15, settingRowY + 6, textColor.getRGB());
                        } else if (setting instanceof NumberSetting) {
                            NumberSetting number = (NumberSetting) setting;
                            settingRowY = (int) y;
                            FontUtil.getSafeNormal().drawStringWithShadow(number.name + ": " + number.getRoundedValue(), x + 15, settingRowY + 4, textColor.getRGB());
                            int sliderTotalWidth = mainPanelWidth - 30;
                            int sliderX = x + 15;
                            double percent = (number.value - number.min) / (number.max - number.min);
                            int sliderWidth = (int) (sliderTotalWidth * percent);
                            Gui.drawRect(sliderX, settingRowY + 14, sliderX + sliderTotalWidth, settingRowY + 16, subtleTextColor.getRGB());
                            Gui.drawRect(sliderX, settingRowY + 14, sliderX + sliderWidth, settingRowY + 16, accentColor.getRGB());
                            Gui.drawRect(sliderX + sliderWidth - 2, settingRowY + 12, sliderX + sliderWidth + 2, settingRowY + 18, textColor.getRGB());
                            y += 5;
                        } else if (setting instanceof ColorSetting) {
                            ColorSetting colorSetting = (ColorSetting) setting;
                            FontUtil.getSafeNormal().drawStringWithShadow(setting.name, x + 15, settingRowY + 6, textColor.getRGB());
                            int size = 12;
                            RenderUtil.drawRoundedRect(x + mainPanelWidth - 15 - size, settingRowY + 4, x + mainPanelWidth - 15, settingRowY + 4 + size, 2, colorSetting.getColor());

                            if (setting == expandedColorSetting) {
                                int pickerX = x + 15;
                                int pickerY = settingRowY + 16;
                                int pickerWidth = mainPanelWidth - 30;
                                int pickerHeight = 60;

                                drawColorPicker(pickerX, pickerY, pickerWidth, pickerHeight, colorSetting);

                                y += 65;
                            }
                        } else if (setting instanceof StringSetting) {
                            StringSetting stringSetting = (StringSetting) setting;
                            stringSetting.render(x + 15, settingRowY + 2, mainPanelWidth - 30, 16, mouseX, mouseY);
                        }
                    }
                }
                y += 20;
                int keybindY = (int) y;
                String text = (bindingModule == module) ? "Binding..." : "Bind: " + Keyboard.getKeyName(module.getKeyCode());
                FontUtil.getSafeNormal().drawStringWithShadow(text, x + 15, keybindY + 6, subtleTextColor.getRGB());
            }
            y += 20;
        }
    }

    private void drawColorPicker(int x, int y, int width, int height, ColorSetting setting) {
        int hueHeight = 8;
        int sbHeight = height - hueHeight - 5;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableCull();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        worldRenderer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int py = 0; py <= sbHeight; py++) {
            float b = 1.0f - (py / (float) sbHeight);
            Color cLeft = Color.getHSBColor(setting.hue, 0f, Math.max(b, 0.2f));
            Color cRight = Color.getHSBColor(setting.hue, 1f, Math.max(b, 0.2f));

            worldRenderer.pos(x, y + py, 0).color(cLeft.getRed() / 255f, cLeft.getGreen() / 255f, cLeft.getBlue() / 255f, 1f).endVertex();
            worldRenderer.pos(x + width, y + py, 0).color(cRight.getRed() / 255f, cRight.getGreen() / 255f, cRight.getBlue() / 255f, 1f).endVertex();
        }
        tessellator.draw();

        int hueY = y + sbHeight + 5;
        worldRenderer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int px = 0; px <= width; px++) {
            float h = px / (float) width;
            Color c = Color.getHSBColor(h, 1f, 1f);

            worldRenderer.pos(x + px, hueY, 0).color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1f).endVertex();
            worldRenderer.pos(x + px, hueY + hueHeight, 0).color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1f).endVertex();
        }
        tessellator.draw();

        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();

        int sbIndicatorX = x + (int) (setting.saturation * width);
        int sbIndicatorY = y + (int) ((1f - setting.brightness) * sbHeight);
        Gui.drawRect(sbIndicatorX - 2, sbIndicatorY - 2, sbIndicatorX + 2, sbIndicatorY + 2, Color.WHITE.getRGB());
        Gui.drawRect(sbIndicatorX - 1, sbIndicatorY - 1, sbIndicatorX + 1, sbIndicatorY + 1, Color.BLACK.getRGB());

        int hueIndicatorX = x + (int) (setting.hue * width);
        Gui.drawRect(hueIndicatorX - 1, hueY - 2, hueIndicatorX + 1, hueY + hueHeight + 2, Color.WHITE.getRGB());
    }

    private void drawToggleButton(int x, int y, boolean enabled) {
        Color accentColor = getAccentColor();
        Color color = enabled ? accentColor : new Color(50, 50, 50);
        int toggleWidth = 14;
        int toggleHeight = 8;
        int radius = 4;
        RenderUtil.drawRoundedRect(x, y, x + toggleWidth, y + toggleHeight, (float)radius, itemColor.getRGB());
        int handleSize = 6;
        if(enabled) {
            RenderUtil.drawRoundedRect(x + toggleWidth - handleSize - 1, y + 1, x + toggleWidth - 1, y + toggleHeight - 1, (float)radius/2, accentColor.getRGB());
        } else {
            RenderUtil.drawRoundedRect(x + 1, y + 1, x + handleSize + 1, y + toggleHeight - 1, (float)radius/2, color.getRGB());
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        ScaledResolution sr = new ScaledResolution(mc);
        if (mouseX >= sr.getScaledWidth() - 140 && mouseX <= sr.getScaledWidth() - 20 && mouseY >= sr.getScaledHeight() - 40 && mouseY <= sr.getScaledHeight() - 18) {
            greencloud.impl.managers.player.PositionManager.open();
            return;
        }

        if (bindingModule != null) {
            bindingModule = null;
            return;
        }

        int categoryY = startY + 40;
        for (Category category : Category.values()) {
            if (isMouseOver(mouseX, mouseY, startX, categoryY, categoryWidth, 20)) {
                selectedCategory = category;
                expandedModule = null;
                targetScroll = 0; scroll = 0;
                return;
            }
            categoryY += 20;
        }

        int mainX = startX + categoryWidth;
        if (mouseX < mainX || mouseX > startX + width || mouseY < startY + 30 || mouseY > startY + height) {
            return;
        }

        float currentY = startY + 30 + scroll + 10;
        for (Module module : GreenCloud.moduleManager.getModulesInCategory(selectedCategory)) {
            if (isMouseOver(mouseX, mouseY, mainX, (int)currentY, mainPanelWidth, 20)) {
                if(mouseButton == 0) {
                    module.toggle();
                    return;
                }
                if(mouseButton == 1) {
                    expandedModule = (expandedModule == module) ? null : module;
                    return;
                }
            }

            if (module == expandedModule) {
                for (Setting setting : module.getSettings()) {
                    if (setting.isVisible()) {
                        int settingRowHeight = (setting instanceof NumberSetting) ? 25 : 20;
                        if (setting == expandedColorSetting) settingRowHeight += 65;

                        currentY += 20;
                        if (setting instanceof NumberSetting) currentY += 5;

                        if (setting == expandedColorSetting) {
                            int pickerX = mainX + 15;
                            int pickerY = (int)currentY + 16;
                            int pickerWidth = mainPanelWidth - 30;
                            int pickerHeight = 60;
                            int hueHeight = 8;
                            int sbHeight = pickerHeight - hueHeight - 5;

                            if (mouseX >= pickerX && mouseX <= pickerX + pickerWidth && mouseY >= pickerY && mouseY <= pickerY + sbHeight) {
                                float s = (mouseX - pickerX) / (float) pickerWidth;
                                float b = 1f - (mouseY - pickerY) / (float) sbHeight;
                                expandedColorSetting.setSaturation(Math.max(0, Math.min(1, s)));
                                expandedColorSetting.setBrightness(Math.max(0.2f, Math.min(1, b)));
                                return;
                            }

                            int hueY = pickerY + sbHeight + 5;
                            if (mouseX >= pickerX && mouseX <= pickerX + pickerWidth && mouseY >= hueY && mouseY <= hueY + hueHeight) {
                                float h = (mouseX - pickerX) / (float) pickerWidth;
                                expandedColorSetting.setHue(Math.max(0, Math.min(1, h)));
                                return;
                            }
                        }

                        if (isMouseOver(mouseX, mouseY, mainX, (int)currentY, mainPanelWidth, settingRowHeight)) {
                            if (setting instanceof BooleanSetting) {
                                if(isMouseOver(mouseX, mouseY, mainX + mainPanelWidth - 25, (int)currentY + 6, 14, 8)) {
                                    ((BooleanSetting) setting).toggle();
                                }
                            }
                            if (setting instanceof ModeSetting) {
                                ((ModeSetting) setting).cycle();
                            }
                            if (setting instanceof NumberSetting) {
                                draggingSlider = (NumberSetting) setting;
                                mouseClickMove(mouseX, mouseY, mouseButton, 0);
                            }
                            if (setting instanceof ColorSetting) {
                                expandedColorSetting = (expandedColorSetting == setting) ? null : (ColorSetting) setting;
                                return;
                            }
                            if (setting instanceof StringSetting) {
                                ((StringSetting) setting).mouseClicked(mouseX, mouseY, mouseButton, mainX + 15, (int)currentY + 2, mainPanelWidth - 30, 16);
                            }
                            return;
                        }
                    }
                }
                currentY += 20;
                if(isMouseOver(mouseX, mouseY, mainX, (int)currentY, mainPanelWidth, 20)) {
                    bindingModule = module;
                    return;
                }
            }
            currentY += 20;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingSlider != null) {
            int mainX = startX + categoryWidth;
            int sliderTotalWidth = mainPanelWidth - 30;
            int sliderX = mainX + 15;
            double mousePercent = Math.max(0, Math.min(1, ((double) mouseX - sliderX) / sliderTotalWidth));
            double newValue = draggingSlider.min + (draggingSlider.max - draggingSlider.min) * mousePercent;
            draggingSlider.value = calculateSnappedValue(newValue, draggingSlider.increment, draggingSlider.min, draggingSlider.max);
        }

        if (expandedColorSetting != null) {
            int mainX = startX + categoryWidth;
            float currentY = startY + 30 + scroll + 10;
            for (Module module : GreenCloud.moduleManager.getModulesInCategory(selectedCategory)) {
                if (module == expandedModule) {
                    for (Setting setting : module.getSettings()) {
                        if (setting.isVisible()) {
                            currentY += 20;
                            if (setting instanceof NumberSetting) currentY += 5;

                            if (setting == expandedColorSetting) {
                                int pickerX = mainX + 15;
                                int pickerY = (int) currentY + 16;
                                int pickerWidth = mainPanelWidth - 30;
                                int pickerHeight = 60;
                                int hueHeight = 8;
                                int sbHeight = pickerHeight - hueHeight - 5;

                                if (mouseX >= pickerX && mouseX <= pickerX + pickerWidth && mouseY >= pickerY && mouseY <= pickerY + sbHeight) {
                                    float s = (mouseX - pickerX) / (float) pickerWidth;
                                    float b = 1f - (mouseY - pickerY) / (float) sbHeight;
                                    expandedColorSetting.setSaturation(Math.max(0, Math.min(1, s)));
                                    expandedColorSetting.setBrightness(Math.max(0.2f, Math.min(1, b)));
                                    return;
                                }

                                int hueY = pickerY + sbHeight + 5;
                                if (mouseX >= pickerX && mouseX <= pickerX + pickerWidth && mouseY >= hueY && mouseY <= hueY + hueHeight) {
                                    float h = (mouseX - pickerX) / (float) pickerWidth;
                                    expandedColorSetting.setHue(Math.max(0, Math.min(1, h)));
                                    return;
                                }
                            }
                        }
                    }
                    break;
                }
                currentY += 20;
            }
        }
    }

    private double calculateSnappedValue(double newValue, double increment, double min, double max) {
        double snappedValue = new BigDecimal(Math.round(newValue / increment) * increment).setScale(2, RoundingMode.HALF_UP).doubleValue();
        return Math.max(min, Math.min(max, snappedValue));
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingSlider = null;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (bindingModule != null) {
            bindingModule.setKeyCode(keyCode == Keyboard.KEY_ESCAPE ? Keyboard.KEY_NONE : keyCode);
            bindingModule = null;
            return;
        }

        for (Module module : GreenCloud.moduleManager.getModules()) {
            for (Setting setting : module.getSettings()) {
                if (setting instanceof StringSetting && ((StringSetting) setting).isFocused()) {
                    ((StringSetting) setting).keyTyped(typedChar, keyCode);
                    return;
                }
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private float calculateContentHeight() {
        float totalHeight = 10;
        for (Module module : GreenCloud.moduleManager.getModulesInCategory(selectedCategory)) {
            totalHeight += 20;
            if (module == expandedModule) {
                for (Setting setting : module.getSettings()) {
                    if (setting.isVisible()) {
                        totalHeight += 20;
                        if (setting instanceof NumberSetting) totalHeight += 5;
                        if (setting == expandedColorSetting) totalHeight += 65;
                    }
                }
                totalHeight += 20;
            }
        }
        return totalHeight;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}