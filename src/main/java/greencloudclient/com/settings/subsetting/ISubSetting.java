package greencloudclient.com.settings.subsetting;

import greencloudclient.com.settings.Setting;
import java.util.List;

public interface ISubSetting {
    String getName();
    List<Setting> getSettings();
    void onEnable();
    void onUpdate();
    void onDisable();
}
