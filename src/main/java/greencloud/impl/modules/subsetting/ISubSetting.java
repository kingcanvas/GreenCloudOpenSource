package greencloud.impl.modules.subsetting;

import greencloud.impl.settings.Setting;
import java.util.List;

public interface ISubSetting {
    String getName();
    List<Setting> getSettings();
    void onEnable();
    void onUpdate();
    void onDisable();
}
