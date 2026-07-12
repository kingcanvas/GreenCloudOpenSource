package greencloud.impl.modules.render;

import greencloud.impl.managers.player.PositionManager;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.utils.render.GreenRender;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;

public class SessionInfo extends Module {

    public final NumberSetting   posX         = new NumberSetting ("X",             this, 10,  0, 2000, 1,  () -> false);
    public final NumberSetting   posY         = new NumberSetting ("Y",             this, 10,  0, 2000, 1,  () -> false);
    private final BooleanSetting blur         = new BooleanSetting("Blur",          this, true);
    private final NumberSetting  blurStrength = new NumberSetting ("Blur Strength", this, 15,  0, 40,   1);
    private final BooleanSetting background   = new BooleanSetting("Background",    this, true);
    private final NumberSetting  bgAlpha      = new NumberSetting ("BG Alpha",      this, 60,  0, 255,  5);

    private boolean draggableRegistered = false;

    private final long sessionStart = System.currentTimeMillis();
    private int kills = 0, wins = 0, games = 0;

    private static final int WIDTH   = 160;
    private static final int PADDING = 8;
    private static final int TITLE_H = 22;
    private static final int ROW_H   = 18;
    private static final int ROWS    = 4;
    private static final int HEIGHT  = TITLE_H + 4 + ROWS * ROW_H + PADDING;
    private static final int CORNER  = 6;

    public SessionInfo() {
        super("SessionInfo", "Displays session statistics.", Category.RENDER);
        addSettings(posX, posY, blur, blurStrength, background, bgAlpha);
    }

    public int getX()      { return (int) posX.value; }
    public int getY()      { return (int) posY.value; }
    public int getWidth()  { return WIDTH; }
    public int getHeight() { return HEIGHT; }
    public void setPosition(int x, int y) { posX.value = x; posY.value = y; }

    public void addKill() { kills++; }
    public void addWin()  { wins++; games++; }
    public void addGame() { games++; }

    @SubscribeEvent
    public void onRender2D(RenderGameOverlayEvent.Text event) {
        ensureDraggableRegistered();

        float x = (float) posX.value;
        float y = (float) posY.value;
        float w = WIDTH;
        float h = HEIGHT;

        if (blur.enabled && blurStrength.value > 0)
            GreenRender.blurRounded(x, y, w, h, (float) blurStrength.value, CORNER);

        if (background.enabled)
            GreenRender.fillRR(x, y, w, h, CORNER, new Color(0, 0, 0, (int) bgAlpha.value));

        int    ac    = HUD.getColor();
        Color  acCol = new Color(ac, true);
        float  lineH = 2.5f;
        float  lineY = y + TITLE_H;
        GreenRender.fillRR(x + PADDING, lineY, w - PADDING * 2, lineH, (int)(lineH / 2), acCol);

        String title = "Session Stats";
        GreenRender.drawStringBold(title, x + (w - GreenRender.strWBold(title)) / 2f, y + 5, Color.WHITE);

        float rowY = lineY + lineH + 4;
        drawRow(x, rowY, w, "Play Time", formatTime());          rowY += ROW_H;
        drawRow(x, rowY, w, "Kills",     String.valueOf(kills)); rowY += ROW_H;
        drawRow(x, rowY, w, "Wins",      String.valueOf(wins));  rowY += ROW_H;
        drawRow(x, rowY, w, "Games",     String.valueOf(games));
    }

    private void drawRow(float px, float ry, float pw, String label, String value) {
        GreenRender.drawString(label, px + PADDING, ry, Color.WHITE);
        GreenRender.drawString(value, px + pw - PADDING - GreenRender.strW(value), ry, Color.WHITE);
    }

    private String formatTime() {
        long elapsed = (System.currentTimeMillis() - sessionStart) / 1000;
        long minutes = elapsed / 60, seconds = elapsed % 60;
        if (minutes >= 60) { long hh = minutes / 60; return hh + "h " + (minutes % 60) + "m"; }
        return minutes + "m " + seconds + "s";
    }

    private void ensureDraggableRegistered() {
        if (draggableRegistered) return;
        draggableRegistered = true;
        PositionManager pm = PositionManager.getInstance();
        if (pm.elements.stream().noneMatch(e -> e.name.equals("SessionInfo")))
            pm.addElement("SessionInfo", getX(), getY(), getWidth(), getHeight(), null);
        pm.elements.stream().filter(e -> e.name.equals("SessionInfo")).findFirst()
                .ifPresent(e -> e.setPositionUpdateCallback((nx, ny) -> setPosition(nx, ny)));
    }
}