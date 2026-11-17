package org.notima.fortnox.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.api.fortnox.oauth2.FileCredentialsProvider;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * 
 * Removes a fortnox client.
 * 
 * @author Daniel Tamm
 *
 */
@Command(scope = "fortnox", name = "remove-fortnox-client", description = "Remove a client to Fortnox integration")
@Service
public class RemoveClient implements Action {

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Reference
	private Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client to remove", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)
	String orgNo;
    
	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {

		BusinessObjectFactory b = null;
		for (BusinessObjectFactory bf : bofs) {
			if ("Fortnox".equals(bf.getSystemName())) {
				b = bf;
				break;
			}
		}

		if (b==null) {
			sess.getConsole().println("No Fortnox adapter available");
			return null;
		}
		
		b.setTenant(orgNo, null);
		BusinessPartner<?> bp = b.getCurrentTenant();
		if (bp==null) {
			sess.getConsole().println("No client [" + orgNo + "] found.");
			return null;
		}
		
		boolean result = false;
		
		String reply = sess.readLine("Do you really want to remove client [ " + orgNo + " ] " + bp.getName() + "? (y/n) ", null);
		if (reply.equalsIgnoreCase("y")) {
			result = b.removeTenant(orgNo, null);
		} else {
			sess.getConsole().println("Cancelled.");
			return null;
		}
		

		if (!result) {
			sess.getConsole().println("Removal unsucessful for [" + orgNo + "].");
		} else {
			sess.getConsole().println(bp.getTaxId() + " removed.");
			askToRemoveCredentials();
		}

		return null;
	}

	/**
	 * Asks if the stored credentials for the client should be removed too.
	 * Needed e.g. when the client is to be re-added with another integration (clientId).
	 */
	private void askToRemoveCredentials() throws Exception {
		FileCredentialsProvider credentialsProvider = new FileCredentialsProvider(orgNo);
		List<FortnoxCredentials> credentials = credentialsProvider.getAllCredentials();
		if (credentials==null || credentials.isEmpty()) {
			sess.getConsole().println("No credentials stored for [" + orgNo + "].");
			return;
		}
		String reply = sess.readLine("Remove also the " + credentials.size() + " stored credential(s) for [ " + orgNo + " ]? (y/n) ", null);
		if (reply!=null && reply.equalsIgnoreCase("y")) {
			credentialsProvider.removeAllCredentials();
			sess.getConsole().println(credentials.size() + " credential(s) removed for [" + orgNo + "].");
		} else {
			sess.getConsole().println("Credentials kept.");
		}
	}
	
}
