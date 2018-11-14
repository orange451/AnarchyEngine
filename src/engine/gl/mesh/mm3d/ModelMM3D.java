package engine.gl.mesh.mm3d;

import java.io.BufferedReader;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.gl.MaterialGL;
import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
import engine.gl.mesh.BufferedPrefab;
import engine.gl.mesh.Triangle;
import engine.gl.mesh.Vertex;
import engine.gl.shader.BaseShader;
import engine.util.FileIO;
import engine.util.TextureUtils;
import luaengine.type.object.PrefabRenderer;
import lwjgui.Color;

public class ModelMM3D {
	public static final boolean DEBUG = true;

	public ArrayList<Mm3dJoint> joints;
	public ArrayList<Mm3dPoint> points;
	public ArrayList<Mm3dSkeletalAnimation> animations;
	public ArrayList<Vertex> vertices;
	public ArrayList<Triangle> triangles;
	public ArrayList<Mm3dGroup> groups;
	public ArrayList<Mm3dWeightedInfluence> influences;
	public ArrayList<Mm3dMaterial> materials;
	public ArrayList<Mm3dTexture> textures;
	public int CURRENT_ANIMATION = 0;
	public float SCALE = 1.0f;

	private BufferedPrefab staticModel;
	private BufferedMesh collapsedModel;

	public ModelMM3D() {
		this.joints     = new ArrayList<Mm3dJoint>();
		this.points     = new ArrayList<Mm3dPoint>();
		this.animations = new ArrayList<Mm3dSkeletalAnimation>();
		this.vertices   = new ArrayList<Vertex>();
		this.triangles  = new ArrayList<Triangle>();
		this.groups     = new ArrayList<Mm3dGroup>();
		this.influences = new ArrayList<Mm3dWeightedInfluence>();
		this.materials  = new ArrayList<Mm3dMaterial>();
		this.textures   = new ArrayList<Mm3dTexture>();
	}

	public void renderStaticModel( BaseShader shader, Matrix4f worldMatrix) {
		this.staticModel.render( shader, worldMatrix );
	}

	public BufferedMesh getCollapsedModel() {
		return this.collapsedModel;
	}

	public Mm3dPoint getPoint(String name) {
		for (int i = 0; i < points.size(); i++) {
			if (points.get(i).getName().equalsIgnoreCase(name)) {
				return points.get(i);
			}
		}
		return null;
	}

	public Mm3dJoint getJoint(String name) {
		for (int i = 0; i < joints.size(); i++) {
			if (joints.get(i).getName().equalsIgnoreCase(name)) {
				return joints.get(i);
			}
		}
		return null;
	}

	public int getJointIndex(Mm3dJoint j) {
		for (int i = 0; i < joints.size(); i++) {
			if (joints.get(i).equals(j)) {
				return i;
			}
		}
		return 0;
	}

	public void renderJoints(BaseShader shader, Matrix4f worldMatrix) {
		// Draw all the Joints
		for (int i = 0; i < joints.size(); i++) {
			Mm3dJoint currentJoint = joints.get(i);
			Matrix4f absoluteMatrix = currentJoint.getAbsoluteMatrix();

			Matrix4f ma_mat = new Matrix4f();
			//Matrix4f.mul(worldMatrix, absoluteMatrix, ma_mat);
			worldMatrix.mul(absoluteMatrix, ma_mat);

			Color color = Color.WHITE;
			if (currentJoint.getParent() == -1)
				color = Color.red;

			/*GameResources.cubeModel.matrix_set_from_4x4(ma_mat);
			GameResources.cubeModel.matrix_set_scaling(0.125f, 0.125f, 0.125f);
			GameResources.cubeModel.matrix_add_translation(0, 0, 0);
			GameResources.cubeModel.draw(Resources.blankTexture, null, null, null, shader, color);
			GameResources.cubeModel.matrix_reset();*/
		}
	}

	public int getAnimationFromName(String animationName) {
		for (int i = 0; i < animations.size(); i++) {
			if (animations.get(i).getName().equalsIgnoreCase(animationName)) {
				return i;
			}
		}
		return 0;
	}

