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

public class Mm3dSkeletalFrame {

	public ArrayList<Mm3dKeyframe> keyframes;
	
	public Mm3dSkeletalFrame() {
		this.keyframes = new ArrayList<Mm3dKeyframe>();
	}

}
