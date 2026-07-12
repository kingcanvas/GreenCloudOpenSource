package greencloud.impl.command;
import java.util.List;
public abstract class Command {

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getUsage();

    public abstract List<String> getAliases();

    public abstract void execute(String[] args);
}
