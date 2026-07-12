package greencloud.impl.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import greencloud.impl.modules.Module;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ModeSetting extends Setting {

    public String currentMode;
    public List<String> modes;
    public int index;

    public ModeSetting(String name, Module parent, String defaultMode, String... modes) {
        super(name, parent);
        this.modes = Arrays.asList(modes);
        this.currentMode = defaultMode;
        this.index = this.modes.indexOf(defaultMode);
    }

    public ModeSetting(String name, Module parent, String defaultMode, Supplier<Boolean> visibility, String... modes) {
        super(name, parent, visibility);
        this.modes = Arrays.asList(modes);
        this.currentMode = defaultMode;
        this.index = this.modes.indexOf(defaultMode);
    }

    public String getMode() {
        return this.currentMode;
    }

    public void setMode(String mode) {
        this.currentMode = mode;
        this.index = modes.indexOf(mode);
    }

    public void cycle() {
        if (index < modes.size() - 1) {
            index++;
        } else {
            index = 0;
        }
        this.currentMode = modes.get(index);
    }

    public boolean is(String name) {
        return this.currentMode.equalsIgnoreCase(name);
    }

    @Override
    public JsonElement serialize() {
        return new JsonPrimitive(currentMode);
    }

    @Override
    public void deserialize(JsonElement element) {
        String modeName = element.getAsString();
        for (int i = 0; i < modes.size(); i++) {
            if (modes.get(i).equalsIgnoreCase(modeName)) {
                currentMode = modes.get(i);
                index = i;
                return;
            }
        }
    }
}
