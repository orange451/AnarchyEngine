package luaengine;

import org.luaj.vm2.LuaValue;

@FunctionalInterface
public interface RunnableArgs {
    public abstract void run(LuaValue[] args);
}