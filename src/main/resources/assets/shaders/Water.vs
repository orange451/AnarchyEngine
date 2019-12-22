/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 textureCoords;

out vec3 passPosition;
out vec2 passTextureCoords;

uniform mat4 transformationMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform float time;

#define PI 3.14159265359

const float A = 0.1; // amplitude
const float L = 8;   // wavelength
const float w = 2 * PI / L;
const float Q = 1;
const float tiling = 1.0;

void main() {
	vec4 worldPosition = transformationMatrix * vec4(position, 1.0);

	vec3 P0 = worldPosition.xyz;
	vec2 D = vec2(1, 0.5);
	float dotD = dot(P0.xz, D);
	float C = cos(w * dotD + time / 4);
	float S = sin(w * dotD + time / 4);

	vec3 P = vec3(P0.x + Q * A * C * D.x, A * S + worldPosition.y, P0.z + Q * A * C * D.y);
	worldPosition.xyz = P;

	passPosition = worldPosition.xyz;
	vec4 positionRelativeToCam = viewMatrix * worldPosition;
	gl_Position = projectionMatrix * positionRelativeToCam;
	passTextureCoords = passPosition.xz * tiling;
}