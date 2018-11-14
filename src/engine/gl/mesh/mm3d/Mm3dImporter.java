package engine.gl.mesh.mm3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import org.joml.Vector3f;

import engine.gl.mesh.Triangle;
import engine.gl.mesh.Vertex;
import engine.util.BinFile;

public class Mm3dImporter {
	private boolean debug = false;
	private float scale = 1.0f;
	private String filePath;

	public static ByteBuffer getByteBufferFromFile(String fileName) {
		BinFile bin = new BinFile(fileName);
		if ( !bin.isLoaded()) {
			//SysConsole.warning("Could not find model: " + fileName);
			System.out.println("Could not find model: " + fileName);
			return null;
		}
		
		ByteBuffer buffer = bin.getByteBuffer();
		if ( buffer == null ) {
			//SysConsole.warning("Could not find model: " + fileName );
			System.out.println("Could not find model: " + fileName);
			return null;
		}
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.rewind();

		return buffer;
	}

	public Mm3dImporter setDebug(boolean b) {
		this.debug = b;
		return this;
	}

	public ModelMM3D importFromDirectory(String directory, String filename) {
		this.filePath = directory;
		ByteBuffer buf = getByteBufferFromFile(directory + filename);
		if ( buf == null )
			return null;

		ModelMM3D model = load(buf);
		model.loadAnimationFile(directory+(filename.replace(".mm3d", ".anim")));
		model.loadMaterialFile(directory, filename.replace(".mm3d", ".mtl") );
		return model;
	}

	public ModelMM3D load(ByteBuffer buf) {
		ModelMM3D model = new ModelMM3D();

		long time = System.currentTimeMillis();

		// Read header
		String header = getString(buf, 8);
		if (!header.equals("MISFIT3D")) {
			System.err.println("Could not load mm3d model! Header:" + header);
			return null;
		}

		int MAJOR_VERSION = getuInt8(buf);
		int MINOR_VERSION = getuInt8(buf);
		int MODEL_FLAGS   = getuInt8(buf);
		int OFFSET_COUNT  = getuInt8(buf);

		log("Major: " + MAJOR_VERSION);
		log("Minor: " + MINOR_VERSION);
		log("Flags: " + MODEL_FLAGS);
		log("Offsets: " + OFFSET_COUNT);

		// Read Data Offsets
		long[] OFFSET_TYPE  = new long[OFFSET_COUNT];
		long[] OFFSET_VALUE = new long[OFFSET_COUNT];
		for (int i = 0; i < OFFSET_COUNT; i++) {
			OFFSET_TYPE[i]  = getuInt16(buf);
			OFFSET_VALUE[i] = getuInt32(buf);

			log("  Offset Id: " + i);
			log("    Type: " + OFFSET_TYPE[i]);
			log("    Value: " + OFFSET_VALUE[i]);
			log("");
		}

		// Start reading Data Blocks
		for (int i = 0; i < OFFSET_COUNT; i++) {
			int blockId = (int)OFFSET_TYPE[i];
			int offset  = (int)OFFSET_VALUE[i];
			Mm3dDataBlock dataBlock = Mm3dDataBlock.getDataBlock(blockId);
			log("Data Block: " + dataBlock.getDescription());
			switch(dataBlock) {
				case GROUPS: {
					readGroups(model, buf, offset);
					break;
				}
				case JOINTS: {
					readJoints(model, buf, offset);
					break;
				}
				case POINTS: {
					readPoints(model, buf, offset);
					break;
				}
				case SKELETAL_ANIMATIONS: {
					readSkeletalAnimations(model, buf, offset);
					break;
				}
				case VERTICES: {
					readVertices(model, buf, offset);
					break;
				}
				case TRIANGLES: {
					readTriangles(model, buf, offset);
					break;
				}
				case TRIANGLE_NORMALS: {
					readTriangleNormals(model, buf, offset);
					break;
				}
				case TEXTURE_COORDINATES: {
					readTextureCoordinates(model, buf, offset);
					break;
				}
				case WEIGHTED_INFLUENCES: {
					readWeightedInfluences(model, buf, offset);
					break;
				}
				case MATERIALS: {
					readMaterials(model, buf, offset);
					break;
				}
				case EXTERNAL_TEXTURES: {
					readExternalTextures(model, buf, offset);
				}
				default: {
					// suk my dik
				}
			}
		}
		model.finish();

		//System.out.println("Took " + (System.currentTimeMillis() - time/1000f) + " seconds");
		return model;
	}

