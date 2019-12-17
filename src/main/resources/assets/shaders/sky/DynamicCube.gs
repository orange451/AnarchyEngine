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

layout(triangles) in;
layout(triangle_strip, max_vertices = 18) out;

in vec2 passTextureCoords[];
in vec3 passNormal[];

out vec2 pass_textureCoords;
out vec3 pass_normal;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrixCube[6];

vec4 positionRelativeToCam;

void main() {
	for (int i = 0; i < 6; i++) {
		gl_Layer = i;

		positionRelativeToCam = viewMatrixCube[i] * gl_in[0].gl_Position;
		pass_textureCoords = passTextureCoords[0];
		pass_normal = passNormal[0];
		gl_Position = projectionMatrix * positionRelativeToCam;
		EmitVertex();

		positionRelativeToCam = viewMatrixCube[i] * gl_in[1].gl_Position;
		pass_textureCoords = passTextureCoords[1];
		pass_normal = passNormal[1];
		gl_Position = projectionMatrix * positionRelativeToCam;
		EmitVertex();

		positionRelativeToCam = viewMatrixCube[i] * gl_in[2].gl_Position;
		pass_textureCoords = passTextureCoords[2];
		pass_normal = passNormal[2];
		gl_Position = projectionMatrix * positionRelativeToCam;
		EmitVertex();

		EndPrimitive();
	}
}