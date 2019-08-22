#version 330

float getReflectionIndex( vec3 viewSpacePos, vec3 surfaceNormal ) {
	// Calculate dot from surface->light from range [0-1]
	float f = abs( dot( viewSpacePos, surfaceNormal ) );
	
	// Return
	return f;
}

vec3 reflectivePBR( vec3 cubemapSample, vec3 viewSpacePos, vec3 surfaceNormal, float metalness, float reflective ) {
	// Get reflection index [0-1]
	float refractIndex = 1.0-getReflectionIndex( viewSpacePos, surfaceNormal );
	
	// Compute reflective amount
	float f = mix(0.04, 1.0, (1.0-metalness)*(1.0-metalness));
	f = (f*f) - (refractIndex*0.2);
	f = clamp( f, 0.0, 1.0 );

	// Combine reflective with cubemap sample
	return mix( cubemapSample, vec3(1.0), f );
}