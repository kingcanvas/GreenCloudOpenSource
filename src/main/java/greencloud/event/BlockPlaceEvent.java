package greencloud.event;

import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;

public class BlockPlaceEvent extends Event {
    private final BlockPos blockPos;

    public BlockPlaceEvent(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }
}