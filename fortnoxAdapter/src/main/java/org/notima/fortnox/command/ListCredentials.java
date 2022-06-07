package org.notima.fortnox.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxAuthenticationException;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.api.fortnox.entities3.CompanySetting;
import org.notima.businessobjects.adapter.fortnox.FileCredentialsProvider;
import org.notima.fortnox.command.table.CredentialTable;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "list-fortnox-credentials", description = "List credentials for client")
@Service
public class ListCredentials extends FortnoxCommand implements Action {

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

		List<FortnoxCredentials> credentials = new FileCredentialsProvider(orgNo).getAllCredentials();

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
		
		CredentialTable table = new CredentialTable(credentials);

		table.getShellTable().print(sess.getConsole());
		
		return null;
	}
	
	
}
