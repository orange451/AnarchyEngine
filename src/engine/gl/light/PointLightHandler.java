package engine.gl.light;

import static org.lwjgl.opengl.GL11.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import engine.gl.Pipeline;
import engine.gl.mesh.BufferedMesh;
import engine.gl.shader.BaseShader;
import engine.util.MeshUtils;

public class PointLightHandler {
	private List<PointLight> lights = Collections.synchronizedList(new ArrayList<PointLight>());
	private BaseShader lightShader = new PointLightShader();
	private BufferedMesh mesh = MeshUtils.sphere(1, 16);
	
	public PointLightHandler() {
		int close = 8;
		int r = 48;
		int b = 10;
		int xx = 8;
		addLight(new PointLight(new Vector3f(-xx, close, xx), r, b));
		addLight(new PointLight(new Vector3f(xx, close, xx), r, b));
		addLight(new PointLight(new Vector3f(-xx, close, -xx), r, b));
		addLight(new PointLight(new Vector3f(xx, close, -xx), r, b));
		addLight(new PointLight(new Vector3f(0, -close*2, 0), r, b/2f));
	}
	
	public void addLight(PointLight l) {
		synchronized(lights) {
			lights.add(l);
		}
	}
	
	public void removeLight(PointLight l) {
		synchronized(lights) {
			lights.remove(l);
		}
	}
	
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
			
			lightShader.shader_set_uniform_matrix( lightShader.shader_get_uniform( "inverseProjectionMatrix"), iProjMatrix );
			lightShader.shader_set_uniform_f( lightShader.shader_get_uniform( "texel"), 1f/(float)pipeline.getWidth(), 1f/(float)pipeline.getHeight());
			lightShader.texture_set_stage( lightShader.shader_get_uniform( "texture_albedo"), pipeline.getGBuffer().getBuffer0(), 0);
			lightShader.texture_set_stage( lightShader.shader_get_uniform( "texture_depth"), pipeline.getGBuffer().getBufferDepth(), 1);
			lightShader.texture_set_stage( lightShader.shader_get_uniform( "texture_normal"), pipeline.getGBuffer().getBuffer1(), 2);
			lightShader.texture_set_stage( lightShader.shader_get_uniform( "texture_pbr"), pipeline.getGBuffer().getBuffer2(), 3);
				
			for (int i = 0; i < lights.size(); i++) {
				PointLight light = lights.get(i);
				Matrix4f worldMatrix = new Matrix4f();
				worldMatrix.translate(light.x, light.y, light.z);
				worldMatrix.scale(light.radius);
				
				Vector4f lightEyePos = new Vector4f(light.x, light.y, light.z, 1.0f);
				viewMatrix.transform(lightEyePos, lightEyePos);

				lightShader.shader_set_uniform_f( lightShader.shader_get_uniform( "radius"), light.radius );
				lightShader.shader_set_uniform_f( lightShader.shader_get_uniform( "intensity"), light.intensity );
				lightShader.shader_set_uniform_f( lightShader.shader_get_uniform( "lightPosition"), lightEyePos.x, lightEyePos.y, lightEyePos.z );
				
				mesh.render(lightShader, worldMatrix, null);
			}
		}
		
		glEnable(GL_DEPTH_TEST);
		glCullFace(GL_BACK);
		glDisable(GL_BLEND);
		glDepthMask(true);
		glDepthFunc(GL_LESS);
	}
	
	class PointLightShader extends BaseShader {
		public PointLightShader() {
			super(
				new URL[] {
						PointLight.class.getResource("pointlight.vert")
				},
				new URL[] {
						PointLight.class.getResource("pbr.frag"),
						PointLight.class.getResource("pointlight.frag")
				}
			);
		}
	}
}
