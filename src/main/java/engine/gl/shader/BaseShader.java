/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.shader;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1fv;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUniform2i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform3i;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUniform4i;
import static org.lwjgl.opengl.GL20.glUniformMatrix3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import engine.gl.SkyBox;
import engine.gl.Texture2D;
import engine.io.Image;
import engine.util.TextureUtils;
import lwjgui.paint.Color;
import lwjgui.scene.Context;

public class BaseShader {
	private final int id;
	private final int[] vertexId;
	private final int[] fragmentId;

	protected final int posLoc;
	protected final int normalLoc;
	protected final int texCoordLoc;
	protected final int colorLoc;

	public int projMatLoc = -1;
	public int viewMatLoc = -1;
	public int worldMatLoc = -1;
	public int worldNormalMatLoc = -1;
	public int normalMatLoc = -1;

	public boolean debug = false;

	public int diffuseTextureLoc = -1;
	public int normalTextureLoc = -1;
	public int metalnessTextureLoc = -1;
	public int roughnessTextureLoc = -1;
	private Texture2D baseTexture;

	private HashMap<String,Integer> uniforms = new HashMap<String,Integer>();

	private static final Matrix4f IDENTITY_MATRIX = new Matrix4f();
	

	private Matrix4f lastWorldMatrix = new Matrix4f();
	private Matrix4f lastViewMatrix = new Matrix4f();
	private Matrix4f lastProjectionMatrix = new Matrix4f();
	private HashMap<Integer,Integer> lastSetTexture = new HashMap<Integer,Integer>();
	private HashMap<Integer,Object> lastSetUniform = new HashMap<Integer,Object>();

	public BaseShader() {
		this(
			new URL[] {BaseShader.class.getResource("vertex.glsl")},
			new URL[] {BaseShader.class.getResource("fragment.glsl")}
		);
	}

	public BaseShader(URL[] vertexShader, URL[] fragmentShader) {

		// make the shader
		vertexId = compileShader(vertexShader, GL20.GL_VERTEX_SHADER);
		fragmentId = compileShader(fragmentShader, GL20.GL_FRAGMENT_SHADER);
		posLoc = 0;
		normalLoc = 1;
		texCoordLoc = 2;
		colorLoc = 3;
		id = createProgram(
				vertexId,
				fragmentId,
				new String[] { "inPos", "inNormal", "inTexCoord", "inColor" },
				new int[] { posLoc, normalLoc, texCoordLoc, colorLoc }
				);

		projMatLoc = GL20.glGetUniformLocation(id, "projectionMatrix");
		viewMatLoc = GL20.glGetUniformLocation(id, "viewMatrix");
		worldMatLoc = GL20.glGetUniformLocation(id, "worldMatrix");
		worldNormalMatLoc = GL20.glGetUniformLocation(id, "worldNormalMatrix");
		normalMatLoc = GL20.glGetUniformLocation(id, "normalMatrix");

		diffuseTextureLoc = GL20.glGetUniformLocation(id, "texture_diffuse");
		normalTextureLoc = GL20.glGetUniformLocation(id, "texture_normal");
		metalnessTextureLoc = GL20.glGetUniformLocation(id, "texture_metalness");
		roughnessTextureLoc = GL20.glGetUniformLocation(id, "texture_roughness");

		// Generic white texture
		baseTexture = TextureUtils.loadRGBATextureFromImage(new Image(Color.WHITE,1,1));
	}

	public void bind() {
		GL20.glUseProgram(id);

		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		baseTexture.bind();
		
		this.shader_set_uniform_f(this.shader_get_uniform("uMaterialColor"), new Vector3f(1,1,1));

		lastWorldMatrix.zero();
		lastViewMatrix.zero();
		lastProjectionMatrix.zero();
		lastSetTexture.clear();
		lastSetUniform.clear();
	}

	public void cleanup() {
		for (int i = 0; i < vertexId.length; i++)
			GL20.glDeleteShader(vertexId[i]);

		for (int i = 0; i < fragmentId.length; i++)
			GL20.glDeleteShader(fragmentId[i]);

		GL20.glDeleteProgram(id);
	}

	public void create(int id) {
		//
	}

