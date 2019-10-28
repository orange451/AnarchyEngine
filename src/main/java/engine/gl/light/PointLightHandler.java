package engine.gl.light;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.GL_GREATER;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import engine.gl.Pipeline;
import engine.gl.mesh.BufferedMesh;
import engine.gl.shader.BaseShader;
import engine.glv2.v2.lights.IPointLightHandler;
import engine.util.MeshUtils;

public class PointLightHandler implements IPointLightHandler {
	private List<PointLightInternal> lights = Collections.synchronizedList(new ArrayList<PointLightInternal>());
	private static BaseShader lightShader = new PointLightShader();
	private BufferedMesh mesh = MeshUtils.sphere(1, 16);
	
	public void addLight(PointLightInternal l) {
		//synchronized(lights) {
			lights.add(l);
		//}
	}
	
	public void removeLight(PointLightInternal l) {
		synchronized(lights) {
			lights.remove(l);
		}
	}
	
	private Matrix4f tempLightMatrix = new Matrix4f();

	private static final String U_INVERSE_PROJ_MAT = "inverseProjectionMatrix";
	private static final String U_TEXEL = "texel";
	private static final String U_TEXTURE_ALBEDO = "texture_albedo";
	private static final String U_TEXTURE_DEPTH = "texture_depth";
	private static final String U_TEXTURE_NORMAL = "texture_normal";
	private static final String U_TEXTURE_PBR = "texture_pbr";
	private static final String U_L_RADIUS = "radius";
	private static final String U_L_INTENSITY = "intensity";
	private static final String U_L_POSITION = "lightPosition";
	private static final String U_L_COLOR = "lightColor";
	
	public void handle(Pipeline pipeline) {
		glEnable(GL_CULL_FACE);
		glCullFace(GL_FRONT);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE);
		glDisable(GL_DEPTH_TEST);
		glDepthFunc(GL_GREATER);
		glDepthMask(false);
		
		Matrix4f viewMatrix = pipeline.getGBuffer().getViewMatrix();
		Matrix4f projMatrix = pipeline.getGBuffer().getProjectionMatrix();
		Matrix4f iProjMatrix = pipeline.getGBuffer().getInverseProjectionMatrix();
		
		synchronized(lights) {
			pipeline.shader_set(lightShader);
			lightShader.setProjectionMatrix(projMatrix);
			lightShader.setViewMatrix(viewMatrix);
			
			lightShader.shader_set_uniform_matrix( lightShader.shader_get_uniform( U_INVERSE_PROJ_MAT ), iProjMatrix );
			lightShader.shader_set_uniform_f( lightShader.shader_get_uniform( U_TEXEL), 1f/(float)pipeline.getWidth(), 1f/(float)pipeline.getHeight());
			lightShader.texture_set_stage( lightShader.shader_get_uniform( U_TEXTURE_ALBEDO), pipeline.getGBuffer().getBuffer0(), 0);
			lightShader.texture_set_stage( lightShader.shader_get_uniform( U_TEXTURE_DEPTH), pipeline.getGBuffer().getBufferDepth(), 1);
			lightShader.texture_set_stage( lightShader.shader_get_uniform( U_TEXTURE_NORMAL), pipeline.getGBuffer().getBuffer1(), 2);
			lightShader.texture_set_stage( lightShader.shader_get_uniform( U_TEXTURE_PBR), pipeline.getGBuffer().getBuffer2(), 3);
				
			for (int i = 0; i < lights.size(); i++) {
				PointLightInternal light = lights.get(i);
				if ( !light.visible )
					continue;
				
				Vector4f lightEyePos = new Vector4f(light.position.x, light.position.y, light.position.z, 1.0f);
				viewMatrix.transform(lightEyePos, lightEyePos);

				lightShader.shader_set_uniform_f( lightShader.shader_get_uniform( U_L_RADIUS), light.radius );
				lightShader.shader_set_uniform_f( lightShader.shader_get_uniform( U_L_INTENSITY), light.intensity );
				lightShader.shader_set_uniform_f( lightShader.shader_get_uniform( U_L_POSITION), lightEyePos.x, lightEyePos.y, lightEyePos.z );
				lightShader.shader_set_uniform_f( lightShader.shader_get_uniform( U_L_COLOR), light.color.x, light.color.y, light.color.z );
				
				tempLightMatrix.identity();
				tempLightMatrix.translate(light.position.x, light.position.y, light.position.z);
				tempLightMatrix.scale(light.radius);
				
				mesh.render(lightShader, tempLightMatrix, null);
			}
		}
		
		glEnable(GL_DEPTH_TEST);
		glCullFace(GL_BACK);
		glDisable(GL_BLEND);
		glDepthMask(true);
		glDepthFunc(GL_LESS);
	}
	
	static class PointLightShader extends BaseShader {
		public PointLightShader() {
			super(
				new URL[] {
						PointLightInternal.class.getResource("pointlightDeferred.vert")
				},
				new URL[] {
						PointLightInternal.class.getResource("pbr.frag"),
						PointLightInternal.class.getResource("pointlight.frag"),
						PointLightInternal.class.getResource("pointlightDeferred.frag")
				}
			);
		}
	}

	public synchronized PointLightInternal[] getLights() {
		synchronized(lights) {
			return lights.toArray(new PointLightInternal[lights.size()]);
		}
	}
}
