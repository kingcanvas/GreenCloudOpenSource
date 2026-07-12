package greencloud.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovementInput;
import net.minecraftforge.fml.common.eventhandler.Event;

public class InputUpdateEvent extends Event {
    public final EntityPlayer entityPlayer;
    public final MovementInput movementInput;

    public InputUpdateEvent(EntityPlayer entityPlayer, MovementInput movementInput) {
        this.entityPlayer = entityPlayer;
        this.movementInput = movementInput;
    }
}