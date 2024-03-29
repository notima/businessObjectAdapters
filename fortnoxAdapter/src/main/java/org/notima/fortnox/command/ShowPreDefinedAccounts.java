package org.notima.fortnox.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.PreDefinedAccountSubset;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;
import org.notima.fortnox.command.table.PreDefinedAccountTable;

@Command(scope = "fortnox", name = "show-fortnox-predefined-accounts", description = "List predefined accounts for given client.")
@Service
public class ShowPreDefinedAccounts extends FortnoxCommand implements Action {
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";

	@Override
	public Object execute() throws Exception {
		
		FortnoxClient3 fc = getFortnoxClient(orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}

		Map<String, PreDefinedAccountSubset> paccts = fc.getPredefinedAccountMap();
		
		List<Object> paList = new ArrayList<Object>();
		for (String s : paccts.keySet()) {
			paList.add(paccts.get(s));
		}
		
		PreDefinedAccountTable at = new PreDefinedAccountTable(paList);
		
		at.print(sess.getConsole());
		
		return null;
	}

	
	
	
	
}