	private void readExternalTextures(ModelMM3D model, ByteBuffer buf, int offset) {
		buf.position(offset);

		// Block Header [TYPE A]
		int DATA_FLAGS   = (int) getuInt16(buf);
		int DATA_COUNT_A = (int) getuInt32(buf);

		log("  Found " + DATA_COUNT_A + " textures");
		for (int i = 0; i < DATA_COUNT_A; i++) {
			int DATA_SIZE_A  = (int) getuInt32(buf);
			Mm3dTexture texture = new Mm3dTexture(filePath, getuInt16(buf), getString(buf));
			//System.out.println("    Loaded texture: " + texture.getFullPath());

			model.textures.add(texture);
		}
	}

	private void readMaterials(ModelMM3D model, ByteBuffer buf, int offset) {
		buf.position(offset);

		// Block Header [TYPE A]
		int DATA_FLAGS   = (int) getuInt16(buf);
		int DATA_COUNT_A = (int) getuInt32(buf);

		log("  Found " + DATA_COUNT_A + " materials");
		for (int i = 0; i < DATA_COUNT_A; i++) {
			int DATA_SIZE_A  = (int) getuInt32(buf);
			Mm3dMaterial material = new Mm3dMaterial(getuInt16(buf),
													getuInt32(buf),
													getString(buf),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat(),
													buf.getFloat());

			model.materials.add(material);

			log("    " + material.getName());
			log("      - " + material.getTextureIndex());
			log("      - " + material.getDiffuse());
		}
	}

	private void readWeightedInfluences(ModelMM3D model, ByteBuffer buf, int offset) {
		buf.position(offset);

		// Block Header [TYPE B]
		int DATA_FLAGS   = (int) getuInt16(buf);
		int DATA_COUNT_B = (int) getuInt32(buf);
		int DATA_SIZE_B  = (int) getuInt32(buf);

		log("  Found: " + DATA_COUNT_B + " influences");
		for (int i = 0; i < DATA_COUNT_B; i++) {
			Mm3dWeightedInfluence influence = new Mm3dWeightedInfluence(getuInt8(buf),
																		getuInt32(buf),
																		getuInt32(buf),
																		getuInt8(buf),
																		getuInt8(buf));

			model.influences.add(influence);
		}
	}

	private void readGroups(ModelMM3D model, ByteBuffer buf, int offset) {
		buf.position(offset);

		// Block Header [TYPE A]
		int DATA_FLAGS   = (int) getuInt16(buf);
		int DATA_COUNT_A = (int) getuInt32(buf);

		log("  Found " + DATA_COUNT_A + " groups");
		for (int i = 0; i < DATA_COUNT_A; i++) {
			int DATA_SIZE_A  = (int) getuInt32(buf);
			int a = (int) getuInt16(buf);
			String b = getString(buf);
			int c = (int) getuInt32(buf);
			int[] d = new int[c];
			for (int ii = 0; ii < d.length; ii++) {
				d[ii] = (int) getuInt32(buf);
			}
			int e = getuInt8(buf);
			int f = (int) getuInt32(buf);

			Mm3dGroup group = new Mm3dGroup(a, b, c, d, e, f);
			model.groups.add(group);
		}
	}

