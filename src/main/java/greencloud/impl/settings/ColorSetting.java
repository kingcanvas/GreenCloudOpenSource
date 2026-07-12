package greencloud.impl.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import greencloud.impl.modules.Module;
import java.awt.Color;
import java.util.function.Supplier;

public class ColorSetting extends Setting {

    public float hue = 0;
    public float saturation = 1;
    public float brightness = 1;
    private int rgb;

    public ColorSetting(String name, Module parent, Color defaultColor) {
        super(name, parent);
        setColor(defaultColor);
    }

    public ColorSetting(String name, Module parent, Color defaultColor, Supplier<Boolean> visibility) {
        super(name, parent, visibility);
        setColor(defaultColor);
    }

    public int getColor() {
        return rgb;
    }

    public Color getColorObject() {
        return new Color(rgb);
    }

    public void setColor(Color color) {
        this.rgb = color.getRGB();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    public void updateColor() {
        this.rgb = Color.getHSBColor(hue, saturation, brightness).getRGB();
    }

    public void setHue(float h) {
        this.hue = h;
        updateColor();
    }

    public void setSaturation(float s) {
        this.saturation = s;
        updateColor();
    }

    public void setBrightness(float b) {
        this.brightness = b;
        updateColor();
    }

    public void setHSV(float h, float s, float b) {
        this.hue = h;
        this.saturation = s;
        this.brightness = b;
        updateColor();
    }

    @Override
    public JsonElement serialize() {
        return new JsonPrimitive(rgb);
    }

    @Override
    public void deserialize(JsonElement element) {
        setColor(new Color(element.getAsInt()));
    }
}
