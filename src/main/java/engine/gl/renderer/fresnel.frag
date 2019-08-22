#version 150

float calculateFresnel( vec3 viewSpacePos, vec3 surfaceNormal, float roughness, float metalness, float reflectiveness ) {
	float dim = 1.0 / 32.0;
	float power = 2.0 + ( 1.0 / ( roughness * 4.0 ) );
	float s = (1.0-metalness+0.125);
	
	// Calculate rim light
	float f = 1.0-abs(dot(viewSpacePos, surfaceNormal ));
	f = pow( f, power );
	f = f * s;
	f = clamp( f, 0.0, 1.0 );
	
	return f * mix( 0.1, 1.0, reflectiveness );
}
