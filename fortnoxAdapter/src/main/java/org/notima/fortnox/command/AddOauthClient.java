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
import org.notima.api.fortnox.FortnoxScope;
import org.notima.api.fortnox.clients.FortnoxClientInfo;
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
	
    @Option(name = "--scope", description = "The scope to be requested for this client. If omitted, the default scope is used (if set)", required = false, multiValued = false)
    private String scopeString;
    
    @Option(name = "--renew", description = "Renew from scratch, even if credentials/client exist")
    private boolean renew;
    
    private FortnoxClient3		fortnoxClient;
    private FortnoxClientManager	clientManager;
    private FortnoxClientInfo	fortnoxClientInfo;
    private Fortnox4jCLI		creator;
    
    private boolean				existingClient = false;
    
    private String				clientIdSource;
    private String				scopeSource;

	@Override
	public Object execute() throws Exception {

		initVariables();
		printFortnoxClient();
		if (!checkParameters()) {
			return null;
		}

		if (existingClient && !renew) {
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
    	fortnoxClientInfo = clientManager.getClientInfoByOrgNo(orgNo);
    	
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

    	if (!hasScope()) {
    		sess.getConsole().println("No scope defined.");
    		sess.getConsole().println("Use: property-set -p FortnoxProperties defaultScope [scope] or use parameter --scope");
    		return false;
    	}
    	
    	return true;
    }
    
    private boolean hasClientId() {
    	if (clientId!=null) {
    		clientIdSource = "--clientId";
    		return true;
    	}
    	// Check for default client Id
    	if (clientManager!=null) {
    		if (fortnoxClientInfo!=null) {
    			clientId = fortnoxClientInfo.getClientId();
    			clientIdSource = "client in fortnoxClients.xml";
    		}
    		if (clientId==null) {
    			clientId = clientManager.getDefaultClientId();
    			clientIdSource = "FortnoxProperties defaultClientId";
    		}
    	}
    	return clientId!=null;
    }
    
    private boolean hasClientSecret() {
    	if (clientSecret!=null) {
    		return true;
    	}
    	// Check for default client secret
    	if (clientManager!=null) {
    		if (fortnoxClientInfo!=null)
    			clientSecret = fortnoxClientInfo.getClientSecret();
    		if (clientSecret==null)
    			clientSecret = clientManager.getDefaultClientSecret();
    	}
    	return clientSecret!=null;
    }
    
    private boolean hasScope() {
    	if (scopeString!=null) {
    		scopeSource = "--scope";
    		return true;
    	}
    	// Check for scope on client
    	if (clientManager!=null) {
    		if (fortnoxClientInfo!=null) {
    			scopeString = fortnoxClientInfo.getDefaultScope();
    			scopeSource = "client in fortnoxClients.xml";
    		}
    		if (scopeString==null) {
    			scopeString = clientManager.getDefaultScopeString();
    			scopeSource = "FortnoxProperties defaultScope";
    		}
    	}
    	return scopeString!=null;
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

    private void createClient() throws Exception {
    	
    	FortnoxScope scope = new FortnoxScope(scopeString);
    	printConnectionInfo(scope);
    	
    	creator = new Fortnox4jCLI();
    	creator.setClientId(clientId);
    	creator.setClientSecret(clientSecret);
   		creator.setScope(scope);
    	
    	creator.setFortnoxClientManager(clientManager, orgNo);
    	if (creator.hasAuthenticationCode() && !renew) {
    		creator.setGetAccessToken(true);
    	} else {
    		creator.setGetAllTokens(true);
    	}
    	
    	creator.determineCommandToRun();
    	
    	while (creator.isServerRunning()) {
    		Thread.sleep(1000);
    	}
    	
    	if (!creator.hasAccessToken()) {
    		printFailure(creator.hasFailed() ? creator.getLastError() : "No access token received.");
    		return;
    	}
    	
    	// When all tokens are fetched, the client info is already updated by the login handler.
    	FortnoxClientInfo fc = creator.getFortnoxClientInfo();
    	if (fc==null || fc.getOrgName()==null) {
    		fc = creator.updateFortnoxClientInfo();
    	}
    	if (fc==null) {
    		printFailure("No client information available.");
    		return;
    	}
    	if (creator.hasFailed()) {
    		sess.getConsole().println("WARNING: " + creator.getLastError());
    	}
    	if (!creator.getMissingScopes().isEmpty()) {
    		sess.getConsole().println("WARNING: Fortnox did not grant: " + String.join(",", creator.getMissingScopes())
    				+ ". Calls needing these will fail with \"Har inte behörighet för scope\".");
    		sess.getConsole().println("Enable them on the integration (client id " + clientId + ") in Fortnox Developer Portal and run again with --renew.");
    	}
    	sess.getConsole().println((fc.getOrgName()!=null ? fc.getOrgName() : orgNo) + " created" 
    			+ (fc.getOrgName()==null ? ", but the connection could not be verified." : "."));
    	
    }

    private void printFailure(String reason) {
    	sess.getConsole().println("FAILED: Client " + orgNo + " could not be connected.");
    	sess.getConsole().println("Reason: " + reason);
    	sess.getConsole().println("Client ID used: " + clientId + " (from " + clientIdSource + ")");
    	sess.getConsole().println("Run again with --renew to start over with a new authorization code.");
    }
    
    private void printConnectionInfo(FortnoxScope scope) {
    	sess.getConsole().println("Client ID: " + clientId + " (from " + clientIdSource + ")");
    	if (scope.hasScopes()) {
    		sess.getConsole().println("Scope: " + scope.getScopesSeparatedByComma() + " (from " + scopeSource + ")");
    	} else {
    		sess.getConsole().println("WARNING: No valid scopes in [" + scopeString + "] (from " + scopeSource + "). Fortnox4j's built-in default scope will be used.");
    	}
    	if (scope.hasInvalidScopes()) {
    		sess.getConsole().println("WARNING: Invalid scope(s) ignored: " + String.join(", ", scope.getInvalidScopes()));
    	}
    }

}
