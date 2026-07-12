package greencloud.impl.modules;

import greencloud.GreenCloud;
import greencloud.impl.managers.notification.NotificationManager;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class Module {

    protected final Minecraft mc = Minecraft.getMinecraft();

    private final String name;
    private final String description;
    private final Category category;

    private int keyCode;
    private boolean toggled;
    private boolean hidden;

    private final List<Setting> settings = new ArrayList<>();

    public Module(String name, String description, Category category) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.category = Objects.requireNonNull(category, "category");
        this.toggled = false;
        this.keyCode = 0;
        this.hidden = false;
    }

    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    public void toggle() {
        setToggled(!this.toggled);
    }

    public void setToggled(boolean toggled) {
        if (this.toggled == toggled) {
            return;
        }

        this.toggled = toggled;

        if (this.toggled) {
            onEnable();
            notifyToggleChange(true);
            return;
        }

        onDisable();
        notifyToggleChange(false);
    }

    private void notifyToggleChange(boolean enabled) {
        if (hidden) {
            return;
        }

        NotificationManager.getInstance().addNotification(
                name,
                enabled ? "Enabled" : "Disabled",
                enabled ? NotificationManager.NotificationType.SUCCESS : NotificationManager.NotificationType.ERROR,
                2000
        );
    }

    public void addSetting(Setting setting) {
        this.settings.add(Objects.requireNonNull(setting, "setting"));
    }

    public void addSettings(Setting... settings) {
        if (settings == null || settings.length == 0) {
            return;
        }

        Arrays.stream(settings)
                .filter(Objects::nonNull)
                .forEach(this.settings::add);
    }

    public List<Setting> getSettings() {
        return Collections.unmodifiableList(this.settings);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public boolean isToggled() {
        return toggled;
    }

    public boolean isHidden() {
        return hidden;
    }

    protected void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean shouldSuppressPacket() {
        return false;
    }
}