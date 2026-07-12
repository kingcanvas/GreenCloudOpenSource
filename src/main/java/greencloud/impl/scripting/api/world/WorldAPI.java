package greencloud.impl.scripting.api.world;

import net.minecraft.client.Minecraft;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public final class WorldAPI {

    private WorldAPI() {}

    public static LuaTable create() {
        LuaTable t = new LuaTable();
        Minecraft mc = Minecraft.getMinecraft();

        t.set("isLoaded", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(mc.theWorld != null);
            }
        });

        t.set("getDimension", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mc.theWorld == null) return LuaValue.ZERO;
                return LuaValue.valueOf(mc.theWorld.provider.getDimensionId());
            }
        });

        t.set("getDifficultyName", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mc.theWorld == null) return LuaValue.NIL;
                return LuaValue.valueOf(mc.theWorld.getDifficulty().name());
            }
        });

        t.set("getTime", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mc.theWorld == null) return LuaValue.ZERO;
                return LuaValue.valueOf((double) mc.theWorld.getWorldTime());
            }
        });

        t.set("isRaining", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mc.theWorld == null) return LuaValue.FALSE;
                return LuaValue.valueOf(mc.theWorld.isRaining());
            }
        });

        t.set("getBlockName", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                if (mc.theWorld == null || !arg.istable()) return LuaValue.NIL;
                int x = arg.get(1).toint();
                int y = arg.get(2).toint();
                int z = arg.get(3).toint();
                net.minecraft.block.Block block = mc.theWorld.getBlockState(
                        new net.minecraft.util.BlockPos(x, y, z)).getBlock();
                return LuaValue.valueOf(block.getRegistryName());
            }
        });

        return t;
    }
}
