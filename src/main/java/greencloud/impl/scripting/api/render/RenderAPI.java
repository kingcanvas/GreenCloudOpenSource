package greencloud.impl.scripting.api.render;

import greencloud.impl.utils.render.GreenRender;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.Varargs;

import java.awt.Color;

public final class RenderAPI {

    private RenderAPI() {}

    public static LuaTable create() {
        LuaTable t = new LuaTable();

        t.set("text", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String text = args.tojstring(1);
                float x = (float) args.todouble(2);
                float y = (float) args.todouble(3);
                int r = args.narg() >= 4 ? args.toint(4) : 255;
                int g = args.narg() >= 5 ? args.toint(5) : 255;
                int b = args.narg() >= 6 ? args.toint(6) : 255;
                int a = args.narg() >= 7 ? args.toint(7) : 255;
                int color = new Color(r, g, b, a).getRGB();
                GreenRender.drawStringBold(text, x, y, color);
                return LuaValue.NIL;
            }
        });

        t.set("rect", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                float x = (float) args.todouble(1);
                float y = (float) args.todouble(2);
                float w = (float) args.todouble(3);
                float h = (float) args.todouble(4);
                int r = args.narg() >= 5 ? args.toint(5) : 255;
                int g = args.narg() >= 6 ? args.toint(6) : 255;
                int b = args.narg() >= 7 ? args.toint(7) : 255;
                int a = args.narg() >= 8 ? args.toint(8) : 255;
                GreenRender.fillRR(x, y, w, h, 0f, new Color(r, g, b, a));
                return LuaValue.NIL;
            }
        });

        t.set("roundedRect", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                float x = (float) args.todouble(1);
                float y = (float) args.todouble(2);
                float w = (float) args.todouble(3);
                float h = (float) args.todouble(4);
                float radius = (float) args.todouble(5);
                int r = args.narg() >= 6 ? args.toint(6) : 255;
                int g = args.narg() >= 7 ? args.toint(7) : 255;
                int b = args.narg() >= 8 ? args.toint(8) : 255;
                int a = args.narg() >= 9 ? args.toint(9) : 255;
                GreenRender.fillRR(x, y, w, h, radius, new Color(r, g, b, a));
                return LuaValue.NIL;
            }
        });

        t.set("textWidth", new org.luaj.vm2.lib.OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return LuaValue.valueOf(GreenRender.strWBold(arg.tojstring()));
            }
        });

        t.set("textHeight", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(GreenRender.fontHBold());
            }
        });

        return t;
    }
}
