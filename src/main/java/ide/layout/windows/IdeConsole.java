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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;

import engine.gl.GPUProfiler;
import engine.gl.GPUTaskProfile;
import engine.lua.LuaEngine;
import ide.layout.IdePane;
import ide.layout.windows.icons.Icons;
import lwjgui.LWJGUI;
import lwjgui.font.Font;
import lwjgui.geometry.Pos;
import lwjgui.scene.Context;
import lwjgui.scene.control.ContextMenu;
import lwjgui.scene.control.MenuItem;
import lwjgui.scene.control.SeparatorMenuItem;
import lwjgui.scene.control.TextArea;
import lwjgui.scene.control.TextField;
import lwjgui.scene.layout.BorderPane;

public class IdeConsole extends IdePane {

	private TextArea console;
	private ArrayList<String> history = new ArrayList<String>();
	private int history_index = -1;

	public IdeConsole() {
		super("Console", true);

		BorderPane editBox = new BorderPane();
		editBox.setAlignment(Pos.TOP_LEFT);
		this.getChildren().add(editBox);

		console = new TextArea();
		console.setFont(Font.COURIER);
		console.setEditable(false);
		console.setFillToParentHeight(true);
		console.setFillToParentWidth(true);
		console.setBorderRadii(0);
		editBox.setCenter(console);

		TextField luaInput = new TextField();
		luaInput.setFont(Font.COURIER);
		luaInput.setPrompt("Lua Command Line");
		luaInput.setFillToParentWidth(true);
		luaInput.setBorderRadii(0);
		editBox.setBottom(luaInput);

		// Console context menu
		{
			ContextMenu menu = new ContextMenu();
			console.setContextMenu(menu);

			MenuItem cut = new MenuItem("Cut", Icons.icon_cut.getView());
			cut.setOnAction((event) -> {
				if (console.isEditing())
					console.cut();
			});
			menu.getItems().add(cut);

			MenuItem copy = new MenuItem("Copy", Icons.icon_copy.getView());
			copy.setOnAction((event) -> {
				console.copy();
			});
			menu.getItems().add(copy);

			MenuItem paste = new MenuItem("Paste", Icons.icon_paste.getView());
			paste.setOnAction((event) -> {
				console.paste();
			});
			menu.getItems().add(paste);

			menu.getItems().add(new SeparatorMenuItem());

			MenuItem clear = new MenuItem("Clear Console");
			clear.setOnAction((event) -> {
				console.clear();
			});
			menu.getItems().add(clear);
		}

		// Command line context menu
		{
			ContextMenu menu = new ContextMenu();
			luaInput.setContextMenu(menu);

			MenuItem cut = new MenuItem("Cut", Icons.icon_cut.getView());
			cut.setOnAction((event) -> {
				if (luaInput.isEditing())
					luaInput.cut();
			});
			menu.getItems().add(cut);

			MenuItem copy = new MenuItem("Copy", Icons.icon_copy.getView());
			copy.setOnAction((event) -> {
				luaInput.copy();
			});
			menu.getItems().add(copy);

			MenuItem paste = new MenuItem("Paste", Icons.icon_paste.getView());
			paste.setOnAction((event) -> {
				luaInput.paste();
			});
			menu.getItems().add(paste);
		}

		this.setOnKeyPressed(event -> {
			if (event.getKey() == GLFW.GLFW_KEY_UP) {
				if (window.getContext().getSelected().isDescendentOf(luaInput)) {
					if ( history.size() == 0 )
						return;
					
					int index = history_index++;
					if (index > history.size() - 1) {
						index = 0;
						history_index = history.size() >= 1 ? 1 : 0;
					}

					String lua = history.get(Math.min((history.size() - 1) - index, history.size() - 1));
					luaInput.setText(lua);

					luaInput.setCaretPosition(lua.length());

					return;
				}
			}

			if (event.getKey() == GLFW.GLFW_KEY_DOWN) {
				if (window.getContext().getSelected().isDescendentOf(luaInput)) {
					if ( history.size() == 0 )
						return;
					
					int index = history_index--;
					if (index < 0) {
						index = history.size() - 1;
						history_index = history.size() > 1 ? history.size() - 2 : index;
					}

					String lua = history.get(Math.max((history.size() - 1) - index, 0));
					luaInput.setText(lua);
					
					luaInput.setCaretPosition(lua.length());

					return;
				}
			}

			if (event.getKey() == GLFW.GLFW_KEY_ENTER) {
				if (window.getContext().getSelected() == null)
					return;

				if (window.getContext().getSelected().isDescendentOf(luaInput)) {
					String lua = luaInput.getText();
					console.appendText("> " + lua + "\n");
					LuaEngine.runLua(lua);
					luaInput.setText("");
					if (history.size() == 0 || !history.get(history.size() - 1).equals(lua)) {
						history.add(lua);
					}
					
					history_index = 0;

					return;
				}
			}
		});

		// Print all text to console
		LuaEngine.globals.STDOUT = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				String letter = "" + (char) b;
				LWJGUI.runLater(() -> {
					console.appendText(letter);
				});
			}
		}, true);

		// Print all errors to console
		LuaEngine.globals.STDERR = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				String letter = "" + (char) b;
				LWJGUI.runLater(() -> {
					console.appendText(letter);
				});
			}
		}, true);

	}

	@Override
	public void render(Context context) {
		if (GPUProfiler.PROFILING_ENABLED) {
			GPUTaskProfile tp;
			while ((tp = GPUProfiler.getFrameResults()) != null) {
				console.setText(tp.dumpS());
				GPUProfiler.recycle(tp);
			}
		}
		super.render(context);
	}

	@Override
	public void onOpen() {
		//
	}

	@Override
	public void onClose() {
		//
	}
}