	public Mm3dMaterial getMaterialFromGroupName(String name) {
		for (int i = 0; i < groups.size(); i++) {
			Mm3dGroup group = groups.get(i);
			if (group.getName().equalsIgnoreCase(name)) {
				return materials.get(group.getMaterial());
			}
		}
		return null;
	}

	public Mm3dMaterial getMaterialByName(String name) {
		for (int i = 0; i < materials.size(); i++) {
			Mm3dMaterial mat = materials.get(i);
			if (mat.getName().equalsIgnoreCase(name)) {
				return mat;
			}
		}
		return null;
	}

	public Mm3dWeightedInfluence[] getWeightedInfluences(int vertexIndex) {
		Mm3dWeightedInfluence[] ret = new Mm3dWeightedInfluence[4];
		int index = 0;
		for (int i = 0; i < this.influences.size() && index < 4; i++) {
			Mm3dWeightedInfluence inf = this.influences.get(i);
			if (inf.getVertexIndex() == vertexIndex) {
				ret[index] = inf;
				index++;
			}
		}

		return ret;
	}

	public int getVertexIndex(Vertex vertex, boolean exact) {
		for (int i = 0; i < vertices.size(); i++) {
			if (exact && vertices.get(i).equals(vertex))
				return i;
			else if (!exact && vertices.get(i).equalsLoose(vertex))
				return i;
		}

		return 0;
	}

	private int getNextFrame(int frame) {
		if (frame == -1)
			return -1;
		if (frame + 1 >= animations.get(CURRENT_ANIMATION).animationFrames.size())
			return 0;
		return frame + 1;
	}

	protected void finish() {
		// Fix weighted Influences
		for (int i = 0; i < vertices.size(); i++) {
			int totalWeight = 0;
			int nonRemainderJoints = 0;
			Mm3dWeightedInfluence[] influences = this.getWeightedInfluences(i);
			for (int ii = 0; ii < influences.length; ii++) {
				if (influences[ii] == null)
					continue;

				int weight = influences[ii].getWeight();
				int type = influences[ii].getInfluenceType();

				totalWeight += weight;
				// If it's not a remainder, count it.
				/*if ( type != 2 ) {
					totalWeight += weight;
					nonRemainderJoints++;
				}*/
			}

			int remainderJoints = influences.length - nonRemainderJoints;
			int remainder = (int) ( (100 - totalWeight) / (float)remainderJoints);
			remainder = Math.max( remainder, 0 );

			for (int ii = 0; ii < influences.length; ii++) {
				if (influences[ii] == null)
					continue;

				int weight = influences[ii].getWeight();
				int type = influences[ii].getInfluenceType();
				
				int newWeight = (int) (( weight / (float)totalWeight ) * 100);
				influences[ii].setWeight( newWeight );

				// If it's not a remainder
				if ( type != 2 ) {
					//int newWeight = (int) (( weight / (float)totalWeight ) * 100);
					//influences[ii].setWeight( newWeight );
				} else {
					//influences[ii].setWeight( remainder );
				}
			}
		}
		// Create Static Model (used for drawing)
		this.staticModel = new BufferedPrefab();
		for (int i = 0; i < groups.size(); i++) {
			Mm3dGroup group = groups.get(i);
			BufferedMesh tempModel = new BufferedMesh(group.getTriangles() * 3);
			for (int ii = 0; ii < group.getTriangles(); ii++) {
				Triangle tri = triangles.get(group.getIndices()[ii]);
				for (int iii = 0; iii < 3; iii++) {
					Vertex vertex = tri.getFinalVertex(iii);
					tempModel.setVertex((ii * 3) + iii, vertex);
				}
			}
			// Make sure we didn't get an empty model!
			if (tempModel.getSize() > 0) {

				// Add the model to the obj model
				if (group.getMaterial() >= 0 && group.getMaterial() < 4096) {
					Mm3dMaterial m3dMat = this.materials.get(group.getMaterial());
					MaterialGL material = m3dMat.getModelMaterial();
					if ( m3dMat.getTextureIndex() < this.textures.size() )
						material.setDiffuseTexture(this.textures.get(m3dMat.getTextureIndex()).getDiffuseTexture());

					staticModel.addModel(tempModel, material);
				} else {
					staticModel.addModel(tempModel, new MaterialGL().setDiffuseTexture(Resources.TEXTURE_WHITE_SRGB));
				}
			}
		}

		this.collapsedModel = staticModel.collapseModel();
		//System.out.println("Collapsing model");
	}

