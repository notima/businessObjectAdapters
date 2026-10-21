package org.notima.fortnox.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.clients.FortnoxClientInfo;
import org.notima.api.fortnox.clients.FortnoxClientManager;
import org.notima.api.fortnox.entities3.CompanySetting;
import org.notima.businessobjects.adapter.fortnox.exception.InsufficientOrWrongInputException;

@Command(scope = _FortnoxCommandNames.SCOPE, name = _FortnoxCommandNames.SetClientName, description = "Set or change the name of a Fortnox client")
@Service
public class SetClientName extends FortnoxCommand implements Action {

	@Reference
	private Session sess;

	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo;

	@Argument(index = 1, name = "name", description ="The new name of the client. If omitted, the name is fetched from Fortnox.", required = false, multiValued = false)
	private String name;

	private FortnoxClientManager clientManager;
	private FortnoxClientInfo clientInfo;

	@Override
	public Object execute() throws Exception {

		try {
			initClientInfo();
			if (name==null) {
				fetchNameFromFortnox();
			}
			updateName();
		} catch (InsufficientOrWrongInputException ii) {
			sess.getConsole().println(ii.getMessage());
		}

		return null;
	}

	private void initClientInfo() throws Exception {

		FortnoxClient3 client = getFortnoxClient(orgNo);
		if (bf==null || client==null) {
			throw new InsufficientOrWrongInputException("Can't get client for " + orgNo);
		}
		clientManager = bf.getClientManager();
		if (clientManager==null) {
			throw new InsufficientOrWrongInputException("No Fortnox Client Manager found.");
		}
		clientInfo = clientManager.getClientInfoByOrgNo(orgNo);
		if (clientInfo==null) {
			throw new InsufficientOrWrongInputException("No client info found for " + orgNo);
		}

	}

	private void fetchNameFromFortnox() throws Exception {

		CompanySetting cs = bf.getClient().getCompanySetting();
		if (cs==null || cs.getName()==null || cs.getName().trim().length()==0) {
			throw new InsufficientOrWrongInputException("Could not get company name from Fortnox for " + orgNo + ". Specify the name manually.");
		}
		name = cs.getName();

	}

	private void updateName() throws Exception {

		String oldName = clientInfo.getOrgName();
		clientInfo.setOrgName(name);
		clientManager.updateAndSaveClientInfo(clientInfo);
		sess.getConsole().println(orgNo + ": name changed from " + oldName + " to " + name);

	}

}
