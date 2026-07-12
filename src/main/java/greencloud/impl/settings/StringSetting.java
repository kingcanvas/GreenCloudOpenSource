package greencloud.impl.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import greencloud.impl.modules.Module;
import greencloud.impl.utils.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.function.Supplier;

public class StringSetting extends Setting {

    private String value;
    private String placeholder;
    private final int maxLength;
    private boolean focused;
    private int cursorBlink;
    private float animationProgress;
    private int cursorPosition;

    public StringSetting(String name, Module parent, String defaultValue) {
        this(name, parent, defaultValue, 256, null);
    }

    public StringSetting(String name, Module parent, String defaultValue, int maxLength) {
        this(name, parent, defaultValue, maxLength, null);
    }

    public StringSetting(String name, Module parent, String defaultValue, int maxLength, Supplier<Boolean> visibility) {
        super(name, parent, visibility);
        this.value = defaultValue == null ? "" : defaultValue;
        this.maxLength = maxLength;
        this.placeholder = name;
        this.focused = false;
        this.cursorBlink = 0;
        this.animationProgress = 0;
        this.cursorPosition = this.value.length();
    }

    public void render(float x, float y, float width, float height, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getMinecraft();

        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        if (focused) {
            animationProgress = Math.min(1.0f, animationProgress + 0.1f);
        } else if (hovered) {
            animationProgress = Math.min(1.0f, animationProgress + 0.05f);
        } else {
            animationProgress = Math.max(0.0f, animationProgress - 0.05f);
        }

        int baseColor = new Color(30, 30, 35, 200).getRGB();
        int focusedColor = new Color(45, 45, 55, 220).getRGB();
        int currentBgColor = interpolateColor(baseColor, focusedColor, animationProgress);

        RenderUtil.drawRoundedRect(x, y, x + width, y + height, 3, currentBgColor);

        if (focused) {
            int accentColor = new Color(120, 180, 255, (int)(200 + 55 * animationProgress)).getRGB();
            RenderUtil.drawRoundedRect(x - 0.5f, y - 0.5f, x + width + 0.5f, y + height + 0.5f, 3, accentColor);
            RenderUtil.drawRoundedRect(x, y, x + width, y + height, 3, currentBgColor);
        } else if (hovered) {
            int hoverColor = new Color(80, 120, 200, (int)(100 * animationProgress)).getRGB();
            RenderUtil.drawRoundedRect(x - 0.5f, y - 0.5f, x + width + 0.5f, y + height + 0.5f, 3, hoverColor);
            RenderUtil.drawRoundedRect(x, y, x + width, y + height, 3, currentBgColor);
        }

        String displayText;
        int textColor;

        if (value == null || value.isEmpty()) {
            displayText = placeholder;
            textColor = new Color(120, 120, 130, 180).getRGB();
        } else {
            displayText = value;
            textColor = Color.WHITE.getRGB();
        }

        float textX = x + 8;
        float textY = y + (height - 8) / 2;
        float availableWidth = width - 20;

        String renderText = displayText;
        int scrollOffset = 0;

        if (mc.fontRendererObj.getStringWidth(renderText) > availableWidth) {
            if (focused && cursorPosition > 0 && !value.isEmpty()) {
                while (scrollOffset < cursorPosition &&
                        mc.fontRendererObj.getStringWidth(renderText.substring(scrollOffset, cursorPosition)) > availableWidth - 10) {
                    scrollOffset++;
                }
                renderText = renderText.substring(scrollOffset);
                while (mc.fontRendererObj.getStringWidth(renderText) > availableWidth) {
                    if (renderText.length() > 0) {
                        renderText = renderText.substring(0, renderText.length() - 1);
                    } else {
                        break;
                    }
                }
            } else {
                while (mc.fontRendererObj.getStringWidth(renderText) > availableWidth - 15 && renderText.length() > 0) {
                    renderText = renderText.substring(0, renderText.length() - 1);
                }
                if (renderText.length() < displayText.length()) {
                    renderText = renderText + "...";
                }
            }
        }

        mc.fontRendererObj.drawStringWithShadow(renderText, textX, textY, textColor);

        if (focused && (value != null && !value.isEmpty())) {
            cursorBlink++;
            if (cursorBlink % 40 < 20) {
                int visibleCursorPos = cursorPosition - scrollOffset;
                if (visibleCursorPos >= 0 && visibleCursorPos <= renderText.length()) {
                    String textBeforeCursor = renderText.substring(0, Math.min(visibleCursorPos, renderText.length()));
                    float cursorX = textX + mc.fontRendererObj.getStringWidth(textBeforeCursor);
                    int cursorColor = new Color(200, 200, 200, 255).getRGB();
                    Gui.drawRect((int)cursorX, (int)(y + 6), (int)(cursorX + 1), (int)(y + height - 6), cursorColor);
                }
            }
        } else if (focused && (value == null || value.isEmpty())) {
            cursorBlink++;
            if (cursorBlink % 40 < 20) {
                float cursorX = textX;
                int cursorColor = new Color(200, 200, 200, 255).getRGB();
                Gui.drawRect((int)cursorX, (int)(y + 6), (int)(cursorX + 1), (int)(y + height - 6), cursorColor);
            }
        }

        if (focused && animationProgress > 0.5f) {
            float glowWidth = width * animationProgress;
            int glowColor = new Color(120, 180, 255, (int)(30 * animationProgress)).getRGB();
            RenderUtil.drawRoundedRect(x + (width - glowWidth) / 2, y, x + (width + glowWidth) / 2, y + height, 3, glowColor);
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton, float x, float y, float width, float height) {
        if (mouseButton == 0) {
            boolean wasInBounds = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
            focused = wasInBounds;
            if (focused) {
                cursorBlink = 0;
                cursorPosition = value.length();
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (!focused) return;

        boolean ctrlPressed = GuiScreen.isCtrlKeyDown();

        if (ctrlPressed && keyCode == Keyboard.KEY_C) {
            if (!value.isEmpty()) setClipboard(value);
            return;
        }

        if (ctrlPressed && keyCode == Keyboard.KEY_V) {
            String clipboard = getClipboard();
            if (clipboard != null && !clipboard.isEmpty()) insertString(clipboard);
            return;
        }

        if (ctrlPressed && keyCode == Keyboard.KEY_X) {
            if (!value.isEmpty()) {
                setClipboard(value);
                value = "";
                cursorPosition = 0;
            }
            return;
        }

        if (ctrlPressed && keyCode == Keyboard.KEY_A) {
            cursorPosition = value.length();
            return;
        }

        if (keyCode == Keyboard.KEY_BACK) {
            removeLastChar();
        } else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
            focused = false;
        } else if (keyCode == Keyboard.KEY_LEFT) {
            cursorPosition = Math.max(0, cursorPosition - 1);
        } else if (keyCode == Keyboard.KEY_RIGHT) {
            cursorPosition = Math.min(value.length(), cursorPosition + 1);
        } else if (keyCode == Keyboard.KEY_HOME) {
            cursorPosition = 0;
        } else if (keyCode == Keyboard.KEY_END) {
            cursorPosition = value.length();
        } else if (keyCode == Keyboard.KEY_DELETE) {
            if (cursorPosition < value.length()) {
                String before = value.substring(0, cursorPosition);
                String after = value.substring(cursorPosition + 1);
                value = before + after;
            }
        } else if (Character.isDefined(typedChar) && !Character.isISOControl(typedChar)) {
            insertChar(typedChar);
        }
    }

    private void setClipboard(String text) {
        try {
            GuiScreen.setClipboardString(text);
            return;
        } catch (Exception ignored) {}

        try {
            StringSelection selection = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        } catch (Exception ignored) {}
    }

    private String getClipboard() {
        try {
            String result = GuiScreen.getClipboardString();
            if (result != null) return result;
        } catch (Exception ignored) {}

        try {
            Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (Exception ignored) {}

        return "";
    }

    private void insertString(String text) {
        if (text == null || text.isEmpty()) return;

        StringBuilder filtered = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 32 && c != 127) filtered.append(c);
        }

        String toInsert = filtered.toString();
        if (toInsert.isEmpty()) return;

        int remainingSpace = maxLength - value.length();
        if (remainingSpace <= 0) return;

        if (toInsert.length() > remainingSpace) toInsert = toInsert.substring(0, remainingSpace);

        String before = value.substring(0, cursorPosition);
        String after = value.substring(cursorPosition);
        value = before + toInsert + after;
        cursorPosition += toInsert.length();
    }

    private void insertChar(char c) {
        if (value.length() >= maxLength) return;

        String before = value.substring(0, cursorPosition);
        String after = value.substring(cursorPosition);
        value = before + c + after;
        cursorPosition++;
    }

    private int interpolateColor(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int)(a1 + (a2 - a1) * ratio);
        int r = (int)(r1 + (r2 - r1) * ratio);
        int g = (int)(g1 + (g2 - g1) * ratio);
        int b = (int)(b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public JsonElement serialize() {
        return new JsonPrimitive(value);
    }

    @Override
    public void deserialize(JsonElement element) {
        setValue(element.getAsString());
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (value == null) {
            this.value = "";
            this.cursorPosition = 0;
            return;
        }

        if (value.length() > maxLength) {
            this.value = value.substring(0, maxLength);
        } else {
            this.value = value;
        }
        this.cursorPosition = this.value.length();
    }

    public void append(String text) {
        setValue(this.value + text);
    }

    public void removeLastChar() {
        if (this.value != null && !this.value.isEmpty() && cursorPosition > 0) {
            String before = value.substring(0, cursorPosition - 1);
            String after = value.substring(cursorPosition);
            this.value = before + after;
            cursorPosition--;
        }
    }

    public int getMaxLength() {
        return maxLength;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }
}
