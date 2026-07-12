package greencloud.impl.scripting.engine;

import greencloud.impl.logger.Log;
import greencloud.impl.logger.Logger;
import greencloud.impl.scripting.script.Script;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class ScriptLoader {

    private static final Logger log = Log.get(ScriptLoader.class);

    private ScriptLoader() {}

    public static List<Script> discover(File scriptsDir) {
        List<Script> found = new ArrayList<>();

        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs();
            log.info("[Scripting] Created scripts directory: " + scriptsDir.getAbsolutePath());
            return found;
        }

        File[] files = scriptsDir.listFiles(f -> f.isFile() && f.getName().endsWith(".lua"));
        if (files == null || files.length == 0) {
            log.info("[Scripting] No scripts found in " + scriptsDir.getAbsolutePath());
            return found;
        }

        for (File file : files) {
            try {
                Script script = parse(file);
                found.add(script);
                log.debug("[Scripting] Discovered: " + script);
            } catch (Exception e) {
                log.warn("[Scripting] Skipping " + file.getName() + ": " + e.getMessage());
            }
        }

        log.info("[Scripting] Discovered " + found.size() + " script(s)");
        return found;
    }

    private static Script parse(File file) throws IOException {
        String baseName = file.getName().replace(".lua", "");
        File metaFile = new File(file.getParentFile(), baseName + ".meta");

        String name = baseName;
        String author = "Unknown";
        String version = "1.0";

        if (metaFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(metaFile)) {
                props.load(fis);
            }
            name = props.getProperty("name", baseName);
            author = props.getProperty("author", "Unknown");
            version = props.getProperty("version", "1.0");
        }

        return new Script(name, author, version, file);
    }
}
