package greencloud.impl.settings;

import com.google.gson.JsonElement;
import greencloud.impl.modules.Module;
import java.util.function.Supplier;

public abstract class Setting {
    public String name;
    public Module parent;
    private final Supplier<Boolean> visibility;

    public Setting(String name, Module parent) {
        this.name = name;
        this.parent = parent;
        this.visibility = () -> true;
    }

    public Setting(String name, Module parent, Supplier<Boolean> visibility) {
        this.name = name;
        this.parent = parent;
        this.visibility = (visibility != null) ? visibility : () -> true;
    }

    public boolean isVisible() {
        return this.visibility.get();
    }

    public abstract JsonElement serialize();

    public abstract void deserialize(JsonElement element);
}
