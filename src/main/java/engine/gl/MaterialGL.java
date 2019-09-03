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
	
	public Vector3f getEmissive() {
		return this.emissive;
	}

	public Vector3f getColor() {
		return this.color;
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

	public Texture2D getDiffuseTexture() {
		return this.diffuseTexture;
	}

	public Texture2D getNormalTexture() {
		return this.normalTexture;
	}

	public Texture2D getMetalnessTexture() {
		return this.metalnessTexture;
	}

	public Texture2D getRoughnessTexture() {
		return this.roughnessTexture;
	}

	private final static String U_METALNESS = "uMetalness";
	private final static String U_ROUGHNESS = "uRoughness";
	private final static String U_REFLECTIVE = "uReflective";
	private final static String U_MATERIALCOLOR = "uMaterialColor";
	private final static String U_MATERIALEMISSIVE = "uMaterialEmissive";
	private final static String U_AMBIENT = "uAmbient";
	private final static String U_ENABLESKYBOX = "enableSkybox";
	private final static String U_ENABLEIBL = "enableIBL";
	private final static String U_NORMALMAPENABLED = "normalMapEnabled";
	private final static String U_TRANSPARENCYMATERIAL = "uTransparencyMaterial";

	private final static String U_TEXTURE_CUBEMAP = "texture_cubemap";
	private final static String U_SKYBOX_POWER = "uSkyBoxLightPower";
	private final static String U_SKYBOX_MULTIPLIER = "uSkyBoxLightMultiplier";
	
	public void bind(BaseShader shader) {
		if ( shader == null )
			return;
		
		shader.debug = true;
		
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
		
		// Bind IBL map (if it exists)
		SkySphere ibl = null;
		if ( cubemap != null ) {
			shader.texture_set_stage(shader.shader_get_uniform(U_TEXTURE_CUBEMAP), cubemap, 4);
			
			if ( cubemap instanceof SkySphereIBL ) {
				shader.shader_set_uniform_f(shader.shader_get_uniform(U_SKYBOX_POWER), ((SkySphereIBL)cubemap).getLightPower());
				shader.shader_set_uniform_f(shader.shader_get_uniform(U_SKYBOX_MULTIPLIER), ((SkySphereIBL)cubemap).getLightMultiplier());
			} else {
				shader.shader_set_uniform_f(shader.shader_get_uniform(U_SKYBOX_MULTIPLIER), 1.0f);
			}
		}

		// Set uniforms
		shader.shader_set_uniform_f(shader.shader_get_uniform(U_METALNESS), metalness);
		shader.shader_set_uniform_f(shader.shader_get_uniform(U_ROUGHNESS), roughness);
		shader.shader_set_uniform_f(shader.shader_get_uniform(U_REFLECTIVE), reflective);
		shader.shader_set_uniform_f(shader.shader_get_uniform(U_MATERIALCOLOR), color);
		shader.shader_set_uniform_f(shader.shader_get_uniform(U_MATERIALEMISSIVE), emissive);
		shader.shader_set_uniform_f(shader.shader_get_uniform(U_AMBIENT), Pipeline.pipeline_get().getGBuffer().getAmbient());
		shader.shader_set_uniform_f(shader.shader_get_uniform(U_ENABLESKYBOX), cubemap == null ? 0:1);
		shader.shader_set_uniform_f(shader.shader_get_uniform(U_ENABLEIBL), ibl == null ? 0:1);
		shader.shader_set_uniform_f(shader.shader_get_uniform(U_NORMALMAPENABLED), normalTexture.equals(Resources.TEXTURE_NORMAL_RGBA) ? 0:1);
		shader.shader_set_uniform_f(shader.shader_get_uniform(U_TRANSPARENCYMATERIAL), transparency);
	}

}