	protected int createProgram(int[] vertexShaderIds, int[] fragmentShaderIds, String[] attrs, int[] indices) {

		// build the shader program
		int id = GL20.glCreateProgram();
		for (int vertexShaderId : vertexShaderIds) {
			GL20.glAttachShader(id, vertexShaderId);
		}
		for (int fragmentShaderId : fragmentShaderIds) {
			GL20.glAttachShader(id, fragmentShaderId);
		}

		create(id);

		assert (attrs.length == indices.length);
		for (int i=0; i<attrs.length; i++) {
			GL20.glBindAttribLocation(id, indices[i], attrs[i]);
		}

		GL20.glLinkProgram(id);
		boolean isSuccess = GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_TRUE;
		if (!isSuccess) {
			throw new RuntimeException("Shader program did not link:\n" + GL20.glGetProgramInfoLog(id, 4096));
		}

		return id;
	}

	protected static int[] compileShader(URL[] shader, int type) {
		int[] ret = new int[shader.length];
		
		for (int i = 0; i < shader.length; i++) {
			try (InputStream in = shader[i].openStream();
					InputStreamReader isr = new InputStreamReader(in);
					BufferedReader br = new BufferedReader(isr)) {
				String source = br.lines().collect(Collectors.joining("\n"));
				ret[i] = compileShader(source, type);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException("can't compile shader at: " + shader[i], ex);
			}
		}

		return ret;
	}

	protected static int compileShader(String source, int type) {

		// try to massage JavaFX shaders into modern OpenG
		if (source.startsWith("#ifdef GL_ES\n")) {
			source = modernizeShader(source, type == GL20.GL_VERTEX_SHADER);
		}

		int id = GL20.glCreateShader(type);
		GL20.glShaderSource(id, source);
		GL20.glCompileShader(id);

		boolean isSuccess = GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) != GL11.GL_FALSE;
		if (!isSuccess) {

			// get debug info
			StringBuilder buf = new StringBuilder();
			buf.append("Shader did not compile\n");

			// show the compiler log
			buf.append("\nCOMPILER LOG:\n");
			buf.append(GL20.glGetShaderInfoLog(id, 4096));

			// show the source with correct line numbering
			buf.append("\nSOURCE:\n");
			String[] lines = source.split("\\n");
			for (int i=0; i<lines.length; i++) {
				buf.append(String.format("%4d: ", i + 1));
				buf.append(lines[i]);
				buf.append("\n");
			}

			throw new RuntimeException(buf.toString());
		}

