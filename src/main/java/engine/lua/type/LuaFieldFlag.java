package engine.lua.type;

public class LuaFieldFlag {
	/**
	 * This flag controls whether the client will try to replicate this field (to the server) when it changes.
	 */
	public static final int CLIENT_SIDE_REPLICATE = 0x00000001;
	
	/**
	 * Similar to {@link #CLIENT_SIDE_REPLICATE}. However it must be replicated from client-to-serve manually.
	 */
	public static final int CLIENT_SIDE_REPLICATE_MANUAL = 0x00000002;
	
	/**
	 * Flag that represents a field that are common among ALL datamodels.
	 */
	public static final int CORE_FIELD = 0x00000004;
	
	/**
	 * Flag that makes a field hidden from properties window.
	 */
	public static final int FIELD_HIDDEN = 0x00000008;
}