	private void readTextureCoordinates(ModelMM3D model, ByteBuffer buf, int offset) {
		buf.position(offset);

		// Block Header [TYPE B]
		int DATA_FLAGS   = (int) getuInt16(buf);
		int DATA_COUNT_B = (int) getuInt32(buf);
		int DATA_SIZE_B  = (int) getuInt32(buf);

		for (int i = 0; i < DATA_COUNT_B; i++) {
			long TEXCOORD_FLAGS = getuInt16(buf);
			long TEXCOORD_TRIANGLE = getuInt32(buf);
			float TEXCOORD_VERTEX_1_S = buf.getFloat();
			float TEXCOORD_VERTEX_2_S = buf.getFloat();
			float TEXCOORD_VERTEX_3_S = buf.getFloat();
			float TEXCOORD_VERTEX_1_T = buf.getFloat();
			float TEXCOORD_VERTEX_2_T = buf.getFloat();
			float TEXCOORD_VERTEX_3_T = buf.getFloat();

			float[] triTexX = new float[]{ TEXCOORD_VERTEX_1_S, TEXCOORD_VERTEX_2_S, TEXCOORD_VERTEX_3_S };
			float[] triTexY = new float[]{ 1.0f - TEXCOORD_VERTEX_1_T, 1.0f - TEXCOORD_VERTEX_2_T, 1.0f - TEXCOORD_VERTEX_3_T };

			Triangle t = model.triangles.get((int) TEXCOORD_TRIANGLE);
			t.texCoordS = triTexX;
			t.texCoordT = triTexY;
		}
	}

	private void readTriangleNormals(ModelMM3D model, ByteBuffer buf, int offset) {
		buf.position(offset);

		// Block Header [TYPE B]
		int DATA_FLAGS   = (int) getuInt16(buf);
		int DATA_COUNT_B = (int) getuInt32(buf);
		int DATA_SIZE_B  = (int) getuInt32(buf);

		log("  Found " + DATA_COUNT_B + " normals");

		for (int i = 0; i < DATA_COUNT_B; i++) {
			long TRI_NORM_FLAGS = getuInt16(buf);
			long TRI_NORM_INDEX = getuInt32(buf);
			float TRI_NORM_V1_NORM_X = buf.getFloat();
			float TRI_NORM_V1_NORM_Y = buf.getFloat();
			float TRI_NORM_V1_NORM_Z = buf.getFloat();
			float TRI_NORM_V2_NORM_X = buf.getFloat();
			float TRI_NORM_V2_NORM_Y = buf.getFloat();
			float TRI_NORM_V2_NORM_Z = buf.getFloat();
			float TRI_NORM_V3_NORM_X = buf.getFloat();
			float TRI_NORM_V3_NORM_Y = buf.getFloat();
			float TRI_NORM_V3_NORM_Z = buf.getFloat();

			float[] triNormX = new float[]{ TRI_NORM_V1_NORM_X, TRI_NORM_V2_NORM_X, TRI_NORM_V3_NORM_X };
			float[] triNormY = new float[]{ TRI_NORM_V1_NORM_Y, TRI_NORM_V2_NORM_Y, TRI_NORM_V3_NORM_Y };
			float[] triNormZ = new float[]{ TRI_NORM_V1_NORM_Z, TRI_NORM_V2_NORM_Z, TRI_NORM_V3_NORM_Z };
			

			if (i < 4) {
				log("    - triangle " + i + " normal v1: " + TRI_NORM_V1_NORM_X + ", " + TRI_NORM_V1_NORM_Y + ", " + TRI_NORM_V1_NORM_Z);
				log("    - triangle " + i + " normal v2: " + TRI_NORM_V2_NORM_X + ", " + TRI_NORM_V2_NORM_Y + ", " + TRI_NORM_V2_NORM_Z);
				log("    - triangle " + i + " normal v3: " + TRI_NORM_V3_NORM_X + ", " + TRI_NORM_V3_NORM_Y + ", " + TRI_NORM_V3_NORM_Z);
			}

			Triangle t = model.triangles.get((int) TRI_NORM_INDEX);
			if (!isZero(TRI_NORM_V1_NORM_X, TRI_NORM_V1_NORM_Y, TRI_NORM_V1_NORM_Z) && !isZero(TRI_NORM_V2_NORM_X, TRI_NORM_V2_NORM_Y, TRI_NORM_V2_NORM_Z) && !isZero(TRI_NORM_V3_NORM_X, TRI_NORM_V3_NORM_Y, TRI_NORM_V3_NORM_Z)) {
				t.normalX = triNormX;
				t.normalY = triNormY;
				t.normalZ = triNormZ;
			}
		}
	}
	
