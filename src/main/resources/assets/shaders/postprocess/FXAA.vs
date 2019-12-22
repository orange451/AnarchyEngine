/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

layout(location = 0) in vec2 position;

out vec2 textureCoords;
out vec4 posPos;

uniform vec2 resolution;

uniform int useFXAA;

#define FXAA_SUBPIX_SHIFT (1.0 / 4.0)

void main() {
	gl_Position = vec4(position, -0.8, 1.0);
	textureCoords = vec2((position.x + 1.0) / 2.0, (position.y + 1.0) / 2.0);
	if (useFXAA == 1) {
		vec2 rcpFrame = vec2(1.0 / resolution.x, 1.0 / resolution.y);
		posPos.xy = textureCoords;
		posPos.zw = textureCoords - (rcpFrame * (0.5 + FXAA_SUBPIX_SHIFT));
	}
}