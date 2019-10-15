package engine;

import java.io.File;

import engine.util.FileUtils;

public class FilePath {
	
	public static final String PROJECT_IDENTIFIER = "%PROJECT%";

	/**
	 * Convert a URL string to a full system filepath.
	 * @param ideFilePath
	 * @return
	 */
	public static String convertToSystem(String ideFilePath) {
		return FileUtils.fixPath(ideFilePath.replace(PROJECT_IDENTIFIER, new File(Game.saveDirectory).getAbsolutePath()));
	}

	/**
	 * Convert a full system filepath to a project-relative filepath.
	 * @param systemFilePath
	 * @return
	 */
	public static String convertToIDE(String systemFilePath) {
		return FileUtils.fixPath(systemFilePath.replace(new File(Game.saveDirectory).getAbsolutePath(), PROJECT_IDENTIFIER));
	}
}
