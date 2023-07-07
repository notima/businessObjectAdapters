package org.notima.fortnox.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.fortnox.command.table.ClientTable;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "list-fortnox-clients", description = "Lists current Fortnox clients")
@Service
public class ListClients implements Action {
	
	@Option(name = "--credentials", description ="Show credentials information")
	private boolean showCredentialsInfo = false;

	@Reference
	private Session sess;
	
	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {
	
		if (bofs==null) {
			sess.getConsole().println("No Fortnox factories registered");
		} else {
			for (BusinessObjectFactory bf : bofs) {
				if ("Fortnox".equals(bf.getSystemName())) {
					BusinessPartnerList<?> bpl = 
							bf.listTenants();
					
					ClientTable tbl = new ClientTable(bpl, showCredentialsInfo);
					tbl.print(sess.getConsole());
				}
			}
		}
		
		return null;
	}

}
