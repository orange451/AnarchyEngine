package engine.gl;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTexSubImage2D;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;

public class Texture2D {

	private int target, internalFormat, dataType, format, width, height;

	protected int id = -1;

	public Texture2D(int target, int internalFormat, int dataType, int format) {
		this.target = target;
		this.internalFormat = internalFormat;
		this.dataType = dataType;
		this.format = format;
	}

	public void gen() {
		id = glGenTextures();
	}

	public Texture2D(int id, int target, int internalFormat, int dataType, int format) {
		this.target = target;
		this.internalFormat = internalFormat;
		this.dataType = dataType;
		this.format = format;
		this.id = id;
	}

	public void bind() {
		if ( id == -1 )
			return;
		glBindTexture(target, id);
	}

	public void unbind() {
		if ( id == -1 )
			return;
		
		glBindTexture(target, 0);
	}

	public void load(int level, int width, int height, ByteBuffer data) {
		if ( id == -1 )
			return;
		
		glTexImage2D(target, level, internalFormat, width, height, 0, format, dataType, data);
		this.width = width;
		this.height = height;
	}

	public void load(int level, int width, int height, ShortBuffer data){
		if ( id == -1 )
			return;
		
		glTexImage2D(target, level, internalFormat, width, height, 0, format, dataType, data);
		this.width = width;
		this.height = height;
	}

	public void load(int level, int width, int height, IntBuffer data){
		if ( id == -1 )
			return;
		
		glTexImage2D(target, level, internalFormat, width, height, 0, format, dataType, data);
		this.width = width;
		this.height = height;
	}

	public void load(int level, int width, int height, FloatBuffer data){
		if ( id == -1 )
			return;
		
		glTexImage2D(target, level, internalFormat, width, height, 0, format, dataType, data);
		this.width = width;
		this.height = height;
	}

	public void load(int level, int width, int height, DoubleBuffer data){
		if ( id == -1 )
			return;
		
		glTexImage2D(target, level, internalFormat, width, height, 0, format, dataType, data);
		this.width = width;
		this.height = height;
	}

	public void subLoad(int x, int y, int level, int width, int height, ByteBuffer data){
		if ( id == -1 )
			return;
		
		glTexSubImage2D(target, level, x, y, width, height, format, dataType, data);
	}

	public void subLoad(int x, int y, int level, int width, int height, ShortBuffer data){
		if ( id == -1 )
			return;
		
		glTexSubImage2D(target, level, x, y, width, height, format, dataType, data);
	}

	public void subLoad(int x, int y, int level, int width, int height, IntBuffer data){
		if ( id == -1 )
			return;
		
		glTexSubImage2D(target, level, x, y, width, height, format, dataType, data);
	}

	public void subLoad(int x, int y, int level, int width, int height, FloatBuffer data){
		if ( id == -1 )
			return;
		
		glTexSubImage2D(target, level, x, y, width, height, format, dataType, data);
	}

	public void subLoad(int x, int y, int level, int width, int height, DoubleBuffer data){
		if ( id == -1 )
			return;
		
		glTexSubImage2D(target, level, x, y, width, height, format, dataType, data);
	}

	public void subLoad(int x, int y, int level, int width, int height, int offset){
		if ( id == -1 )
			return;
		
		glTexSubImage2D(target, level, x, y, width, height, format, dataType, offset);
	}

	public void setMipMapLevels(int baseLevel, int lastLevel){
		if ( id == -1 )
			return;
		
		glTexParameteri(target, GL_TEXTURE_BASE_LEVEL, baseLevel);
		glTexParameteri(target, GL_TEXTURE_MAX_LEVEL, lastLevel);
	}

	public void setMipMapFilter(int nearest, int farthest) {
		if ( id == -1 )
			return;
		
		GL11.glTexParameteri(target, GL11.GL_TEXTURE_MAG_FILTER, nearest);
		GL11.glTexParameteri(target, GL11.GL_TEXTURE_MIN_FILTER, farthest);
		GL11.glTexParameterf( GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 16 );
	}

	public void generateMipmaps(){
		if ( id == -1 )
			return;
		
		glGenerateMipmap(target);
	}

	public void unload(){
		if ( id == -1 )
			return;
		
		glDeleteTextures(id);
		id = glGenTextures();
	}

	public void delete(){
		if ( id == -1 )
			return;
		
		glDeleteTextures(id);
	}

	public int getTarget(){
		return target;
	}

	public int getID(){
		return id;
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public int getInternalFormat() {
		return this.internalFormat;
	}
}