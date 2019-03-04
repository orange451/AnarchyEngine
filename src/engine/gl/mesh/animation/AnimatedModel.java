package engine.gl.mesh.animation;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import engine.gl.Pipeline;
import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
import engine.gl.mesh.Vertex;
import engine.gl.mesh.mm3d.Mm3dJoint;
import engine.gl.mesh.mm3d.Mm3dPoint;
import engine.gl.mesh.mm3d.Mm3dWeightedInfluence;
import engine.gl.mesh.mm3d.ModelMM3D;
import engine.util.MeshUtils;

public class AnimatedModel {
	protected static final int MAX_BONES = 64;

	private Animation currentAnimation;
	private AnimationFrame currentFrame;
	private AnimationFrame lastFrame;
	private AnimationFrame tempFrame;

	private Animatable parent;

	private ArrayList<Bone> bones;
	private ArrayList<Point> points;
	private ArrayList<Animation> animations;

	protected ArrayList<AnimatedModelSubMesh> models = new ArrayList<AnimatedModelSubMesh>();
	protected final FloatBuffer boneBuffer = BufferUtils.createFloatBuffer(MAX_BONES * 16);

	public static AnimatedModel fromMM3D( ModelMM3D mm3d ) {
		// Create a new animated model
		AnimatedModel model = new AnimatedModel();

		// Copy joints into model
		ArrayList<Mm3dJoint> joints = mm3d.joints;
		for (int i = 0; i < joints.size(); i++) {
			Mm3dJoint j = joints.get(i);
			Bone b = new Bone( j.getName(), j.getParent(), j.getOffsetMatrix() );
			model.bones.add(b);
		}
		model.updateBones();

		// Copy points into model
		ArrayList<Mm3dPoint> pnts = mm3d.points;
		for (int i = 0; i < pnts.size(); i++) {
			Mm3dPoint j = pnts.get(i);

			if ( j.getParent() >= 0 ) {
				Bone bone = model.bones.get( j.getParent() );
				Vector3f worldPos = j.getPosition();
				Vector3f offset = VectorMath.sub( worldPos, MatrixUtils.getTranslation( bone.getAbsoluteMatrix() ) );

				Point b = new Point( j.getName(), bone, offset, j.getRotation() );
				model.points.add(b);
			}
		}

		// Copy animations/keyframes into model
		ArrayList<Mm3dSkeletalAnimation> anims = mm3d.animations;
		for (int i = 0; i < anims.size(); i++) {
			Mm3dSkeletalAnimation a = anims.get(i);
			ArrayList<Mm3dSkeletalFrame> frames = a.animationFrames;
			Animation animation = new Animation( model, a.getName(), a.fps, a.loop );
			for (int j = 0; j < frames.size(); j++) {
				Mm3dSkeletalFrame f = frames.get(j);
				ArrayList<Mm3dKeyframe> kfs = f.keyframes;
				AnimationFrame frame = new AnimationFrame( animation );
				for (int k = 0; k < kfs.size(); k++) {
					Mm3dKeyframe kf = kfs.get(k);

					// Create keyframe and add to frame
					Bone bone = model.bones.get(kf.getJoint());
					AnimationKeyFrameType type = (kf.getType() == 0)?AnimationKeyFrameType.ROTATION:AnimationKeyFrameType.TRANSLATION;
					AnimationKeyFrame keyFrame = new AnimationKeyFrame( bone, type, kf.getOffset() );
					frame.keyframes.add(keyFrame);
				}
				animation.frames.add(frame);
			}
			model.animations.add(animation);
		}

		if ( model.animations.size() > 0 )
			model.currentAnimation = model.animations.get(0);

		// Create animatible versions of the models
		BufferedPrefab prefab = mm3d.getPrefab();
		for (int a = 0; a < prefab.meshes.size(); a++) {
			BufferedMesh mesh = prefab.meshes.get(a);

			AnimatedModelSubMesh tempModel = new AnimatedModelSubMesh(model, mesh.getSize(), mm3d.joints.size());
			tempModel.material = prefab.materials.get(a);

			// Add vertices
			for (int i = 0; i < mesh.getSize(); i++) {
				Vertex vertex = mesh.getVertex(i);
				tempModel.setVertex(vertex, i);
			}

			// Add bone weight indices
			for (int i = 0; i < tempModel.getVertices().length; i++) {
				Vector4f boneIndex = new Vector4f();
				Vector4f boneValue = new Vector4f();
				Vertex[] verts = tempModel.getVertices();
				int vindex = mm3d.getVertexIndex(verts[i], false);
				Mm3dWeightedInfluence[] inf = mm3d.getWeightedInfluences(vindex);
				//System.out.println(inf[0] + " / " + inf[1] + " / " + inf[2] + " / " + inf[3]);
				for (int ii = 0; ii < inf.length; ii++) {
					if (inf[ii] == null)
						continue;

					int bone = inf[ii].getJointIndex();
					float value = inf[ii].getWeight()/100f;
					switch(ii) {
						case 0: {
							boneIndex.x = bone;
							boneValue.x = value;
							break;
						}
						case 1: {
							boneIndex.y = bone;
							boneValue.y = value;
							break;
						}
						case 2: {
							boneIndex.z = bone;
							boneValue.z = value;
							break;
						}
						case 3: {
							boneIndex.w = bone;
							boneValue.w = value;
							break;
						}
					}
				}
				tempModel.setBoneIndices(i, boneIndex);
				tempModel.setBoneWeights(i, boneValue);
			}

			model.models.add(tempModel);
		}

		// Generate bind matrices used for animating
		model.generateBindMatrix();

		// Return model
		return model;
	}