	private boolean isZero( float x, float y, float z ) {
		return x == 0 && y == 0 && z == 0;
	}

	private void readTriangles(ModelMM3D model, ByteBuffer buf, int offset) {
		buf.position(offset);

		// Block Header [TYPE B]
		int DATA_FLAGS   = (int) getuInt16(buf);
		int DATA_COUNT_B = (int) getuInt32(buf);
		int DATA_SIZE_B  = (int) getuInt32(buf);

		log("  Found " + DATA_COUNT_B + " triangles");

		for (int i = 0; i < DATA_COUNT_B; i++) {
			int TRIANGLE_FLAGS = (int) getuInt16(buf);
			int TRIANGLE_VERTEX_1 = (int) getuInt32(buf);
			int TRIANGLE_VERTEX_2 = (int) getuInt32(buf);
			int TRIANGLE_VERTEX_3 = (int) getuInt32(buf);

			if (i < 10) {
				log("    - triangle " + i + " vertices: " + TRIANGLE_VERTEX_1 + ", " + TRIANGLE_VERTEX_2 + ", " + TRIANGLE_VERTEX_3);
			}

			Triangle t = new Triangle();
			t.vertices = new Vertex[] { model.vertices.get(TRIANGLE_VERTEX_1), model.vertices.get(TRIANGLE_VERTEX_2), model.vertices.get(TRIANGLE_VERTEX_3) };

			model.triangles.add(t);
		}
	}

	private void readVertices(ModelMM3D model, ByteBuffer buf, int offset) {
		buf.position(offset);

		// Block Header [TYPE B]
		int DATA_FLAGS   = (int) getuInt16(buf);
		int DATA_COUNT_B = (int) getuInt32(buf);
		int DATA_SIZE_B  = (int) getuInt32(buf);

		log("  Found " + DATA_COUNT_B + " vertices");
		for (int i = 0; i < DATA_COUNT_B; i++) {
			int VERTEX_FLAGS = (int) getuInt16(buf);
			float VERTEX_COORD_X = buf.getFloat() * scale;
			float VERTEX_COORD_Y = buf.getFloat() * scale;
			float VERTEX_COORD_Z = buf.getFloat() * scale;

			Vertex v = new Vertex(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1);
			v.setXYZ(VERTEX_COORD_X, VERTEX_COORD_Y, VERTEX_COORD_Z);

			if (i < 10) {
				log("vertex " + i + " pos: " + VERTEX_COORD_X + ", " + VERTEX_COORD_Y + ", " + VERTEX_COORD_Z);
			}

			model.vertices.add(v);
		}
	}

	private void readSkeletalAnimations(ModelMM3D model, ByteBuffer buf, int offset) {
		buf.position(offset);

		// Block Header [TYPE A]
		int DATA_FLAGS   = (int) getuInt16(buf);
		int DATA_COUNT_A = (int) getuInt32(buf);

		log("  Found " + DATA_COUNT_A + " animations");
		for (int i = 0; i < DATA_COUNT_A; i++) {
			int DATA_SIZE_A  = (int) getuInt32(buf);

			Mm3dSkeletalAnimation animation = new Mm3dSkeletalAnimation(getuInt16(buf),
																		getString(buf),
																		buf.getFloat(),
																		getuInt32(buf));

			log("    Reading animation: " + animation.getName() + " / Frames: " + animation.getFrames() + " / Flags: " + animation.getFlags());

			// Loop through each frame in the animation
			for (int ii = 0; ii < animation.getFrames(); ii++) {
				Mm3dSkeletalFrame frame = new Mm3dSkeletalFrame();
				animation.animationFrames.add(frame);

				log("      Reading Data for frame: " + ii);
				long SKEL_KEYFRAME_COUNT = getuInt32(buf);

				// Loop through each keyframe
				for (long iii = 0; iii < SKEL_KEYFRAME_COUNT; iii++) {
					Mm3dKeyframe keyframe = new Mm3dKeyframe(getuInt32(buf),
															getuInt8(buf),
															buf.getFloat(),
															buf.getFloat(),
															buf.getFloat());
					
					if ( keyframe.getType() != 0 )
						keyframe.getOffset().mul(scale);

					frame.keyframes.add(keyframe);

					Vector3f posOffset = keyframe.getOffset();
					log("        Keyframe: " + iii);
					log("          - Parent Joint:  " + keyframe.getJoint());
					log("          - Keyframe Type: " + keyframe.getType());
					log("          - offset:        " + posOffset.toString());
				}
			}

			animation.smooth();

			model.animations.add(animation);
		}
	}

