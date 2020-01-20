/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package ide.layout;

import ide.IDE;
import lwjgui.LWJGUI;
import lwjgui.collections.ObservableList;
import lwjgui.geometry.Pos;
import lwjgui.scene.control.Tab;
import lwjgui.scene.control.TabPane;

public class IdeDockPane extends TabPane {
	private ObservableList<IdePane> dockedPanes;

	public IdeDockPane() {
		this.setAlignment(Pos.TOP_LEFT);
		undock(null);
		
		this.dockedPanes = new ObservableList<IdePane>();
		
		this.setOnSelectionChange(event -> {
			Tab previous = event.previous;
			Tab current = event.current;
			if ( previous != null ) {
				((IdePane)previous.getContent()).onClose();
			}
			if ( current != null ) {
				((IdePane)current.getContent()).onOpen();
			}
		});
	}
	
	public void setTab(IdePane pane) {
		Tab t = getTab(pane);
		if ( t == null )
			return;
		select(t);
	}
	
	private Tab getTab(IdePane pane) {
		ObservableList<Tab> tabs = getTabs();
		for (int i= 0; i < tabs.size(); i++) {
			if ( tabs.get(i).getContent().equals(pane) ) {
				return tabs.get(i);
			}
		}
		return null;
	}

	public void dock(IdePane pane) {
		pane.dockedTo = this;
		
		dockedPanes.add(pane);

		Tab tab = new Tab();
		tab.setText(pane.getName());
		tab.setContent(pane);
		getTabs().add(tab);
		
		tab.setOnCloseRequest(event -> {
			if ( !pane.closable ) {
				event.consume();
				return;
			}
			undock(pane);
		});

		this.setMaxWidth(2048);
		this.setMaxHeight(2048);
		this.setPrefHeight(pane.getPrefHeight());
		this.setPrefWidth(pane.getPrefWidth());
		this.setMinSize(8, 24);
		
		select(tab);
	}

	public void undock(IdePane pane) {
		if ( pane == null )
			return;
		
		dockedPanes.remove(pane);
		this.getTabs().remove(getTab(pane));
		IDE.layout.update();
	}
}
