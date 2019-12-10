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

out vec3 pass_position;
out vec4 clipSpace;
out vec4 clipSpacePrev;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 transformationMatrix;
uniform mat4 projectionMatrixPrev;
uniform mat4 viewMatrixPrev;

void main() {
	vec4 worldPosition = transformationMatrix * vec4(position, 1.0);
	vec4 positionRelativeToCam = viewMatrix * worldPosition;
	clipSpace = projectionMatrix * positionRelativeToCam;
	gl_Position = clipSpace;
	pass_position = worldPosition.xyz;

	worldPosition = transformationMatrix * vec4(position, 1.0);
	positionRelativeToCam = viewMatrixPrev * worldPosition;
	clipSpacePrev = projectionMatrixPrev * positionRelativeToCam;
}