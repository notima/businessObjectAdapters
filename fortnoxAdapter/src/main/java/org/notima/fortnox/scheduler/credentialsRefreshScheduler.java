package org.notima.fortnox.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.scheduler.Scheduler;
import org.notima.api.fortnox.FortnoxCredentialsProvider;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.api.fortnox.oauth2.FortnoxOAuth2Client;
import org.notima.businessobjects.adapter.fortnox.FileCredentialsProvider;
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

    @SuppressWarnings("rawtypes")
	@Override
    public void run() {
	    List<BusinessObjectFactory> bofs;
        try {
            bofs = getServiceReferences(BusinessObjectFactory.class);

            if (bofs==null) {
			    System.out.println("No Fortnox factories registered");
            } else {
                for (BusinessObjectFactory bf : bofs) {
                    if ("Fortnox".equals(bf.getSystemName())) {
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
        FortnoxCredentials newCredentials = FortnoxOAuth2Client.refreshAccessToken(
            credentials.getClientId(), 
            credentials.getClientSecret(), 
            credentials.getRefreshToken());
        credentialsProvider.setCredentials(newCredentials);
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
