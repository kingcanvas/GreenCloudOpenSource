package greencloud.impl.modules;

import com.google.common.reflect.ClassPath;
import greencloud.impl.logger.Log;
import greencloud.impl.logger.Logger;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager {

    private static final Logger log = Log.get(ModuleManager.class);

    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module> moduleMap = new HashMap<>();
    private final Map<Category, List<Module>> categoryCache = new HashMap<>();

    public void init() {
        log.info("Discovering modules");

        try {
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            ClassPath classPath = ClassPath.from(loader);

            for (String pkg : new String[]{"greencloud.impl.modules", "greencloud.impl.MinecraftUI"}) {
                log.debug("Scanning package: " + pkg);
                for (final ClassPath.ClassInfo info : classPath.getTopLevelClassesRecursive(pkg)) {
                    try {
                        final Class<?> clazz = info.load();
                        if (Module.class.isAssignableFrom(clazz) && clazz != Module.class && !Modifier.isAbstract(clazz.getModifiers())) {
                            final Module module = (Module) clazz.getDeclaredConstructor().newInstance();
                            addModule(module);
                            log.debug("Loaded module: " + module.getName());
                        }
                    } catch (Exception e) {
                        log.error("Failed to load module class: " + info.getName(), e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan classpath for modules", e);
        }

        log.info("ModuleManager initialized with " + modules.size() + " modules");
    }

    private void addModule(Module module) {
        modules.add(module);
        moduleMap.put(module.getClass(), module);
        categoryCache.computeIfAbsent(module.getCategory(), k -> new ArrayList<>()).add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesInCategory(Category category) {
        return categoryCache.getOrDefault(category, new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        return (T) moduleMap.get(clazz);
    }

    public Module getModuleByName(String name) {
        for (Module module : modules) {
            if (module.getName().replace(" ", "").equalsIgnoreCase(name.replace(" ", ""))) {
                return module;
            }
        }
        return null;
    }
}
