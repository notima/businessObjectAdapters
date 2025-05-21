package org.notima.fortnox.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxAuthenticationException;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.api.fortnox.entities3.CompanySetting;
import org.notima.api.fortnox.oauth2.FileCredentialsProvider;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;
import org.notima.fortnox.command.table.CredentialTable;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = _FortnoxCommandNames.SCOPE, name = _FortnoxCommandNames.ListCredentials, description = "List credentials for client")
@Service
public class ListCredentials extends FortnoxCommand implements Action {
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgnos of the clients. Leave empty to show credentials for all clients.", required = false, multiValued = true)
	@Completion(FortnoxTenantCompleter.class)	
	private String[] orgNos;

	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	private FortnoxClient3 fc;
	private CompanySetting cs;


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

			sess.getConsole().println("\033[4mCredentials for \033[1m" + orgNo + "\033[0m");

			List<FortnoxCredentials> credentials = new FileCredentialsProvider(orgNo).getAllCredentials();

			if(credentials == null) {
				sess.getConsole().println("No credentials found");
				return null;
			}

			CredentialTable table = new CredentialTable(credentials);

			table.getShellTable().print(sess.getConsole());
			
			fc = getFortnoxClient(orgNo);
			if (fc == null) {
				sess.getConsole().println("Can't get client for " + orgNo);
				return null;
			}
			
			try {
				cs = fc.getCompanySetting();
				sess.getConsole().println("[ " + cs.getOrganizationNumber() + " ] - " + cs.getName());
				sess.getConsole().println("Contact: " + cs.getContactFirstName() + " " + cs.getContactLastName());
				sess.getConsole().println("Email: " + cs.getEmail());
				sess.getConsole().println("Subscription-ID: " + cs.getDatabaseNumber());
				
				if (credentials.size()>4) {
					sess.getConsole().println("\nUse " + _FortnoxCommandNames.PurgeCredentials + " to purge old credentials.");
				}
				
			} catch(FortnoxAuthenticationException e) {
				sess.getConsole().println("Authentication failed!");
				sess.getConsole().println(e.getMessage());
				if (e.getCredentials()!=null) {
					sess.getConsole().println(e.getCredentials().toString());
				}
			}

		}
		
		
		return null;
	}
	
	
}
