package greencloud.impl.scripting.api.module;

import greencloud.GreenCloud;
import greencloud.impl.modules.Module;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

public final class ModuleAPI {

    private ModuleAPI() {}

    public static LuaTable create() {
        LuaTable t = new LuaTable();

        t.set("enable", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                Module m = GreenCloud.moduleManager.getModuleByName(arg.tojstring());
                if (m != null && !m.isToggled()) m.toggle();
                return LuaValue.NIL;
            }
        });

        t.set("disable", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                Module m = GreenCloud.moduleManager.getModuleByName(arg.tojstring());
                if (m != null && m.isToggled()) m.toggle();
                return LuaValue.NIL;
            }
        });

        t.set("toggle", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                Module m = GreenCloud.moduleManager.getModuleByName(arg.tojstring());
                if (m != null) m.toggle();
                return LuaValue.NIL;
            }
        });

        t.set("isEnabled", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                Module m = GreenCloud.moduleManager.getModuleByName(arg.tojstring());
                if (m == null) return LuaValue.FALSE;
                return LuaValue.valueOf(m.isToggled());
            }
        });

        t.set("getAll", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                LuaTable list = new LuaTable();
                int i = 1;
                for (Module m : GreenCloud.moduleManager.getModules()) {
                    list.set(i++, LuaValue.valueOf(m.getName()));
                }
                return list;
            }
        });

        return t;
    }
}
