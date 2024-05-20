package org.notima.fortnox.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.PreDefinedAccount;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;

@Command(scope = "fortnox", name = "update-fortnox-predefined-account", description = "Updates a pre-defined account")
@Service
public class UpdatePreDefinedAccount extends FortnoxCommand implements Action {
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";
	
	@Argument(index = 1, name = "predefinition", description ="The pre-definition to update", required = true, multiValued = false)
	private String preDef = "";

	@Argument(index = 2, name = "accountNo", description ="The orgno of the client", required = true, multiValued = false)
	private Integer accountNo;

	@Override
	public Object execute() throws Exception {
		
		FortnoxClient3 fc = getFortnoxClient(orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}

		PreDefinedAccount result = fc.updatePreDefinedAccount(preDef, accountNo);
		
		sess.getConsole().println(result.getName() + " => " + accountNo);
		
		return null;
	}
	
}
