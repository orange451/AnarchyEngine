//
// This file is part of Light Engine
//
// Copyright (C) 2016-2019 Lux Vacuos
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 textureCoords;
layout(location = 3) in vec4 inColor;

out vec2 pass_textureCoords;
out vec3 pass_position;
out mat3 TBN;
out vec4 clipSpace;
out vec4 clipSpacePrev;

uniform mat4 transformationMatrix;
uniform mat4 transformationMatrixPrev;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 jitterMatrix;
uniform int frame;

uniform bool useTAA;

void main() {
	vec4 worldPosition = transformationMatrix * vec4(position, 1.0);
	vec4 positionRelativeToCam = viewMatrix * worldPosition;
	clipSpace = projectionMatrix * positionRelativeToCam;
	if (useTAA) {
		gl_Position = jitterMatrix * clipSpace;
	} else {
		gl_Position = clipSpace;
	}

	pass_textureCoords = textureCoords;

	vec3 t;
	vec3 c1 = cross(normal, vec3(0.0, 0.0, 1.0));
	vec3 c2 = cross(normal, vec3(0.0, 1.0, 0.0));
	if (length(c1) > length(c2))
		t = c1;
	else
		t = c2;
	vec3 T = normalize(vec3(transformationMatrix * vec4(t, 0.0)));
	vec3 N = normalize(vec3(transformationMatrix * vec4(normal, 0.0)));
	T = normalize(T - dot(T, N) * N);
	vec3 B = cross(N, T);
	TBN = mat3(T, B, N);

	pass_position = worldPosition.xyz;

	worldPosition = transformationMatrixPrev * vec4(position, 1.0);
	positionRelativeToCam = viewMatrix * worldPosition;
	clipSpacePrev = projectionMatrix * positionRelativeToCam;
}