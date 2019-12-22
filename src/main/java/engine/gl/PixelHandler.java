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