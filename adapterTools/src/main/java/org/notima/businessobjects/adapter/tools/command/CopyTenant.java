package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.notima.businessobjects.adapter.tools.command.completer.AdapterCompleter;
import org.notima.businessobjects.adapter.tools.exception.AdapterNotFoundException;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "notima", name = "copy-tenant", description = "Copy tenant information from one adapter to another")
@Service
public class CopyTenant implements Action {

	@SuppressWarnings("rawtypes")
	@Reference
	protected List<BusinessObjectFactory> bofs;
	
    @Option(name = "--country-code", description="Country code if it needs to be specified", required = false, multiValued = false)
    protected String countryCode;
	
    @Argument(index = 0, name = "fromAdapter", description = "The from adapter to use", required = true, multiValued = false)
    @Completion(AdapterCompleter.class)
    protected String fromAdapter;
    
    @Argument(index = 1, name = "toAdapter", description = "The target adapter to copy to", required = true, multiValued = false)
    @Completion(AdapterCompleter.class)
    protected String toAdapter;

    @Argument(index = 2, name = "orgNo", description = "The org no of the tenant", required = true, multiValued = false)
    protected String orgNo;

    @SuppressWarnings("rawtypes")
	private BusinessObjectFactory fromBof;
    @SuppressWarnings("rawtypes")    
    private BusinessObjectFactory toBof;
    
    @SuppressWarnings("rawtypes")
	private BusinessPartner	tenant;
    
	@Override
	public Object execute() throws Exception {

		initFromAdapter();
		initToAdapter();
		copyTenant();
		
		return null;
	}
	
	
	private void copyTenant() throws NoSuchTenantException {
		
		fromBof.setTenant(orgNo, countryCode);
		tenant = fromBof.getCurrentTenant();
		
		try {
			toBof.setTenant(orgNo, countryCode);
		} catch (NoSuchTenantException nte) {
			toBof.addTenant(orgNo, countryCode, tenant.getName(), null);
		}
		
	}
	
	
	@SuppressWarnings("rawtypes")
	private BusinessObjectFactory getFactoryFor(String adapter) throws AdapterNotFoundException {
		BusinessObjectFactory bf = null;
		for (BusinessObjectFactory bff : bofs) {
			if (adapter.equalsIgnoreCase(bff.getSystemName())) {
				bf = bff;
				break;
			}
		}
		if (bf==null) throw(new AdapterNotFoundException(adapter));
		return bf;
	}
	
	private void initFromAdapter() throws AdapterNotFoundException {
		fromBof = getFactoryFor(fromAdapter);
	}
	
	private void initToAdapter() throws AdapterNotFoundException {
		toBof = getFactoryFor(toAdapter);
	}
	
	
}
