package files.kkolanow;

import java.io.IOException;
import java.util.Properties;

public class PropertiesManager {

	public static String INPUT_DIR = "INPUT_DIR";
	public static String OUTPUT_DIR = "OUTPUT_DIR";
	public static String DATE_PRECISION = "datePrecision";
	public static String MAX_PATH_LENGTH = "MAX_PATH_LENGTH";
	
	private static Properties  prop = null; 
	
	private static void load() {
		prop = new Properties(); 
	    try {
	    	//load a properties file from class path, inside static method
	        prop.load(PropertiesManager.class.getClassLoader().getResourceAsStream("config.properties"));
	        } 
	   catch (IOException ex) {
	        ex.printStackTrace();
	    }
		
	}
	
	public static String getProperty(String name) {
		if (prop==null) {
			load();
		}
		return prop.getProperty(name);
	}
	
	public static int getIntProperty(String name) {
		if (prop==null) {
			load();
		}
		return Integer.parseInt(prop.getProperty(name));
	}
	
	
	
	
}
