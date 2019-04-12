package engine.util;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ClassFinder {
	public static ArrayList<String>getClassNamesFromPackage(String packageName) {
	    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	    URL packageURL;
	    ArrayList<String> names = new ArrayList<String>();

	    packageName = packageName.replace(".", "/");
	    packageURL = classLoader.getResource(packageName);

	    if(packageURL.getProtocol().equals("jar")){
	        String jarFileName;
	        JarFile jf ;
	        Enumeration<JarEntry> jarEntries;
	        String entryName;

	        // build jar file name, then loop through zipped entries
	        try {
		        jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
		        jarFileName = jarFileName.substring(5,jarFileName.indexOf("!"));
		        System.out.println(">"+jarFileName);
		        jf = new JarFile(jarFileName);
		        jarEntries = jf.entries();
		        while(jarEntries.hasMoreElements()){
		            entryName = jarEntries.nextElement().getName();
		            if(entryName.startsWith(packageName) && entryName.length()>packageName.length()+5){
		                entryName = entryName.substring(packageName.length(),entryName.lastIndexOf('.'));
		                names.add(entryName);
		            }
		        }
	        }catch(Exception e) {
	        	//
	        }

	    // loop through files in classpath
	    }else{
		    URI uri;
			try {
				uri = new URI(packageURL.toString());
				
			    File folder = new File(uri.getPath());
		        // won't work with path which contains blank (%20)
		        // File folder = new File(packageURL.getFile()); 
		        File[] contenuti = folder.listFiles();
		        String entryName;
		        for(File actual: contenuti){
		            entryName = actual.getName();
		            entryName = entryName.substring(0, entryName.lastIndexOf('.'));
		            names.add(entryName);
		        }
			} catch (URISyntaxException e) {
				//
			}
	    }
	    
	    // Chop off starting "/" character
	    for (int i = 0; i < names.size(); i++) {
	    	String name = names.get(i);
	    	
	    	if ( name.startsWith("/") ) {
	    		names.set(i, name.substring(1));
	    	}
	    }
	    
		return names;
	}

	public static ArrayList<Class<?>> getClassesFromPackage(String packageName) {
		ArrayList<String> temp = getClassNamesFromPackage(packageName);
		ArrayList<Class<?>> cls = new ArrayList<Class<?>>();
		for (int i = 0; i < temp.size(); i++) {
			String name = temp.get(i);
			try {
				Class<?> cl = Class.forName(packageName+"."+name);
				cls.add(cl);
			} catch (ClassNotFoundException e) {
				//
			}
		}
		
		return cls;
	}
}