	private void readJoints(ModelMM3D model, ByteBuffer buf, int offset) {
		buf.position(offset);

		// Block Header [TYPE B]
		int DATA_FLAGS   = (int) getuInt16(buf);
		int DATA_COUNT_B = (int) getuInt32(buf);
		int DATA_SIZE_B  = (int) getuInt32(buf);

		log("  Found " + DATA_COUNT_B + " joints");

		// Set Globals
		for (int i = 0; i < DATA_COUNT_B; i++) {
			Mm3dJoint joint = new Mm3dJoint(getuInt16(buf),
											getString(buf, 40),
											getuInt32(buf),
											buf.getFloat(),
											buf.getFloat(),
											buf.getFloat(),
											buf.getFloat() * scale,
											buf.getFloat() * scale,
											buf.getFloat() * scale);

			log( "    " + joint.getName() );
			model.joints.add(joint);
		}
	}

	private void readPoints(ModelMM3D model, ByteBuffer buf, int offset) {
		buf.position(offset);

		// Block Header [TYPE B]
		int DATA_FLAGS   = (int) getuInt16(buf);
		int DATA_COUNT_B = (int) getuInt32(buf);
		int DATA_SIZE_B  = (int) getuInt32(buf);

		log("  Found " + DATA_COUNT_B + " points");

		// Set Globals
		for (int i = 0; i < DATA_COUNT_B; i++) {
			Mm3dPoint point = new Mm3dPoint(getuInt16(buf),
											getString(buf, 40),
											getuInt32(buf),
											getuInt32(buf),
											buf.getFloat(),
											buf.getFloat(),
											buf.getFloat(),
											buf.getFloat() * scale,
											buf.getFloat() * scale,
											buf.getFloat() * scale);

			log( "    " + point.getName() );
			model.points.add( point );
		}
	}

	private void log(String string) {
		if (debug)
			System.out.println(string);
	}

	private static String getString(ByteBuffer b, int length) {
		byte[] headerBytes = new byte[length];
		for (int i = 0; i < length; i++)
			headerBytes[i] = b.get();
		return new String(headerBytes).trim();
	}

	private static String getString(ByteBuffer b) {
		ArrayList<Byte> stringBytes = new ArrayList<Byte>();
		for (int i = 0; i < 1024; i++) {
			byte by = b.get();
			if (by == 0)
				break;
			stringBytes.add(by);
		}

		if (stringBytes.size() > 0) {
			byte[] headerBytes = new byte[stringBytes.size()];
			for (int i = 0; i < stringBytes.size(); i++)
				headerBytes[i] = stringBytes.get(i);
			return new String(headerBytes).trim();
		} else {
			return "";
		}
	}

	private static int getuInt8(ByteBuffer b) {
		return ((short) (b.get() & 0xff));
	}

	private static long getuInt16(ByteBuffer b) {
		return (b.getShort() & 0xffff);
	}

	private static long getuInt32(ByteBuffer b) {
		return ((long) b.getInt() & 0xffffffffL);
	}

	public Mm3dImporter scale(float f) {
		scale = f;
		return this;
	}
}