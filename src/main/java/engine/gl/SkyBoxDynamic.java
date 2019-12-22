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

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.io.Image;

public abstract class SkyBoxDynamic extends SkyBox {

	private Matrix4f tempMat4 = new Matrix4f();
	private Matrix4f tempViewMatrix = new Matrix4f();
	
	public SkyBoxDynamic(Image image) {
		super(image);
	}

	@Override
	public boolean draw(LegacyPipeline pipeline) {
		// Bind cubemap
		glBindTexture(GL_TEXTURE_CUBE_MAP, cubemap);
		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
		glViewport(0, 0, width, height);
		
		Matrix4f projectionMatrix = tempMat4.identity().perspective((float)Math.toRadians(90), 1.0f, 1.0f, 1024);

		for (int i = 0; i < 6; i++) {
			int face = i;

	        // attach new texture and renderbuffer to fbo
	        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X + (int)face, cubemap, 0);

	        // clear
	        glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);

	        Matrix4f viewMatrix = tempViewMatrix.identity();
	        viewMatrix.lookAt(new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1));
	        switch(face) {
		        case 0: { // POSITIVE X
			        	// Do nothing.
			        	break;
		        }

		        case 1: { // NEGATIVE X
			        	viewMatrix.rotateZ((float)Math.PI);
			        	break;
		        }

		        case 2: { // POSITIVE Y
			        	viewMatrix.rotateY((float)-Math.PI/2f);
			        	viewMatrix.rotateZ((float)-Math.PI/2f);
			        	break;
		        }

		        case 3: { // NEGATIVE Y
			        	viewMatrix.rotateY((float)Math.PI/2f);
			        	viewMatrix.rotateZ((float)-Math.PI/2f);
			        	break;
		        }

		        case 4: { // POSITIVE Z
			        	viewMatrix.rotateZ((float)-Math.PI/2f);
			        	break;
		        }

		        case 5: { // NEGATIVE Z
			        	viewMatrix.rotateZ((float)Math.PI/2f);
			        	break;
		        }
	        }

	        pipeline.shader_get().setProjectionMatrix( projectionMatrix );
	        pipeline.shader_get().setViewMatrix( viewMatrix );
	        renderGeometry(pipeline);
		}

		// Unbind FBO
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, 0, 0);
		glBindFramebuffer(GL_FRAMEBUFFER, 0);

		// Generate mipmaps
		glBindTexture(GL_TEXTURE_CUBE_MAP, cubemap);
		glGenerateMipmap(GL_TEXTURE_CUBE_MAP);

		return true;
	}



	protected abstract void renderGeometry(LegacyPipeline pipeline);

}
