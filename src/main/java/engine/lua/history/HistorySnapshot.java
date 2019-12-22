/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.history;

import java.util.ArrayList;

public class HistorySnapshot {
	public ArrayList<HistoryChange> changes = new ArrayList<HistoryChange>();

	public void addChange(HistoryChange change) {
		changes.add(change);
	}
}
