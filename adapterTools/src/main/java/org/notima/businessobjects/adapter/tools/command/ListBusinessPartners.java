package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.notima.businessobjects.adapter.tools.table.TenantTable;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "notima", name = "list-business-partners", description = "Lists business partners for given adapter and tenant")
@Service
public class ListBusinessPartners extends AdapterCommand {
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object onExecute() throws Exception {

		populateAdapters();
		listBusinessPartners();
		
		return null;
	}

	private void listBusinessPartners() {

		if (adaptersToList.size()>0) {
			TenantTable tt = null;
			
			for (BusinessObjectFactory bf : adaptersToList) {
				
				BusinessPartnerList<Object> bpl = 
						bf.listTenants();
				if (bpl!=null) {
					List<BusinessPartner<Object>> tenants = bpl.getBusinessPartner();
					tt = new TenantTable(tenants);
				} else {
					tt = new TenantTable(null);
				}
				tt.setAdapterName(bf.getSystemName());

				tt.getShellTable().print(sess.getConsole());
				
			}
				
		}
		
	}
	
}
