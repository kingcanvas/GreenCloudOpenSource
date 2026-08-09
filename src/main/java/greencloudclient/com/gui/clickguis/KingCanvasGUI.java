package greencloudclient.com.gui.clickguis;

import greencloudclient.com.GreenCloud;
import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import greencloudclient.com.modules.render.HUD;
import greencloudclient.com.settings.BooleanSetting;
import greencloudclient.com.settings.ColorSetting;
import greencloudclient.com.settings.ModeSetting;
import greencloudclient.com.settings.NumberSetting;
import greencloudclient.com.settings.Setting;
import greencloudclient.com.settings.StringSetting;
import greencloudclient.com.utils.font.FontUtil;
import greencloudclient.com.utils.render.GreenRender;
import greencloudclient.com.utils.render.shaders.BlurUtil;
import greencloudclient.com.modules.render.ClickGUIModule;
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
import java.awt.Desktop;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KingCanvasGUI extends GuiScreen {
    private Category selectedCategory = Category.COMBAT;
    private final List<Module> expandedModules = new ArrayList<>();
    private Module bindingModule = null;
    private NumberSetting draggingSlider = null;
    private boolean draggingMax = false;
    private ColorSetting expandedColorSetting = null;
    
    private final Color bgColor = new Color(20, 20, 20, 220);
    private final Color itemColor = new Color(30, 30, 30, 230);
    private final Color headerColor = new Color(18, 18, 18, 240);
    private final Color subtleTextColor = new Color(140, 140, 140);
    private final Color textColor = new Color(220, 220, 220);
    
    private int startX, startY, width, height;
    private int categoryWidth, mainPanelWidth;
    private float scroll, targetScroll;
    
    private String searchText = "";
    private boolean searchFocused = false;
    
    private boolean configMode = false;
    private String configInputText = "";
    private boolean configInputFocused = false;
    private String selectedConfig = null;
    private long lastConfigClickTime = 0;
    
    private int configScroll = 0;
    private int configTargetScroll = 0;
    
    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    
    private final List<Category> orderedCategories = Arrays.asList(Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.UTILITY);
    
    @Override
    public void initGui() {
        this.width = 500;
        this.height = 320;
        ScaledResolution sr = new ScaledResolution(mc);
        this.startX = (sr.getScaledWidth() / 2) - (width / 2);
        this.startY = (sr.getScaledHeight() / 2) - (height / 2);
        this.categoryWidth = 130;
        this.mainPanelWidth = this.width - this.categoryWidth;
    }
    
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            if (configMode) {
                if (wheel > 0) configTargetScroll += 20;
                else configTargetScroll -= 20;
            } else {
                if (wheel > 0) targetScroll += 20;
                else targetScroll -= 20;
            }
        }
    }
    
    private Color getAccentColor() {
        return new Color(HUD.getColor(), true);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Color accentColor = getAccentColor();
        ScaledResolution sr = new ScaledResolution(mc);
        
        ClickGUIModule clickGui = GreenCloud.moduleManager.getModule(ClickGUIModule.class);
        if (clickGui != null && clickGui.blur.enabled && !BlurUtil.isFastRenderActive()) {
            BlurUtil.blurRegionRounded(startX, startY, width, height, (float) clickGui.blurStrength.value, 8);
        }
        
        GreenRender.fillRect(startX, startY, width, height, bgColor);
        GreenRender.fillRect(startX, startY, width, 35, headerColor);
        
        FontUtil.getSafeNormal().drawStringWithShadow("GreenCloud", startX + 15, startY + 12, accentColor.getRGB());
        FontUtil.getSafeNormal().drawStringWithShadow(GreenCloud.VERSION, startX + 15 + FontUtil.getSafeNormal().getStringWidth("GreenCloud") + 5, startY + 13, subtleTextColor.getRGB());
        
        int searchX = startX + 15;
        int searchY = startY + 45;
        int searchW = categoryWidth - 30;
        int searchH = 20;
        GreenRender.fillRR(searchX, searchY, searchW, searchH, 4f, itemColor);
        if (searchFocused) {
            GreenRender.fillRR(searchX, searchY, searchW, searchH, 4f, new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 40));
        }
        FontUtil.getSafeNormal().drawStringWithShadow(searchText.isEmpty() && !searchFocused ? "Search..." : searchText, searchX + 8, searchY + 6, searchText.isEmpty() && !searchFocused ? subtleTextColor.getRGB() : textColor.getRGB());
        
        int categoryY = startY + 75;
        for (Category category : orderedCategories) {
            boolean isSelected = !configMode && category == selectedCategory;
            if (isSelected) {
                GreenRender.fillRR(startX, categoryY, categoryWidth, 22, 4f, new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 25));
                GreenRender.fillRect(startX, categoryY, 2, 22, accentColor);
            }
            String displayName = category.name().charAt(0) + category.name().substring(1).toLowerCase();
            FontUtil.getSafeNormal().drawString(displayName, startX + 15, categoryY + 7, isSelected ? textColor.getRGB() : subtleTextColor.getRGB());
            categoryY += 22;
        }
        
        boolean configHovered = isMouseOver(mouseX, mouseY, startX, categoryY, categoryWidth, 22);
        if (configMode) {
            GreenRender.fillRR(startX, categoryY, categoryWidth, 22, 4f, new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 25));
            GreenRender.fillRect(startX, categoryY, 2, 22, accentColor);
        }
        FontUtil.getSafeNormal().drawString("Configs", startX + 15, categoryY + 7, configMode ? textColor.getRGB() : (configHovered ? textColor.getRGB() : subtleTextColor.getRGB()));
        
        GreenRender.fillRect(startX + categoryWidth, startY + 35, 1, height - 35, new Color(40, 40, 40));
        
        if (configMode) {
            drawConfigPanel(mouseX, mouseY);
        } else {
            drawModulePanel(startX + categoryWidth, startY + 35, mouseX, mouseY);
        }
        
        float btnX = sr.getScaledWidth() - 135;
        float btnY = sr.getScaledHeight() - 35;
        float btnW = 120;
        float btnH = 20;
        boolean btnHov = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        GreenRender.fillRR(btnX, btnY, btnW, btnH, 4f, btnHov ? accentColor : itemColor);
        FontUtil.getSafeNormal().drawString("Edit HUD", btnX + 10, btnY + 6, btnHov ? Color.WHITE.getRGB() : textColor.getRGB());
    }
    
    private void drawConfigPanel(int mouseX, int mouseY) {
        Color accentColor = getAccentColor();
        int inputY = startY + 45;
        int inputW = mainPanelWidth - 30;
        int inputX = startX + categoryWidth + 15;
        int inputH = 20;
        
        GreenRender.fillRR(inputX, inputY, inputW, inputH, 4f, itemColor);
        if (configInputFocused) {
            GreenRender.fillRR(inputX, inputY, inputW, inputH, 4f, new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 40));
        }
        FontUtil.getSafeNormal().drawStringWithShadow(configInputText.isEmpty() && !configInputFocused ? "Config Name..." : configInputText, inputX + 8, inputY + 6, configInputText.isEmpty() && !configInputFocused ? subtleTextColor.getRGB() : textColor.getRGB());
        if (configInputFocused && System.currentTimeMillis() % 1000 > 500) {
            int textW = FontUtil.getSafeNormal().getStringWidth(configInputText);
            GreenRender.fillRect(inputX + 8 + textW, inputY + 5, 1, 10, textColor);
        }
        
        int btnY = inputY + 30;
        int btnH = 20;
        int btnW = (inputW - 30) / 4;
        int gap = 10;
        
        String[] btnNames = {"Save", "Load", "Delete", "Open Folder"};
        for (int i = 0; i < 4; i++) {
            int bx = inputX + i * (btnW + gap);
            boolean hovered = isMouseOver(mouseX, mouseY, bx, btnY, btnW, btnH);
            GreenRender.fillRR(bx, btnY, btnW, btnH, 4f, hovered ? accentColor : itemColor);
            String n = btnNames[i];
            FontUtil.getSafeNormal().drawString(n, bx + (btnW / 2) - (FontUtil.getSafeNormal().getStringWidth(n) / 2), btnY + 6, hovered ? Color.WHITE.getRGB() : textColor.getRGB());
        }
        
        int listY = btnY + btnH + 20;
        int listH = (startY + height) - listY - 10;
        int listX = inputX;
        int listW = inputW;
        
        GreenRender.fillRect(listX, listY, listW, listH, new Color(0, 0, 0, 60));
        
        List<String> configs = GreenCloud.configManager.getConfigList();
        int contentH = configs.size() * 22;
        configTargetScroll = Math.max(0, Math.min(Math.max(0, contentH - listH), configTargetScroll));
        configScroll += (configTargetScroll - configScroll) * 0.2f;
        
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        GL11.glScissor(listX * sf, mc.displayHeight - (listY + listH) * sf, listW * sf, listH * sf);
        
        int currentY = listY - configScroll;
        for (String config : configs) {
            if (currentY + 22 > listY && currentY < listY + listH) {
                boolean selected = config.equals(selectedConfig);
                boolean hovered = isMouseOver(mouseX, mouseY, listX, currentY, listW, 22);
                if (selected || hovered) {
                    GreenRender.fillRR(listX, currentY, listW, 22, 4f, selected ? new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60) : new Color(255, 255, 255, 10));
                }
                FontUtil.getSafeNormal().drawStringWithShadow(config, listX + 8, currentY + 6, selected ? accentColor.getRGB() : textColor.getRGB());
            }
            currentY += 22;
        }
        
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
    
    private void drawModulePanel(int x, int y, int mouseX, int mouseY) {
        float contentHeight = calculateContentHeight();
        float maxScroll = Math.max(0, contentHeight - (height - 35));
        targetScroll = Math.max(-maxScroll, Math.min(0, targetScroll));
        scroll += (targetScroll - scroll) * 0.2f;
        
        ScaledResolution sr = new ScaledResolution(mc);
        int scaleFactor = sr.getScaleFactor();
        
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scaleFactor, mc.displayHeight - (startY + height) * scaleFactor, mainPanelWidth * scaleFactor, (height - 35) * scaleFactor);
        
        float currentY = y + 10 + scroll;
        if (!searchText.isEmpty()) {
            for (Module module : GreenCloud.moduleManager.getModules()) {
                if (!module.getName().toLowerCase().contains(searchText.toLowerCase())) continue;
                currentY = drawModuleRow(module, x, currentY, mouseX, mouseY);
            }
        } else {
            for (Module module : GreenCloud.moduleManager.getModulesInCategory(selectedCategory)) {
                currentY = drawModuleRow(module, x, currentY, mouseX, mouseY);
            }
        }
        
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
    
    private float drawModuleRow(Module module, int x, float y, int mouseX, int mouseY) {
        Color accentColor = getAccentColor();
        int moduleRowY = (int) y;
        boolean moduleHovered = isMouseOver(mouseX, mouseY, x, moduleRowY, mainPanelWidth, 20);
        Color rowColor = module.isToggled() ? new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 50) : (moduleHovered ? itemColor.brighter() : itemColor);
        
        GreenRender.fillRect(x, moduleRowY, mainPanelWidth, 20, rowColor);
        
        FontUtil.getSafeNormal().drawStringWithShadow(module.getName(), x + 10, moduleRowY + 6, module.isToggled() ? accentColor.getRGB() : textColor.getRGB());
        
        if (expandedModules.contains(module)) {
            int settingPanelHeight = 0;
            for (Setting s : module.getSettings()) {
                if (s.isVisible()) {
                    settingPanelHeight += (s instanceof NumberSetting) ? 28 : 20;
                    if (s == expandedColorSetting) settingPanelHeight += 65;
                }
            }
            settingPanelHeight += 20;
            GreenRender.fillRect(x, moduleRowY + 20, mainPanelWidth, settingPanelHeight, itemColor.darker());
            
            for (Setting setting : module.getSettings()) {
                if (setting.isVisible()) {
                    int settingRowHeight = (setting instanceof NumberSetting) ? 28 : 20;
                    if (setting == expandedColorSetting) settingRowHeight += 65;
                    
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
                        if (number.isRange) {
                            FontUtil.getSafeNormal().drawStringWithShadow(number.name + ": " + number.getRoundedValue() + " - " + number.getRoundedMaxValue(), x + 15, settingRowY + 4, textColor.getRGB());
                            int sliderTotalWidth = mainPanelWidth - 30;
                            int sliderX = x + 15;
                            
                            double minPercent = (number.value - number.min) / (number.max - number.min);
                            double maxPercent = (number.maxValue - number.min) / (number.max - number.min);
                            int minSliderWidth = (int) (sliderTotalWidth * minPercent);
                            int maxSliderWidth = (int) (sliderTotalWidth * maxPercent);
                            
                            GreenRender.fillRR(sliderX, settingRowY + 17, sliderTotalWidth, 4, 2f, subtleTextColor.darker());
                            GreenRender.fillRR(sliderX + minSliderWidth, settingRowY + 17, maxSliderWidth - minSliderWidth, 4, 2f, accentColor);
                            GreenRender.fillCircle(sliderX + minSliderWidth, settingRowY + 19, 4, Color.WHITE);
                            GreenRender.fillCircle(sliderX + maxSliderWidth, settingRowY + 19, 4, Color.WHITE);
                        } else {
                            FontUtil.getSafeNormal().drawStringWithShadow(number.name + ": " + number.getRoundedValue(), x + 15, settingRowY + 4, textColor.getRGB());
                            int sliderTotalWidth = mainPanelWidth - 30;
                            int sliderX = x + 15;
                            double percent = (number.value - number.min) / (number.max - number.min);
                            int sliderWidth = (int) (sliderTotalWidth * percent);
                            
                            GreenRender.fillRR(sliderX, settingRowY + 17, sliderTotalWidth, 4, 2f, subtleTextColor.darker());
                            GreenRender.fillRR(sliderX, settingRowY + 17, sliderWidth, 4, 2f, accentColor);
                            GreenRender.fillCircle(sliderX + sliderWidth, settingRowY + 19, 4, Color.WHITE);
                        }
                        y += 8;
                    } else if (setting instanceof ColorSetting) {
                        ColorSetting colorSetting = (ColorSetting) setting;
                        FontUtil.getSafeNormal().drawStringWithShadow(setting.name, x + 15, settingRowY + 6, textColor.getRGB());
                        int size = 12;
                        GreenRender.fillRR(x + mainPanelWidth - 15 - size, settingRowY + 4, size, size, 2, colorSetting.getColorObject());
                        
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
        return y;
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
        GreenRender.fillCircle(sbIndicatorX, sbIndicatorY, 2, Color.WHITE);
        GreenRender.fillCircle(sbIndicatorX, sbIndicatorY, 1, Color.BLACK);
        
        int hueIndicatorX = x + (int) (setting.hue * width);
        GreenRender.fillRR(hueIndicatorX - 3, hueY - 2, 6, hueHeight + 4, 3f, Color.WHITE);
    }
    
    private void drawToggleButton(int x, int y, boolean enabled) {
        Color accentColor = getAccentColor();
        int toggleWidth = 14;
        int toggleHeight = 8;
        GreenRender.fillRR(x, y, toggleWidth, toggleHeight, 4f, itemColor);
        int handleSize = 6;
        if(enabled) {
            GreenRender.fillRR(x + toggleWidth - handleSize - 1, y + 1, handleSize, handleSize, 3f, accentColor);
        } else {
            GreenRender.fillRR(x + 1, y + 1, handleSize, handleSize, 3f, new Color(50, 50, 50));
        }
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        ScaledResolution sr = new ScaledResolution(mc);
        if (mouseX >= sr.getScaledWidth() - 140 && mouseX <= sr.getScaledWidth() - 20 && mouseY >= sr.getScaledHeight() - 40 && mouseY <= sr.getScaledHeight() - 18) {
            greencloudclient.com.managers.player.PositionManager.open();
            return;
        }
        
        if (isMouseOver(mouseX, mouseY, startX, startY, width, 35) && mouseButton == 0) {
            isDragging = true;
            dragOffsetX = mouseX - startX;
            dragOffsetY = mouseY - startY;
            return;
        }
        
        if (bindingModule != null) {
            bindingModule = null;
            return;
        }
        
        int searchX = startX + 15;
        int searchY = startY + 45;
        int searchW = categoryWidth - 30;
        int searchH = 20;
        if (isMouseOver(mouseX, mouseY, searchX, searchY, searchW, searchH)) {
            searchFocused = true;
            configInputFocused = false;
            return;
        } else {
            searchFocused = false;
        }
        
        int categoryY = startY + 75;
        for (Category category : orderedCategories) {
            if (isMouseOver(mouseX, mouseY, startX, categoryY, categoryWidth, 22)) {
                selectedCategory = category;
                expandedModules.clear();
                configMode = false;
                targetScroll = 0; scroll = 0;
                return;
            }
            categoryY += 22;
        }
        
        if (isMouseOver(mouseX, mouseY, startX, categoryY, categoryWidth, 22)) {
            configMode = true;
            expandedModules.clear();
            configInputFocused = false;
            return;
        }
        
        if (configMode) {
            handleConfigMouseClick(mouseX, mouseY, mouseButton);
            return;
        }
        
        int mainX = startX + categoryWidth;
        if (mouseX < mainX || mouseX > startX + width || mouseY < startY + 35 || mouseY > startY + height) {
            return;
        }
        
        float currentY = startY + 35 + scroll + 10;
        List<Module> modules = searchText.isEmpty() ? GreenCloud.moduleManager.getModulesInCategory(selectedCategory) : GreenCloud.moduleManager.getModules();
        for (Module module : modules) {
            if (!searchText.isEmpty() && !module.getName().toLowerCase().contains(searchText.toLowerCase())) continue;
            
            if (isMouseOver(mouseX, mouseY, mainX, (int)currentY, mainPanelWidth, 20)) {
                if(mouseButton == 0) {
                    module.toggle();
                    return;
                }
                if(mouseButton == 1) {
                    if (expandedModules.contains(module)) {
                        expandedModules.remove(module);
                    } else {
                        expandedModules.add(module);
                    }
                    return;
                }
            }
            
            if (expandedModules.contains(module)) {
                for (Setting setting : module.getSettings()) {
                    if (setting.isVisible()) {
                        int settingRowHeight = (setting instanceof NumberSetting) ? 28 : 20;
                        if (setting == expandedColorSetting) settingRowHeight += 65;
                        
                        currentY += 20;
                        if (setting instanceof NumberSetting) currentY += 8;
                        
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
                                NumberSetting num = (NumberSetting) setting;
                                int sliderTotalWidth = mainPanelWidth - 30;
                                int sliderX = mainX + 15;
                                
                                if (num.isRange) {
                                    double minPercent = (num.value - num.min) / (num.max - num.min);
                                    double maxPercent = (num.maxValue - num.min) / (num.max - num.min);
                                    int minSliderWidth = (int) (sliderTotalWidth * minPercent);
                                    int maxSliderWidth = (int) (sliderTotalWidth * maxPercent);
                                    
                                    if (Math.abs(mouseX - (sliderX + minSliderWidth)) < Math.abs(mouseX - (sliderX + maxSliderWidth))) {
                                        draggingMax = false;
                                    } else {
                                        draggingMax = true;
                                    }
                                } else {
                                    draggingMax = false;
                                }
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
    
    private void handleConfigMouseClick(int mouseX, int mouseY, int mouseButton) {
        int inputY = startY + 45;
        int inputW = mainPanelWidth - 30;
        int inputX = startX + categoryWidth + 15;
        int inputH = 20;
        
        if (isMouseOver(mouseX, mouseY, inputX, inputY, inputW, inputH)) {
            configInputFocused = true;
            return;
        } else {
            configInputFocused = false;
        }
        
        int btnY = inputY + 30;
        int btnH = 20;
        int btnW = (inputW - 30) / 4;
        int gap = 10;
        
        for (int i = 0; i < 4; i++) {
            int bx = inputX + i * (btnW + gap);
            if (isMouseOver(mouseX, mouseY, bx, btnY, btnW, btnH)) {
                if (i == 0) {
                    String name = configInputText.isEmpty() ? (selectedConfig != null ? selectedConfig : "default") : configInputText;
                    GreenCloud.configManager.saveConfig(name);
                } else if (i == 1) {
                    if (selectedConfig != null) {
                        GreenCloud.configManager.loadConfig(selectedConfig);
                    }
                } else if (i == 2) {
                    if (selectedConfig != null) {
                        GreenCloud.configManager.deleteConfig(selectedConfig);
                        selectedConfig = null;
                    }
                } else if (i == 3) {
                    openConfigFolder();
                }
                return;
            }
        }
        
        int listY = btnY + btnH + 20;
        int listH = (startY + height) - listY - 10;
        int listX = inputX;
        int listW = inputW;
        
        List<String> configs = GreenCloud.configManager.getConfigList();
        int currentY = listY - configScroll;
        for (String config : configs) {
            if (isMouseOver(mouseX, mouseY, listX, currentY, listW, 22)) {
                long time = System.currentTimeMillis();
                if (config.equals(selectedConfig) && time - lastConfigClickTime < 500) {
                    GreenCloud.configManager.loadConfig(config);
                    lastConfigClickTime = 0;
                } else {
                    selectedConfig = config;
                    configInputText = config;
                    lastConfigClickTime = time;
                }
                return;
            }
            currentY += 22;
        }
    }
    
    private void openConfigFolder() {
        try {
            Desktop.getDesktop().open(GreenCloud.configManager.configDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isDragging) {
            startX = mouseX - dragOffsetX;
            startY = mouseY - dragOffsetY;
        }
        
        if (draggingSlider != null) {
            int mainX = startX + categoryWidth;
            int sliderTotalWidth = mainPanelWidth - 30;
            int sliderX = mainX + 15;
            double mousePercent = Math.max(0, Math.min(1, ((double) mouseX - sliderX) / sliderTotalWidth));
            double newValue = draggingSlider.min + (draggingSlider.max - draggingSlider.min) * mousePercent;
            
            double snappedValue = calculateSnappedValue(newValue, draggingSlider.increment, draggingSlider.min, draggingSlider.max);
            
            if (draggingSlider.isRange) {
                if (draggingMax) {
                    draggingSlider.setMaxValue(snappedValue);
                } else {
                    draggingSlider.setValue(snappedValue);
                }
            } else {
                draggingSlider.setValue(snappedValue);
            }
        }
        
        if (expandedColorSetting != null) {
            int mainX = startX + categoryWidth;
            float currentY = startY + 35 + scroll + 10;
            List<Module> modules = searchText.isEmpty() ? GreenCloud.moduleManager.getModulesInCategory(selectedCategory) : GreenCloud.moduleManager.getModules();
            for (Module module : modules) {
                if (!searchText.isEmpty() && !module.getName().toLowerCase().contains(searchText.toLowerCase())) continue;
                if (expandedModules.contains(module)) {
                    for (Setting setting : module.getSettings()) {
                        if (setting.isVisible()) {
                            currentY += 20;
                            if (setting instanceof NumberSetting) currentY += 8;
                            
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
        isDragging = false;
        draggingSlider = null;
        draggingMax = false;
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (bindingModule != null) {
            bindingModule.setKeyCode(keyCode == Keyboard.KEY_ESCAPE ? Keyboard.KEY_NONE : keyCode);
            bindingModule = null;
            return;
        }
        
        if (searchFocused) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (searchText.length() > 0) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
            } else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
                searchFocused = false;
            } else if (ChatAllowedCharacter(typedChar)) {
                searchText += typedChar;
            }
            return;
        }
        
        if (configInputFocused) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (configInputText.length() > 0) {
                    configInputText = configInputText.substring(0, configInputText.length() - 1);
                }
            } else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
                configInputFocused = false;
            } else if (ChatAllowedCharacter(typedChar)) {
                configInputText += typedChar;
            }
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
    
    private boolean ChatAllowedCharacter(char character) {
        return character != 167 && character >= ' ' && character != 127;
    }
    
    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    private float calculateContentHeight() {
        float totalHeight = 10;
        List<Module> modules = searchText.isEmpty() ? GreenCloud.moduleManager.getModulesInCategory(selectedCategory) : GreenCloud.moduleManager.getModules();
        for (Module module : modules) {
            if (!searchText.isEmpty() && !module.getName().toLowerCase().contains(searchText.toLowerCase())) continue;
            totalHeight += 20;
            if (expandedModules.contains(module)) {
                for (Setting setting : module.getSettings()) {
                    if (setting.isVisible()) {
                        totalHeight += 20;
                        if (setting instanceof NumberSetting) totalHeight += 8;
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