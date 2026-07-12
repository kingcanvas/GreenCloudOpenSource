package greencloud.impl.managers.alt;

import greencloud.GreenCloud;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import java.io.IOException;

public class TokenLogin extends GuiScreen {
    private final GuiScreen previousScreen;
    private GuiTextField tokenField;

    public TokenLogin(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        this.buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 96 + 12, "Login & Add"));
        this.buttonList.add(new GuiButton(1, width / 2 - 100, height / 4 + 120 + 12, "Back"));

        this.tokenField = new GuiTextField(2, fontRendererObj, width / 2 - 100, 60, 200, 20);
        this.tokenField.setMaxStringLength(32767);
        this.tokenField.setFocused(true);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            if (!tokenField.getText().isEmpty()) {
                Alt alt = new Alt(tokenField.getText(), Alt.AccountType.TOKEN);
                GreenCloud.altManager.addAlt(alt);
                GreenCloud.altManager.login(alt);
                mc.displayGuiScreen(previousScreen);
            }
        } else if (button.id == 1) {
            mc.displayGuiScreen(previousScreen);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        tokenField.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_RETURN) {
            actionPerformed(buttonList.get(0));
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            actionPerformed(buttonList.get(1));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        tokenField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Token Login", width / 2, 20, 0xFFFFFF);
        drawString(fontRendererObj, "Paste Token / Bearer:", width / 2 - 100, 47, 0xA0A0A0);
        tokenField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
}