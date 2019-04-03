package engine.lua.type.object;

import org.luaj.vm2.LuaValue;

import engine.io.FileResource;

public abstract class AssetLoadable extends Asset implements FileResource {

	public AssetLoadable(String type) {
		super(type);
		this.defineField("FilePath", LuaValue.valueOf(""), false);
	}
	
	public void setFilePath(String path) {
		if ( path == null ) {
			this.set("FilePath", LuaValue.NIL);
		} else {
			this.set("FilePath", LuaValue.valueOf(path));
		}
	}
	
	@Override
	public String getFilePath() {
		return this.get("FilePath").toString();
	}
	
	public void resetFilePath() {
		this.set("FilePath", LuaValue.valueOf(""));
	}
	
	public static String getFileTypes() {
		return "";
	}
}
