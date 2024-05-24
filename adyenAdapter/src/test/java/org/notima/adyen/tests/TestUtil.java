package org.notima.adyen.tests;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

import com.google.gson.Gson;

public class TestUtil {

	public static Gson createGson() {
		
		Gson gson = org.notima.util.json.JsonUtil.buildGson();
		
		return gson;
	}
	
	/**
     * Returns a File object for a file located in the src/test/resources folder.
     *
     * @param fileName The name of the file.
     * @return A File object representing the file.
     * @throws IllegalArgumentException if the file is not found.
     */
    public static File getFileFromResources(String fileName) {
        // Get the class loader of the current class
        ClassLoader classLoader = TestUtil.class.getClassLoader();
        
        // Get the URL of the resource
        URL resource = classLoader.getResource(fileName);
        
        if (resource == null) {
            throw new IllegalArgumentException("File not found! " + fileName);
        }
        
        // Convert URL to File
        return Paths.get(resource.getPath()).toFile();
    }	
	
}
