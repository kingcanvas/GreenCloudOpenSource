package greencloud.impl.gui.irc;

import greencloud.GreenCloud;
import greencloud.impl.irc.IRCMessage;
import greencloud.impl.utils.render.RenderHelper;
import greencloud.impl.utils.Theme;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

public class IRCGui extends GuiScreen {

    private GuiTextField inputField;
    private int scrollOffset = 0;
    private final Theme theme = Theme.GREENCLOUD;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        int fieldWidth = width - 40;
        int fieldHeight = 20;
        int fieldX = 20;
        int fieldY = height - 40;

        inputField = new GuiTextField(0, fontRendererObj, fieldX, fieldY, fieldWidth, fieldHeight);
        inputField.setMaxStringLength(500);
        inputField.setFocused(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int panelX = 10;
        int panelY = 10;
        int panelWidth = width - 20;
        int panelHeight = height - 70;

        RenderHelper.drawRoundedRect(panelX, panelY, panelWidth, panelHeight, 3, theme.panelColor.getRGB());

        String connectionStatus = "";
        int userCount = 0;
        if (GreenCloud.ircManager != null && GreenCloud.ircManager.isConnected()) {
            connectionStatus = GreenCloud.ircManager.isGuest() ? " §7(Guest)" : " §a(Logged In)";
            userCount = GreenCloud.ircManager.getOnlineUserCount();
        }

        String title = "IRC Chat" + connectionStatus + (userCount > 0 ? " §7- §f" + userCount + " online" : "");
        fontRendererObj.drawStringWithShadow(title, panelX + 10, panelY + 10, theme.accentColor.getRGB());

        if (GreenCloud.ircManager == null || !GreenCloud.ircManager.isConnected()) {
            String error = "Not connected to IRC";
            String hint = "Type #connect in chat to join as guest";
            int errorX = panelX + (panelWidth / 2) - (fontRendererObj.getStringWidth(error) / 2);
            int hintX = panelX + (panelWidth / 2) - (fontRendererObj.getStringWidth(hint) / 2);
            fontRendererObj.drawStringWithShadow(error, errorX, panelY + (panelHeight / 2) - 10, Color.RED.getRGB());
            fontRendererObj.drawStringWithShadow(hint, hintX, panelY + (panelHeight / 2) + 10, Color.GRAY.getRGB());
        } else {
            drawMessages(panelX + 10, panelY + 30, panelWidth - 20, panelHeight - 40);
        }

        inputField.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawMessages(int x, int y, int width, int height) {
        List<IRCMessage> messages = GreenCloud.ircManager.getMessages();

        int messageHeight = 20;
        int maxVisibleMessages = height / messageHeight;
        int startIndex = Math.max(0, messages.size() - maxVisibleMessages - scrollOffset);
        int endIndex = Math.min(messages.size(), startIndex + maxVisibleMessages);

        int currentY = y;

        for (int i = startIndex; i < endIndex; i++) {
            IRCMessage msg = messages.get(i);

            String username = msg.getUsername();
            String content = msg.getContent();
            String time = msg.getFormattedTime();
            boolean isGuest = msg.isGuest();

            int usernameColor = username.equals("System") ? Color.YELLOW.getRGB() :
                    (isGuest ? Color.LIGHT_GRAY.getRGB() : theme.accentColor.getRGB());

            String guestTag = isGuest && !username.equals("System") ? " §7(G)" : "";
            String fullMessage = "§7[" + time + "]§r " + username + guestTag + "§7:§r " + content;

            if (fontRendererObj.getStringWidth(fullMessage) > width) {
                fullMessage = fontRendererObj.trimStringToWidth(fullMessage, width - 10) + "...";
            }

            fontRendererObj.drawString(fullMessage, x, currentY, Color.WHITE.getRGB());
            currentY += messageHeight;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(null);
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                if (text.startsWith("#")) {
                    GreenCloud.commandManager.handleCommand(text);
                } else {
                    if (GreenCloud.ircManager != null && GreenCloud.ircManager.isConnected()) {
                        GreenCloud.ircManager.sendChatMessage(text);
                    }
                }
                inputField.setText("");
            }
            return;
        }

        inputField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        inputField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel != 0) {
            if (wheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset++;
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}