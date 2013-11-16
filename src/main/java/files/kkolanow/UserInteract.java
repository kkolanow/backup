package files.kkolanow;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class UserInteract {
	
	public static boolean confirmChanges(List<Path> filesToCopy, List<Path> filesToDelete) {
		if (filesToCopy.isEmpty() && filesToDelete.isEmpty()) {
			System.out.println("There are no changes.");
			return true;
		} else {
			System.out.println("There are changes to sync ");
			System.out.println("Files to copy: ");
			for(Path path: filesToCopy) {
				System.out.println(path.toString());
			}
			System.out.println("Files to delete: ");
			for(Path path: filesToDelete) {
				System.out.println(path.toString());
			}
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
