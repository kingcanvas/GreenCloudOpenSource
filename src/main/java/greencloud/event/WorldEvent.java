package greencloud.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class WorldEvent extends Event {

    public static class Load extends WorldEvent {
        public Load() {}
    }

    public static class Unload extends WorldEvent {
        public Unload() {}
    }
}
