package engine.lua.lib;

public class EnumType extends LuaTableReadOnly {
	private String type;
	
	public EnumType(String string) {
		this.type = string;
	}

	public String getType() {
		return this.type;
	}
}
