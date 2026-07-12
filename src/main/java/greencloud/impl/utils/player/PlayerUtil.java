package greencloud.impl.utils.player;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;

public class PlayerUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isInsideBlock() {
        for (int x = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minX); x < MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().maxX) + 1; ++x) {
            for (int y = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minY); y < MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().maxY) + 1; ++y) {
                for (int z = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minZ); z < MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().maxZ) + 1; ++z) {
                    Block block = mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
                    if (block != null && !(block instanceof BlockAir)) {
                        AxisAlignedBB boundingBox = block.getCollisionBoundingBox(mc.theWorld, new BlockPos(x, y, z), mc.theWorld.getBlockState(new BlockPos(x, y, z)));
                        if (boundingBox != null && mc.thePlayer.getEntityBoundingBox().intersectsWith(boundingBox)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static void addChatMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}