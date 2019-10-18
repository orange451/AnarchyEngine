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

uniform mat4 transformationMatrix;
uniform mat4 viewMatrix;

uniform mat4 boneMat[MAX_BONES];

uniform mat4 projectionMatrix;

void main() {
	mat4 boneTransform = boneMat[int(boneIndices[0])] * boneWeights[0];
	boneTransform += boneMat[int(boneIndices[1])] * boneWeights[1];
	boneTransform += boneMat[int(boneIndices[2])] * boneWeights[2];
	boneTransform += boneMat[int(boneIndices[3])] * boneWeights[3];

	vec4 BToP = boneTransform * vec4(position, 1.0);

	vec4 worldPosition = transformationMatrix * BToP;
	vec4 positionRelativeToCam = viewMatrix * worldPosition;
	gl_Position = projectionMatrix * positionRelativeToCam;;
}