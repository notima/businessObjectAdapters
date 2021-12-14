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
    private static final Type KEYS_TYPE = new TypeToken<List<FortnoxCredentials>>() {}.getType();
    public static final String CREDENTIALS_FILE_PROPERTY = "FortnoxCredentialsFile";

    private Gson gson = new GsonBuilder().create();
    private File file;

    public FileCredentialsProvider(String orgNo) throws IOException {
        super(orgNo);
        file = new File(System.getProperty(CREDENTIALS_FILE_PROPERTY));
        if(!file.exists()) {
            Files.createFile(file.toPath());
        }
    }

    @Override
    public FortnoxCredentials getCredentials() throws Exception {
        for(FortnoxCredentials credentials : getKeyList()) {
            if (credentials.getOrgNo().equals(orgNo)){
                return credentials;
            }
        }
        return null;
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

    private List<FortnoxCredentials> getKeyList() throws FileNotFoundException {
        JsonReader reader = new JsonReader(new FileReader(file.getPath()));
        List<FortnoxCredentials> keyList = gson.fromJson(reader, KEYS_TYPE);
        return keyList != null ? keyList : new ArrayList<FortnoxCredentials>();
    }
    
}
