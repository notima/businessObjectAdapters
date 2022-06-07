package org.notima.fortnox.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxAuthenticationException;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.api.fortnox.entities3.CompanySetting;
import org.notima.businessobjects.adapter.fortnox.FileCredentialsProvider;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "show-fortnox-support-info", description = "Show support info for client")
@Service
public class ShowSupportInfo extends FortnoxCommand implements Action {

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";

	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	@Override
	public Object execute() throws Exception {
		
		FortnoxClient3 fc = getFortnoxClient(bofs, orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}

		FortnoxCredentials credentials = new FileCredentialsProvider(orgNo).getCredentials();

		if(credentials == null) {
			sess.getConsole().println("No credentials found");
			return null;
		}
		
		try {
			CompanySetting cs = fc.getCompanySetting();
			sess.getConsole().println("[ " + cs.getOrganizationNumber() + " ] - " + cs.getName());
			sess.getConsole().println("Contact: " + cs.getContactFirstName() + " " + cs.getContactLastName());
			sess.getConsole().println("Email: " + cs.getEmail());
			sess.getConsole().println("Subscription-ID: " + cs.getDatabaseNumber());
			
		} catch(FortnoxAuthenticationException e) {
			sess.getConsole().println("Authentication failed!");
			sess.getConsole().println(e.getMessage());
		}
			
		if(credentials.getLegacyToken() != null) {
				sess.getConsole().println("Using Legacy Access Token with client ID: " + credentials.getClientId());
		}
		else if(credentials.getAccessToken() != null) {
			sess.getConsole().println("Using OAuth2 Access Token with client ID: " + credentials.getClientId());
			sess.getConsole().println("Last token refresh: " + dateFormat.format(credentials.getLastRefreshAsDate()));
		}
		
		return null;
	}
	
	
}