	public BufferedMesh cube;
	public AnimatedModel() {
		this.currentAnimation = new Animation( this, "animation", 0.1f, false );
		this.animations = new ArrayList<Animation>();
		this.bones = new ArrayList<Bone>();
		this.points = new ArrayList<Point>();
		this.cube = MeshUtils.cube(1);
	}
	
	public AnimatedModel( AnimatedModel base ) {
		this();
		this.bones  = base.bones;
		this.points = base.points;
		
		this.currentAnimation = base.currentAnimation;
		this.currentFrame     = base.currentFrame;
		this.lastFrame        = base.lastFrame;
		this.tempFrame        = null;
		
		this.models = base.models;
		this.parent = base.parent;
		
		for(int i = 0; i < base.animations.size(); i++) {
			Animation anim = base.animations.get(i);
			Animation a = new Animation( this, anim.getName(), anim.getSpeed(), anim.doesLoop() );
			
			for (int j = 0; j < anim.frames.size(); j++) {
				AnimationFrame frame = anim.frames.get(j);
				
				AnimationFrame f = new AnimationFrame( a );
				f.keyframes = frame.keyframes;
				a.frames.add(f);
			}
			this.animations.add(a);
		}
	}

	/**
	 * This method steps along the animation and updates all of the bone matrices.
	 * @param delta
	 */
	public void updateAnimation( float delta ) {
		if ( animations.size() == 0 )
			return;
		
		
		// Override speed
		if ( lastFrame != null && !lastFrame.parent.equals( currentAnimation ) )
			currentAnimation.overrideSpeed( Math.max( lastFrame.parent.getSpeed(), currentAnimation.getSpeed() ) );
		
		// Figure out what the current frame and last frame were
		AnimationFrame temp = currentAnimation.getCurrentFrame();
		currentAnimation.tick( delta );
		currentFrame = currentAnimation.getCurrentFrame();

		// Check if frame changed
		if ( lastFrame == null || !temp.equals( currentFrame ) ) {
			lastFrame = temp;

			if ( parent != null ) {
				parent.onFrameChange();
				
			}
		}

		if (tempFrame == null)
			tempFrame = new AnimationFrame( currentAnimation );

		// Calculate the current usable frame
		float merge = Math.min(1, currentAnimation.getExactFrame() - currentAnimation.getCurrent() );
		mergeFrames( currentFrame, lastFrame, tempFrame, merge );

		// Update the bone matrices to match the current frame
		updateBones();
	}

