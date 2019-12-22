/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.mesh.mm3d;

import java.util.ArrayList;

public enum Mm3dDataBlock {
	
	// Type A
	META_DATA					( Mm3dDataBlockType.TYPE_A, 0x1001, "Meta Data"),
	UNKNOWN						( Mm3dDataBlockType.TYPE_A, 0x1002, "Unknown type information (not implemented)"),
	GROUPS						( Mm3dDataBlockType.TYPE_A, 0x0101, "Groups"),
	EMBEDDED_TEXTURES			( Mm3dDataBlockType.TYPE_A, 0x0141, "Embedded textures (not implemented)"),
	EXTERNAL_TEXTURES			( Mm3dDataBlockType.TYPE_A, 0x0142, "External textures"),
	MATERIALS					( Mm3dDataBlockType.TYPE_A, 0x0161, "Materials"),
	TEXTURE_PROJECTIONS_TRI		( Mm3dDataBlockType.TYPE_A, 0x016c, "Texture Projections Triangles"),
	CANVAS_BACKGROUND_IMAGES	( Mm3dDataBlockType.TYPE_A, 0x0191, "Canvas Background Images"),
	SKELETAL_ANIMATIONS			( Mm3dDataBlockType.TYPE_A, 0x0301, "Skeletal Animations"),
	FRAME_ANIMATIONS			( Mm3dDataBlockType.TYPE_A, 0x0321, "Frame Animations"),
	FRAME_ANIMATION_POINTS		( Mm3dDataBlockType.TYPE_A, 0x0326, "Frame Animation Points"),
	FRAME_RELATIVE_ANIMATIONS	( Mm3dDataBlockType.TYPE_A, 0x0341, "Frame Relative Animations (not implemented)"),
	EOF							( Mm3dDataBlockType.TYPE_A, 0x3fff, "End of file (offset is file size)"),
	
	// Type B
	VERTICES					( Mm3dDataBlockType.TYPE_B, 0x8001, "Vertices"),
	TRIANGLES					( Mm3dDataBlockType.TYPE_B, 0x8021, "Triangles"),
	TRIANGLE_NORMALS			( Mm3dDataBlockType.TYPE_B, 0x8026, "Triangle Normals"),
	JOINTS						( Mm3dDataBlockType.TYPE_B, 0x8041, "Joints"),
	JOINT_VERTICES				( Mm3dDataBlockType.TYPE_B, 0x8046, "Joint Vertices"),
	POINTS						( Mm3dDataBlockType.TYPE_B, 0x8061, "Points"),
	SMOOTHNESS_ANGLES			( Mm3dDataBlockType.TYPE_B, 0x8106, "Smoothness Angles"),
	WEIGHTED_INFLUENCES			( Mm3dDataBlockType.TYPE_B, 0x8146, "Weighted Influences"),
	TEXTURE_PROJECTIONS_OTHER	( Mm3dDataBlockType.TYPE_B, 0x8168, "Texture Projections (sphere/cylinder map)"),
	TEXTURE_COORDINATES			( Mm3dDataBlockType.TYPE_B, 0x8121, "Texture Coordinates");
	
	private static ArrayList<Mm3dDataBlock> blocks = new ArrayList<Mm3dDataBlock>();
	
	private Mm3dDataBlock(Mm3dDataBlockType type, int id, String description) {
		this.type = type;
		this.id = id;
		this.description = description;
	}
	
	private Mm3dDataBlockType type;
	private int id;
	private String description;
	
	public Mm3dDataBlockType getType() {
		return type;
	}
	public int getId() {
		return id;
	}
	public String getDescription() {
		return description;
	}
	
	enum Mm3dDataBlockType {
		TYPE_A, TYPE_B;
	}

	public static Mm3dDataBlock getDataBlock(int blockId) {
		for (int i = 0; i < blocks.size(); i++) {
			if (blocks.get(i).getId() == blockId) {
				return blocks.get(i);
			}
		}
		return EOF;
	}
	
	static {
		for (Mm3dDataBlock block : values()) {
			blocks.add(block);
		}
	}
}
