package engine.gl;

import java.nio.ByteBuffer;

public interface PixelHandler {
    public int getBytesPerPixel();
    public void handlePixel(ByteBuffer b, int pixel);

    public static final PixelHandler PIXEL_HANDLER_RGBA = new PixelHandler() {

        @Override
        public int getBytesPerPixel() {
            return 4;
        }

        @Override
        public void handlePixel(ByteBuffer b, int pixel) {
            b.put((byte) ((pixel >> 16) & 0xFF));	// Red component
            b.put((byte) ((pixel >> 8) & 0xFF));	// Green component
            b.put((byte) (pixel & 0xFF));			// Blue component
            b.put((byte) ((pixel >> 24) & 0xFF));	// Alpha component
        }
    };

    public static final PixelHandler PIXEL_HANDLER_RGB = new PixelHandler() {

        @Override
        public int getBytesPerPixel() {
            return 3;
        }

        @Override
        public void handlePixel(ByteBuffer b, int pixel) {
            b.put((byte) ((pixel >> 16) & 0xFF));		// Red component
            b.put((byte) ((pixel >> 8) & 0xFF));		// Green component
            b.put((byte) (pixel & 0xFF));				// Blue component
        }
    };

}