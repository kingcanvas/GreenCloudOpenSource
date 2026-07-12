package greencloud.impl.scripting.sandbox;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

public final class Sandbox {

    private Sandbox() {}

    public static Globals createGlobals() {
        Globals globals = JsePlatform.standardGlobals();

        globals.set("io", LuaValue.NIL);
        globals.set("os", LuaValue.NIL);
        globals.set("package", LuaValue.NIL);
        globals.set("require", LuaValue.NIL);
        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        globals.set("load", LuaValue.NIL);
        globals.set("debug", LuaValue.NIL);

        return globals;
    }
}
