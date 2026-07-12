package greencloud.impl.gui.supabase;

import greencloud.impl.managers.io.SupabaseManager;
import greencloud.impl.utils.HWIDUtil;
import greencloud.impl.utils.font.FontUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;

public class GuiLogin extends GuiScreen {
    private final boolean enableGuestMode = true;

    private GuiTextField emailField;
    private GuiTextField passwordField;

    private String status = "";
    private int statusColor = 0xFFAAAAAA;

    private float fadeIn = 0f;
    private float particleTime = 0f;
    private long lastFrame = System.currentTimeMillis();

    private final int ACCENT_GREEN = new Color(0, 255, 120).getRGB();
    private final int ACCENT_CYAN = new Color(0, 200, 255).getRGB();
    private final int PANEL_BG = new Color(12, 12, 16, 250).getRGB();
    private final int FIELD_BG = new Color(16, 16, 20).getRGB();
    private final int TEXT_GREY = new Color(100, 100, 100).getRGB();

    @Override
    public void initGui() {
        if (FontUtil.normal == null) FontUtil.bootstrap();

        emailField = new GuiTextField(0, fontRendererObj, 0, 0, 270, 32);
        emailField.setFocused(true);
        emailField.setMaxStringLength(64);
        emailField.setEnableBackgroundDrawing(false);

        passwordField = new GuiTextField(1, fontRendererObj, 0, 0, 270, 32);
        passwordField.setMaxStringLength(64);
        passwordField.setEnableBackgroundDrawing(false);

        Keyboard.enableRepeatEvents(true);
        updateFieldPositions();
    }

    private void updateFieldPositions() {
        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;
        int panelY = centerY - 160;
        int fieldStartY = panelY + 75;

        emailField.xPosition = centerX - 135;
        emailField.yPosition = fieldStartY + 8;
        emailField.width = 270;
        emailField.height = 32;

        passwordField.xPosition = centerX - 135;
        passwordField.yPosition = (fieldStartY + 65) + 8;
        passwordField.width = 270;
        passwordField.height = 32;
    }

