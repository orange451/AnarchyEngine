package ide;

import java.io.File;

import engine.Game;
import engine.util.FileUtils;

public class IDEFilePath {

	/**
	 * Convert a URL string to a full system filepath.
	 * @param ideFilePath
	 * @return
	 */
	public static String convertToSystem(String ideFilePath) {
		return FileUtils.fixPath(ideFilePath.replace("%PROJECT%", new File(Game.saveDirectory).getAbsolutePath()));
	}

	/**
	 * Convert a full system filepath to a project-relative filepath.
	 * @param systemFilePath
	 * @return
	 */
	public static String convertToIDE(String systemFilePath) {
		return FileUtils.fixPath(systemFilePath.replace(new File(Game.saveDirectory).getAbsolutePath(), "%PROJECT%"));
	}
}
