package greencloudclient.com.gui.loading;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.fml.client.SplashProgress;
import net.minecraftforge.fml.common.ProgressManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

public final class ForgeSplashLoadingScreen {

    private static final String[][] DEVELOPERS = {
            {"Founder", "kingcanvas (_kingdev.)"},
            {"Founder", "SparkyEclipseXD"},
            {"Founder", "aslam.xyz"},
            {"Lead Developer", "clilys"},
            {"Developer", "qloha"},
            {"Developer", "ozzi.one (Criminal Cat)"}
    };

    private static final int IMAGE_SIZE = 1254;
    private static final float IMAGE_CROP = 220.0f / IMAGE_SIZE;
    private static final float IMAGE_CROP_END = 1034.0f / IMAGE_SIZE;
    private static final Field DONE_FIELD = field("done");
    private static final Field PAUSE_FIELD = field("pause");
    private static final Field FONT_RENDERER_FIELD = field("fontRenderer");
    private static final Field MUTEX_FIELD = field("mutex");
    private static final OverallLoadingProgress OVERALL_PROGRESS = new OverallLoadingProgress();
    private static int logoTexture;
    private static int creditsTexture;
    private static int creditsWidth;
    private static int creditsHeight;
    private static float displayedProgress;
    private static String progressStage = "";
    private static int renderedFrames;
    private static Thread renderThread;
    private static boolean active;

    private ForgeSplashLoadingScreen() {}

    public static void run(Object splashRenderer) {
        start();
        try {
            while (!isDone()) {
                renderFrame();
                mutex().acquireUninterruptibly();
                try {
                    Display.update();
                } finally {
                    mutex().release();
                }
                if (isPaused()) {
                    invoke(splashRenderer, "clearGL");
                    invoke(splashRenderer, "setGL");
                }
                Display.sync(100);
            }
        } finally {
            finish();
        }
    }

    public static synchronized boolean start() {
        if (active) return true;
        try {
            logoTexture = uploadTexture(loadImage("/assets/greencloudclient/textures/gui/loading_logo.png"));
            if (logoTexture == 0) throw new IllegalStateException("Loading logo texture could not be created");
            BufferedImage credits = createCreditsImage();
            creditsTexture = uploadTexture(credits);
            creditsWidth = credits.getWidth();
            creditsHeight = credits.getHeight();
            displayedProgress = 0.0f;
            progressStage = "";
            renderedFrames = 0;
            renderThread = Thread.currentThread();
            OVERALL_PROGRESS.reset();
            active = true;
            present();
            System.out.println("[GreenCloud] Main-context startup loading screen active");
            return true;
        } catch (Throwable throwable) {
            cleanup();
            System.err.println("[GreenCloud] Falling back to Forge splash renderer: " + throwable);
            return false;
        }
    }

    public static synchronized void onProgress() {
        if (!active || Thread.currentThread() != renderThread) return;
        displayedProgress = OVERALL_PROGRESS.update();
        try {
            present();
        } catch (Throwable throwable) {
            System.err.println("[GreenCloud] Loading frame failed: " + throwable);
            cleanup();
        }
    }

