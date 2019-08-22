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

in vec2 textureCoords;

out vec4 out_Color;

uniform sampler2D lensFlare;
uniform sampler2D lensDirt;
uniform sampler2D lensStar;
uniform sampler2D image;

uniform int useLensFlares;

const float mult = 2.0;

void main() {
	vec4 textureColor = texture(image, textureCoords);
	if (useLensFlares == 1) {
		vec4 lensMod = texture(lensDirt, textureCoords);
		lensMod += texture(lensStar, textureCoords);
		vec4 lensFlare = texture(lensFlare, textureCoords) * (lensMod * mult);
		textureColor += lensFlare;
	}
	out_Color = textureColor;
}