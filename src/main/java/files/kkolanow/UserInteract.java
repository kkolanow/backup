package files.kkolanow;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class UserInteract {
	
	/**
	 * 
	 * @param filesToCopy
	 * @param filesToDelete
	 * @param charsToDisplay how long the logged file name should be. if non-zero, takes only int charsToDisplay last characters
	 * @return 
	 */
	public static boolean confirmChanges(List<Path> filesToCopy, List<Path> filesToDelete, int charsToDisplay, Path outputPath) {
		if (filesToCopy.isEmpty() && filesToDelete.isEmpty()) {
			System.out.println("There are no changes.");
			return true;
		} else {
			System.out.println("There are changes to sync ");
			if (filesToCopy.isEmpty()) {
				System.out.println("There are no files to copy");
			} else {
				System.out.println("Files to copy: ");
				for(Path path: filesToCopy) {
					String filePath = HelperMethods.truncate(charsToDisplay, path);
					System.out.println(filePath);
				}
			}
			if (filesToDelete.isEmpty()) {
				System.out.println("There are no files to delete");
			} else {
				System.out.println("Files to delete: ");
				for(Path path: filesToDelete) {
					Path relativePath = outputPath.relativize(path);
					String filePath = HelperMethods.truncate(charsToDisplay, relativePath);
					System.out.println(filePath);
				}
			}
			System.out.println("There are total " +filesToCopy.size()+" file(s) to copy and " + filesToDelete.size()+" file(s) to delete" );
			System.out.println("Execute changes? [Y/N] ...");
			char input = 0;
			do {
				try {
					input = (char) System.in.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} while (!"Y".equalsIgnoreCase(String.valueOf(input)) && !"N".equalsIgnoreCase(String.valueOf(input)));
			return "y".equalsIgnoreCase(String.valueOf(input));
		}
		
		
	}
	
	

}
