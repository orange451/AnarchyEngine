package engine.physics;

public class SoundMaterial {
	private final String name;
	private final String directory;
	
	public SoundMaterial(String name, String directory) {
		this.name = name;
		this.directory = directory;
	}
	
	public String getDirectory() {
		return directory + name + "/";
	}
}
