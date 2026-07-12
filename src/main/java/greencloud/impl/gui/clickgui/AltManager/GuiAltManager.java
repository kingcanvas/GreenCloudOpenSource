package greencloud.impl.gui.clickgui.AltManager;

import greencloud.GreenCloud;
import greencloud.impl.managers.alt.Alt;
import greencloud.impl.managers.alt.AltManager;
import greencloud.impl.managers.alt.MicrosoftLogin;
import greencloud.impl.managers.alt.TokenLogin;
import greencloud.impl.utils.font.FontUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import greencloud.impl.utils.render.GreenRender;
import greencloud.impl.utils.render.AnimationUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import greencloud.impl.modules.render.HUD;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiAltManager extends GuiScreen {

    private final GuiScreen parent;
    private AltManager altManager;

    private Alt selectedAlt = null;
    private Alt hoveredAlt = null;

    private GuiTextField usernameField;
    private GuiTextField searchField;

    private String status = "Ready";
    private int statusColor = 0xFF888899;

    private float scrollY = 0f;
    private float targetScroll = 0f;
    private float maxScroll = 0f;

    private long lastFrame = System.currentTimeMillis();
    private float animTime = 0f;
    private float fadeIn = 0f;

    private final java.util.Map<Alt, Float> hoverAnims = new java.util.HashMap<>();
    private final java.util.Map<Alt, Long> lastClickTimes = new java.util.HashMap<>();

    private static final int PW = 420;
    private static final int PH_TOP = 55;
    private static final int PH_SEARCH = 38;
    private static final int PH_FOOTER = 120;
    private static final int ITEM_H = 48;
    private static final int ITEM_GAP = 3;
    private static final int SCROLL_SPD = 35;

    private static final int C_SIDEBAR = col(10, 10, 15, 220);
    private static final int C_BG_START = 0xFF0F0F13;
    private static final int C_BG_END = 0xFF14141A;
    private static final int C_ACCENT = 0xFF50C882;
    private static final int C_CARD = col(30, 30, 35, 120);
    private static final int C_GLOW = col(80, 200, 130, 60);
    private static final int C_OUTLINE = col(255, 255, 255, 8);

    public GuiAltManager(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        if (GreenCloud.altManager == null) GreenCloud.altManager = new AltManager();
        altManager = GreenCloud.altManager;
        if (FontUtil.normal == null) FontUtil.bootstrap();
        Keyboard.enableRepeatEvents(true);

        usernameField = new GuiTextField(0, fontRendererObj, 0, 0, 200, 22);
        usernameField.setMaxStringLength(128);
        usernameField.setEnableBackgroundDrawing(false);

        searchField = new GuiTextField(1, fontRendererObj, 0, 0, PW - 30, 22);
        searchField.setEnableBackgroundDrawing(false);
    }

    private int px()       { return width / 2 - PW / 2; }
    private int py()       { return (height - panelH()) / 2; }
    private int panelH()   { return Math.min(height - 40, 520); }

    private int listY()    { return py() + PH_TOP + PH_SEARCH; }
    private int listH()    { return panelH() - PH_TOP - PH_SEARCH - PH_FOOTER; }
    private int footerY()  { return py() + panelH() - PH_FOOTER; }

    @Override
    public void updateScreen() {
        usernameField.updateCursorCounter();
        searchField.updateCursorCounter();
        float diff = targetScroll - scrollY;
        scrollY += diff * 0.22f;
        if (Math.abs(diff) < 0.4f) scrollY = targetScroll;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastFrame) / 1000f, 0.05f);
        lastFrame = now;
        fadeIn = AnimationUtil.moveUD(fadeIn, 1f, 0.1f, 0.02f);
        animTime += dt;

        GreenRender.fillRRGradientV(0, 0, width, height, 0, C_BG_START, C_BG_END);
        
        float sidebarW = 200;

        GreenRender.blur(0, 0, sidebarW, height, 15f);
        GreenRender.fillRR(0, 0, sidebarW, height, 0, C_SIDEBAR);
        drawSidebar(sidebarW, mx, my);

        drawMainContent(sidebarW, mx, my);

        super.drawScreen(mx, my, pt);
    }

    private void drawSidebar(float w, int mx, int my) {
        String user = mc.getSession().getUsername();

        drawSkinHead(user, (int)(w / 2 - 24), 30, 48);
        FontUtil.bold.drawCenteredString(user, w / 2, 85, -1);
        FontUtil.small.drawCenteredString("Active Session", w / 2, 98, 0xFF777777);

        drawRect(20, 120, (int)w - 20, 121, col(255, 255, 255, 8));

        int total = altManager.getAlts().size();
        FontUtil.small.drawString("ACCOUNTS: " + total, 25, 140, 0xFF999999);

        int accent = HUD.getColor();
        FontUtil.small.drawString("QUICK ADD", 25, 180, accent);
        float fieldY = 195;
        GreenRender.fillRR(20, fieldY, w - 40, 24, 4, col(0, 0, 0, 100));
        GreenRender.outlineRR(20, fieldY, w - 40, 24, 4, 1f, usernameField.isFocused() ? accent : C_OUTLINE);
        
        usernameField.xPosition = 26;
        usernameField.yPosition = (int)fieldY + 6;
        usernameField.width = (int)w - 52;
        if (usernameField.getText().isEmpty() && !usernameField.isFocused()) {
            FontUtil.small.drawString("Username...", 28, fieldY + 7, 0xFF555555);
        } else {
            usernameField.drawTextBox();
        }

        drawModernBtn("Add", 20, fieldY + 30, w - 40, 22, mx, my, this::doAdd);

        float botY = height - 70;
        drawModernBtn("Microsoft", 20, botY, w - 40, 22, mx, my, this::doMicrosoft);
        drawModernBtn("Back", 20, botY + 26, w - 40, 22, mx, my, () -> mc.displayGuiScreen(parent));

        if (selectedAlt != null) {
            float previewY = botY - 100;
            drawPlayerPreview((int)w / 2, (int)previewY, 45, selectedAlt.getUsername());
        }
    }

    private void drawMainContent(float sidebarW, int mx, int my) {
        float x = sidebarW + 40;
        float y = 40;
        
        int accent = HUD.getColor();
        FontUtil.large.drawString("GREENCLOUD", x, y, -1);
        FontUtil.normal.drawString("ALT MANAGER", x + FontUtil.large.getStringWidth("GREENCLOUD") + 6, y + 2, accent);

        float listY = y + 40;
        float listH = height - listY - 40;
        float listW = width - sidebarW - 80;

        GreenRender.pushScissor(x - 5, listY, listW + 10, listH);
        
        List<Alt> alts = filtered(searchField.getText());
        float cardW = 160;
        float cardH = 60;
        float gap = 15;
        
        int cols = (int)(listW / (cardW + gap));
        if (cols < 1) cols = 1;

        float cy = listY - scrollY;
        hoveredAlt = null;

        for (int i = 0; i < alts.size(); i++) {
            Alt alt = alts.get(i);
            int row = i / cols;
            int col = i % cols;
            
            float cx = x + col * (cardW + gap);
            float rowY = cy + row * (cardH + gap);

            if (rowY + cardH < listY) continue;
            if (rowY > listY + listH) break;

            boolean hov = mx >= cx && mx <= cx + cardW && my >= rowY && my <= rowY + cardH;
            if (hov) hoveredAlt = alt;

            int bg = (alt == selectedAlt) ? GreenRender.withAlphaARGB(accent, 0.2f) : col(35, 35, 40, 150);
            
            GlStateManager.pushMatrix();
            GreenRender.fillRR(cx, rowY, cardW, cardH, 8, bg);

            drawRoundedHead(alt.getUsername(), (int)cx + 10, (int)rowY + 10, 40, 5);
            FontUtil.normal.drawString(alt.getUsername(), cx + 58, rowY + 15, -1);
            
            String st = alt.getStatus() == Alt.Status.LoggedIn ? "Logged In" : alt.getType().name();
            int stCol = alt.getStatus() == Alt.Status.LoggedIn ? accent : 0xFF777777;
            FontUtil.small.drawString(st, cx + 58, rowY + 30, stCol);

            if (hov) {
                float dx = cx + cardW - 18, dy = rowY + 6;
                boolean hovD = mx >= dx && mx <= dx + 12 && my >= dy && my <= dy + 12;
                FontUtil.small.drawString("x", dx, dy, hovD ? 0xFFFF4444 : 0xFF777777);
            }

            GlStateManager.popMatrix();
        }

        maxScroll = Math.max(0, (float)Math.ceil(alts.size() / (double)cols) * (cardH + gap) - listH);
        GreenRender.popScissor();
    }

    private void drawModernBtn(String text, float x, float y, float w, float h, int mx, int my, Runnable action) {
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int accent = HUD.getColor();
        int bg = hov ? col(255, 255, 255, 15) : col(30, 30, 35, 200);
        GreenRender.fillRR(x, y, w, h, 6, bg);
        GreenRender.outlineRR(x, y, w, h, 6, 1.2f, hov ? accent : C_OUTLINE);
        FontUtil.normal.drawCenteredString(text, x + w / 2f, y + (h - 8) / 2f, hov ? -1 : 0xFFBBBBBB);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        usernameField.mouseClicked(mx, my, btn);
        
        float sidebarW = 200;
        float fieldY = 195;

        if (hoveredAlt != null && btn == 0) {
            float listX = sidebarW + 40;
            float listY = 120;
            float cardW = 160, cardH = 60, gap = 15;
            float listW = width - sidebarW - 80;
            int cols = (int)(listW / (cardW + gap));
            if (cols < 1) cols = 1;
            
            int idx = filtered(searchField.getText()).indexOf(hoveredAlt);
            int row = idx / cols;
            int col = idx % cols;
            float cx = listX + col * (cardW + gap);
            float cy = listY - scrollY + row * (cardH + gap);

            float dx = cx + cardW - 18, dy = cy + 6;
            if (mx >= dx && mx <= dx + 12 && my >= dy && my <= dy + 12) {
                altManager.removeAlt(hoveredAlt);
                if (selectedAlt == hoveredAlt) selectedAlt = null;
                hoveredAlt = null;
                return;
            }

            long now = System.currentTimeMillis();
            if (now - lastClickTimes.getOrDefault(hoveredAlt, 0L) < 300) {
                selectedAlt = hoveredAlt;
                doLogin();
            } else {
                selectedAlt = hoveredAlt;
            }
            lastClickTimes.put(hoveredAlt, now);
            return;
        }

        if (hit(mx, my, 20, (int)fieldY, (int)(sidebarW - 40), 24)) usernameField.setFocused(true);
        if (hit(mx, my, 20, (int)(fieldY + 30), (int)(sidebarW - 40), 22)) doAdd();
        
        float botY = height - 70;
        if (hit(mx, my, 20, (int)botY, (int)(sidebarW - 40), 22)) doMicrosoft();
        if (hit(mx, my, 20, (int)(botY + 26), (int)(sidebarW - 40), 22)) mc.displayGuiScreen(parent);
    }

    private boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x+w && my >= y && my <= y+h;
    }

    private void doAdd() {
        String u = usernameField.getText().trim();
        if (u.isEmpty()) { setStatus("Enter a username", 0xFFFF4444); return; }
        for (Alt a : altManager.getAlts()) {
            if (a.getUsername().equalsIgnoreCase(u)) { setStatus("Already exists", 0xFFFF4444); return; }
        }
        altManager.addAlt(new Alt(u));
        usernameField.setText("");
        setStatus("Added: " + u, C_ACCENT);
    }

    private void doLogin() {
        if (selectedAlt == null) { setStatus("Select an account first", 0xFFFF4444); return; }
        setStatus("Logging in...", 0xFFDDCC55);
        altManager.login(selectedAlt);
    }

    private void doRemove() {
        if (selectedAlt == null) { setStatus("Select an account first", 0xFFFF4444); return; }
        altManager.removeAlt(selectedAlt);
        setStatus("Removed account", 0xFFFF4444);
        selectedAlt = null;
    }

    private void doMicrosoft() {
        setStatus("Opening Microsoft login...", 0xFF9096FF);
        MicrosoftLogin.login(msg -> this.status = msg);
    }

    private void setStatus(String msg, int color) { status = msg; statusColor = color; }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); return; }
        usernameField.textboxKeyTyped(c, key);
        searchField.textboxKeyTyped(c, key);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) targetScroll -= (scroll > 0 ? -1 : 1) * SCROLL_SPD;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        altManager.saveAlts();
    }

    private void rr(int x, int y, int x2, int y2, float r, int color) {
        float cr = (color >> 16 & 255) / 255f;
        float cg = (color >> 8  & 255) / 255f;
        float cb = (color       & 255) / 255f;
        float ca = (color >> 24 & 255) / 255f;
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(cr, cg, cb, ca);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0;   i <= 90;  i += 6) GL11.glVertex2d(x  + r + Math.sin(i * Math.PI/180) * -r, y  + r + Math.cos(i * Math.PI/180) * -r);
        for (int i = 90;  i <= 180; i += 6) GL11.glVertex2d(x  + r + Math.sin(i * Math.PI/180) * -r, y2 - r + Math.cos(i * Math.PI/180) * -r);
        for (int i = 180; i <= 270; i += 6) GL11.glVertex2d(x2 - r + Math.sin(i * Math.PI/180) * -r, y2 - r + Math.cos(i * Math.PI/180) * -r);
        for (int i = 270; i <= 360; i += 6) GL11.glVertex2d(x2 - r + Math.sin(i * Math.PI/180) * -r, y  + r + Math.cos(i * Math.PI/180) * -r);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void border(int x, int y, int x2, int y2, float r, int color) {
        float cr = (color >> 16 & 255) / 255f;
        float cg = (color >> 8  & 255) / 255f;
        float cb = (color       & 255) / 255f;
        float ca = (color >> 24 & 255) / 255f;
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(cr, cg, cb, ca);
        GL11.glLineWidth(1f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0;   i <= 90;  i += 6) GL11.glVertex2d(x  + r + Math.sin(i * Math.PI/180) * -r, y  + r + Math.cos(i * Math.PI/180) * -r);
        for (int i = 90;  i <= 180; i += 6) GL11.glVertex2d(x  + r + Math.sin(i * Math.PI/180) * -r, y2 - r + Math.cos(i * Math.PI/180) * -r);
        for (int i = 180; i <= 270; i += 6) GL11.glVertex2d(x2 - r + Math.sin(i * Math.PI/180) * -r, y2 - r + Math.cos(i * Math.PI/180) * -r);
        for (int i = 270; i <= 360; i += 6) GL11.glVertex2d(x2 - r + Math.sin(i * Math.PI/180) * -r, y  + r + Math.cos(i * Math.PI/180) * -r);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void scissor(int x, int y, int x2, int y2) {
        ScaledResolution sr = new ScaledResolution(mc);
        int f = sr.getScaleFactor();
        GL11.glScissor(x * f, (sr.getScaledHeight() - y2) * f, (x2 - x) * f, (y2 - y) * f);
    }

    private void drawPlayerPreview(int x, int y, int scale, String name) {
        try {
            GlStateManager.enableColorMaterial();
            GlStateManager.pushMatrix();
            GlStateManager.translate((float)x, (float)y, 50.0F);
            GlStateManager.scale((float)(-scale), (float)scale, (float)scale);
            GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
            net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
            GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-((float)Math.atan((double)(animTime / 40.0F))) * 20.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(animTime * 20.0F, 0.0F, 1.0F, 0.0F);

            mc.getRenderManager().playerViewY = 180.0F;
            drawRoundedHead(name, -20, -50, 40, 8);

            GlStateManager.popMatrix();
            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit);
        } catch (Exception e) {}
    }

    private void drawRoundedHead(String username, int x, int y, int size, float radius) {
        GlStateManager.enableBlend();
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glColorMask(false, false, false, false);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);
        GL11.glStencilMask(0xFF);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        
        rr(x, y, x + size, y + size, radius, -1);
        
        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilMask(0x00);
        
        drawSkinHead(username, x, y, size);
        
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private void drawSkinHead(String username, int x, int y, int size) {
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        ResourceLocation loc;
        try {
            loc = AbstractClientPlayer.getLocationSkin(username);
            AbstractClientPlayer.getDownloadImageSkin(loc, username);
        } catch (Exception e) {
            loc = DefaultPlayerSkin.getDefaultSkinLegacy();
        }
        mc.getTextureManager().bindTexture(loc);
        Gui.drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, size, size, 64, 64);
        Gui.drawScaledCustomSizeModalRect(x, y, 40, 8, 8, 8, size, size, 64, 64);
    }

    private List<Alt> filtered(String q) {
        List<Alt> out = new ArrayList<>();
        for (Alt a : altManager.getAlts())
            if (q.isEmpty() || a.getUsername().toLowerCase().contains(q)) out.add(a);
        return out;
    }

    private String truncate(String s, int maxW) {
        if (FontUtil.normal.getStringWidth(s) <= maxW) return s;
        while (s.length() > 0 && FontUtil.normal.getStringWidth(s + "…") > maxW)
            s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private int brighten(int color, float f) {
        Color c = new Color(color);
        return new Color(Math.min(255,(int)(c.getRed()*(1+f))), Math.min(255,(int)(c.getGreen()*(1+f))), Math.min(255,(int)(c.getBlue()*(1+f)))).getRGB();
    }

    private int darken(int color, float f) {
        Color c = new Color(color);
        return new Color((int)(c.getRed()*(1-f)), (int)(c.getGreen()*(1-f)), (int)(c.getBlue()*(1-f))).getRGB();
    }

    private static int col(int r, int g, int b)         { return new Color(r,g,b).getRGB(); }
    private static int col(int r, int g, int b, int a)  { return new Color(r,g,b,a).getRGB(); }
}