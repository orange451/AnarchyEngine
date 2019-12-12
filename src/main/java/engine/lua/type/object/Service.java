package engine.lua.type.object;

public abstract class Service extends Instance {

	public Service(String name) {
		super(name);
		this.setInstanceable(false);
		
		this.getField(C_NAME).setLocked(true);
		this.getField(C_PARENT).setLocked(true);
		this.getField(C_ARCHIVABLE).setLocked(true);
		
		this.setLocked(true);
	}

	public void onDestroy() {
		//
	}
}
