package org.notima.businessobjects.adapter.tools;

import org.osgi.framework.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class BundleUtil {

	  @SuppressWarnings("rawtypes")
	private Class clazz;
	
	  public BundleUtil(@SuppressWarnings("rawtypes") Class classInBundle) {
		  clazz = classInBundle;
	  }
	
	   public void copyResourcesToDirectory(String sourceDir, String destinationDir) {
	        Bundle bundle = FrameworkUtil.getBundle(clazz);
	        if (bundle == null) {
	            throw new RuntimeException("Bundle not found.");
	        }

	        // Get the resource URL within the bundle
	        URL resourceURL = bundle.getResource(sourceDir);
	        if (resourceURL == null) {
	            throw new RuntimeException("Resource directory not found: " + sourceDir);
	        }

	        // Convert URL to a URI
	        URI resourceURI;
	        try {
	            resourceURI = resourceURL.toURI();
	        } catch (URISyntaxException e) {
	            throw new RuntimeException("Invalid URI: " + resourceURL);
	        }

	        // Check if the resource is within the bundle JAR
	        if ("jar".equals(resourceURI.getScheme())) {
	            try (JarInputStream jarStream = new JarInputStream(resourceURL.openStream())) {
	                JarEntry entry;
	                while ((entry = jarStream.getNextJarEntry()) != null) {
	                    if (!entry.isDirectory() && entry.getName().startsWith(sourceDir)) {
	                        Path destination = Paths.get(destinationDir, entry.getName().substring(sourceDir.length()));
	                        Files.copy(jarStream, destination, StandardCopyOption.REPLACE_EXISTING);
	                    }
	                }
	            } catch (IOException e) {
	                throw new RuntimeException("Error reading JAR: " + resourceURL, e);
	            }
	        } else if ("bundle".equals(resourceURI.getScheme())) {
	        	Enumeration<String> files = bundle.getEntryPaths(sourceDir);
	        	if (files!=null) {
	        		while(files.hasMoreElements()) {
	        			String filePath = files.nextElement();
	        			File onlyFile = new File(filePath);
	        			String destPath = destinationDir + File.separator + onlyFile.getName();
	        			copyFileFromBundle(bundle, filePath, destPath);
	        		}
	        	}
	        } else {
	            // If it's not within a JAR, simply copy the files
	            try {
	                Files.walk(Paths.get(resourceURI))
	                        .forEach(source -> {
	                            try {
	                                Path destination = Paths.get(destinationDir, Paths.get(resourceURI).relativize(source).toString());
	                                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
	                            } catch (IOException e) {
	                                e.printStackTrace();
	                            }
	                        });
	            } catch (IOException e) {
	                throw new RuntimeException("Error walking file tree: " + resourceURI, e);
	            }
	        }
	    }	
	
	   /**
	    * Only copies the file if the destination doesn't exist
	    * 
	    * @param bundle
	    * @param sourcePath
	    * @param destinationPath
	    */
	   private void copyFileFromBundle(Bundle bundle, String sourcePath, String destinationPath) {
	        // Get the URL for the file within the bundle
	        URL fileURL = bundle.getEntry(sourcePath);

	        if (fileURL != null) {
	            try {

	            	File destinationFile = new File(destinationPath);
	            	if (destinationFile.exists()) {
	            		return;
	            	}
	            	
	                // Create the destination directory if it does not exist
	                File destinationDir = destinationFile.getParentFile();
	                if (!destinationDir.exists()) {
	                    destinationDir.mkdirs();
	                }
	                
	                // Open an InputStream to read the file
	                InputStream inputStream = fileURL.openStream();
	                
	                // Open an OutputStream to write the file
	                OutputStream outputStream = new FileOutputStream(destinationPath);

	                // Copy the file content
	                byte[] buffer = new byte[1024];
	                int length;
	                while ((length = inputStream.read(buffer)) > 0) {
	                    outputStream.write(buffer, 0, length);
	                }

	                // Close the streams
	                inputStream.close();
	                outputStream.close();
	            } catch (IOException e) {
	                // Handle any IO exceptions
	                e.printStackTrace();
	            }
	        } else {
	            // File not found within the bundle
	            System.out.println("File not found: " + sourcePath);
	        }
	    }	   
	   
}
