package files.kkolanow;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 *
 */
public class App 
{
	private static final String LAST_SYNC_TXT = "lastSync.txt";
	private static Logger logger = Logger.getLogger(App.class.getName()); 
	
	static {
		logger.setLevel(Level.FINE);
	}
	
	
    public static void main( String[] args ) throws IOException
    {
        logger.info("Starting ");
        new App().process();
        logger.info("End");
    }
    
    private void process() throws IOException {
        
    	Path backupDrivePath = driveLetter();
    	Path inputPathRoot = propertyToPath(PropertiesManager.INPUT_DIR); 
    	Path outputPathRoot = propertyToPath(PropertiesManager.OUTPUT_DIR);
    	if( !outputPathRoot.isAbsolute()) {
    		outputPathRoot = FileSystems.getDefault().getPath(backupDrivePath.toString()+outputPathRoot.toString(), "");
    	} 
    			
        List<Path> inputFiles = listFolder(inputPathRoot,inputPathRoot);
        List<Path> outputFiles = listFolder(outputPathRoot,outputPathRoot);
        List<Path> filesToCopy = findFilesToCopy(inputFiles,outputFiles, inputPathRoot, outputPathRoot);
        List<Path> filesToDelete = findDeletedFiles(inputFiles,outputFiles, inputPathRoot, outputPathRoot);
        if (UserInteract.confirmChanges(filesToCopy, filesToDelete)) {
        	copyFiles(filesToCopy,inputPathRoot,outputPathRoot);
        	deleteFiles(filesToDelete);
            updateLastSyncDate(inputPathRoot);
        } else {
        	System.out.println("Process aborted");
        }
        
        
    }

    
private void updateLastSyncDate(Path inputRootPath) {

		DateFormat df = DateFormat.getDateTimeInstance();
		String date = df.format(new Date());
		try {
	            Files.deleteIfExists(inputRootPath.resolve(LAST_SYNC_TXT));
				Files.write( FileSystems.getDefault().getPath(inputRootPath.toString(), LAST_SYNC_TXT), 
	                         date.getBytes(), 
	                         StandardOpenOption.CREATE);
	        }
	        catch ( IOException ioe ) {
	            ioe.printStackTrace();
	        }
}

private Path propertyToPath(String property) {
		return FileSystems.getDefault().getPath(PropertiesManager.getProperty(property), "");
}

private Path driveLetter() {
		return new File("").toPath().toAbsolutePath().getRoot();
	}

/**
 * removes files from output files path
 * @param filesToDelete
 * @throws IOException 
 */
private void deleteFiles(List<Path> filesToDelete) throws IOException {
		logger.entering(this.getClass().getName(), "delete files");
		for (Path fileToDelete:  filesToDelete ) {
			Files.delete(fileToDelete);
			logger.info("file deleted: "+filesToDelete.toString());
		}
	}

private void copyFiles(List<Path> filesToCopy, Path inputPathRoot, Path outputPathRoot) {
		logger.entering(this.getClass().getName(), "copy files");
		for (Path fileToCopy: filesToCopy) {
			Path from = FileSystems.getDefault().getPath(inputPathRoot.toString(), fileToCopy.toString());
			Path to = FileSystems.getDefault().getPath(outputPathRoot.toString(), fileToCopy.toString());
			try {
				logger.info("copy file "+fileToCopy.toString());
				Files.createDirectories(to.getParent());
				Files.copy(from, to, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		
	}

/**
 * Iterate over ouptut and check if they are in input.
 * If they are not, they are added to files to delete list.
 * 
 * @param inputPaths
 * @param outputPath 
 * @param inputPath 
 * @param filesToCopy
 * @return
 */
	private List<Path> findDeletedFiles(List<Path> inputPaths,
			List<Path> outputPaths, Path inputPath, Path outputPath) {
		logger.entering(this.getClass().getName(), "findDeletedFiles");
		List<Path> deleteList =  new ArrayList<Path>();
		for(Path outputFile: outputPaths) {
			if(!inputPaths.contains(outputFile)) {
				logger.fine("file to delete detected "+outputFile);
				deleteList.add(
						FileSystems.getDefault().getPath(
								outputPath.toString(), outputFile.toString())
						);
			}
		}
		logger.exiting(this.getClass().getName(), "findDeletedFiles");
		return deleteList;
	}

	private List<Path> findFilesToCopy(List<Path> inputFiles,
			List<Path> outputFiles, Path rootInputPath, Path rootOutputPath) throws IOException {
		logger.entering(this.getClass().getName(), "findFilesToCopy ");		
		List<Path> filesToCopy = new ArrayList<Path>();
		for(Path inputPath : inputFiles) {
			if (outputFiles.contains(inputPath)) {
				if (datesAreDifferent(inputPath, rootInputPath, rootOutputPath)) {
					logger.fine("file to copy detected "+inputPath);
					filesToCopy.add(inputPath);
				} 
			} else {
				logger.fine("file to copy detected "+inputPath);
				filesToCopy.add(inputPath);
			}
		}
		logger.exiting(this.getClass().getName(), "findFilesToCopy ");
		return filesToCopy;
	}

	
	/**
	 * Compares last modified time of files
	 * @param inputFile
	 * @param rootOutputPath 
	 * @param rootInputPath 
	 * @return
	 */
	private boolean datesAreDifferent(Path inputFile, Path rootInputPath, Path rootOutputPath) throws IOException {
		logger.entering(this.getClass().getName(), "datesAreDifferent");
		FileTime inputTime = lastModificationTime(inputFile, rootInputPath);
		FileTime outputTime = lastModificationTime(inputFile, rootOutputPath);
		long diff = inputTime.to(TimeUnit.SECONDS) - outputTime.to(TimeUnit.SECONDS);
		return Math.abs(diff)>Integer.parseInt(PropertiesManager.getProperty(PropertiesManager.DATE_PRECISION));
	}

	private FileTime lastModificationTime(Path inputFile, Path rootPath) {
		Path filePath = rootPath.resolve(inputFile);
		BasicFileAttributes fileAttributes = null;
		try {
			fileAttributes = Files.getFileAttributeView(filePath, BasicFileAttributeView.class).readAttributes();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fileAttributes.lastModifiedTime();
	}
	
	

	private void printAll(List<Path> input) {
		for(Path fileName: input) {
			System.out.println(fileName.toString());
		}
	}


	private  List<Path> listFolder(Path inputPath, Path root) {
		logger.entering(this.getClass().getName(), "listFolder: "+inputPath.toString());
		List<Path> files = new ArrayList<Path>();
		try {
			DirectoryStream<Path> directoryIn = Files.newDirectoryStream(inputPath);
			for (Path path : directoryIn) {
				if (path.toFile().isDirectory()) {
					files.addAll(listFolder(path,root));
				} else if (!LAST_SYNC_TXT.equals(path.getFileName().toString())) {
					logger.log(Level.FINE, "file detected "+path.toString());
					files.add(root.relativize(path));
				}
			}
			
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.toString());
		}
		logger.exiting(this.getClass().getName(), "listFolder: "+inputPath.toString());
		return files;
	}
	
	
}
	