package engine.util;

import java.io.File;

public class FileUtils {
	/**
	 * Returns the directory of a file from a given file-path.<br>
	 * <br>
	 * If a directory is given as the path, that directory will be returned.<br>
	 * <br>
	 * If a non-existent file is given as a path, the directory leading to that file will be returned.
	 * @param path
	 * @return File Directory
	 */
	public static String getFileDirectoryFromPath( String path ) {
		if ( path == null )
			return null;
		
		// Fix the path to make sure its in the correct OS format
		path = fixPath(path);
		
		// Get the system file
		File f = new File(path);
		
		// Get the file-name
		String fileName = getFileNameFromPath(path);
		
		// User supplied a file-path
		if ( f.isFile() || fileName.contains(".") ) {
			
			// Return this files directory
			return path.substring( 0, path.lastIndexOf(File.separatorChar) );
		}
		
		// User already supplied a directory
		return path;
	}
	
	/**
	 * Returns the name of the file (with extension) of a file from a given file-path.
	 * @param path
	 * @return File Name
	 */
	public static String getFileNameFromPath( String path ) {
		if ( path == null )
			return null;
		
		// Fix the path to make sure its in the correct OS format
		path = fixPath(path);
		
		// Get filename
		return path.substring( path.lastIndexOf(File.separatorChar)+1, path.length() );
	}
	
	public static String fixPath(String path) {
		path = path.replace("\\/", File.separator);
		path = path.replace("\\\\", File.separator);
		path = path.replace("\\", File.separator);
		path = path.replace("/", File.separator);
		return path;
	}

	/**
	 * Returns the name of a the file without an extension. This does not filter out the path of a file.<br>
	 * <br>
	 * If you wish to filter out the path, first use {@link #getFileNameFromPath(String)}
	 * @param file
	 * @return File Name without Extension
	 */
	public static String getFileNameWithoutExtension( String file ) {
		String fileWithoutExtension = file;
		
		// Get the last position of the . character
		int pos1 = fileWithoutExtension.lastIndexOf(".");
		
		// If it exists, there is an extension
		if (pos1 != -1) {
			
			// update the string without this extension
			fileWithoutExtension = fileWithoutExtension.substring(0, pos1);
		}
		
		return fileWithoutExtension;
	}
	
	/**
	 * Return the extension (with . included) of a file name or file path.
	 * @param fileNameOrPath
	 * @return File Extension
	 */
	public static String getFileExtension( String fileNameOrPath ) {
		String s1 = getFileNameFromPath(fileNameOrPath);
		if ( s1 == null )
			return null;
		
		String f1 = getFileNameWithoutExtension(s1);
		return s1.replace(f1, "");
	}
}
