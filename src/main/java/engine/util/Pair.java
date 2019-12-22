/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.util;

public class Pair<T1, T2> {
	private T1 value1;
	private T2 value2;
	
	public Pair(T1 value1, T2 value2) {
		this.value1 = value1;
		this.value2 = value2;
	}

	public T1 value1() {
		return value1;
	}

	public T2 value2() {
		return value2;
	}

	
	@Override
	public String toString() {
		return "(" + value1 + ", " + value2 + ")";
	}
}
