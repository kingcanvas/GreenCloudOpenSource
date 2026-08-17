package greencloudclient.com.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import greencloudclient.com.modules.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class MultiModeSetting extends Setting {
    public final List<String> modes;
    private final Set<String> selectedModes = new LinkedHashSet<>();

    public MultiModeSetting(String name, Module parent, String[] modes, String... defaults) {
        super(name, parent);
        this.modes = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(modes)));
        selectDefaults(defaults);
    }

    public MultiModeSetting(String name, Module parent, Supplier<Boolean> visibility, String[] modes, String... defaults) {
        super(name, parent, visibility);
        this.modes = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(modes)));
        selectDefaults(defaults);
    }

    public boolean isSelected(String mode) {
        String resolved = resolve(mode);
        return resolved != null && selectedModes.contains(resolved);
    }

    public void toggle(String mode) {
        String resolved = resolve(mode);
        if (resolved == null) return;
        if (!selectedModes.remove(resolved)) selectedModes.add(resolved);
    }

    public void setSelected(String mode, boolean selected) {
        String resolved = resolve(mode);
        if (resolved == null) return;
        if (selected) selectedModes.add(resolved);
        else selectedModes.remove(resolved);
    }

    public List<String> getSelectedModes() {
        List<String> selected = new ArrayList<>();
        for (String mode : modes) {
            if (selectedModes.contains(mode)) selected.add(mode);
        }
        return Collections.unmodifiableList(selected);
    }

    public String getDisplayValue() {
        List<String> selected = getSelectedModes();
        if (selected.isEmpty()) return "None";
        if (selected.size() == 1) return selected.get(0);
        return selected.size() + " selected";
    }

    @Override
    public JsonElement serialize() {
        JsonArray values = new JsonArray();
        for (String mode : getSelectedModes()) values.add(new JsonPrimitive(mode));
        return values;
    }

    @Override
    public void deserialize(JsonElement element) {
        selectedModes.clear();
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonArray()) {
            for (JsonElement value : element.getAsJsonArray()) {
                if (value.isJsonPrimitive()) setSelected(value.getAsString(), true);
            }
            return;
        }
        if (element.isJsonPrimitive()) setSelected(element.getAsString(), true);
    }

    private void selectDefaults(String[] defaults) {
        if (defaults == null) return;
        for (String mode : defaults) setSelected(mode, true);
    }

    private String resolve(String mode) {
        if (mode == null) return null;
        for (String allowed : modes) {
            if (allowed.equalsIgnoreCase(mode)) return allowed;
        }
        return null;
    }
}
