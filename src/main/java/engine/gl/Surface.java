/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameterf;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_R16F;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;

import java.nio.IntBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import lwjgui.paint.Color;
import lwjgui.gl.GenericShader;
import lwjgui.gl.TexturedQuad;

public class Surface {
	public static boolean textureRepeat = true;
	public static int filter = GL_LINEAR;
	public static boolean depthOnly;
	private int fboId;
	private int textureId;
	private int depthTextureId;
	private int width;
	private int height;
	private int targets = 1;

	protected boolean loaded;

	private Surface depthSurface;

	private Texture2D diffuseTexture;
	private ArrayList<Integer> externalTextures = new ArrayList<Integer>();

	//Draw buffers
	private static final int MAX_RENDER_TARGETS = 6;
	protected static final IntBuffer[] drawBuffers;

	static{
		drawBuffers = new IntBuffer[MAX_RENDER_TARGETS + 1];
		for(int i = 0; i <= MAX_RENDER_TARGETS; i++){
			IntBuffer db = BufferUtils.createIntBuffer(i);
			for(int j = 0; j < i; j++){
				db.put(GL_COLOR_ATTACHMENT0 + j);
			}
			db.rewind();
			drawBuffers[i] = db;
		}
	}

	protected Surface(Surface parent, int textureId) {
		this.textureId = textureId;

		this.depthTextureId = parent.depthTextureId;
		this.fboId = parent.fboId;
		this.width = parent.width;
		this.height = parent.height;
		this.targets = parent.targets;

		this.depthSurface = parent.depthSurface;
		this.diffuseTexture = textureId==-1?null:new Texture2D(textureId, GL_TEXTURE_2D, parent.diffuseTexture.getInternalFormat(), GL_UNSIGNED_BYTE, GL_RGBA);
	}

	public Surface( int width, int height, int internalFormat ) {

		this.width = width;
		this.height = height;

		this.fboId = glGenFramebuffers();
		this.textureId = !depthOnly?generateTexture( false, internalFormat ):-1;
		this.depthTextureId = generateTexture( true, GL14.GL_DEPTH_COMPONENT24 );

		// Bind FBO
		glBindFramebuffer(GL_FRAMEBUFFER, fboId);
		//glEnable(GL_FRAMEBUFFER_SRGB);

		// Bind diffuse to FBO
		if ( textureId != -1 ) {
			glBindTexture (GL_TEXTURE_2D, textureId);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);
		}

		// Bind depth to FBO
		if ( depthTextureId != -1 ) {
			glBindTexture (GL_TEXTURE_2D, depthTextureId);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureId, 0);
		}

		// Finish
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		this.diffuseTexture = new Texture2D(getTextureId(), GL_TEXTURE_2D, GL_RGBA, GL_UNSIGNED_BYTE, GL_RGBA);
		this.depthSurface   = new Surface( this, depthTextureId );
		this.loaded = true;
	}

	public Surface(int width, int height, boolean soloChannel) {
		this( width, height, soloChannel?GL_R16F:GL_RGBA16F );
	}
	
	public Surface(int width, int height) {
		this(width, height, false);
	}
	
	public void cleanup() {
		if ( textureId > 0 ) {
			GL11.glDeleteTextures(textureId);
		}
		if ( depthTextureId > 0 ) {
			GL11.glDeleteTextures(depthTextureId);
		}
		if ( fboId > 0 ) {
			GL30.glDeleteFramebuffers(fboId);
		}
		
		for (int i = 0; i < externalTextures.size(); i++) {
			int tex = externalTextures.get(i);
			GL11.glDeleteTextures(tex);
		}
		externalTextures.clear();
		
		textureId = -1;
		depthTextureId = -1;
		fboId = -1;
	}

	public int generateTexture( boolean isDepthBuffer, int internalFormat ) {
		int t = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, t);
		if (textureRepeat) {
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		} else {
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		}

		// find mag filter
		int mag = GL_LINEAR;
		if ( filter == GL_NEAREST )
			mag = GL_NEAREST;

		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, mag);
		
		//int internalFormat = isDepthBuffer?GL14.GL_DEPTH_COMPONENT24:(highPrecision?(soloChannel?GL_R16F:HP):(soloChannel?GL_R8:(colorSpace==GL_RGBA?LP:GL_SRGB8_ALPHA8)));
		int externalFormat = isDepthBuffer?GL_DEPTH_COMPONENT:GL_RGBA;
		
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, externalFormat, GL_FLOAT, (java.nio.ByteBuffer)null);

		return t;
	}

	public void bindTexture(int id, int attachment) {
		targets++;

		glBindFramebuffer(GL_FRAMEBUFFER, fboId);
		glBindTexture (GL_TEXTURE_2D, id);

		// Bind texture to FBO
		glFramebufferTexture2D(GL_FRAMEBUFFER, attachment, GL_TEXTURE_2D, id, 0);
		GL20.glDrawBuffers(drawBuffers[targets]);

		// unbind FBO
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		
		externalTextures.add(id);
	}

	public void bind() {
		glViewport(0, 0, width, height);

		glBindTexture(GL_TEXTURE_2D, 0);
		glBindFramebuffer(GL_FRAMEBUFFER, fboId);
	}
	
	public void unbind() {
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	public float getWidth() {
		return this.width;
	}

	public float getHeight() {
		return this.height;
	}

	public int getTextureId() {
		return textureId==-1?getDepthTextureId():this.textureId;
	}

	public int getDepthTextureId() {
		return this.depthTextureId;
	}

	public Surface getDepthSurface() {
		return this.depthSurface;
	}

	public Texture2D getTextureDiffuse() {
		return diffuseTexture;
	}

	public Texture2D getTextureDepth() {
		return this.depthSurface.getTextureDiffuse();
	}

	
	private TexturedQuad quad;
	public void render(GenericShader shader) {
		render(shader, false);
	}
	
	public void render(GenericShader shader, boolean flipY) {
		int w = width;
		int h = height;
		
		shader.bind();
		if ( flipY )
			shader.projectOrtho(0, h, w, -h);
		else
			shader.projectOrtho(0, 0, w, h);
		
		if ( quad == null ) {
			quad = new TexturedQuad(0, 0, w, h, getTextureId());
		}
		quad.render();
	}
	
	/**
	 * Clear a surface to a desired color.
	 * @param color
	 * @param alpha
	 */
	public void draw_clear_alpha(Color color, float alpha) {
		glClearColor(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, alpha);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}
}