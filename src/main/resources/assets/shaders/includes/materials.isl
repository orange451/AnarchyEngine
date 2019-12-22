/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

#struct Material
struct Material {
	vec3 diffuse;
	vec3 emissive;
	float roughness;
	float metallic;
	sampler2D diffuseTex;
	sampler2D normalTex;
	sampler2D roughnessTex;
	sampler2D metallicTex;
};
#end