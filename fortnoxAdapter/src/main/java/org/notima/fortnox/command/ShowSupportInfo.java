package org.notima.fortnox.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

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
import org.notima.api.fortnox.oauth2.FileCredentialsProvider;

@Command(scope = "fortnox", name = "show-fortnox-support-info", description = "Show support info for client")
@Service
public class ShowSupportInfo extends FortnoxCommand implements Action {
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";

	@Option(name = "--show-secrets", description = "Show secrets (use with caution)", required = false, multiValued = false)
	private boolean	showSecrets;
	
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private FortnoxClient3 fc;
	private FortnoxCredentials credentials;
	
	private CompanySetting cs;
	
	private String 			message;
	
	@Override
	public Object execute() throws Exception {

		try {
		
			getFortnoxClient();
			
			getCredentials();
			
		} catch (Exception ee) {
			if (message!=null)
				sess.getConsole().println(message);
			else
				ee.printStackTrace();
			return null;
		}

		try {
			printCompanySettings();
		} catch (Exception e) {
			sess.getConsole().println("Could not get company settings: " + e.getMessage());
		}

		printTokens();
		
		return null;
	}
	
	
	private void printCompanySettings() throws Exception {

		try {
			cs = fc.getCompanySetting();
			sess.getConsole().println("[ " + cs.getOrganizationNumber() + " ] - " + cs.getName());
			sess.getConsole().println("Contact: " + cs.getContactFirstName() + " " + cs.getContactLastName());
			sess.getConsole().println("Email: " + cs.getEmail());
			sess.getConsole().println("Subscription-ID: " + cs.getDatabaseNumber());
			
		} catch(FortnoxAuthenticationException e) {
			sess.getConsole().println("Authentication failed!");
			sess.getConsole().println(e.getMessage());
		}
		
		
	}

	private void printTokens() {

		if(credentials.getLegacyToken() != null) {
			printLegacy();
		}	else if(credentials.getAccessToken() != null) {
			printAccessToken();
		}
		
	}
	
	
	private void printLegacy() {
		sess.getConsole().println("Using Legacy Access Token with client ID: " + credentials.getClientId());

		if (showSecrets) {
			sess.getConsole().println("** Legacy Access Token: " + credentials.getLegacyToken());
			sess.getConsole().println("** Client secret: " + credentials.getClientSecret());
		}
		
	}
	
	
	private void printAccessToken() {
		
		sess.getConsole().println("Using OAuth2 Access Token with client ID: " + credentials.getClientId());
		sess.getConsole().println("Last token refresh: " + dateFormat.format(credentials.getLastRefreshAsDate()));

		if (showSecrets) {
			
			sess.getConsole().println("** Secrets below ** ");
			sess.getConsole().println(credentials.getAccessToken() + " " + credentials.getRefreshToken() + " " + credentials.getLastRefresh());
			sess.getConsole().println("** Last refresh : " + dateFormat.format(credentials.getLastRefreshAsDate()));
			
		}
		
	}
	
	private void getCredentials() throws Exception {
		credentials = new FileCredentialsProvider(orgNo).getCredentials();

		if(credentials == null) {
			message = "No credentials found";
			throw new Exception(message);
		}

	}
	
	
	private void getFortnoxClient() throws Exception {
		
		fc = getFortnoxClient(orgNo);
		if (fc == null) {
			message = "Can't get client for " + orgNo;
			throw new Exception(message);
		}
		
		
	}
	
	
}