    public static void renderFrame() {
        int width = Math.max(1, Display.getWidth());
        int height = Math.max(1, Display.getHeight());
        GL11.glClearColor(0.012f, 0.01f, 0.02f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glViewport(0, 0, width, height);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, width, height, 0.0D, -1.0D, 1.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        fillRect(0.0f, 0.0f, width, height, 0x05040A, 1.0f);

        int leftWidth = Math.max(1, Math.round(width * 0.58f));
        int logoSize = Math.max(160, Math.min(Math.round(height * 0.56f), Math.round(width * 0.38f)));
        int logoX = (leftWidth - logoSize) / 2;
        int logoY = Math.max(24, (height - logoSize) / 2 - Math.max(14, height / 28));
        drawLogo(logoX, logoY, logoSize);

        int barWidth = Math.max(220, Math.min(520, Math.round(leftWidth * 0.64f)));
        int barHeight = Math.max(7, height / 90);
        int barX = (leftWidth - barWidth) / 2;
        int barY = Math.min(height - 64, logoY + logoSize + Math.max(14, height / 45));
        drawProgressBar(barX, barY, barWidth, barHeight, displayedProgress);

        drawCredits(width, height, leftWidth);

        int separatorTop = Math.max(32, height / 8);
        fillRect(leftWidth, separatorTop, leftWidth + Math.max(1, width / 1200), height - separatorTop, 0x4B315D, 0.7f);
        renderedFrames++;
    }

    public static synchronized void finish() {
        if (!active || Thread.currentThread() != renderThread) return;
        try {
            displayedProgress = 1.0f;
            present();
            GL11.glFinish();
            System.out.println("[GreenCloud] Main-context startup loading screen finished after " + renderedFrames + " frames");
        } catch (Throwable throwable) {
            System.err.println("[GreenCloud] Final loading frame failed: " + throwable);
        } finally {
            cleanup();
        }
    }

    public static synchronized boolean isActive() {
        return active;
    }

    public static boolean isDone() {
        return getBoolean(DONE_FIELD);
    }

    public static boolean isPaused() {
        return getBoolean(PAUSE_FIELD);
    }

    public static Semaphore mutex() {
        try {
            return (Semaphore) MUTEX_FIELD.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void present() {
        if (!Display.isCreated()) return;
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
            renderFrame();
            Display.update();
        } finally {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(previousMatrixMode);
            GL11.glPopAttrib();
        }
    }

    private static void updateProgress(ProgressState state) {
        if (!state.stage.equals(progressStage)) {
            progressStage = state.stage;
            displayedProgress = Math.min(displayedProgress, state.progress);
        }
        float difference = state.progress - displayedProgress;
        displayedProgress += difference * (difference < 0.0f ? 0.35f : 0.13f);
        if (state.progress >= 0.999f) displayedProgress = 1.0f;
        displayedProgress = Math.max(0.0f, Math.min(1.0f, displayedProgress));
    }

    private static ProgressState currentProgress() {
        ProgressManager.ProgressBar current = null;
        Iterator<ProgressManager.ProgressBar> bars = ProgressManager.barIterator();
        if (bars.hasNext()) current = bars.next();
        if (current == null) return new ProgressState("Starting", "Starting Minecraft", 0.0f);
        int steps = Math.max(1, current.getSteps());
        float progress = Math.max(0.0f, Math.min(1.0f, current.getStep() / (float) steps));
        String title = current.getTitle() == null ? "Loading" : current.getTitle();
        String message = current.getMessage() == null || current.getMessage().trim().isEmpty() ? title : current.getMessage();
        return new ProgressState(title, message, progress);
    }

    private static void drawLogo(int x, int y, int size) {
        if (logoTexture == 0) return;
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, logoTexture);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_QUADS);
        int cells = 16;
        for (int row = 0; row < cells; row++) {
            float top = row / (float) cells;
            float bottom = (row + 1) / (float) cells;
            for (int column = 0; column < cells; column++) {
                float left = column / (float) cells;
                float right = (column + 1) / (float) cells;
                logoVertex(x, y, size, left, top);
                logoVertex(x, y, size, left, bottom);
                logoVertex(x, y, size, right, bottom);
                logoVertex(x, y, size, right, top);
            }
        }
        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private static void logoVertex(int x, int y, int size, float horizontal, float vertical) {
        float edge = Math.min(Math.min(horizontal, 1.0f - horizontal), Math.min(vertical, 1.0f - vertical));
        float alpha = Math.max(0.0f, Math.min(1.0f, edge / 0.16f));
        alpha = alpha * alpha * (3.0f - 2.0f * alpha);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, alpha);
        GL11.glTexCoord2f(
                IMAGE_CROP + (IMAGE_CROP_END - IMAGE_CROP) * horizontal,
                IMAGE_CROP + (IMAGE_CROP_END - IMAGE_CROP) * vertical
        );
        GL11.glVertex2f(x + size * horizontal, y + size * vertical);
    }

    private static void drawProgressBar(int x, int y, int width, int height, float progress) {
        fillRect(x - 3, y - 3, x + width + 3, y + height + 3, 0x261B31, 1.0f);
        fillRect(x, y, x + width, y + height, 0x100D16, 1.0f);
        int fill = Math.max(0, Math.min(width, Math.round(width * progress)));
        int segments = Math.min(fill, 96);
        for (int i = 0; i < segments; i++) {
            int start = x + fill * i / segments;
            int end = x + fill * (i + 1) / segments;
            float mix = segments <= 1 ? 1.0f : i / (float) (segments - 1);
            fillRect(start, y, Math.max(start + 1, end), y + height, interpolate(0x8658FF, 0xFF62D4, mix), 1.0f);
        }
    }

