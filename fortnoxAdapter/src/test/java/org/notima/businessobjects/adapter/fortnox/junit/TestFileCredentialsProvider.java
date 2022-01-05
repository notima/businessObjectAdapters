package org.notima.businessobjects.adapter.fortnox.junit;

import org.junit.Test;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.businessobjects.adapter.fortnox.FileCredentialsProvider;

public class TestFileCredentialsProvider {

    @Test
    public void testSetKey() throws Exception {
        FileCredentialsProvider provider = new FileCredentialsProvider("111111-1111");
        FortnoxCredentials key = new FortnoxCredentials();
        key.setAccessToken("Hello");
        provider.setCredentials(key);
    }
}
