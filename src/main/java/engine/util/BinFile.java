package engine.util;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

public class BinFile {
	private int pointer;
	private int[] bytes;
	private boolean loaded;
	
	public BinFile(String filePath) {
		try {
			InputStream input = null;
			DataInputStream in = null;
			try {
				
				//Create the input stream
				URL url = IOUtil.ioResourceGetURL(filePath);
				if ( url == null )
					return;
				input = url.openStream();
					
				
				//create the byte array
				int amountBytes = input.available();
				bytes = new int[amountBytes];
				
				//Create input stream
				in = new DataInputStream(input);
				
				//Fill byte array with file data
				int count = 0;
				for (int i = 0; i < amountBytes; i++) {
					bytes[count] = in.read();
					count++;
				}
				loaded = true;
			} finally {
				if (input != null)
					input.close();
				
				if (in != null)
					in.close();
			}
		} catch (FileNotFoundException ex) {
			System.out.println("File not found.");
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}
	
	public ByteBuffer getByteBuffer() {
		ByteBuffer ret = BufferUtils.createByteBuffer(bytes.length);
		for (int i = 0; i < bytes.length; i++) {
			ret.put((byte) (bytes[i] & 0xFF));
		}
		
		ret.flip();
		return ret;
	}
	
	public int readByte() {
		pointer++;
		return (int) bytes[pointer - 1];
	}
	
	public boolean hasNext() {
		return pointer < bytes.length - 1;
	}

	public int readInt() {
		int ne1 = readByte();
		int ne2 = readByte();
		int ne3 = readByte();
		int ne4 = readByte();
		//return (((((ne4 << 8) + ne3) << 8) + ne2) << 8) + ne1;
		return toInt(new int[] {ne1, ne2, ne3, ne4});
	}
	
	public static int toInt(int[] bytes) {
		int ret = 0;
		for (int i = 0; i < bytes.length; i++) {
			ret <<= 8;
			ret |= bytes[((bytes.length-1) - i)];
		}
		return ret;
	}
	
	public float readFloat() {
		int ne1 = readByte();
		int ne2 = readByte();
		int ne3 = readByte();
		int ne4 = readByte();
		
		int asInt = (ne1 & 0xFF) 
	            | ((ne2 & 0xFF) << 8) 
	            | ((ne3 & 0xFF) << 16) 
	            | ((ne4 & 0xFF) << 24);
		
		return Float.intBitsToFloat(asInt);
		
		
		/*int csign=(ne4 & 128)>>7;
		int cexp=(ne4 & 127)<<1;
		cexp+=(ne3&128)>>7;
		cexp-=127;

		int n3p2=(ne3&127);
		int cmant=(((n3p2<<8)+ne2)<<8)+(ne1);
		int cmant2=1;
		for(int a=0;a<23;a+=1) {
			int fe=(cmant>>(22-a))&1;
			if (fe == 1)
				cmant2+=(1/Math.pow(2,a+1));
		}
		if (csign == 0) {
			ne=(float) (cmant2*Math.pow(2,cexp));
		}
		else ne=(float) (-cmant2*Math.pow(2,cexp));

		return ne;*/
	}
	
	public short readShort() {
		return (short) ((readByte() & 0xff) | ((readByte() & 0xff) << 8));
	}
	
	public void clear() {
		this.bytes = null;
	}

	public int currentPointer() {
		return pointer;
	}

	public String readString(int length) {
		char[] bytes = new char[length];
		for (int i = 0; i < length; i++) {
			char b = (char) readByte();
			bytes[i] = b;
		}
		
		String temp = new String(bytes).trim();
		char[] old = temp.toCharArray();
		bytes = new char[old.length];
		for (int i = 0; i < old.length; i++) {
			if (((byte)old[i]) != 0)
				bytes[i] = old[i];
			else
				break;
		}
		
		return new String(bytes).trim();
	}

	public void seek(int i) {
		pointer = i;
	}
	
	public byte[] getBytes() {
		byte[] byt = new byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			byt[i] = (byte)bytes[i];
		}
		return byt;
	}

	public boolean isLoaded() {
		return this.loaded;
	}
}
