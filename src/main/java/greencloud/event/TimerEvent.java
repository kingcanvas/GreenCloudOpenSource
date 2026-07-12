package greencloud.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class TimerEvent extends Event {
    private float speed;

    public TimerEvent(float speed) {
        this.speed = speed;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }
}