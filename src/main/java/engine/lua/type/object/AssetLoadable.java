package engine.lua.type.object;

import org.luaj.vm2.LuaValue;

import engine.FilePath;
import engine.io.FileResource;

public abstract class AssetLoadable extends Asset implements FileResource {

	protected final static LuaValue C_FILEPATH = LuaValue.valueOf("FilePath");
	
	public AssetLoadable(String type) {
		super(type);
		this.defineField("FilePath", LuaValue.valueOf(""), false);
	}
	
	public void setFilePath(String path) {
		if ( path == null ) {
			this.set(C_FILEPATH, LuaValue.NIL);
		} else {
			this.set(C_FILEPATH, LuaValue.valueOf(path));
		}
	}
	
	@Override
	public String getFilePath() {
		return this.get(C_FILEPATH).toString();
	}
	
	public String getAbsoluteFilePath() {
		return FilePath.convertToSystem(getFilePath());
	}
	
	public void resetFilePath() {
		this.set(C_FILEPATH, LuaValue.valueOf(""));
	}
	
	public static String getFileTypes() {
		return "";
	}
}
