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

import lwjgui.ManagedThread;
import lwjgui.geometry.Pos;
import lwjgui.scene.Window;
import lwjgui.scene.WindowHandle;
import lwjgui.scene.WindowManager;
import lwjgui.scene.control.Button;
import lwjgui.scene.control.Label;
import lwjgui.scene.layout.BorderPane;
import lwjgui.scene.layout.HBox;
import lwjgui.scene.layout.StackPane;
import lwjgui.theme.Theme;

public class ErrorWindow {

	private static final int WIDTH = 300;
	private static final int HEIGHT = 120;
	private final boolean fatalError;

	public ErrorWindow(String string) {
		this(string, false);
	}

	public ErrorWindow(String string, boolean fatal) {
		fatalError = fatal;
		errorLWJGUI(string);
	}

	private void errorLWJGUI(String string) {
		WindowManager.runLater(() -> {
			String prefix = "";
			if (fatalError)
				prefix = "Fatal ";
			new ManagedThread(WIDTH, HEIGHT, prefix + "Error!") {
				@Override
				protected void setupHandle(WindowHandle handle) {
					super.setupHandle(handle);
					handle.canResize(false);
				}

				@Override
				protected void init(Window window) {
					super.init(window);
					BorderPane root = new BorderPane();
					window.getScene().setRoot(root);

					Label l = new Label(string);
					root.setCenter(l);

					StackPane bottom = new StackPane();
					bottom.setPrefHeight(32);
					bottom.setAlignment(Pos.CENTER);
					bottom.setFillToParentWidth(true);
					bottom.setBackgroundLegacy(Theme.current().getControlAlt());
					root.setBottom(bottom);

					HBox hbox = new HBox();
					hbox.setAlignment(Pos.CENTER);
					hbox.setBackground(null);
					hbox.setSpacing(16);
					bottom.getChildren().add(hbox);

					Button b = new Button("Ok");
					b.setMinWidth(64);
					hbox.getChildren().add(b);

					b.setOnAction(event -> {
						window.close();
					});
				}
			}.start();
		});
	}
}
