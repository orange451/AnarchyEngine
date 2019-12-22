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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;

public class FileIO {
	public static BufferedReader file_text_open_read(ClassLoader loader, String path) {
		try{
			URL url = loader.getResource(path);
			return new BufferedReader(new InputStreamReader(url.openStream()));
		}catch(Exception e) {
			return null;
		}
	}

	public static BufferedReader file_text_open_read(String str) {
		try{
			return new BufferedReader(new FileReader(str));
		}catch(Exception e) {
			return null;
		}
	}

	public static BufferedReader file_text_open_read(URL str) {
		try{
			return new BufferedReader(new InputStreamReader(str.openStream()));
		}catch(Exception e) {
			return null;
		}
	}

	public static String file_text_read_line(BufferedReader out) {
		try{
			return out.readLine();
		}catch(Exception e) {
			//
		}
		return null;
	}



	public static String file_text_read_line_all(BufferedReader out) {
		String ret = "";
		String curLine = null;
		while ( (curLine = file_text_read_line(out)) != null ) {
			ret = ret + curLine + "\n";
		}
		return ret;
	}

	public static BufferedWriter file_text_open_write(String str) {
		try{
			return new BufferedWriter(new FileWriter(str));
		}catch(Exception e) {
			return null;
		}
	}

	public static boolean file_text_write_line(BufferedWriter out, String str) {
		try{
			out.write(str);
			out.newLine();
		}catch(Exception e) {
			return false;
		}
		return true;
	}

	public static void file_text_close(BufferedWriter out) {
		try{
			out.flush();
		}catch(Exception e) {

		}
		try{
			out.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void file_text_close(BufferedReader out) {
		try{
			out.close();
		}catch(Exception e) {
			//
		}
	}
}
