/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.shaders;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import engine.glv2.exceptions.IncludeShaderException;
import engine.glv2.exceptions.LoadShaderException;

public final class ShaderIncludes {

	private static Map<String, String> variablesMap = new HashMap<>();
	private static Map<String, String> functionsMap = new HashMap<>();
	private static Map<String, String> structMap = new HashMap<>();

	private ShaderIncludes() {
	}

	public static void processIncludeFile(String path) {
		StringBuilder source = new StringBuilder();
		String includeName = "";
		InputStream filet = null;
		BufferedReader reader = null;
		try {
			filet = ShaderIncludes.class.getClassLoader().getResourceAsStream(path);
			reader = new BufferedReader(new InputStreamReader(filet));
			System.out.println("Processing Shader Include File: " + path);
			String line;
			boolean var = false, struct = false, func = false;
			while ((line = reader.readLine()) != null) {
				if (line.equals("#end") && (var || func || struct)) {
					System.out.println("Parsed ISL Object '" + includeName + "'");
					if (var) {
						var = false;
						variablesMap.put(includeName, source.toString());
						source = new StringBuilder();
						continue;
					} else if (struct) {
						struct = false;
						structMap.put(includeName, source.toString());
						source = new StringBuilder();
						continue;
					} else if (func) {
						func = false;
						functionsMap.put(includeName, source.toString());
						source = new StringBuilder();
						continue;
					}
					continue;
				} else if (line.startsWith("#variable")) { // Process a
															// variable
					var = true;
					String[] cat = line.split(" ");
					includeName = cat[1];
					continue;
				} else if (line.startsWith("#struct")) { // Process a struct
					struct = true;
					String[] cat = line.split(" ");
					includeName = cat[1];
					continue;
				} else if (line.startsWith("#function")) { // Process a
															// function
					func = true;
					String[] cat = line.split(" ");
					includeName = cat[1];
					continue;
				} else if ((var || func || struct)) {
					source.append(line).append("//\n");
				}
			}
		} catch (Exception e) {
			throw new LoadShaderException(e);
		} finally {
			try {
				if (filet != null)
					filet.close();
				if (reader != null)
					reader.close();
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}

	public static String getFunction(String name) {
		if (!functionsMap.containsKey(name))
			throw new IncludeShaderException("ISL Function '" + name + "' does not exist");
		return functionsMap.get(name);
	}

	public static String getVariable(String name) {
		if (!variablesMap.containsKey(name))
			throw new IncludeShaderException("ISL Variable '" + name + "' does not exist");
		return variablesMap.get(name);
	}

	public static String getStruct(String name) {
		if (!structMap.containsKey(name))
			throw new IncludeShaderException("ISL Struct '" + name + "' does not exist");
		return structMap.get(name);
	}

}
