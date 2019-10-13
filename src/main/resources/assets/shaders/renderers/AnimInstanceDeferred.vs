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
layout(location = 4) in vec4 boneIndices;
layout(location = 5) in vec4 boneWeights;

#define MAX_BONES 128

out vec2 pass_textureCoords;
out vec3 pass_position;
out mat3 TBN;

uniform mat4 transformationMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 jitterMatrix;
uniform int frame;

uniform bool useTAA;

uniform mat4 boneMat[MAX_BONES];

void main() {
	mat4 boneTransform = boneMat[int(boneIndices[0])] * boneWeights[0];
	boneTransform += boneMat[int(boneIndices[1])] * boneWeights[1];
	boneTransform += boneMat[int(boneIndices[2])] * boneWeights[2];
	boneTransform += boneMat[int(boneIndices[3])] * boneWeights[3];

	vec4 BToP = boneTransform * vec4(position, 1.0);
	vec4 BToN = boneTransform * vec4(normal, 0.0);

	vec4 worldPosition = transformationMatrix * BToP;
	vec4 positionRelativeToCam = viewMatrix * worldPosition;
	if (useTAA) {
		vec4 preJitter = projectionMatrix * positionRelativeToCam;
		gl_Position = jitterMatrix * preJitter;
	} else {
		gl_Position = projectionMatrix * positionRelativeToCam;
	}
	pass_textureCoords = textureCoords;

	vec3 t;
	vec3 c1 = cross(BToN.xyz, vec3(0.0, 0.0, 1.0));
	vec3 c2 = cross(BToN.xyz, vec3(0.0, 1.0, 0.0));
	if (length(c1) > length(c2))
		t = c1;
	else
		t = c2;
	vec3 T = normalize(vec3(transformationMatrix * vec4(t, 0.0)));
	vec3 N = normalize(vec3(transformationMatrix * vec4(BToN.xyz, 0.0)));
	T = normalize(T - dot(T, N) * N);
	vec3 B = cross(N, T);
	TBN = mat3(T, B, N);

	pass_position = worldPosition.xyz;
}