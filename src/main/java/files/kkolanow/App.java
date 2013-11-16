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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
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
	private static String version = App.class.getPackage().getImplementationVersion();
	
	static {
		logger.setLevel(Level.FINE);
		logger.setUseParentHandlers(false);
	}
	
	
    public static void main( String[] args ) throws IOException
    {
        print("Backup tool version: "+version);
        new App().process();
        print("End.");
    }
    
    private void process() throws IOException {
        
    	Path backupDrivePath = driveLetter();
    	Path inputPathRoot = propertyToPath(PropertiesManager.INPUT_DIR); 
    	Path outputPathRoot = propertyToPath(PropertiesManager.OUTPUT_DIR);
    	int maxPathLength = PropertiesManager.getIntProperty(PropertiesManager.MAX_PATH_LENGTH);
    	
    	if( !outputPathRoot.isAbsolute()) {
    		outputPathRoot = FileSystems.getDefault().getPath(backupDrivePath.toString()+outputPathRoot.toString(), "");
    	} 
    	
    	setupLog(inputPathRoot); 	 	

    	List<Path> inputFiles = listFolder(inputPathRoot,inputPathRoot);
        List<Path> outputFiles = listFolder(outputPathRoot,outputPathRoot);
        List<Path> filesToCopy = findFilesToCopy(inputFiles,outputFiles, inputPathRoot, outputPathRoot);
        List<Path> filesToDelete = findDeletedFiles(inputFiles,outputFiles, inputPathRoot, outputPathRoot);
        
        if (UserInteract.confirmChanges(filesToCopy, filesToDelete,maxPathLength, outputPathRoot)) {
        	int copiedFiles = copyFiles(filesToCopy,inputPathRoot,outputPathRoot,maxPathLength);
        	int deletedFiles = deleteFiles(filesToDelete,maxPathLength, outputPathRoot);
        	
        	print("Number of copiedFiles: "+copiedFiles);
        	print("Number of deletedFiles: "+deletedFiles);
            updateLastSyncDate(inputPathRoot);
        } else {
        	print("Process aborted by user");
        }
        
        
        
    }

	private void setupLog(Path inputPathRoot) throws IOException {
		Path logPath = inputPathRoot.getParent();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyy-mm-dd_hh-mm");
    	FileHandler fh = new FileHandler(logPath.toAbsolutePath().toString()+ File.separator+"backupTool-"+sdf.format(new Date())+".log");
		logger.addHandler(fh);
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
 * @return number of deleted files
 */
private int deleteFiles(List<Path> filesToDelete, int charsToDisplay, Path outputPath) throws IOException {
		logger.entering(this.getClass().getName(), "delete files");
		print("Deleted files: ");
		int deletedFiles=0;
		for (Path fileToDelete:  filesToDelete ) {
			Files.delete(fileToDelete);
			Path relativePath = outputPath.relativize(fileToDelete);
			print(HelperMethods.truncate(charsToDisplay, relativePath));
			deletedFiles++;
		}
		logger.exiting(this.getClass().getCanonicalName(), "deleteFiles");
		return deletedFiles;
	}

/**
 * 
 * @param filesToCopy
 * @param inputPathRoot
 * @param outputPathRoot
 * @return number of successfuly copied files;
 */
private int copyFiles(List<Path> filesToCopy, Path inputPathRoot, Path outputPathRoot, int charsToDisplay) {
		logger.entering(this.getClass().getName(), "copy files");
		print("Copied files :");
		int filesCopied=0;
		for (Path fileToCopy: filesToCopy) {
			Path from = FileSystems.getDefault().getPath(inputPathRoot.toString(), fileToCopy.toString());
			Path to = FileSystems.getDefault().getPath(outputPathRoot.toString(), fileToCopy.toString());
			try {
				print(HelperMethods.truncate(charsToDisplay,fileToCopy));
				Files.createDirectories(to.getParent());
				Files.copy(from, to, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
				filesCopied++;
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Can't copy file "+fileToCopy);
				logger.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}

		}
		logger.exiting(this.getClass().getCanonicalName().toString(), "copyFiles");
		return filesCopied;
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
				logger.info(outputFile.toString());
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
					/**
					 * TODO compare also a file size to ask user if that should be taken care... or maybe several modes of comparision are needed data, size, data and size...
					 * as there was a problem when day saving time has been changed and all backup has been detected to be moved as hours were different by 1 
					 */
					logger.info(inputPath.toString());
					filesToCopy.add(inputPath);
				} 
			} else {
				logger.info(""+inputPath);
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
		logger.entering(this.getClass().getName(), "lastModificationTime");
		Path filePath = rootPath.resolve(inputFile);
		BasicFileAttributes fileAttributes = null;
		try {
			fileAttributes = Files.getFileAttributeView(filePath, BasicFileAttributeView.class).readAttributes();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Can't get last modificaiton time for file "+filePath.toAbsolutePath().toString());
			logger.log(Level.SEVERE, e.getMessage());
			e.printStackTrace();
		}
		return fileAttributes.lastModifiedTime();
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
	
	private static void print(String message) {
		logger.info(message);
		System.out.println(message);
	}
	
	
}
	