package greencloud.impl.modules.subsetting.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;

public class SubSettingSerializer {

    public static JsonObject serialize(ISubSetting subSetting) {
        JsonObject obj = new JsonObject();
        JsonArray settings = new JsonArray();
        for (Setting s : subSetting.getSettings()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", s.name);
            entry.add("value", s.serialize());
            settings.add(entry);
        }
        obj.add("settings", settings);
        return obj;
    }

    public static void deserialize(ISubSetting subSetting, JsonObject obj) {
        if (!obj.has("settings")) return;
        for (JsonElement e : obj.getAsJsonArray("settings")) {
            JsonObject entry = e.getAsJsonObject();
            String name = entry.get("name").getAsString();
            subSetting.getSettings().stream()
                .filter(s -> s.name.equalsIgnoreCase(name))
                .findFirst()
                .ifPresent(s -> s.deserialize(entry.get("value")));
        }
    }
}
