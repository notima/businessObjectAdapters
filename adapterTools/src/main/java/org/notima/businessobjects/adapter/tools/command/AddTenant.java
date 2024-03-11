package org.notima.businessobjects.adapter.tools.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "notima", name = "add-tenant", description = "Adds tenant for given adapter")
@Service
public class AddTenant implements Action {

	@Reference
	private Session sess;
	
	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
    @Argument(index = 0, name = "adapter", description = "The adapter to use", required = true, multiValued = false)
    private String systemName;
    
    @Argument(index = 1, name = "orgNo", description = "Org no of the tenant to add", required = true, multiValued = false)
    private String orgNo;

    @Argument(index = 2, name = "countryCode", description = "The country code", required = true, multiValued = false)
    private String countryCode;

    @Argument(index = 3, name = "tenantName", description = "The name of the tenant", required = true, multiValued = false)
    private String tenantName;
    
	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {
	
		if (bofs==null) {
			System.out.println("No adapters registered");
		} else {

			List<BusinessObjectFactory> adaptersToList = new ArrayList<BusinessObjectFactory>();

			for (BusinessObjectFactory bf : bofs) {
				
				if (systemName==null || systemName.equals(bf.getSystemName())) {
					adaptersToList.add(bf);
				}
				
			}
			
			for (BusinessObjectFactory bf : adaptersToList) {
				
				sess.getConsole().println(bf.getSystemName());

				bf.addTenant(orgNo, countryCode, tenantName, null);
				
			}
				
		}
		
		return null;
	}

}
