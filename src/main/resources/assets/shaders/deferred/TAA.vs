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

layout(location = 0) in vec2 position;

out vec2 textureCoords;
out vec2 pixelSize;

uniform vec2 resolution;

void main() {
	gl_Position = vec4(position, -0.8, 1.0);
	textureCoords = vec2((position.x + 1.0) / 2.0, (position.y + 1.0) / 2.0);
	pixelSize = vec2(1.0 / resolution.x, 1.0 / resolution.y);
}