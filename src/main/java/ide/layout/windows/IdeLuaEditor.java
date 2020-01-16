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

import engine.lua.type.object.ScriptBase;
import ide.layout.IdePane;
import lwjgui.paint.Color;
import lwjgui.scene.Node;
import lwjgui.theme.Theme;

public class IdeLuaEditor extends IdePane {
	private ScriptBase inst;
	private SearchableCodeEditor code;
	
	public IdeLuaEditor(ScriptBase script) {
		super(script.getName() + ".lua", true);

		code = new SearchableCodeEditor() {
			
		};
		this.getChildren().add(code);

		// Do syntax highlighting on pattern
		code.setOnTextChange((event) -> {
			code.resetHighlighting();
			if (Theme.current() instanceof lwjgui.theme.ThemeWhite)
				code.setFontFill(Color.BLACK);

			code.applyHighlighting();
		});

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
