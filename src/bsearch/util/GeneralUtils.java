package bsearch.util;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Scanner;

public class GeneralUtils {
	private static final String VERSION_STRING = "0.74 (beta)";
	private static final double VERSION_NUMBER = 0.74;
	
	public static String getVersionString()
	{
		return VERSION_STRING;
	}
	public static double getVersionNumber()
	{
		return VERSION_NUMBER;
	}

	public static String stringContentsOfFile(File file) throws java.io.FileNotFoundException
	{
		Scanner scanner = new java.util.Scanner(file);
		String contents = scanner.useDelimiter("\\Z").next(); // slurp the whole file
		scanner.close();
		return contents;
	}
	
	public static String stringJoin(java.util.Collection<?> s, String delimiter) {
	     StringBuilder builder = new StringBuilder();
	     java.util.Iterator<?> iter = s.iterator();
	     while (iter.hasNext()) {
	         builder.append(iter.next());
	         if (!iter.hasNext()) {
	           break;                  
	         }
	         builder.append(delimiter);
	     }
	     return builder.toString();
	 }
	
	public static String formatTimeNicely(long millis)
	{
		int seconds = (int) Math.round(millis / 1000.0);
		int minutes = seconds / 60;
		int hours = minutes / 60;
		seconds = seconds % 60;
		minutes = minutes % 60;
		if (hours > 0)
		{
			return hours + ":" + String.format("%02d",minutes) + ":" + String.format("%02d",seconds);
		}
		else
		{
			return minutes + ":" + String.format("%02d",seconds);			
		}
	}

	/** returns html text for the parameter settings given, separated by newlines */
	public static String getParamSettingsTextHTML( LinkedHashMap<String, Object> paramSettings)
	{
		StringBuilder sb = new StringBuilder();
		for (String param : paramSettings.keySet())
		{
			sb.append(param);
			sb.append("=");
			Object obj = paramSettings.get(param);
			if (obj instanceof Double)
			{
				obj = String.format("%.6g", obj);
			}
			sb.append(obj);
			sb.append("<BR>");
		}
		return sb.toString();
	}
	
	public static boolean isOSWindows()
	{
	    return System.getProperty("os.name").toLowerCase().indexOf( "windows" ) >= 0; 
	}
	
	
	//Because NetLogo is finicky, we *must* launch BehaviorSearch with the working folder (current directory)
	//set to the NetLogo folder.  However, our launching scripts set these Java properties, so we know
	//what the original "working folder" is, and also where the BehaviorSearch application folder is.
	private static String startupFolderStr = System.getProperty("bsearch.startupfolder");
	private static String bsearchAppFolderStr = System.getProperty("bsearch.appfolder");

	public static String attemptResolvePathFromStartupFolder(String pathStr)
	{
		if (startupFolderStr == null)
		{
			return pathStr;
		}
		File path = new File(pathStr);

		if (path.isAbsolute())
		{
			return pathStr;
		}
		
		File startupFolder = new File(startupFolderStr);
		
		return new File(startupFolder,pathStr).getAbsolutePath();
	}
	
	private static File protocolFolder = null;
	public static void updateProtocolFolder(String protocolFilename)
	{
		if (protocolFilename != null)
		{
			protocolFilename = attemptResolvePathFromStartupFolder(protocolFilename);
			File protocolFile = new File(protocolFilename);
			protocolFolder = protocolFile.getParentFile();
		}
	}
	public static String attemptResolvePathFromProtocolFolder(String pathStr)
	{
		if (protocolFolder == null)
		{
			return pathStr;
		}
		File path = new File(pathStr);

		if (path.isAbsolute())
		{
			return pathStr;
		}
		
		return new File(protocolFolder,pathStr).getAbsolutePath();		
	}

	public static String attemptResolvePathFromBSearchRoot(String pathStr)
	{
		// check if the program was started in the NetLogo folder, in which case behaviorsearch is in a subfolder.
		// NOTE: As of NetLogo 4.1 anyway,
		// if you don't start the JVM with the NetLogo app folder as the current working directory,
		// then NetLogo doesn't find its built-in extensions folder, and some other things... 		
		File rootDir;
		if (bsearchAppFolderStr != null && !bsearchAppFolderStr.equals(""))
		{
			rootDir = new File(bsearchAppFolderStr);
		}
		else
		{
			rootDir = new File("behaviorsearch");			
		}
		if (rootDir.exists() && rootDir.isDirectory())
		{
			return new File(rootDir,pathStr).getAbsolutePath();	
		}
		return pathStr;
	}
	
	public static File getResource(String fileName)
	{
		return new File(attemptResolvePathFromBSearchRoot("resources/" + fileName));
	}

}
