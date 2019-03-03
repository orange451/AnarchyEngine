package engine.gl;

import org.joml.Vector3f;

import engine.gl.ibl.SkySphere;
import engine.gl.ibl.SkySphereIBL;
import engine.gl.shader.BaseShader;
import lwjgui.paint.Color;

public class MaterialGL {
	private Texture2D diffuseTexture;
	private Texture2D normalTexture;
	private Texture2D metalnessTexture;
	private Texture2D roughnessTexture;

	private float metalness = 0.2f;
	private float roughness = 0.3f;
	private float reflective = 0.1f;
	private float transparency = 0;
	
	private Vector3f emissive;
	private Vector3f color;
	
	public MaterialGL() {
		setDiffuseTexture(null);
		setNormalTexture(null);
		setMetalnessTexture(null);
		setRoughnessTexture(null);
		setColor(Color.LIGHT_GRAY);
		setEmissive(Color.BLACK);
	}
	
	public MaterialGL setColor(Color color) {
		this.color = new Vector3f(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
		return this;
	}
	
	public MaterialGL setColor(Vector3f vector) {
		this.color.set(vector);
		return this;
	}	
	
	public MaterialGL setEmissive(Color color) {
		this.emissive = new Vector3f(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
		return this;
	}
	
	public MaterialGL setEmissive(Vector3f vector) {
		this.emissive.set(vector);
		return this;
	}
	
	public float getReflective() {
		return this.reflective;
	}

	public float getMetalness() {
		return this.metalness;
	}
	
	public float getRoughness() {
		return this.roughness;
	}
	
	public MaterialGL setReflective(float value) {
		this.reflective = value;
		return this;
	}
	
	public MaterialGL setMetalness(float value) {
		this.metalness = value;
		return this;
	}
	
	public MaterialGL setRoughness(float value) {
		this.roughness = value;
		return this;
	}
	
	public MaterialGL setTransparency(float value) {
		this.transparency = value;
		return this;
	}
	
	public float getTransparency() {
		return this.transparency;
	}
	
	public MaterialGL setDiffuseTexture(Texture2D texture) {
		if ( texture == null ) {
			this.diffuseTexture = Resources.TEXTURE_WHITE_SRGB;
		} else {
			this.diffuseTexture = texture;
		}
		
		return this;
	}	
	
	public MaterialGL setNormalTexture(Texture2D texture) {
		if ( texture == null ) {
			this.normalTexture = Resources.TEXTURE_NORMAL_RGBA;
		} else {
			this.normalTexture = texture;
		}
		
		return this;
	}
	
	public MaterialGL setMetalnessTexture(Texture2D texture) {
		if ( texture == null ) {
			this.metalnessTexture = Resources.TEXTURE_WHITE_RGBA;
		} else {
			this.metalnessTexture = texture;
		}
		
		return this;
	}
	
	public MaterialGL setRoughnessTexture(Texture2D texture) {
		if ( texture == null ) {
			this.roughnessTexture = Resources.TEXTURE_WHITE_RGBA;
		} else {
			this.roughnessTexture = texture;
		}
		
		return this;
	}
	
	public void bind(BaseShader shader) {
		if ( shader == null )
			return;
		
		// Bind diffuse/albedo
		shader.texture_set_stage(shader.diffuseTextureLoc, diffuseTexture, 0);
		
		// Bind normalmap
		shader.texture_set_stage(shader.normalTextureLoc, normalTexture, 1);
		
		// Bind metalness
		shader.texture_set_stage(shader.metalnessTextureLoc, metalnessTexture, 2);
		
		// Bind roughness
		shader.texture_set_stage(shader.roughnessTextureLoc, roughnessTexture, 3);
		
		// Bind environment map
		SkyBox cubemap = Pipeline.pipeline_get().getGBuffer().getMergeProcessor().getSkybox();
		shader.texture_set_stage(shader.shader_get_uniform("texture_cubemap"), cubemap, 4);
		
		// Bind IBL map (if it exists)
		SkySphere ibl = null;
		if ( cubemap instanceof SkySphereIBL ) {
			shader.shader_set_uniform_f(shader.shader_get_uniform("uSkyBoxLightPower"), ((SkySphereIBL)cubemap).getLightPower());
			shader.shader_set_uniform_f(shader.shader_get_uniform("uSkyBoxLightMultiplier"), ((SkySphereIBL)cubemap).getLightMultiplier());
		} else {
			shader.shader_set_uniform_f(shader.shader_get_uniform("uSkyBoxLightMultiplier"), 1.0f);
		}

		// Set uniforms
		shader.shader_set_uniform_f(shader.shader_get_uniform("uMetalness"), metalness);
		shader.shader_set_uniform_f(shader.shader_get_uniform("uRoughness"), roughness);
		shader.shader_set_uniform_f(shader.shader_get_uniform("uReflective"), reflective);
		shader.shader_set_uniform_f(shader.shader_get_uniform("uMaterialColor"), color);
		shader.shader_set_uniform_f(shader.shader_get_uniform("uMaterialEmissive"), emissive);
		shader.shader_set_uniform_f(shader.shader_get_uniform("uAmbient"), Pipeline.pipeline_get().getGBuffer().getAmbient());
		shader.shader_set_uniform_f(shader.shader_get_uniform("enableSkybox"), cubemap == null ? 0:1);
		shader.shader_set_uniform_f(shader.shader_get_uniform("enableIBL"), ibl == null ? 0:1);
		shader.shader_set_uniform_f(shader.shader_get_uniform("normalMapEnabled"), normalTexture.equals(Resources.TEXTURE_NORMAL_RGBA) ? 0:1);
		shader.shader_set_uniform_f(shader.shader_get_uniform("uTransparencyMaterial"), transparency);
	}

}
