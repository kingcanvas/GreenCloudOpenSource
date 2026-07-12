package greencloud.impl.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import greencloud.impl.modules.Module;
import java.util.function.Supplier;

public class NumberSetting extends Setting {

    public double value;
    public double min, max, increment;
    public boolean isRange;
    public double maxValue;

    public NumberSetting(String name, Module parent, double value, double min, double max, double increment) {
        super(name, parent);
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.isRange = false;
    }

    public NumberSetting(String name, Module parent, double value, double min, double max, double increment, Supplier<Boolean> visibility) {
        super(name, parent, visibility);
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.isRange = false;
    }

    /** Range constructor: value = low handle, maxValue = high handle, min/max = allowed bounds. */
    public NumberSetting(String name, Module parent, double value, double maxValue, double min, double max, double increment, boolean range) {
        super(name, parent);
        this.value = value;
        this.maxValue = maxValue;
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.isRange = true;
    }

    /** Range constructor with visibility. */
    public NumberSetting(String name, Module parent, double value, double maxValue, double min, double max, double increment, boolean range, Supplier<Boolean> visibility) {
        super(name, parent, visibility);
        this.value = value;
        this.maxValue = maxValue;
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.isRange = true;
    }

    public double getValue() {
        return this.value;
    }

    public void setValue(double value) {
        double precision = 1.0 / this.increment;
        double cap = isRange ? maxValue : max;
        this.value = Math.round(Math.max(min, Math.min(cap, value)) * precision) / precision;
    }

    public void setMaxValue(double v) {
        double precision = 1.0 / this.increment;
        this.maxValue = Math.round(Math.max(value, Math.min(max, v)) * precision) / precision;
    }

    public double getRoundedValue() {
        return Math.round(this.value * 100.0) / 100.0;
    }

    public double getRoundedMaxValue() {
        return Math.round(this.maxValue * 100.0) / 100.0;
    }

    @Override
    public JsonElement serialize() {
        if (isRange) {
            JsonObject obj = new JsonObject();
            obj.addProperty("min", value);
            obj.addProperty("max", maxValue);
            return obj;
        }
        return new JsonPrimitive(value);
    }

    @Override
    public void deserialize(JsonElement element) {
        if (isRange && element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            value = obj.get("min").getAsDouble();
            maxValue = obj.get("max").getAsDouble();
        } else {
            value = element.getAsDouble();
        }
    }
}
