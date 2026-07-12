package greencloud.impl.config;

import com.google.gson.*;
import greencloud.GreenCloud;
import greencloud.impl.logger.Log;
import greencloud.impl.logger.Logger;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;

import java.awt.Color;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private static final Logger log = Log.get(ConfigManager.class);

    public static final int CONFIG_VERSION = 1;

    private final File configDirectory;
    private final Gson gson;

    public ConfigManager() {
        File root = (GreenCloud.mainDir != null) ? GreenCloud.mainDir : Minecraft.getMinecraft().mcDataDir;
        this.configDirectory = new File(root, "configs");

        if (!configDirectory.exists()) {
            boolean created = configDirectory.mkdirs();
            if (created) {
                log.debug("Created config directory: " + configDirectory.getAbsolutePath());
            } else {
                log.error("Failed to create config directory: " + configDirectory.getAbsolutePath());
            }
        }

        this.gson = new GsonBuilder().setPrettyPrinting().create();
        log.info("ConfigManager initialized, config dir: " + configDirectory.getAbsolutePath());
    }

    public void saveConfig(String configName) {
        if (configName == null || configName.trim().isEmpty()) {
            log.warn("Attempted to save config with null or empty name");
            return;
        }
        log.debug("Saving config: " + configName);
        try {
            File configFile = new File(configDirectory, configName + ".json");
            JsonObject root = new JsonObject();
            root.addProperty("version", CONFIG_VERSION);

            JsonArray modulesArray = new JsonArray();
            for (Module module : GreenCloud.moduleManager.getModules()) {
                JsonObject moduleObj = new JsonObject();
                moduleObj.addProperty("name", module.getName());
                moduleObj.addProperty("enabled", module.isToggled());
                moduleObj.addProperty("keybind", module.getKeyCode());

                JsonArray settingsArray = new JsonArray();
                for (Setting setting : module.getSettings()) {
                    JsonElement value = setting.serialize();
                    if (value != null) {
                        JsonObject settingObj = new JsonObject();
                        settingObj.addProperty("name", setting.name);
                        settingObj.add("value", value);
                        settingsArray.add(settingObj);
                    }
                }
                moduleObj.add("settings", settingsArray);
                modulesArray.add(moduleObj);
            }

            root.add("modules", modulesArray);

            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(root, writer);
            }

            log.info("Config saved: " + configName);
        } catch (Exception e) {
            log.error("Failed to save config '" + configName + "'", e);
        }
    }

    public void loadConfig(String configName) {
        File configFile = new File(configDirectory, configName + ".json");
        if (!configFile.exists()) {
            log.warn("Config file not found: " + configName);
            return;
        }

        log.debug("Loading config: " + configName);
        try {
            String content = new String(Files.readAllBytes(configFile.toPath()));
            JsonObject root = gson.fromJson(content, JsonObject.class);

            if (root.has("version")) {
                int version = root.get("version").getAsInt();
                if (version != CONFIG_VERSION) {
                    log.warn("Config '" + configName + "' version " + version + " does not match current version " + CONFIG_VERSION + ", some settings may not load correctly");
                }
            } else {
                log.warn("Config '" + configName + "' has no version field and may be from an older format");
            }

            JsonArray modulesArray = root.getAsJsonArray("modules");
            for (JsonElement element : modulesArray) {
                JsonObject moduleObj = element.getAsJsonObject();
                String moduleName = moduleObj.get("name").getAsString();

                Module module = GreenCloud.moduleManager.getModuleByName(moduleName);
                if (module == null) {
                    log.debug("No module found for config entry: " + moduleName);
                    continue;
                }

                boolean enabled = moduleObj.get("enabled").getAsBoolean();
                if (module.isToggled() != enabled) module.setToggled(enabled);

                module.setKeyCode(moduleObj.get("keybind").getAsInt());

                if (!moduleObj.has("settings")) continue;

                for (JsonElement settingElement : moduleObj.getAsJsonArray("settings")) {
                    JsonObject settingObj = settingElement.getAsJsonObject();
                    String settingName = settingObj.get("name").getAsString();
                    Setting setting = getSettingByName(module, settingName);
                    if (setting == null || !settingObj.has("value")) continue;

                    try {
                        setting.deserialize(settingObj.get("value"));
                    } catch (Exception e) {
                        log.warn("Failed to deserialize setting '" + settingName + "' in module '" + moduleName + "': " + e.getMessage(), e);
                    }
                }
            }

            log.info("Config loaded: " + configName);
        } catch (Exception e) {
            log.error("Failed to load config '" + configName + "'", e);
        }
    }

    public void deleteConfig(String configName) {
        try {
            File configFile = new File(configDirectory, configName + ".json");
            if (configFile.exists()) {
                boolean deleted = configFile.delete();
                if (deleted) {
                    log.info("Config deleted: " + configName);
                } else {
                    log.warn("Failed to delete config file: " + configName);
                }
            } else {
                log.warn("Tried to delete non-existent config: " + configName);
            }
        } catch (Exception e) {
            log.error("Exception deleting config '" + configName + "'", e);
        }
    }

    public List<String> getConfigList() {
        List<String> configs = new ArrayList<>();
        File[] files = configDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                configs.add(file.getName().replace(".json", ""));
            }
        } else {
            log.warn("Failed to list configs, directory may be inaccessible: " + configDirectory.getAbsolutePath());
        }
        return configs;
    }

    public String serialize() {
        log.debug("Serializing full config state");
        try {
            JsonObject root = new JsonObject();
            root.addProperty("version", CONFIG_VERSION);

            JsonArray modulesArray = new JsonArray();
            for (Module module : GreenCloud.moduleManager.getModules()) {
                JsonObject moduleObj = new JsonObject();
                moduleObj.addProperty("name", module.getName());
                moduleObj.addProperty("enabled", module.isToggled());
                moduleObj.addProperty("keybind", module.getKeyCode());

                JsonArray settingsArray = new JsonArray();
                for (Setting setting : module.getSettings()) {
                    JsonElement value = setting.serialize();
                    if (value != null) {
                        JsonObject settingObj = new JsonObject();
                        settingObj.addProperty("name", setting.name);
                        settingObj.add("value", value);
                        settingsArray.add(settingObj);
                    }
                }
                moduleObj.add("settings", settingsArray);
                modulesArray.add(moduleObj);
            }

            root.add("modules", modulesArray);
            return gson.toJson(root);
        } catch (Exception e) {
            log.error("Failed to serialize config state", e);
            return "{}";
        }
    }

    private Setting getSettingByName(Module module, String name) {
        for (Setting setting : module.getSettings()) {
            if (setting.name.equalsIgnoreCase(name)) return setting;
        }
        return null;
    }
}
