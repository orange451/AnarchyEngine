#version 330

vec3 reflectivePBR( vec3 cubemapSample, vec3 viewSpacePos, vec3 surfaceNormal, float roughness, float reflective ) {

	// Calculate dot from surface->light from range [0-1]
	float f = dot( viewSpacePos, surfaceNormal ) / 2.0 + 0.5;
	f = clamp( f, 0.0, 1.0 );

	// Boost color. Why not? Looks good
	f = f * (1.0 + (1.0-roughness));

	// Limit reflectiveness and apply user-defined reflective value
	f = clamp( f + reflective, 0.0, 1.0 );

	// Make it linear
	f = 1.0 - pow( 1.0 - f, 2.0 );

	// Combine reflective with cubemap sample
	return mix( vec3(1.0), cubemapSample, clamp( f, 0.0, 1.0 ) );
}
