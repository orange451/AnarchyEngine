package engine.gl.ibl;

import engine.gl.Pipeline;
import engine.io.Image;
import engine.util.TextureUtils;

public class SkySphereIBL extends SkySphere {
	private SkySphereIBL lightSphere;
	private int buffers;
	private float lightMultiplier = 1.0f;
	
	public SkySphereIBL(Image reflection, Image light) {
		this(reflection, light, 512);
	}
	
	public SkySphereIBL(Image reflection, Image light, int size) {
		super(reflection, size);
		
		if ( reflection != null && light != null ) {
			lightSphere = new SkySphereIBL(light,null, 128);
		}
	}
	
	@Override
	public boolean draw(Pipeline pipeline) {
		if ( buffers > 5 )
			return true;
		
		pipeline.shader_reset();
		boolean b = super.draw(pipeline);
		if ( b && lightSphere != null ) {
			lightSphere.draw(pipeline);
		}
		
		buffers++;
		return b;
	}
	
	public void setLightMultiplier(float i) {
		this.lightMultiplier = i;
	}
	

	public float getLightMultiplier() {
		return lightMultiplier;
	}

	public SkySphere getLightSphere() {
		return lightSphere;
	}

	/**
	 * Convenience method to create a SkySphereIBL. For this method to work, your IBL must be structured like this:
	 * <br>
	 * <br>
	 * File 1: office.hdr
	 * <br>
	 * File 2: office_env.hdr
	 * <br>
	 * <br>
	 * SkySPhereIBL ibl = SkySphereIBL.create("path/office.hdr");
	 */
	public static SkySphereIBL create(String path) {
		String ref = path;
		String[] t = path.split("\\.");
		String extension = "." + t[t.length-1];
		String base = path.replace(extension, "");
		String env = base + "_env" + extension;
		
		return new SkySphereIBL(
				TextureUtils.loadImage(ref),
				TextureUtils.loadImage(env));
	}
}
