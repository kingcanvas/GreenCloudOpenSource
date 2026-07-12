package greencloud.impl.scripting.script;

import java.io.File;

public final class Script {

    private final String name;
    private final String author;
    private final String version;
    private final File file;

    public Script(String name, String author, String version, File file) {
        this.name = name;
        this.author = author;
        this.version = version;
        this.file = file;
    }

    public String getName() { return name; }
    public String getAuthor() { return author; }
    public String getVersion() { return version; }
    public File getFile() { return file; }

    @Override
    public String toString() {
        return name + " v" + version + " by " + author;
    }
}
