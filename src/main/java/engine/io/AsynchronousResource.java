package engine.io;

public abstract class AsynchronousResource<E> {
	protected String filePath;
	
	public AsynchronousResource(String path) {
		this.filePath = path;
	}
	
	public String getPath() {
		return filePath;
	}
	
	public abstract E getResource();
	public abstract boolean isLoaded();
	protected abstract void internalLoad();
}
