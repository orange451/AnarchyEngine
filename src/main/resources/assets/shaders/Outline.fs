/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

#include struct Material

in vec3 pass_position;

out vec4 out_Color;

uniform vec3 color;

#include variable MASK

void main() {
	out_Color = vec4(color, 1.0);
}