    private static void drawCredits(int width, int height, int leftWidth) {
        if (creditsTexture == 0 || creditsWidth <= 0 || creditsHeight <= 0) return;
        float panelX = leftWidth + Math.max(26, (width - leftWidth) / 12.0f);
        float availableWidth = Math.max(1.0f, width - panelX - Math.max(18.0f, width / 70.0f));
        float drawWidth = Math.min(creditsWidth, availableWidth);
        float drawHeight = creditsHeight * drawWidth / creditsWidth;
        float maxHeight = height - Math.max(56.0f, height / 9.0f);
        if (drawHeight > maxHeight) {
            drawHeight = maxHeight;
            drawWidth = creditsWidth * drawHeight / creditsHeight;
        }
        float y = Math.max(28.0f, (height - drawHeight) / 2.0f);
        drawTexture(creditsTexture, panelX, y, drawWidth, drawHeight, 0.0f, 0.0f, 1.0f, 1.0f);
    }

    private static void drawTexture(int texture, float x, float y, float width, float height,
                                    float u0, float v0, float u1, float v1) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(u0, v0);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(u0, v1);
        GL11.glVertex2f(x, y + height);
        GL11.glTexCoord2f(u1, v1);
        GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(u1, v0);
        GL11.glVertex2f(x + width, y);
        GL11.glEnd();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private static void fillRect(float left, float top, float right, float bottom, int color, float alpha) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(((color >> 16) & 255) / 255.0f, ((color >> 8) & 255) / 255.0f, (color & 255) / 255.0f, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(left, top);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glEnd();
    }

    private static BufferedImage loadImage(String path) throws Exception {
        try (InputStream stream = ForgeSplashLoadingScreen.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing loading asset " + path);
            BufferedImage image = ImageIO.read(stream);
            if (image == null) throw new IllegalStateException("Could not decode loading asset " + path);
            return image;
        }
    }

