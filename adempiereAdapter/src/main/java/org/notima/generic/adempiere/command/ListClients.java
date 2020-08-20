package org.notima.generic.adempiere.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "adempiere", name = "list-adempiere-tenants", description = "Lists current Adempiere tenants")
@Service
public class ListClients implements Action {

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {
	
		if (bofs==null) {
			System.out.println("No factories registered");
		} else {
			for (BusinessObjectFactory bf : bofs) {
				if ("Adempiere".equals(bf.getSystemName())) {
					BusinessPartnerList<?> bpl = 
							bf.listTenants();
					for (BusinessPartner<?> bp : bpl.getBusinessPartner() ) {
						System.out.println(bp.getName());
					}
					
				}
			}
			System.out.println(bofs.size() + " factories registered");
		}
		
		return null;
	}

}