		return id;
	}

	private static String modernizeShader(String source, boolean isVertex) {

		// replace attribute with in
		source = source.replaceAll("attribute ", "in ");

		if (isVertex) {

			// replace varying with out
			source = source.replaceAll("varying ", "out ");

		} else {

			// replace varying with in
			source = source.replaceAll("varying ", "in ");

			// add an out var for the color
			source = source.replaceAll("gl_FragColor", "outFragColor");
			source = "out vec4 outFragColor;\n\n" + source;

			// replace calls to texture2D with texture
			source = source.replaceAll("texture2D", "texture");
		}

		source = "#version 150\n\n" + source;

		return source;
	}

	private FloatBuffer matrix44Buffer = BufferUtils.createFloatBuffer(16);
	private FloatBuffer matrix33Buffer = BufferUtils.createFloatBuffer(9);

	/**
	 * Fits the projection around the current contexts size.
	 * @param context
	 */
	public void project(Context context) {
		int width = context.getWidth();
		int height = context.getHeight();

		projectOrtho(0, 0, width, height);
	}

	/**
	 * Manually fit the projection.
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	private Matrix4f orthoMatrix = new Matrix4f();
	public void projectOrtho(float x, float y, float w, float h) {
		setProjectionMatrix(orthoMatrix.identity().ortho(x, x+w, y+h, y, -32000, 32000));
		setViewMatrix(IDENTITY_MATRIX);
		setWorldMatrix(IDENTITY_MATRIX);
	}

	public void setProjectionMatrix(Matrix4f mat) {
		if ( projMatLoc == -1 || mat.equals(lastProjectionMatrix) )
			return;

		mat.get(matrix44Buffer);
		glUniformMatrix4fv(projMatLoc, false, matrix44Buffer);
		lastProjectionMatrix.set(mat);
	}

	private Matrix4f inverseViewMatrix = new Matrix4f();
	public void setViewMatrix(Matrix4f mat) {
		if ( viewMatLoc == -1 || mat.equals(lastViewMatrix) )
			return;

		mat.get(matrix44Buffer);
		glUniformMatrix4fv(viewMatLoc, false, matrix44Buffer);
		
		int iViewMatrixLoc = shader_get_uniform("uInverseViewMatrix");
		if ( iViewMatrixLoc > 0 ) {
			shader_set_uniform_matrix(iViewMatrixLoc, inverseViewMatrix.set(mat).invert());
		}

		if ( normalMatLoc > 0 ) {
			Matrix3f normalMatrix = mat.normal(tempNormal);
			normalMatrix.get(matrix33Buffer);
			glUniformMatrix3fv(normalMatLoc, false, matrix33Buffer);
		}
		lastViewMatrix.set(mat);
	}

	private static final Matrix3f IDENTITY_NORMAL_MATRIX = new Matrix3f().identity().normal();
	private Matrix3f tempNormal = new Matrix3f();
	public void setWorldMatrix(Matrix4f mat) {
		if ( worldMatLoc == -1 || mat.equals(lastWorldMatrix) )
			return;

		mat.get(matrix44Buffer);
		glUniformMatrix4fv(worldMatLoc, false, matrix44Buffer);

		if ( worldNormalMatLoc > 0 ) {
			
			// Check if there is a rotation
			boolean hasRotation = true;
			if ( mat.m00() == 1 && mat.m11() == 1 && mat.m22() == 1) {
				hasRotation = false;
			}
			
			// Choose which normal matrix to use
			if ( hasRotation )
				mat.normal(tempNormal);
			else
				tempNormal.set(IDENTITY_NORMAL_MATRIX);
			
			// Store normal to shader
			tempNormal.get(matrix33Buffer);
			glUniformMatrix3fv(worldNormalMatLoc, false, matrix33Buffer);
		}
		lastWorldMatrix.set(mat);
	}

	public int getProgram() {
		return id;
	}

	/**
	 * Set a uniform integer value.
	 * @param handle
	 * @param value
	 */
	public void shader_set_uniform_i(int handle, int ... value ) {
		if (handle == -1)
			return;

		// Check if last uniform at this handle is the same value (dont resend it to GPU)
		if ( eq(value, lastSetUniform.get(handle)) ) {
			return;
		}
		lastSetUniform.put(handle, value);
		
		int len = value.length;
		switch(len) {
			case 1: {
				glUniform1i(handle, value[0]);
				break;
			}
			case 2: {
				glUniform2i(handle, value[0], value[1]);
				break;
			}
			case 3: {
				glUniform3i(handle, value[0], value[1], value[2]);
				break;
			}
			case 4: {
				glUniform4i(handle, value[0], value[1], value[2], value[3]);
				break;
			}
		}
	}
	
	private boolean eq(float[] a, Object object) {
		if ( ! (object instanceof float[]) )
			return false;
		
		float[] b = (float[]) object;
		if ( a.length != b.length )
			return false;
		
		for (int i = 0; i < a.length; i++) {
			if ( b[i] != a[i] )
				return false;
		}
		
		return true;
	}
	
	private boolean eq(int[] a, Object object) {
		if ( ! (object instanceof int[]) )
			return false;
		
		int[] b = (int[]) object;
		if ( a.length != b.length )
			return false;
		
		for (int i = 0; i < a.length; i++) {
			if ( b[i] != a[i] )
				return false;
		}
		
		return true;
	}
	
	/**
	 * Set a uniform float value.
	 * @param handle
	 * @param value
	 */
	public void shader_set_uniform_f(int handle, float ... value ) {
		if (handle == -1)
			return;
		
		// Check if last uniform at this handle is the same value (dont resend it to GPU)
		if ( eq(value, lastSetUniform.get(handle)) ) {
			return;
		}
		lastSetUniform.put(handle, value);

		int len = value.length;
		switch(len) {
			case 1: {
				glUniform1f(handle, value[0]);
				break;
			}
			case 2: {
				glUniform2f(handle, value[0], value[1]);
				break;
			}
			case 3: {
				glUniform3f(handle, value[0], value[1], value[2]);
				break;
			}
			case 4: {
				glUniform4f(handle, value[0], value[1], value[2], value[3]);
				break;
			}
		}
	}

	/**
	 * Set a 2-dimensional uniform float value
	 * @param handle
	 * @param vector
	 */
	public void shader_set_uniform_f( int handle, Vector2f vector ) {
		shader_set_uniform_f( handle, vector.x, vector.y );
	}

	/**
	 * Set a 3-dimensional uniform float value
	 * @param handle
	 * @param vector
	 */
	public void shader_set_uniform_f( int handle, Vector3f vector ) {
		shader_set_uniform_f( handle, vector.x, vector.y, vector.z );
	}

	/**
	 * Set a 4-dimensional uniform float value
	 * @param handle
	 * @param vector
	 */
	public void shader_set_uniform_f( int handle, Vector4f vector ) {
		shader_set_uniform_f( handle, vector.x, vector.y, vector.z, vector.w );
	}

	public void shader_set_uniform_array(int handle, float ... value ) {
		if (handle == -1)
			return;
		glUniform1fv( handle, value );
	}

	/**
	 * Set a uniform value by a matrix.
	 * @param handle
	 * @param matrix
	 */
	public void shader_set_uniform_matrix(int handle, Matrix3f matrix) {
		if (handle == -1)
			return;

		matrix.get(matrix33Buffer);
		GL20.glUniformMatrix3fv(handle, false, matrix33Buffer);
	}

	/**
	 * Set a uniform value by a matrix.
	 * @param handle
	 * @param matrix
	 */
	public void shader_set_uniform_matrix(int handle, Matrix4f matrix) {
		if (handle == -1)
			return;

		matrix.get(matrix44Buffer);
		GL20.glUniformMatrix4fv(handle, false, matrix44Buffer);
	}

	/**
	 * Get a uniform location from a shader.
	 * @param shader
	 * @param uniform
	 * @return
	 */
	public int shader_get_uniform(String uniform) {
		int location = -1;
		if ( uniforms.containsKey(uniform) ) {
			location = uniforms.get(uniform);
		} else {
			location = GL20.glGetUniformLocation(id, uniform);
			uniforms.put(uniform, location);
		}

		return location;
	}

	/**
	 * Sets the current texture unit in the shader.
	 * @param uniform
	 * @param texture
	 * @param unit
	 */
	public void texture_set_stage(int uniform, Texture2D texture, int unit) {
		if ( texture == null || (lastSetTexture.containsKey(unit)&&lastSetTexture.get(unit)==texture.getID()) )
			return;

		GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
		texture.bind();
		if ( uniform != -1 ) {
			shader_set_uniform_i(uniform, unit);
		}
		
		lastSetTexture.put(unit, texture.getID());
	}

	/**
	 * Sets the current texture unit in the shader.
	 * @param uniform
	 * @param texture
	 * @param unit
	 */
	public void texture_set_stage(int uniform, int texture, int unit) {
		if ( texture == -1 || (lastSetTexture.containsKey(unit)&&lastSetTexture.get(unit)==texture) )
			return;

		GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
		glBindTexture(GL11.GL_TEXTURE_2D, texture);
		if ( uniform != -1 ) {
			shader_set_uniform_i(uniform, unit);
		}

		lastSetTexture.put(unit, texture);
	}

	/**
	 * Sets the current texture unit in the shader.
	 * @param uniform
	 * @param texture
	 * @param unit
	 */
	public void texture_set_stage(int uniform, SkyBox texture, int unit) {
		if ( texture == null || (lastSetTexture.containsKey(unit)&&lastSetTexture.get(unit)==texture.getTextureID()) )
			return;

		GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
		texture.bind();
		if ( uniform != -1 ) {
			shader_set_uniform_i(uniform, unit);
		}
		
		lastSetTexture.put(unit, texture.getTextureID());
	}
}
