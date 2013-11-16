package files.kkolanow;

import java.nio.file.Path;

public class HelperMethods {

	static String truncate(int charsToDisplay, Path path) {
		String filePath =path.toString();
		if (charsToDisplay>0 && filePath.length()>charsToDisplay) {
			int start = filePath.length() - charsToDisplay;
			filePath = filePath.substring(start, filePath.length()); 
		}
		return filePath;
	}

}
