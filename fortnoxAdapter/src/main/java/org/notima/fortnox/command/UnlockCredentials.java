package org.notima.fortnox.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.api.fortnox.oauth2.FileCredentialsProvider;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = _FortnoxCommandNames.SCOPE, name = "unlock-credentials", description = "Unlock fortnox credentials to allow them to be refreshed")
@Service
public class UnlockCredentials extends FortnoxCommand implements Action {
	
	@Reference 
	Session sess;

    @Argument(index = 0, name = "orgNo", description ="The orgnos of clients to unlock credentials for. All credentials will be unlocked if this is omitted", required = false, multiValued = true)
    private String[] orgNos;

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;

    @Override
    public Object execute() throws Exception {
        if(orgNos == null) {
			for (BusinessObjectFactory bf : bofs) {
				if ("Fortnox".equals(bf.getSystemName())) {
					BusinessPartnerList<?> bpl = bf.listTenants();
					orgNos = new String[bpl.getBusinessPartner().size()];
					for(int i = 0; i < orgNos.length; i++) {
						orgNos[i] = bpl.getBusinessPartner().get(i).getTaxId();
					}
				}
			}
		}

        for(String orgNo : orgNos) {
			FileCredentialsProvider credentialsProvider = new FileCredentialsProvider(orgNo);
            List<FortnoxCredentials> credentials = credentialsProvider.getAllCredentials();
            for(FortnoxCredentials cred : credentials) {
                cred.unlockRefresh();
				credentialsProvider.setCredentials(cred);
                sess.getConsole().printf("Credentials for %s (%s) have been \u001B[32munlocked\u001B[0m\n", orgNo, cred.getAccessTokenAbbreviated());
            }
        }
        return null;
    }
}