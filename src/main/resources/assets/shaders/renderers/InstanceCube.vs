/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec3 tangent;
layout(location = 3) in vec2 textureCoords;
layout(location = 4) in vec4 inColor;

out vec2 passTextureCoords;
out vec4 passPosition;
out mat3 passTBN;

uniform mat4 transformationMatrix;

void main() {
	vec4 worldPosition = transformationMatrix * vec4(position, 1.0);
	gl_Position = worldPosition;
	passTextureCoords = textureCoords;

	vec3 T = normalize(vec3(transformationMatrix * vec4(tangent, 0.0)));
	vec3 N = normalize(vec3(transformationMatrix * vec4(normal, 0.0)));
	T = normalize(T - dot(T, N) * N);
	vec3 B = cross(N, T);
	passTBN = mat3(T, B, N);

	passPosition = worldPosition;
}