	/**
	 * This method is used to render all of the bones of the animated mesh.
	 * @param worldMatrix
	 */
	public void drawBones( Matrix4f worldMatrix ) {
		for ( int i = 0; i < bones.size(); i++ ) {
			Bone b = bones.get(i);

			// Generate matrix to draw cube
			Matrix4f finalMatrix = new Matrix4f();
			Matrix4f absoluteMatrix = b.getAbsoluteMatrix();
			worldMatrix.mul(absoluteMatrix, finalMatrix);

			// Render cube
			cube.render( Pipeline.pipeline_get().shader_get(), finalMatrix, Resources.MATERIAL_BLANK );
		}

		for ( int i = 0; i < points.size(); i++ ) {
			Point p = points.get(i);

			// Generate matrix to draw cube
			Matrix4f finalMatrix = new Matrix4f();
			Matrix4f absoluteMatrix = p.getAbsoluteMatrix();
			worldMatrix.mul(absoluteMatrix, finalMatrix);

			cube.render( Pipeline.pipeline_get().shader_get(), finalMatrix, Resources.MATERIAL_BLANK );
			//RenderingPipeline.draw_set_color( Color.RED );
			//cube.render( finalMatrix, Resources.blankMaterial,  Pipeline.shader_get_current() );
			//RenderingPipeline.draw_set_color( Color.WHITE );
		}
	}

	/**
	 * This method is used to render the animated mesh in the current scene
	 * @param worldMatrix
	 */
	public void drawDirect( Matrix4f worldMatrix ) {
		// Send bones to GPU
		int boneLocation = Pipeline.pipeline_get().shader_get().shader_get_uniform( "boneMat");
		if (boneLocation != -1) {
			GL20.glUniformMatrix4fv(boneLocation, false, boneBuffer);
		}

		// Loop through each material and render
		for (int i = 0; i < models.size(); i++) {
			models.get(i).render( Pipeline.pipeline_get().shader_get(), worldMatrix );
		}
	}
	
	/**
	 * This method is used to render the animated mesh in the current scene
	 * @param worldMatrix
	 */
	public void drawStandard( Matrix4f worldMatrix ) {

		// Setup skinning shader
		GameShader shader = RenderingPipeline.shader_get_current();
		RenderingPipeline.shader_set( RenderingPipeline.skinningShader );

		drawDirect( worldMatrix );

		// Reset shader
		RenderingPipeline.shader_set( shader );
	}

	private void generateBindMatrix() {
		tempFrame = null;
		updateBones();
		for (int i = 0; i < bones.size(); i++) {
			Bone bone = bones.get(i);

			// Get current bones absolute matrix
			Matrix4f absoluteMatrix = bone.getAbsoluteMatrix();

			// Generate bind matrix used for animation
			Matrix4f bindMatrix = bone.getBindMatrix();
			Matrix4f.invert(absoluteMatrix, bindMatrix);
		}
	}

	private void updateBones() {
		if (bones.size() == 0)
			return;

		// Find all root bones
		for (int i = 0; i < bones.size(); i++) {
			Bone joint = bones.get(i);
			if ( joint.getParent() < 0 ) {
				updateRootBone( joint );
			}
		}

		// Update point matrices
		for (int i = 0; i < points.size(); i++) {
			Point p = points.get(i);
			Bone b = p.getParent();

			Matrix4f pointAbsoluteMatrix = new Matrix4f();
			Matrix4f pointOffsetMatrix = p.getOffsetMatrix();
			Matrix4f boneAbsoluteMatrix = b.getAbsoluteMatrix();
			Matrix4f.mul( boneAbsoluteMatrix, pointOffsetMatrix, pointAbsoluteMatrix );

			p.setAbsoluteMatrix( pointAbsoluteMatrix );
		}

		// Store bones to buffer
		for (int i = 0; i < bones.size(); i++) {
			if (i >= bones.size())
				continue;

			Matrix4f absoluteMatrix = bones.get(i).getAbsoluteMatrix();
			Matrix4f bindMatrix     = bones.get(i).getBindMatrix();
			Matrix4f.mul(absoluteMatrix, bindMatrix, tempMat);
			tempMat.store(boneBuffer);
		}
		boneBuffer.flip();
	}

	/**
	 * Updates every bones absolute matrix that is a descendent of the root bone.
	 * @param root
	 */
	private void updateRootBone( Bone root ) {
		if (bones.size() == 0)
			return;

		// Create the skeleton matrix that will be used for every bone.
		Matrix4f rootMatrix = new Matrix4f();

		// Apply root bones matrix to skeleton matrix
		Matrix4f.mul(rootMatrix, root.getOffsetMatrix(), rootMatrix);

		// Apply root bones keyframe transformations
		applyKeyFrameTransformation( rootMatrix, root, AnimationKeyFrameType.TRANSLATION );
		applyKeyFrameTransformation( rootMatrix, root, AnimationKeyFrameType.ROTATION );

		// Set the root's absolute matrix to the current matrix
		root.setAbsoluteMatrix( rootMatrix );

		// Recursively update the matrices of all this root bones children
		updateBonesChildren( root, rootMatrix );
	}

