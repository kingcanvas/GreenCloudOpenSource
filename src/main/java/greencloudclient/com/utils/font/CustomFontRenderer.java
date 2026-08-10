package greencloudclient.com.utils.font;

import greencloudclient.com.utils.AndroidUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class CustomFontRenderer {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String COLOR_CODE_CHARACTERS = "0123456789abcdefklmnor";
    private static final Color TRANSPARENT_COLOR = new Color(255, 255, 255, 0);
    private static final float SCALE = 0.5f;
    private static final float SCALE_INVERSE = 1 / SCALE;
    private static final char COLOR_INVOKER = '\u00A7';
    private static final int[] COLOR_CODES = new int[32];
    private static final int LATIN_MAX_AMOUNT = 256;
    private static final int MARGIN_WIDTH = 4;
    private static final int MASK = 0xFF;
    private static final int ATLAS_SIZE = 1024;
    
    private final Font font;
    private final boolean antiAlias;
    private final boolean fractionalMetrics;
    private final float fontHeight;
    private final CharData[] defaultCharacters = new CharData[LATIN_MAX_AMOUNT];
    private final CharData[] boldCharacters = new CharData[LATIN_MAX_AMOUNT];
    private int defaultAtlasTexture = -1;
    private int boldAtlasTexture = -1;

    public static boolean isSupported() {
        return !AndroidUtil.isAndroid();
    }
    
    public CustomFontRenderer(Font font, boolean antiAlias, boolean fractionalMetrics) {
        this.font = font;
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        this.fontHeight = (float) (font.getStringBounds(ALPHABET, new FontRenderContext(new AffineTransform(), antiAlias, fractionalMetrics)).getHeight() / 2);
        defaultAtlasTexture = createAtlas(defaultCharacters, Font.PLAIN);
        boldAtlasTexture = createAtlas(boldCharacters, Font.BOLD);
        calculateColorCodes();
    }
    
    public CustomFontRenderer(Font font) {
        this(font, true, true);
    }
    
    private static void calculateColorCodes() {
        for (int i = 0; i < 32; ++i) {
            final int amplifier = (i >> 3 & 1) * 85;
            int red = (i >> 2 & 1) * 170 + amplifier;
            int green = (i >> 1 & 1) * 170 + amplifier;
            int blue = (i & 1) * 170 + amplifier;
            if (i == 6) {
                red += 85;
            }
            if (i >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }
            COLOR_CODES[i] = (red & 255) << 16 | (green & 255) << 8 | blue & 255;
        }
    }
    
    private int createAtlas(CharData[] characters, int style) {
        Font styledFont = this.font.deriveFont(style);
        BufferedImage atlasImage = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D atlasGraphics = (Graphics2D) atlasImage.getGraphics();
        atlasGraphics.setFont(styledFont);
        setRenderHints(atlasGraphics);
        
        int currentX = 0;
        int currentY = 0;
        int maxRowHeight = 0;
        
        BufferedImage fontImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D fontGraphics = (Graphics2D) fontImage.getGraphics();
        FontMetrics fontMetrics = fontGraphics.getFontMetrics(styledFont);
        
        for (int i = 0; i < characters.length; ++i) {
            final char character = (char) i;
            final Rectangle2D charRectangle = fontMetrics.getStringBounds(character + "", fontGraphics);
            int charWidth = MathHelper.ceiling_float_int((float) charRectangle.getWidth()) + MARGIN_WIDTH * 2;
            int charHeight = MathHelper.ceiling_float_int((float) charRectangle.getHeight() + 5);
            
            if (currentX + charWidth > ATLAS_SIZE) {
                currentX = 0;
                currentY += maxRowHeight;
                maxRowHeight = 0;
            }
            
            if (charHeight > maxRowHeight) {
                maxRowHeight = charHeight;
            }
            
            atlasGraphics.setColor(TRANSPARENT_COLOR);
            atlasGraphics.fillRect(currentX, currentY, charWidth, charHeight);
            setRenderHints(atlasGraphics);
            atlasGraphics.drawString(character + "", currentX + MARGIN_WIDTH, currentY + styledFont.getSize());
            
            float u = (float) currentX / ATLAS_SIZE;
            float v = (float) currentY / ATLAS_SIZE;
            float u2 = (float) (currentX + charWidth) / ATLAS_SIZE;
            float v2 = (float) (currentY + charHeight) / ATLAS_SIZE;
            
            characters[i] = new CharData(u, v, u2, v2, charWidth, charHeight);
            
            currentX += charWidth;
        }
        
        atlasGraphics.dispose();
        fontGraphics.dispose();
        
        final int[] pixels = atlasImage.getRGB(0, 0, ATLAS_SIZE, ATLAS_SIZE, new int[ATLAS_SIZE * ATLAS_SIZE], 0, ATLAS_SIZE);
        final ByteBuffer byteBuffer = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE * 4);
        for (int y = 0; y < ATLAS_SIZE; ++y) {
            for (int x = 0; x < ATLAS_SIZE; ++x) {
                final int pixel = pixels[x + y * ATLAS_SIZE];
                byteBuffer.put((byte) ((pixel >> 16) & MASK));
                byteBuffer.put((byte) ((pixel >> 8) & MASK));
                byteBuffer.put((byte) (pixel & MASK));
                byteBuffer.put((byte) ((pixel >> 24) & MASK));
            }
        }
        byteBuffer.flip();
        
        final int texture = GL11.glGenTextures();
        GlStateManager.bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, ATLAS_SIZE, ATLAS_SIZE, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);
        
        return texture;
    }
    
    private void setRenderHints(final Graphics2D graphics) {
        graphics.setColor(Color.WHITE);
        if (antiAlias) {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    }
    
    public void drawString(String text, float x, float y, int color) {
        drawString(text, x, y, color, false);
    }
    
    public void drawCenteredString(String text, float x, float y, int color) {
        float width = getStringWidth(text) / 2f;
        drawString(text, x - width, y, color, false);
    }
    
    public void drawStringWithShadow(String text, float x, float y, int color) {
        drawString(text, x + 0.5f, y + 0.5f, color, true);
        drawString(text, x, y, color, false);
    }
    
    public void drawString(String text, float x, float y, int color, boolean shadow) {
        if (text == null) return;
        
        y += 2;
        CharData[] characterSet = defaultCharacters;
        int boundTexture = defaultAtlasTexture;
        
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glScalef(SCALE, SCALE, SCALE);
        
        x -= MARGIN_WIDTH / SCALE_INVERSE;
        y -= MARGIN_WIDTH / SCALE_INVERSE;
        x *= SCALE_INVERSE;
        y *= SCALE_INVERSE;
        y -= fontHeight / 5;
        
        final int shadowColor = Color.BLACK.getRGB();
        final float startX = x;
        final int length = text.length();
        
        setColor(shadow ? shadowColor : color);
        char previousCharacter = '.';
        
        GlStateManager.bindTexture(boundTexture);
        
        for (int i = 0; i < length; ++i) {
            final char character = text.charAt(i);
            
            try {
                if (character == '\n') {
                    x = startX;
                    y += getHeight() * 2;
                    continue;
                }
                
                if (previousCharacter != COLOR_INVOKER) {
                    if (character == COLOR_INVOKER) {
                        final int index = COLOR_CODE_CHARACTERS.indexOf(text.toLowerCase().charAt(i + 1));
                        if (index < 16) {
                            setColor(shadow ? shadowColor : COLOR_CODES[index]);
                        } else if (index == 17) {
                            characterSet = boldCharacters;
                            boundTexture = boldAtlasTexture;
                            GlStateManager.bindTexture(boundTexture);
                        }
                    } else if (characterSet.length > character) {
                        final CharData charData = characterSet[character];
                        charData.render(x, y);
                        x += charData.width - MARGIN_WIDTH * 2;
                    }
                }
            } catch (Exception exception) {
            }
            previousCharacter = character;
        }
        
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GlStateManager.bindTexture(0);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
    
    public int getStringWidth(String text) {
        if (text == null) return 0;
        
        CharData[] characterSet = defaultCharacters;
        final int length = text.length();
        char previousCharacter = '.';
        float width = 0;
        
        for (int i = 0; i < length; ++i) {
            final char character = text.charAt(i);
            if (previousCharacter != COLOR_INVOKER) {
                if (character == COLOR_INVOKER) {
                    final int index = COLOR_CODE_CHARACTERS.indexOf(text.toLowerCase().charAt(i + 1));
                    if (index < 16) {
                        characterSet = defaultCharacters;
                    } else if (index == 17) {
                        characterSet = boldCharacters;
                    }
                } else if (characterSet.length > character) {
                    width += characterSet[character].width - MARGIN_WIDTH * 2;
                }
            }
            previousCharacter = character;
        }
        
        return (int) (width / 2);
    }
    
    public int getHeight() {
        return (int) fontHeight;
    }
    
    private void setColor(int color) {
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        
        if (alpha == 0) alpha = 1.0F;
        
        GL11.glColor4f(red, green, blue, alpha);
    }
    
    protected static class CharData {
        public float u, v, u2, v2;
        public float width;
        public float height;
        
        public CharData() {
            this.u = 0;
            this.v = 0;
            this.u2 = 0;
            this.v2 = 0;
            this.width = 0;
            this.height = 0;
        }
        
        public CharData(float u, float v, float u2, float v2, float width, float height) {
            this.u = u;
            this.v = v;
            this.u2 = u2;
            this.v2 = v2;
            this.width = width;
            this.height = height;
        }
        
        public void render(final float x, final float y) {
            if (width == 0 || height == 0) {
                return;
            }
            
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(u, v);
            GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(u, v2);
            GL11.glVertex2f(x, y + height);
            GL11.glTexCoord2f(u2, v2);
            GL11.glVertex2f(x + width, y + height);
            GL11.glTexCoord2f(u2, v);
            GL11.glVertex2f(x + width, y);
            GL11.glEnd();
        }
    }
}
