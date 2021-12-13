package org.notima.businessobjects.adapter.fortnox;

import java.io.File;
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

    Gson gson = new GsonBuilder().create();

    public FileCredentialsProvider(String orgNo) {
        super(orgNo);
    }

    @Override
    public FortnoxCredentials getCredentials() throws Exception {
        for(FortnoxCredentials key : getKeyList()) {
            if (key.getOrgNo().equals(orgNo)){
                return key;
            }
        }
        return null;
    }

    @Override
    public void setCredentials(FortnoxCredentials key) throws Exception {
        key.setOrgNo(orgNo);
        List<FortnoxCredentials> keys = getKeyList();
        boolean updated = false;
        for(int i = 0; i < keys.size(); i++) {
            if(keys.get(0).getOrgNo().equals(orgNo)) {
                keys.set(i, key);
                updated = true;
            }
        }
        if(!updated) {
            keys.add(key);
        }
        FileWriter fileWriter = new FileWriter("private/credentials.json");
        gson.toJson(keys, fileWriter);
        fileWriter.close();
    }

    private List<FortnoxCredentials> getKeyList() throws IOException {
        if(!new File("private/credentials.json").exists()) {
            Files.createFile(new File("private/credentials.json").toPath());
        }
        JsonReader reader = new JsonReader(new FileReader("private/credentials.json"));
        List<FortnoxCredentials> keyList = gson.fromJson(reader, KEYS_TYPE);
        return keyList != null ? keyList : new ArrayList<FortnoxCredentials>();
    }
    
}
