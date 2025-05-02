package org.notima.fortnox.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.scheduler.Scheduler;
import org.notima.api.fortnox.FortnoxCredentialsProvider;
import org.notima.api.fortnox.clients.FortnoxClientInfo;
import org.notima.api.fortnox.clients.FortnoxClientManager;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.api.fortnox.oauth2.FortnoxOAuth2Client;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.api.fortnox.oauth2.FileCredentialsProvider;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, property = {
        Scheduler.PROPERTY_SCHEDULER_EXPRESSION + "=0 0 5 ? * MON,TUE,WED,THU,FRI,SAT,SUN *",
        Scheduler.PROPERTY_SCHEDULER_NAME + "=" + "FortnoxCredentialsRefreshSchedule"
} )
public class credentialsRefreshScheduler implements Runnable {
	private Logger log = LoggerFactory.getLogger(credentialsRefreshScheduler.class);

	private FortnoxAdapter fa;
	private FortnoxClientInfo fci;
	private FortnoxClientManager fcm;
	
    @SuppressWarnings("rawtypes")
	@Override
    public void run() {
	    List<BusinessObjectFactory> bofs;
	    List<FortnoxClientManager> managers;
        try {
        	
        	managers = getServiceReferences(FortnoxClientManager.class);
        	if (managers!=null && managers.size()>0) {
        		fcm = managers.get(0);
        	}
        	
            bofs = getServiceReferences(BusinessObjectFactory.class);

            if (bofs==null) {
			    System.out.println("No Fortnox factories registered");
            } else {
                for (BusinessObjectFactory bf : bofs) {
                    if ("Fortnox".equals(bf.getSystemName())) {
                        fa = (FortnoxAdapter)bf;
                    	BusinessPartnerList<?> bpl = 
                                bf.listTenants();
                        for(BusinessPartner<?> bp : bpl.getBusinessPartner()) {
                            checkCredentials(bp);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	    
    }

    private void checkCredentials(BusinessPartner<?> bp) throws Exception {
        FortnoxCredentialsProvider credentialsProvider = new FileCredentialsProvider(bp.getTaxId());
        FortnoxCredentials credentials = credentialsProvider.getCredentials();
        if (fa.getClientManager()!=null) {
        	fci = fa.getClientManager().getClientInfoByOrgNo(bp.getTaxId());
        }
        if(credentials != null && credentials.hasRefreshToken()) {
            Instant expiryDate = credentials.getLastRefreshAsDate().toInstant();
            Duration expiresIn = Duration.between(Instant.now(), expiryDate);
            log.info(String.format("Credentials for %s expires in %d days \n", bp.getName(), expiresIn.toDays()));
            if(expiresIn.toDays() < 3) {
                try {
                    updateCredentials(credentialsProvider);
                } catch (Exception e) {
                    log.error("Token refresh failed", e);
                }
            }
        }
    }

    private void updateCredentials(FortnoxCredentialsProvider credentialsProvider) throws Exception {
        FortnoxCredentials credentials = credentialsProvider.getCredentials();
        String clientId = getClientId(credentials, credentialsProvider);
        String clientSecret = getClientSecret(credentials, credentialsProvider);
        if (clientId==null) {
       		throw new Exception("No clientId for orgNo " + credentialsProvider.getOrgNo());
        }
        if (clientSecret==null) {
       		throw new Exception("No clientSecret for orgNo " + credentialsProvider.getOrgNo());
        }
        FortnoxCredentials newCredentials = FortnoxOAuth2Client.refreshAccessToken(
            clientId, 
            clientSecret, 
            credentials.getRefreshToken());
        credentialsProvider.setCredentials(newCredentials);
    }

    
    private String getClientId(FortnoxCredentials credentials, FortnoxCredentialsProvider credentialsProvider) {
    	String clientId = credentials.getClientId();
    	if (clientId==null) {
    		clientId = credentialsProvider.getDefaultClientId();
    	}
    	if (clientId==null && fci!=null) {
    		clientId = fci.getClientId();
    	}
		if (clientId==null && fcm!=null) {
			clientId = fcm.getDefaultClientId();
		}
    	return clientId;
		
    }
    
    private String getClientSecret(FortnoxCredentials credentials, FortnoxCredentialsProvider credentialsProvider) {
    	String secret = credentials.getClientSecret();
    	if (secret==null) {
    		secret = credentialsProvider.getDefaultClientSecret();
    	}
    	if (secret==null && fci!=null) {
    		secret = fci.getClientSecret();
    	}
    	if (secret==null && fcm!=null) {
    		secret = fcm.getDefaultClientSecret();
    	}
    	
    	return secret;
    	
    }
    
    @SuppressWarnings("unchecked")
	protected <S> List<S> getServiceReferences(Class<S> clazz) throws InvalidSyntaxException {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		if (bundle != null) {
			BundleContext ctx = bundle.getBundleContext();
			ServiceReference<?>[] references = ctx.getAllServiceReferences(clazz.getName(), null);
			if (references != null){
                List<S> result = new ArrayList<S>();
                for(ServiceReference<?> reference : references) {
                    result.add((S) ctx.getService(reference));
                }
				return result;
            }
		}
		return null;
    }
    
}