    private static BufferedImage createCreditsImage() {
        int displayWidth = Math.max(640, Display.getWidth());
        int displayHeight = Math.max(360, Display.getHeight());
        int targetWidth = Math.max(260, Math.round(displayWidth * 0.34f));
        int fontSize = Math.max(15, Math.min(25, displayHeight / 34));
        Font bodyFont = new Font("SansSerif", Font.PLAIN, fontSize);
        Font sectionFont = new Font("SansSerif", Font.BOLD, fontSize);
        Font titleFont = new Font("SansSerif", Font.BOLD, fontSize + 5);
        BufferedImage measureImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measure = measureImage.createGraphics();
        while (fontSize > 13 && widestCreditsLine(measure, bodyFont, sectionFont, titleFont) > targetWidth - 4) {
            fontSize--;
            bodyFont = new Font("SansSerif", Font.PLAIN, fontSize);
            sectionFont = new Font("SansSerif", Font.BOLD, fontSize);
            titleFont = new Font("SansSerif", Font.BOLD, fontSize + 5);
        }
        FontMetrics bodyMetrics = measure.getFontMetrics(bodyFont);
        FontMetrics sectionMetrics = measure.getFontMetrics(sectionFont);
        FontMetrics titleMetrics = measure.getFontMetrics(titleFont);
        int lineHeight = bodyMetrics.getHeight() + Math.max(3, fontSize / 5);
        int headingGap = Math.max(10, fontSize / 2);
        int imageHeight = Math.max(1, 3 + headingGap + titleMetrics.getHeight() + headingGap
                + sectionMetrics.getHeight() + Math.max(4, fontSize / 4)
                + DEVELOPERS.length * lineHeight + headingGap
                + sectionMetrics.getHeight() + Math.max(4, fontSize / 4) + bodyMetrics.getHeight());
        measure.dispose();

        BufferedImage image = new BufferedImage(targetWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(new Color(0x55D98A));
        graphics.fillRect(0, 0, Math.max(52, targetWidth / 4), Math.max(2, fontSize / 9));
        int y = 3 + headingGap;
        graphics.setFont(titleFont);
        y += titleMetrics.getAscent();
        graphics.setColor(new Color(0x55D98A));
        graphics.drawString("Green", 0, y);
        int greenWidth = titleMetrics.stringWidth("Green");
        graphics.setColor(new Color(0xECE8F2));
        graphics.drawString("Cloud", greenWidth, y);
        y += titleMetrics.getDescent() + headingGap + sectionMetrics.getAscent();
        graphics.setFont(sectionFont);
        graphics.setColor(new Color(0x9B7BFF));
        graphics.drawString("Client Developers", 0, y);
        y += sectionMetrics.getDescent() + Math.max(4, fontSize / 4) + bodyMetrics.getAscent();
        graphics.setFont(bodyFont);
        for (String[] developer : DEVELOPERS) {
            String role = developer[0];
            graphics.setColor(new Color(0xC6A8FF));
            graphics.drawString(role, 0, y);
            graphics.setColor(new Color(0xECE8F2));
            graphics.drawString(" - " + developer[1], bodyMetrics.stringWidth(role), y);
            y += lineHeight;
        }
        y += headingGap - lineHeight + sectionMetrics.getAscent();
        graphics.setFont(sectionFont);
        graphics.setColor(new Color(0x9B7BFF));
        graphics.drawString("Actively Developing the Client", 0, y);
        y += sectionMetrics.getDescent() + Math.max(4, fontSize / 4) + bodyMetrics.getAscent();
        graphics.setFont(bodyFont);
        graphics.setColor(new Color(0xECE8F2));
        graphics.drawString("kingcanvas (_kingdev.)", 0, y);
        graphics.dispose();
        return image;
    }

    private static int widestCreditsLine(Graphics2D graphics, Font bodyFont, Font sectionFont, Font titleFont) {
        FontMetrics body = graphics.getFontMetrics(bodyFont);
        FontMetrics section = graphics.getFontMetrics(sectionFont);
        FontMetrics title = graphics.getFontMetrics(titleFont);
        int width = title.stringWidth("GreenCloud");
        width = Math.max(width, section.stringWidth("Client Developers"));
        width = Math.max(width, section.stringWidth("Actively Developing the Client"));
        width = Math.max(width, body.stringWidth("kingcanvas (_kingdev.)"));
        for (String[] developer : DEVELOPERS) {
            width = Math.max(width, body.stringWidth(developer[0] + " - " + developer[1]));
        }
        return width;
    }

    private static int uploadTexture(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int argb : pixels) {
            buffer.put((byte) ((argb >> 16) & 255));
            buffer.put((byte) ((argb >> 8) & 255));
            buffer.put((byte) (argb & 255));
            buffer.put((byte) ((argb >> 24) & 255));
        }
        buffer.flip();
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texture;
    }

    private static void cleanup() {
        if (logoTexture != 0 && Display.isCreated()) GL11.glDeleteTextures(logoTexture);
        if (creditsTexture != 0 && Display.isCreated()) GL11.glDeleteTextures(creditsTexture);
        logoTexture = 0;
        creditsTexture = 0;
        creditsWidth = 0;
        creditsHeight = 0;
        displayedProgress = 0.0f;
        progressStage = "";
        OVERALL_PROGRESS.reset();
        renderThread = null;
        active = false;
    }

    private static boolean getBoolean(Field field) {
        try {
            return field.getBoolean(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field field(String name) {
        try {
            Field field = SplashProgress.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void invoke(Object instance, String name) {
        try {
            java.lang.reflect.Method method = instance.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(instance);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int interpolate(int start, int end, float amount) {
        int r = Math.round(((start >> 16) & 255) + (((end >> 16) & 255) - ((start >> 16) & 255)) * amount);
        int g = Math.round(((start >> 8) & 255) + (((end >> 8) & 255) - ((start >> 8) & 255)) * amount);
        int b = Math.round((start & 255) + ((end & 255) - (start & 255)) * amount);
        return r << 16 | g << 8 | b;
    }

    private static final class ProgressState {
        private final String stage;
        private final String message;
        private final float progress;

        private ProgressState(String stage, String message, float progress) {
            this.stage = stage;
            this.message = message;
            this.progress = progress;
        }
    }
}
