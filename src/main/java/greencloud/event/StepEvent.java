package greencloud.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class StepEvent extends Event {
    private float stepHeight;
    private final State state;

    public StepEvent(float stepHeight, State state) {
        this.stepHeight = stepHeight;
        this.state = state;
    }

    public float getStepHeight() {
        return stepHeight;
    }

    public void setStepHeight(float stepHeight) {
        this.stepHeight = stepHeight;
    }

    public State getState() {
        return state;
    }

    public enum State {
        PRE, POST
    }
}