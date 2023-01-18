package org.notima.fortnox.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.clients.FortnoxClientManager;
import org.notima.businessobjects.adapter.fortnox.FileCredentialsProvider;
import org.notima.businessobjects.adapter.fortnox.exception.InsufficientOrWrongInputException;

@Command(scope = "fortnox", name = "set-fortnox-clientId", description = "Set client id")
@Service
public class SetClientId extends FortnoxCommand2 implements Action {
	
	@Reference
	private FortnoxClientManager 		fortnoxClientManager;
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";

	@Argument(index = 1, name = "clientId", description ="The clientID. If omitted default client id is used.", required = false, multiValued = false)
	private String clientId;

	private FortnoxClient3 fc = null;
	
	
	@Override
	public Object execute() throws Exception {
		
		try {
			getFortnoxClient();
			checkFortnoxClientManager();
			checkClientId();
			readAndUpdateCredentialsFileWithClientId();
			
		} catch (InsufficientOrWrongInputException ii) {
			sess.getConsole().println(ii.getMessage());
		}
		
		return null;
	}

	private void readAndUpdateCredentialsFileWithClientId() throws Exception {
		
		FileCredentialsProvider fc = new FileCredentialsProvider(orgNo);
		fc.setClientId(clientId);
		fc.setCredentials(fc.getCredentials());
		sess.getConsole().println("Client ID updated for " + orgNo);
		
	}
	
	private void checkClientId() throws InsufficientOrWrongInputException {
		if (clientId==null) {
			clientId = fortnoxClientManager.getDefaultClientId();
		}
		if (clientId==null) {
			throw new InsufficientOrWrongInputException("No default client id");
		}
	}
	
	private void checkFortnoxClientManager() throws InsufficientOrWrongInputException {
		if (fortnoxClientManager==null) {
			throw new InsufficientOrWrongInputException("No Fortnox Client Manager found.");
		}
	}
	
	private void getFortnoxClient() throws InsufficientOrWrongInputException, Exception {

		fc = getFortnoxClient(orgNo);
		if (fc == null) {
			throw new InsufficientOrWrongInputException("Can't get client for " + orgNo);
		}
		
	}
	
}
