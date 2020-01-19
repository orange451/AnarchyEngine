/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package ide.layout.windows;

import engine.lua.type.object.insts.ui.CSS;
import ide.layout.IdePane;
import ide.layout.windows.code.CSSEditor;
import ide.layout.windows.code.HighlightableCodeEditor;
import lwjgui.scene.Node;

public class IdeCSSEditor extends IdePane {
	private CSS inst;
	private HighlightableCodeEditor code;
	
	public IdeCSSEditor(CSS script) {
		super(script.getName() + ".css", true);

		code = new CSSEditor();
		this.getChildren().add(code);

		code.setText(script.getSource());
		this.inst = script;
	}

	private long lastSave = System.currentTimeMillis();

	@Override
	protected void position(Node parent) {
		super.position(parent);

		if (System.currentTimeMillis() - lastSave > 500) {
			try {
				inst.setSource(code.getText());
				lastSave = System.currentTimeMillis();
			} catch (Exception e) {
				inst = null;
				this.dockedTo.undock(this);
			}
		}
	}

	@Override
	public void onOpen() {
		//
	}

	@Override
	public void onClose() {
		if (inst == null)
			return;
		inst.setSource(code.getText());
	}
}
