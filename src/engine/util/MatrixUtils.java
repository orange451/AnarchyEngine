package engine.util;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import engine.gl.Pipeline;

public class MatrixUtils {
	private static final Vector3f POSITIVE = new Vector3f( 1, 1, 1 );
	
	public static Vector2f project3Dto2D( Vector3f input ) {
		Matrix4f projMat = Pipeline.pipeline_get().getGBuffer().getProjectionMatrix();
		Matrix4f viewMat = Pipeline.pipeline_get().getGBuffer().getViewMatrix();
		Matrix4f viewProjMat = new Matrix4f();
		
		// Calculate view-projection matrix
		projMat.mul(viewMat, viewProjMat);
		
		// Get view-space position
		Vector3f pos = new Vector3f( input.x, input.y, input.z );
		pos.mulProject( viewProjMat, pos );
		
		// Put in [0-1] space (currently in NDC space [-1 to 1])
		pos.y *= -1; // Invert Y for OpenGL
		pos = pos.add(POSITIVE);
		pos = pos.mul(0.5f);
		
		// Put into screen space
		pos.mul(new Vector3f( Pipeline.pipeline_get().getWidth(), Pipeline.pipeline_get().getHeight(), 1f ));
		
		// Return it as vec2
		return new Vector2f( pos.x, pos.y );
	}
	
	public static Vector3f project2Dto3D( Vector2f screenSpaceCoordinates ) {
		Matrix4f iProjMat = Pipeline.pipeline_get().getGBuffer().getInverseProjectionMatrix();
		Matrix4f iViewMat = Pipeline.pipeline_get().getGBuffer().getInverseViewMatrix();
		
		// Put in Normalized Device Coordinate space [-1 to 1] Currently in Screen space [0 to screen size]
		Vector2f ndc = screenSpaceCoordinates.mul(1/(float)Pipeline.pipeline_get().getWidth(), 1/(float)Pipeline.pipeline_get().getHeight(), new Vector2f());
		ndc.mul(2);
		ndc.sub(1,1);
		ndc.y *= -1;
		
		Vector3f mCoords = new Vector3f( ndc.x, ndc.y, 1.0f );
		
		// Put Mouse-coords from NDC space into view space
		mCoords.mulProject(iProjMat);
	
		// Put Mouse-coords from view space into world space
		mCoords.mulProject(iViewMat);
	
		// Subtract cameras position ( World-space into Object space )
		Vector3f camPos = Pipeline.pipeline_get().getCameraPosition();
		Vector3f finalCoords = mCoords.sub(camPos, new Vector3f());
	
		// Normalize
		finalCoords.normalize();
		
		return finalCoords;
	}
}

