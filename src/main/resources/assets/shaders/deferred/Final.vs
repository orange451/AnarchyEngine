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

out vec2 textureCoords;

void main() {
	gl_Position = vec4(position, 0.5, 1.0);
	textureCoords = vec2((position.x + 1.0) / 2.0, (position.y + 1.0) / 2.0);
}