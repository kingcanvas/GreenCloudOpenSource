package greencloud.impl.settings;

import greencloud.impl.modules.Module;
import java.util.ArrayList;
import java.util.List;

public class SettingManager {
    public List<Setting> settings;

    public SettingManager() {
        this.settings = new ArrayList<>();
    }

    public void addSetting(Setting setting) {
        this.settings.add(setting);
    }

    public List<Setting> getSettingsByModule(Module module) {
        List<Setting> settings = new ArrayList<>();
        for (Setting s : this.settings) {
            if (s.parent.equals(module)) {
                settings.add(s);
            }
        }
        return settings;
    }
}