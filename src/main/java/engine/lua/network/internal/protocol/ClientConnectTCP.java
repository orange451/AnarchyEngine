/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.network.internal.protocol;

public class ClientConnectTCP {
	public final String username;
	public final String version;
	public final String data;
	
	public ClientConnectTCP() {
		username = "Test";
		version = "";
		data = "";
	}
	
	public ClientConnectTCP(String username, String version, String data) {
		this.username = username;
		this.version = version;
		this.data = data;
	}

}
