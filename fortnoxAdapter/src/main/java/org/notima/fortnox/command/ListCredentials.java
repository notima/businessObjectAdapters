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
import org.notima.businessobjects.adapter.fortnox.FileCredentialsProvider;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;
import org.notima.fortnox.command.table.CredentialTable;

@Command(scope = "fortnox", name = _CommandNames.ListCredentials, description = "List credentials for client")
@Service
public class ListCredentials extends FortnoxCommand implements Action {
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";

	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	private FortnoxClient3 fc;
	private CompanySetting cs;
	
	@Override
	public Object execute() throws Exception {

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
				sess.getConsole().println("\nUse " + _CommandNames.PurgeCredentials + " to purge old credentials.");
			}
			
		} catch(FortnoxAuthenticationException e) {
			sess.getConsole().println("Authentication failed!");
			sess.getConsole().println(e.getMessage());
			if (e.getCredentials()!=null) {
				sess.getConsole().println(e.getCredentials().toString());
			}
		}
		
		
		return null;
	}
	
	
}