	/**
	 * Updates every bones absolute matrix that is a descendent of the root bone. Recursively called from updateSubBones()
	 * @param root
	 * @param rootMatrix
	 */
	private void updateBonesChildren(Bone root, Matrix4f rootMatrix) {
		ArrayList<Bone> children = getRootBonesChildren( root );
		if ( children == null )
			return;

		for (int i = 0; i < children.size(); i++) {
			Bone child = children.get(i);

			// Set current bones absolute matrix to its offset matrix
			Matrix4f absoluteMatrix = child.getAbsoluteMatrix();
			absoluteMatrix.setIdentity();
			Matrix4f.mul(child.getOffsetMatrix(), absoluteMatrix, absoluteMatrix);


			// Apply the current animations keyframe
			applyKeyFrameTransformation( absoluteMatrix, child, AnimationKeyFrameType.TRANSLATION );
			applyKeyFrameTransformation( absoluteMatrix, child, AnimationKeyFrameType.ROTATION );

			// Apply parents transformation to current bones absolute matrix
			Matrix4f.mul(rootMatrix, absoluteMatrix, absoluteMatrix);

			// Recursively update the matrices of this bones children
			updateBonesChildren( child, absoluteMatrix );
		}
	}

	final Matrix4f tempMat = new Matrix4f();
	final Vector3f rot_z = new Vector3f(0, 0, 1);
	final Vector3f rot_y = new Vector3f(0, 1, 0);
	final Vector3f rot_x = new Vector3f(1, 0, 0);
	private void applyKeyFrameTransformation(Matrix4f absoluteMatrix, Bone bone, AnimationKeyFrameType keyFrameType ) {
		AnimationKeyFrame keyframe = getCurrentAnimationKeyframe( bone, keyFrameType );
		if (keyframe != null) {
			if (keyFrameType.equals(AnimationKeyFrameType.ROTATION)) {
				Vector3f rot = keyframe.offset;
				tempMat.setIdentity();
				tempMat.rotate(rot.z, rot_z);
				tempMat.rotate(rot.y, rot_y);
				tempMat.rotate(rot.x, rot_x);
				Matrix4f.mul(absoluteMatrix, tempMat, absoluteMatrix);
			} else {
				Vector3f trans = keyframe.offset;
				absoluteMatrix.translate(trans);
			}
		}
	}

	private static void mergeFrames(AnimationFrame current, AnimationFrame previous, AnimationFrame dest, float merge) {
		dest.keyframes.clear();

		// Generate all merged keyframes from the current animation to the previous animation
		for (int i = 0; i < current.keyframes.size(); i++) {
			AnimationKeyFrame kf1 = current.keyframes.get(i);
			AnimationKeyFrame kf2 = findKeyFrameByType( previous, kf1 );
			AnimationKeyFrame mergedKeyFrame = AnimationKeyFrame.mergeKeyFrames(kf2, kf1, merge);
			dest.keyframes.add(mergedKeyFrame);
		}

		// Generate all merged keyframes from the previous animation THAT DO NOT EXIST in the current animation
		for (int i = 0; i < previous.keyframes.size(); i++) {
			AnimationKeyFrame kf1 = previous.keyframes.get(i);
			AnimationKeyFrame kf2 = findKeyFrameByType( current, kf1 );
			if (kf2 == null) {
				//AnimationKeyFrame mergedKeyFrame = AnimationKeyFrame.mergeKeyFrames(null, kf1, merge);
				AnimationKeyFrame mergedKeyFrame = AnimationKeyFrame.mergeKeyFrames(kf1, null, merge);
				dest.keyframes.add(mergedKeyFrame);
			} else {
				//kf2.offset = AnimationKeyFrame.mergeKeyFrames(kf1, kf2, 0.5f).offset; 
			}
		}
	}

