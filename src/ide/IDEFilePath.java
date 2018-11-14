package ide;

import java.io.File;

import engine.Game;
import engine.util.FileUtils;

public class IDEFilePath {

	public static String convertToSystem(String ideFilePath) {
		return FileUtils.fixPath(ideFilePath.replace("%PROJECT%", new File(Game.saveDirectory).getAbsolutePath()));
	}

	public static String convertToIDE(String systemFilePath) {
		return FileUtils.fixPath(systemFilePath.replace(new File(Game.saveDirectory).getAbsolutePath(), "%PROJECT%"));
	}
}
