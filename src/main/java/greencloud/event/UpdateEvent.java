package greencloud.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class UpdateEvent extends Event {

    private final boolean pre;

    public UpdateEvent(boolean pre) {
        this.pre = pre;
    }

    public boolean isPre() {
        return pre;
    }

    public boolean isPost() {
        return !pre;
    }

    public static class Pre extends UpdateEvent {
        public Pre() {
            super(true);
        }
    }

    public static class Post extends UpdateEvent {
        public Post() {
            super(false);
        }
    }
}