    @Override
    public void updateScreen() {
        emailField.updateCursorCounter();
        passwordField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (SupabaseManager.isLoggedIn) {
            mc.displayGuiScreen(new GuiMainMenu());
            return;
        }

        updateFieldPositions();

        long now = System.currentTimeMillis();
        float delta = (now - lastFrame) / 1000f;
        lastFrame = now;
        fadeIn = Math.min(1f, fadeIn + delta * 2f);
        particleTime += delta;

        drawGradientRect(0, 0, width, height, new Color(5, 5, 5).getRGB(), new Color(10, 10, 12).getRGB());
        drawParticles();

        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;

        int panelWidth = 340;
        int panelHeight = enableGuestMode ? 320 : 280;
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - 160;

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1 - fadeIn) * -20, 0);
        GlStateManager.color(1f, 1f, 1f, fadeIn);

        drawRoundedRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 6, PANEL_BG);
        drawGradientBar(panelX, panelY, panelWidth, 3);
        FontUtil.large.drawCenteredString("GreenCloud", centerX, panelY + 25, ACCENT_GREEN);

        int fieldStartY = panelY + 75;
        drawModernField(emailField, "Email", centerX, fieldStartY);
        drawModernField(passwordField, "Password", centerX, fieldStartY + 65);

        int btnY = fieldStartY + 125;
        int btnW = 280;
        int btnH = 28;

        drawRoundedButton("Login", centerX, btnY, btnW, btnH, ACCENT_GREEN, new Color(0, 200, 100).getRGB(), mouseX, mouseY);
        drawRoundedButton("Copy Hwid", centerX, btnY + 35, btnW, btnH, new Color(40, 40, 45).getRGB(), new Color(50, 50, 55).getRGB(), mouseX, mouseY);

        if (enableGuestMode) {
            drawRoundedButton("Continue as Guest", centerX, btnY + 70, btnW, btnH, new Color(40, 40, 45).getRGB(), new Color(50, 50, 55).getRGB(), mouseX, mouseY);
        }

        GlStateManager.popMatrix();

        if (!status.isEmpty()) {
            FontUtil.normal.drawCenteredString(status, centerX, panelY + panelHeight - 15, statusColor);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawModernField(GuiTextField field, String label, int centerX, int y) {
        FontUtil.small.drawString(label, centerX - 140, y - 10, ACCENT_GREEN);
        int boxX = centerX - 140, boxY = y, boxW = 280, boxH = 30;
        boolean focused = field.isFocused();
        int borderColor = focused ? ACCENT_GREEN : new Color(60, 60, 65).getRGB();
        drawRoundedRect(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 4, borderColor);
        drawRoundedRect(boxX, boxY, boxX + boxW, boxY + boxH, 3.5f, FIELD_BG);
        String text = field.getText();
        if (text.isEmpty() && !focused) {
            FontUtil.normal.drawString("Enter your " + label.toLowerCase(), field.xPosition, field.yPosition + 3, TEXT_GREY);
        } else {
            String display = label.equals("Password") ? text.replaceAll(".", "*") : text;
            FontUtil.normal.drawString(display, field.xPosition, field.yPosition + 3, focused ? 0xFFFFFFFF : 0xFFAAAAAA);
            if (focused && (particleTime % 1.0f > 0.5f)) {
                float width = FontUtil.normal.getStringWidth(display);
                Gui.drawRect(field.xPosition + (int)width + 1, field.yPosition + 2, field.xPosition + (int)width + 2, field.yPosition + 12, ACCENT_GREEN);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        updateFieldPositions();
        emailField.mouseClicked(mouseX, mouseY, mouseButton);
        passwordField.mouseClicked(mouseX, mouseY, mouseButton);
        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2, centerY = sr.getScaledHeight() / 2, panelY = centerY - 160, fieldStartY = panelY + 75, btnY = fieldStartY + 125;
        if (isHovered(centerX - 140, btnY, 280, 28, mouseX, mouseY)) doLogin();
        if (isHovered(centerX - 140, btnY + 35, 280, 28, mouseX, mouseY)) {
            String hwid = HWIDUtil.getHWID();
            StringSelection selection = new StringSelection(hwid);
            try { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection); } catch (Exception ignored) {}
            status = "HWID Copied!"; statusColor = ACCENT_GREEN;
        }
        if (enableGuestMode && isHovered(centerX - 140, btnY + 70, 280, 28, mouseX, mouseY)) SupabaseManager.loginAsGuest();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_TAB) { emailField.setFocused(!emailField.isFocused()); passwordField.setFocused(!passwordField.isFocused()); }
        if (keyCode == Keyboard.KEY_RETURN) doLogin();
        emailField.textboxKeyTyped(typedChar, keyCode);
        passwordField.textboxKeyTyped(typedChar, keyCode);
    }

    private void drawGradientBar(int x, int y, int w, int h) { drawHorizontalGradient(x, y, w, h, ACCENT_GREEN, ACCENT_CYAN); }
    private void drawRoundedButton(String text, int centerX, int y, int w, int h, int color, int hoverColor, int mx, int my) {
        int x = centerX - w / 2; boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + h;
        drawRoundedRect(x, y, x + w, y + h, 4, hovered ? hoverColor : color);
        FontUtil.normal.drawCenteredString(text, centerX, y + (h / 2) - 3, -1);
    }

    private void drawHorizontalGradient(float x, float y, float width, float height, int leftColor, int rightColor) {
        float f = (float)(leftColor >> 24 & 255) / 255.0F, f1 = (float)(leftColor >> 16 & 255) / 255.0F, f2 = (float)(leftColor >> 8 & 255) / 255.0F, f3 = (float)(leftColor & 255) / 255.0F;
        float f4 = (float)(rightColor >> 24 & 255) / 255.0F, f5 = (float)(rightColor >> 16 & 255) / 255.0F, f6 = (float)(rightColor >> 8 & 255) / 255.0F, f7 = (float)(rightColor & 255) / 255.0F;
        GlStateManager.disableTexture2D(); GlStateManager.enableBlend(); GlStateManager.disableAlpha(); GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); GlStateManager.shadeModel(7425);
        GL11.glBegin(GL11.GL_QUADS); GL11.glColor4f(f1, f2, f3, f); GL11.glVertex2f(x, y); GL11.glVertex2f(x, y + height);
        GL11.glColor4f(f5, f6, f7, f4); GL11.glVertex2f(x + width, y + height); GL11.glVertex2f(x + width, y); GL11.glEnd();
        GlStateManager.shadeModel(7424); GlStateManager.disableBlend(); GlStateManager.enableAlpha(); GlStateManager.enableTexture2D();
    }

    public static void drawRoundedRect(float x, float y, float x2, float y2, float round, int color) {
        float r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F, a = (color >> 24 & 255) / 255.0F;
        GlStateManager.pushMatrix(); GlStateManager.enableBlend(); GlStateManager.disableTexture2D(); GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); GlStateManager.color(r, g, b, a);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i <= 90; i += 3) GL11.glVertex2d(x + round + (Math.sin((i * Math.PI / 180)) * -1 * round), y + round + (Math.cos((i * Math.PI / 180)) * -1 * round));
        for (int i = 90; i <= 180; i += 3) GL11.glVertex2d(x + round + (Math.sin((i * Math.PI / 180)) * -1 * round), y2 - round + (Math.cos((i * Math.PI / 180)) * -1 * round));
        for (int i = 180; i <= 270; i += 3) GL11.glVertex2d(x2 - round + (Math.sin((i * Math.PI / 180)) * -1 * round), y2 - round + (Math.cos((i * Math.PI / 180)) * -1 * round));
        for (int i = 270; i <= 360; i += 3) GL11.glVertex2d(x2 - round + (Math.sin((i * Math.PI / 180)) * -1 * round), y + round + (Math.cos((i * Math.PI / 180)) * -1 * round));
        GL11.glEnd(); GlStateManager.enableTexture2D(); GlStateManager.disableBlend(); GlStateManager.popMatrix();
    }

    private void drawParticles() {
        GlStateManager.pushMatrix(); GlStateManager.enableBlend();
        for (int i = 0; i < 30; i++) {
            float x = (float)((Math.sin(particleTime * 0.2 + i) * 0.5 + 0.5) * width), y = (float)(((particleTime * 20 + i * 50) % (height + 50)) - 20);
            Gui.drawRect((int)x, (int)y, (int)(x + 1 + (i % 3)), (int)(y + 1 + (i % 3)), new Color(0, 255, 120, (int)(50 + Math.sin(particleTime + i) * 30)).getRGB());
        }
        GlStateManager.popMatrix();
    }

    private void doLogin() {
        if (emailField.getText().isEmpty() || passwordField.getText().isEmpty()) { status = "Credentials Missing"; statusColor = 0xFFFF5555; return; }
        status = "Verifying..."; statusColor = 0xFFFFFF55;
        SupabaseManager.login(emailField.getText(), passwordField.getText(), () -> { status = "Success!"; statusColor = ACCENT_GREEN; }, () -> { status = "Access Denied"; statusColor = 0xFFFF5555; });
    }

    private boolean isHovered(int x, int y, int w, int h, int mx, int my) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    @Override
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
}