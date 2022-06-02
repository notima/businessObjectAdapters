package org.notima.businessobjects.adapter.fortnox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import org.notima.api.fortnox.FortnoxCredentialsProvider;
import org.notima.api.fortnox.clients.FortnoxCredentials;

public class FileCredentialsProvider extends FortnoxCredentialsProvider {
    public static final Type KEYS_TYPE = new TypeToken<List<FortnoxCredentials>>() {}.getType();
    public static final String CREDENTIALS_FILE_PROPERTY = "FortnoxCredentialsFile";

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File file;

    public FileCredentialsProvider(String orgNo) throws IOException {
        super(orgNo);
        file = new File(System.getProperty(CREDENTIALS_FILE_PROPERTY));
        if(!file.exists()) {
            Files.createFile(file.toPath());
        }
    }

    /**
     * Return the credentials with the highest lastRefresh.
     */
    @Override
    public FortnoxCredentials getCredentials() throws Exception {
    	
    	FortnoxCredentials result = null;
    	
        for(FortnoxCredentials credentials : getKeyList()) {
            if (credentials.getOrgNo().equals(orgNo)){
                result = credentials;
            }
        }
        return result;
    }

    @Override
    public void setCredentials(FortnoxCredentials credentials) throws Exception {
        credentials.setOrgNo(orgNo);
        List<FortnoxCredentials> keys = getKeyList();
        boolean updated = false;
        for(int i = 0; i < keys.size(); i++) {
            if(keys.get(0).getOrgNo().equals(orgNo)) {
                keys.set(i, credentials);
                updated = true;
            }
        }
        if(!updated) {
            keys.add(credentials);
        }
        FileWriter fileWriter = new FileWriter(file.getPath());
        gson.toJson(keys, fileWriter);
        fileWriter.close();
    }

    @Override
    public void removeCredentials() throws IOException {
        List<FortnoxCredentials> credentialsList = getKeyList();
        for(int i = 0; i < credentialsList.size(); i++) {
            if (credentialsList.get(i).getOrgNo().equals(orgNo)){
                credentialsList.remove(i);
                break;
            }
        }
        FileWriter fileWriter = new FileWriter(file.getPath());
        gson.toJson(credentialsList, fileWriter);
        fileWriter.close();
    }

    private List<FortnoxCredentials> getKeyList() throws FileNotFoundException {
        JsonReader reader = new JsonReader(new FileReader(file.getPath()));
        List<FortnoxCredentials> keyList = gson.fromJson(reader, KEYS_TYPE);
        return keyList != null ? keyList : new ArrayList<FortnoxCredentials>();
    }
    
}
