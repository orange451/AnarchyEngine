package engine.util;

import org.lwjgl.BufferUtils;

import engine.util.DeallocationHelper.Deallocator;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static org.lwjgl.BufferUtils.*;

public final class IOUtil {
	private static Deallocator deallocate;
	
	private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
		ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
		buffer.flip();
		newBuffer.put(buffer);
		return newBuffer;
	}

	public static URL ioResourceGetURL( String resource ) {
		URL url = IOUtil.class.getClassLoader().getResource(resource);
		if (url == null) {
			try {
				return new File(resource).toURI().toURL();
			} catch (MalformedURLException e) {
				//
			}
		}

		return url;
	}

	/**
	 * Reads the specified resource and returns the raw data as a ByteBuffer.
	 *
	 * @param resource   the resource to read
	 * @param bufferSize the initial buffer size
	 *
	 * @return the resource data
	 *
	 * @throws IOException if an IO error occurs
	 */
	public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
		if ( resource == null )
			return null;

		// Get URL for file
		URL url = ioResourceGetURL( resource );

		ByteBuffer buffer = createByteBuffer(bufferSize);
		InputStream source = url.openStream();

		if ( source == null )
			throw new FileNotFoundException(resource);

		try {
			ReadableByteChannel rbc = Channels.newChannel(source);
			try {
				while ( true ) {
					int bytes = rbc.read(buffer);
					if ( bytes == -1 )
						break;
					if ( buffer.remaining() == 0 )
						buffer = resizeBuffer(buffer, buffer.capacity() * 2);
				}
			} finally {
				rbc.close();
			}
		} finally {
			source.close();
		}

		buffer.flip();
		return buffer;
	}

	public static void freeBuffer(ByteBuffer buffer) {
		try {
			if ( deallocate == null ) {
				deallocate = new DeallocationHelper.OracleSunOpenJdkDeallocator();
			}
			deallocate.run(buffer);
		}catch(Exception e) {
			System.err.println("Failed to initialize deallocater");
		}
	}
}