	private static AnimationKeyFrame findKeyFrameByType( AnimationFrame frame, AnimationKeyFrame keyframe ) {
		for (int i = 0; i < frame.keyframes.size(); i++) {
			AnimationKeyFrame kf = frame.keyframes.get(i);
			if (kf.getBone().equals(keyframe.getBone()) && kf.getType().equals(keyframe.getType())) {
				return kf;
			}
		}
		return null;
	}

	private AnimationKeyFrame getCurrentAnimationKeyframe(Bone joint, AnimationKeyFrameType type) {
		if (tempFrame == null)
			return null;

		for (int ii = 0; ii < tempFrame.keyframes.size(); ii++) {
			AnimationKeyFrame keyframe = tempFrame.keyframes.get(ii);
			Bone parentjoint = keyframe.getBone();
			if (parentjoint.equals(joint) && keyframe.getType().equals(type)) {
				return keyframe;
			}
		}
		return null;
	}

	/**
	 * Get a list of a bones direct children.
	 * @param root
	 * @return
	 */
	private ArrayList<Bone> getRootBonesChildren(Bone root) {
		ArrayList<Bone> ret = new ArrayList<Bone>();
		int rootId = getBoneId( root );
		for (int i = 0; i < bones.size(); i++) {
			Bone b = bones.get(i);
			if ( b.getParent() == rootId ) {
				ret.add(b);
			}
		}

		return ret;
	}

	/**
	 * Scan list of bones for a specific bones id.
	 * @param root
	 * @return
	 */
	private int getBoneId(Bone root) {
		for (int i = 0; i < bones.size(); i++) {
			if (bones.get(i).equals(root)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Returns all the bones in this model.
	 * @return
	 */
	public ArrayList<Bone> getBones() {
		return this.bones;
	}

	/**
	 * Find a bone by its name
	 * @param bone
	 * @return
	 */
	public Bone getBoneByName( String bone ) {
		for (int i = 0; i < bones.size(); i++) {
			Bone b = bones.get(i);
			if ( b.getName().equals( bone ) ) {
				return b;
			}
		}
		return null;
	}

	/**
	 * Find a point by its name
	 * @param point
	 * @return
	 */
	public Point getPointByName( String point ) {
		for (int i = 0; i < points.size(); i++) {
			Point p = points.get(i);
			if ( p.getName().equals( point ) ) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Get the animation currently affecting the mesh
	 * @return
	 */
	public Animation getCurrentAnimation() {
		return this.currentAnimation;
	}

	/**
	 * Get the index of an animation by its object.
	 * @param anim
	 * @return
	 */
	public int getAnimationIndex(Animation anim) {
		for (int i = 0; i < animations.size(); i++) {
			if (animations.get(i).equals(anim)) {
				return i;
			}
		}

		return 0;
	}

	/**
	 * Get an animation by an index.
	 * @param i
	 * @return
	 */
	public Animation getAnimation(int i) {
		if ( i >= animations.size() )
			i = 0;

		return animations.get(i);
	}

	/**
	 * Get an animation by its name.
	 * @param string
	 * @return
	 */
	public Animation getAnimation(String string) {
		for (int i = 0; i < animations.size(); i++) {
			if (animations.get(i).getName().equals(string)) {
				return animations.get(i);
			}
		}

		return animations.get(0);
	}
	
	/**
	 * This method sets the current animation of the mesh. It works similarly to {@link #setAnimation(Animation)} however this method instantly overwrites the animation data. There is no blend with the previous animation.
	 * @param animation
	 */
	public void setAnimationForce(Animation animation) {
		animation.reset();
		currentAnimation = animation;
		currentFrame = animation.getCurrentFrame();
		lastFrame = animation.getCurrentFrame();
	}

	/**
	 * Set the current animation of the mesh.
	 * @param animation
	 */
	public void setAnimation(Animation animation) {
		animation.reset();
		currentAnimation = animation;
	}
	
	/**
	 * Set the current animation of the mesh.
	 * @param animation
	 */
	public void setAnimationIfNotPlaying(Animation animation) {
		if ( currentAnimation.equals( animation ) )
			return;
		setAnimation( animation );
	}

	/**
	 * Set the parent/controller of this mesh. The parent can be used to handle frame changing.
	 * @param parent
	 */
	public void setParent( Animatable parent ) {
		this.parent = parent;
	}

	protected void onAnimationEnd(Animation animation) {
		if ( parent != null )
			parent.onAnimationEnd( animation );
	}
}
