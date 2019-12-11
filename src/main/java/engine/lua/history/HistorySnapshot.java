package engine.lua.history;

import java.util.ArrayList;

public class HistorySnapshot {
	public ArrayList<HistoryChange> changes = new ArrayList<HistoryChange>();

	public void addChange(HistoryChange change) {
		changes.add(change);
	}
}
