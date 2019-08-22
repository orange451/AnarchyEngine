#version 150

out vec4 gBuffer0;
out vec4 gBuffer1;
out vec4 gBuffer2;
out vec4 gBuffer3;

void write(vec3 diffuse, vec3 normal, float metalness, float roughness, float reflective, vec3 emissive) {
	gBuffer0 = vec4( diffuse.rgb, 1.0 );
	gBuffer1 = vec4( normal.rgb,  1.0 );
	gBuffer2 = vec4( metalness, roughness, reflective, 1.0);
	gBuffer3 = vec4( emissive, 1.0 ); // Emissive
}