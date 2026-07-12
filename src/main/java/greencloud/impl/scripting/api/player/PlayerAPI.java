package greencloud.impl.scripting.api.player;

import net.minecraft.client.Minecraft;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public final class PlayerAPI {

    private PlayerAPI() {}

    public static LuaTable create() {
        LuaTable t = new LuaTable();
        Minecraft mc = Minecraft.getMinecraft();

        t.set("getName", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mc.thePlayer == null) return LuaValue.NIL;
                return LuaValue.valueOf(mc.thePlayer.getName());
            }
        });

        t.set("getHealth", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mc.thePlayer == null) return LuaValue.ZERO;
                return LuaValue.valueOf(mc.thePlayer.getHealth());
            }
        });

        t.set("getX", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mc.thePlayer == null) return LuaValue.ZERO;
                return LuaValue.valueOf(mc.thePlayer.posX);
            }
        });

        t.set("getY", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mc.thePlayer == null) return LuaValue.ZERO;
                return LuaValue.valueOf(mc.thePlayer.posY);
            }
        });

        t.set("getZ", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mc.thePlayer == null) return LuaValue.ZERO;
                return LuaValue.valueOf(mc.thePlayer.posZ);
            }
        });

        t.set("isOnGround", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mc.thePlayer == null) return LuaValue.FALSE;
                return LuaValue.valueOf(mc.thePlayer.onGround);
            }
        });

        t.set("isSprinting", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mc.thePlayer == null) return LuaValue.FALSE;
                return LuaValue.valueOf(mc.thePlayer.isSprinting());
            }
        });

        t.set("getFoodLevel", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mc.thePlayer == null) return LuaValue.ZERO;
                return LuaValue.valueOf(mc.thePlayer.getFoodStats().getFoodLevel());
            }
        });

        t.set("sendMessage", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                if (mc.thePlayer == null) return LuaValue.NIL;
                mc.thePlayer.addChatMessage(
                        new net.minecraft.util.ChatComponentText(arg.tojstring())
                );
                return LuaValue.NIL;
            }
        });

        t.set("sendCommand", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                if (mc.thePlayer == null) return LuaValue.NIL;
                mc.thePlayer.sendChatMessage("/" + arg.tojstring());
                return LuaValue.NIL;
            }
        });

        t.set("getYaw", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mc.thePlayer == null) return LuaValue.ZERO;
                return LuaValue.valueOf(mc.thePlayer.rotationYaw);
            }
        });

        t.set("getPitch", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mc.thePlayer == null) return LuaValue.ZERO;
                return LuaValue.valueOf(mc.thePlayer.rotationPitch);
            }
        });

        return t;
    }
}
