package org.notima.fortnox.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.Fortnox4jCLI;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.clients.FortnoxClientManager;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.businessobjects.adapter.tools.FactorySelector;

@Command(scope = _FortnoxCommandNames.SCOPE, name = _FortnoxCommandNames.AddOauthClient, description = "Add an oauth client to Fortnox integration")
@Service
public class AddOauthClient extends FortnoxCommand implements Action {

	@Reference
	private Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client to configure", required = false, multiValued = false)
	String orgNo;

    @Option(name = "--clientId", description = "The client ID for our Fortnox integration. If omitted, the default client secret is used (if set).", required = false, multiValued = false)
    private String clientId;
	
    @Option(name = "--clientSecret", description = "The client secret for our Fortnox integration. If omitted, the default client secret is used (if set).", required = false, multiValued = false)
    private String clientSecret;
	
    private FortnoxClient3		fortnoxClient;
    private FortnoxClientManager	clientManager;
    private Fortnox4jCLI		creator;
    
    private boolean				existingClient = false;

	@Override
	public Object execute() throws Exception {

		initVariables();
		printFortnoxClient();
		if (!checkParameters()) {
			return null;
		}

		if (existingClient) {
			sess.getConsole().println("Client already exists");
		}
		
		createClient();
		
		return null;
	}
    
    private void initVariables() throws Exception {

    	fortnoxClient = this.getFortnoxClient(orgNo);
    	if (fortnoxClient!=null) {
    		existingClient = true;
    	}
    	if (bf==null) {
    		FactorySelector selector = new FactorySelector(bofs);
    		bf = (FortnoxAdapter)selector.getFirstFactoryFor(FortnoxAdapter.SYSTEMNAME);
    	}
    	clientManager = bf.getClientManager();
    	
    }

    private boolean checkParameters() {
    	
    	if (!hasClientId()) {
    		sess.getConsole().println("No clientID defined.");
    		sess.getConsole().println("Use: property-set -p FortnoxProperties defaultClientId [clientId]");
    		return false;
    	}
    	
    	if (!hasClientSecret()) {
    		sess.getConsole().println("No client secret defined.");
    		sess.getConsole().println("Use: property-set -p FortnoxProperties defaultClientSecret [clientSecret]");
    		return false;
    	}
    	
    	return true;
    }
    
    private boolean hasClientId() {
    	if (clientId!=null) {
    		return true;
    	}
    	// Check for default client Id
    	if (clientManager!=null) {
    		clientId = clientManager.getDefaultClientId();
    	}
    	return clientId!=null;
    }
    
    private boolean hasClientSecret() {
    	if (clientSecret!=null) {
    		return true;
    	}
    	// Check for default client secret
    	if (clientManager!=null) {
    		clientSecret = clientManager.getDefaultClientSecret();
    	}
    	return clientSecret!=null;
    }
    
    private void printFortnoxClient() {
    	String message;
    	if (fortnoxClient!=null) {
    		message = "Exists: " + fortnoxClient.getTenantOrgNo();
    	} else {
    		message = orgNo + " is not yet defined";
    	}
    	sess.getConsole().println(message);
    }

    private void createClient() {
    	
    	creator = new Fortnox4jCLI();
    	creator.setClientId(clientId);
    	creator.setClientSecret(clientSecret);
    	
    	creator.setFortnoxClientManager(clientManager, orgNo);
    	if (creator.hasAuthenticationCode()) {
    		creator.setGetAccessToken(true);
    	} else {
    		creator.setGetAllTokens(true);
    	}
    	
    	creator.determineCommandToRun();
    	
    	
    }

}
