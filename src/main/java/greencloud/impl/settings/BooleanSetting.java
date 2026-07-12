package greencloud.impl.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import greencloud.impl.modules.Module;
import java.util.function.Supplier;

public class BooleanSetting extends Setting {
    public boolean enabled;

    public BooleanSetting(String name, Module parent, boolean enabled) {
        super(name, parent);
        this.enabled = enabled;
    }

    public BooleanSetting(String name, Module parent, boolean enabled, Supplier<Boolean> visibility) {
        super(name, parent, visibility);
        this.enabled = enabled;
    }

    public void toggle() {
        this.enabled = !this.enabled;
    }

    @Override
    public JsonElement serialize() {
        return new JsonPrimitive(enabled);
    }

    @Override
    public void deserialize(JsonElement element) {
        enabled = element.getAsBoolean();
    }
}
