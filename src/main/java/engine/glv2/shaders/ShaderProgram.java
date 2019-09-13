/*
 * This file is part of Light Engine
 * 
 * Copyright (C) 2016-2019 Lux Vacuos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package engine.glv2.shaders;

import static org.lwjgl.opengl.GL11C.GL_FALSE;
import static org.lwjgl.opengl.GL20C.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20C.glAttachShader;
import static org.lwjgl.opengl.GL20C.glBindAttribLocation;
import static org.lwjgl.opengl.GL20C.glCompileShader;
import static org.lwjgl.opengl.GL20C.glCreateProgram;
import static org.lwjgl.opengl.GL20C.glCreateShader;
import static org.lwjgl.opengl.GL20C.glDeleteProgram;
import static org.lwjgl.opengl.GL20C.glDeleteShader;
import static org.lwjgl.opengl.GL20C.glDetachShader;
import static org.lwjgl.opengl.GL20C.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20C.glGetShaderi;
import static org.lwjgl.opengl.GL20C.glLinkProgram;
import static org.lwjgl.opengl.GL20C.glShaderSource;
import static org.lwjgl.opengl.GL20C.glUseProgram;
import static org.lwjgl.opengl.GL20C.glValidateProgram;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL;

import engine.glv2.exceptions.CompileShaderException;
import engine.glv2.exceptions.LoadShaderException;
import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.IUniform;

public abstract class ShaderProgram {
	private int program;
	private boolean loaded;
	private List<IUniform> uniforms = new ArrayList<>();
	private List<Shader> shaders = new ArrayList<>();
	private Attribute[] attributes;

	public ShaderProgram(String vertexFile, String fragmentFile, Attribute... attributes) {
		this.attributes = attributes;
		shaders.add(new Shader(vertexFile, GL_VERTEX_SHADER));
		shaders.add(new Shader(fragmentFile, GL_FRAGMENT_SHADER));
		this.loadShaderProgram();
		loaded = true;
	}

	public ShaderProgram(String vertexFile, String geometryFile, String fragmentFile, Attribute... attributes) {
		this.attributes = attributes;
		shaders.add(new Shader(vertexFile, GL_VERTEX_SHADER));
		shaders.add(new Shader(geometryFile, GL_GEOMETRY_SHADER));
		shaders.add(new Shader(fragmentFile, GL_FRAGMENT_SHADER));
		this.loadShaderProgram();
		loaded = true;
	}

	protected void storeUniforms(IUniform... uniforms) {
		for (IUniform uniform : uniforms)
			uniform.storeUniformLocation(program);
		this.uniforms.addAll(Arrays.asList(uniforms));
	}

	protected void loadInitialData() {
	}

	protected void validate() {
		glValidateProgram(program);
	}

	public void start() {
		glUseProgram(program);
	}

	public void stop() {
		glUseProgram(0);
	}

	public void reload() {
		glDeleteProgram(program);
		this.loadShaderProgram();
		for (IUniform uniform : uniforms)
			uniform.storeUniformLocation(program);
		this.validate();
		this.loadInitialData();
	}

	public void dispose() {
		if (!loaded)
			return;
		loaded = false;
		glDeleteProgram(program);
		shaders.clear();
		for (IUniform uniform : uniforms)
			uniform.dispose();
		uniforms.clear();
	}

	private void bindAttributes(Attribute[] attributes) {
		for (Attribute attribute : attributes)
			glBindAttribLocation(program, attribute.getId(), attribute.getName());
	}

	private void loadShaderProgram() {
		program = glCreateProgram();
		for (Shader shader : shaders)
			glAttachShader(program, loadShader(shader));

		bindAttributes(attributes);
		glLinkProgram(program);

		for (Shader shader : shaders) {
			glDetachShader(program, shader.id);
			glDeleteShader(shader.id);
		}
	}

	private int loadShader(Shader shader) {
		StringBuilder shaderSource = new StringBuilder();
		InputStream filet = getClass().getClassLoader().getResourceAsStream(shader.file);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(filet))) {

			System.out.println("Loading Shader: " + shader.file);

			shaderSource.append("#version 330 core").append("//\n");
			if (!GL.getCapabilities().GL_ARB_clip_control)
				shaderSource.append("#define OneToOneDepth").append("//\n");
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#include")) {
					String[] split = line.split(" ");
					String name = split[2];
					if (split[1].equalsIgnoreCase("variable"))
						shaderSource.append(ShaderIncludes.getVariable(name)).append("//\n");
					else if (split[1].equalsIgnoreCase("struct"))
						shaderSource.append(ShaderIncludes.getStruct(name)).append("//\n");
					else if (split[1].equalsIgnoreCase("function"))
						shaderSource.append(ShaderIncludes.getFunction(name)).append("//\n");
					continue;
				}
				shaderSource.append(line).append("//\n");
			}
		} catch (IOException e) {
			throw new LoadShaderException(e);
		}
		int shaderID = shader.id = glCreateShader(shader.type);
		glShaderSource(shaderID, shaderSource);
		glCompileShader(shaderID);
		System.out.println("test");
		if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == GL_FALSE) {
			System.out.println(glGetShaderInfoLog(shaderID, 500));
			throw new CompileShaderException(glGetShaderInfoLog(shaderID, 500));
		}
		return shaderID;
	}

	protected class Shader {
		protected final String file;
		protected final int type;
		protected int id;

		public Shader(String file, int type) {
			this.file = file;
			this.type = type;
		}
	}

}