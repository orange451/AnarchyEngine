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

package engine.glv2;

import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RED;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameterf;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS;
import static org.lwjgl.opengl.GL30.GL_RG;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL32.GL_SYNC_FLUSH_COMMANDS_BIT;
import static org.lwjgl.opengl.GL32.GL_SYNC_GPU_COMMANDS_COMPLETE;
import static org.lwjgl.opengl.GL32.glClientWaitSync;
import static org.lwjgl.opengl.GL32.glDeleteSync;
import static org.lwjgl.opengl.GL32.glFenceSync;

import org.lwjgl.opengl.EXTTextureFilterAnisotropic;

import engine.glv2.objects.RawTexture;
import engine.resources.IResourcesManagerBackend;

public class GLResourcesManagerBackend implements IResourcesManagerBackend {

	public int loadTexture(int filter, int textureWarp, int format, boolean af, RawTexture data) {
		int textureID = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, textureID);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, textureWarp);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, textureWarp);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0);
		if (af) {
			float amount = Math.min(16f, EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
			glTexParameterf(GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, amount);
		} else {
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
		}
		if (data.getComp() == 3) {
			if ((data.getWidth() & 3) != 0)
				glPixelStorei(GL_UNPACK_ALIGNMENT, 2 - (data.getWidth() & 1));
			glTexImage2D(GL_TEXTURE_2D, 0, format, data.getWidth(), data.getHeight(), 0, GL_RGB, GL_UNSIGNED_BYTE,
					data.getBuffer());
		} else if (data.getComp() == 2)
			glTexImage2D(GL_TEXTURE_2D, 0, format, data.getWidth(), data.getHeight(), 0, GL_RG, GL_UNSIGNED_BYTE,
					data.getBuffer());
		else if (data.getComp() == 1)
			glTexImage2D(GL_TEXTURE_2D, 0, format, data.getWidth(), data.getHeight(), 0, GL_RED, GL_UNSIGNED_BYTE,
					data.getBuffer());
		else
			glTexImage2D(GL_TEXTURE_2D, 0, format, data.getWidth(), data.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE,
					data.getBuffer());
		glGenerateMipmap(GL_TEXTURE_2D);
		glBindTexture(GL_TEXTURE_2D, 0);

		long fence = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
		glClientWaitSync(fence, GL_SYNC_FLUSH_COMMANDS_BIT, 10000 * 1000000);
		glDeleteSync(fence);
		return textureID;
	}

}
