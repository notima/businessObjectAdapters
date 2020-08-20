package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.table.TenantTable;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "notima", name = "list-tenants", description = "Lists tenants for given adapter")
@Service
public class ListTenants implements Action {

	@Reference
	private Session sess;
	
	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {
	
		if (bofs==null) {
			System.out.println("No adapters registered");
		} else {
			
			TenantTable tt = null;
			
			for (BusinessObjectFactory bf : bofs) {
				
				sess.getConsole().println(bf.getSystemName());
				
				BusinessPartnerList<Object> bpl = 
						bf.listTenants();
				List<BusinessPartner<Object>> tenants = bpl.getBusinessPartner();
				tt = new TenantTable(tenants);

				tt.print(sess.getConsole());
				
			}
			System.out.println(bofs.size() + " factories registered");
		}
		
		return null;
	}

}