	public void loadAnimationFile(String string) {
		BufferedReader br = FileIO.file_text_open_read(this.getClass().getClassLoader(), string);
		ArrayList<String> lines = new ArrayList<String>();
		String strLine = null;
		while ((strLine = FileIO.file_text_read_line(br)) != null) {
			lines.add(strLine);
		}

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.startsWith("#"))
				continue;

			String[] split = line.split(",");
			if (split != null && split.length == 3) {
				int anim = this.getAnimationFromName(split[0]);
				animations.get(anim).fps = Float.parseFloat(split[1]);
				animations.get(anim).loop = Boolean.parseBoolean(split[2]);
			}
		}
	}

	public void loadMaterialFile(String directory, String string) {
		if (materials.size() == 0)
			return;

		URL url = TextureUtils.class.getClassLoader().getResource( directory + string );
		if (url == null) {
			try {
				url = new File( directory + string ).toURL();
			} catch (MalformedURLException e) {
				//
			}
		}

		BufferedReader br = FileIO.file_text_open_read( url );
		ArrayList<String> lines = new ArrayList<String>();
		String strLine = null;
		while ((strLine = FileIO.file_text_read_line(br)) != null) {
			lines.add(strLine);
		}

		MaterialGL currentMaterial = materials.get(0).getModelMaterial();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.startsWith("#"))
				continue;

			// Get values
			line = line.trim();
			String[] values = line.split(" ");

			// Check for new material
			if (line.startsWith("newmtl")) {
				Mm3dMaterial tmp = this.getMaterialByName( values[1] );
				if ( tmp == null )
					tmp = materials.get(0);

				currentMaterial = tmp.getModelMaterial();
			}

			// Specularity
			if ( values[0].equals("Ns") ) {
				//currentMaterial.setSpecularity(Float.parseFloat( values[1] ));
			}

			// Glossiness
			if ( values[0].equals("Ng") ) {
				//currentMaterial.setGlossiness(Float.parseFloat( values[1] ));
			}

			// Diffuse color
			if ( values[0].equals("Kd") ) {
				Vector3f color = new Vector3f( Float.parseFloat( values[1] ), Float.parseFloat( values[2] ), Float.parseFloat( values[3] ) );
				color.x *= 255;
				color.y *= 255;
				color.z *= 255;
				//currentMaterial.setDiffuseColor( new Color( (int)color.x, (int)color.y, (int)color.z ) );
			}

			// Emissive color
			if ( values[0].equals("Ke") ) {
				Vector3f color = new Vector3f( Float.parseFloat( values[1] ), Float.parseFloat( values[2] ), Float.parseFloat( values[3] ) );
				//currentMaterial.setEmissiveColor( color );
			}

			// Material
			if ( values[0].equals("map_Km") ) {
				//Material m = TextureUtils.loadMaterial(directory + values[1], directory + values[2], directory + values[3], directory + values[4]);
				//if ( m != null ) {
					//currentMaterial.setDiffuseTexture( m.getDiffuseTexture() );
					//currentMaterial.setNormalTexture( m.getNormalTexture() );
				//}
			}

			// Diffuse Texture
			if ( values[0].equals("map_Kd") ) {
				String tex = values[1];
				currentMaterial.setDiffuseTexture( TextureUtils.loadSRGBTexture( directory + tex ) );
			}

			// Normal Texture
			if ( values[0].equals("map_Kn") ) {
				String tex = values[1];
				currentMaterial.setNormalTexture( TextureUtils.loadRGBATexture( directory + tex ) );
			}
		}
	}

	public BufferedPrefab getPrefab() {
		return this.staticModel;
	}
}
