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
layout(location = 1) in mat4 modelViewMatrix;
layout(location = 5) in vec4 texOffsets;
layout(location = 6) in float blendFactor;

out vec2 textureCoords0;
out vec2 textureCoords1;
out float blend;

uniform mat4 projectionMatrix;
uniform float numberOfRows;

void main() {
	vec2 textureCoords = position + vec2(0.5, 0.5);
	textureCoords.y = 1.0 - textureCoords.y;
	textureCoords /= numberOfRows;
	textureCoords0 = textureCoords + texOffsets.xy;
	textureCoords1 = textureCoords + texOffsets.zw;
	blend = blendFactor;

	vec4 worldPosition = modelViewMatrix * vec4(position, 0.0, 1.0);
	gl_Position = projectionMatrix * worldPosition;
}