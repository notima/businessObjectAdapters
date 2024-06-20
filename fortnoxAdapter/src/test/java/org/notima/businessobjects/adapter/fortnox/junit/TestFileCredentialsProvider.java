package org.notima.businessobjects.adapter.fortnox.junit;

import org.junit.Before;
import org.junit.Test;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.api.fortnox.oauth2.CredentialsFile;
import org.notima.api.fortnox.oauth2.FileCredentialsProvider;

public class TestFileCredentialsProvider {
	
	public static final String TEST_ORGNO = "111111-1111";
	
	private FileCredentialsProvider provider;
	
	@Before
	public void setup() throws Exception {
		
		System.setProperty(CredentialsFile.CREDENTIALS_FILE_PROPERTY, "src/test/resources/credentials.json");

        provider = new FileCredentialsProvider(TEST_ORGNO);
		
	}
	
    // @Test
    public void testSetKey() throws Exception {
    	
        FortnoxCredentials key = createTestCredentials();
        provider.setCredentials(key);
        
    }

    @Test
    public void testRemoveAllCredentials() throws Exception {
    	
    	provider = new FileCredentialsProvider("555555-5555");
    	provider.removeAllCredentials();
    	
    }
    
    // @Test
    public void removeCredential() throws Exception {

    	FortnoxCredentials remove = createTestCredentials();
    	provider.removeCredential(remove);
    	
    }

    private FortnoxCredentials createTestCredentials() {
    	
        FortnoxCredentials key = new FortnoxCredentials();
        key.setOrgNo(TEST_ORGNO);
        key.setAccessToken("Hello");
        key.setLastRefresh(1000);
        return key;
    	
    }
    
}
