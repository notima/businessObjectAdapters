package org.notima.businessobjects.adapter.tools.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

public abstract class AdapterCommand extends AbstractAction {

	@SuppressWarnings("rawtypes")
	@Reference
	protected List<BusinessObjectFactory> bofs;
	
    @Argument(index = 0, name = "adapter", description = "The adapter to use", required = true, multiValued = false)
    protected String systemName;

    @Argument(index = 1, name = "orgNo", description = "The org no of the tenant", required = true, multiValued = false)
    protected String orgNo;

    @SuppressWarnings("rawtypes")
	protected List<BusinessObjectFactory> adaptersToList;
    
    protected void populateAdapters() {
    	
		adaptersToList = new ArrayList<BusinessObjectFactory>();
		if (bofs!=null) {
			for (BusinessObjectFactory bf : bofs) {
				
				if (systemName==null || systemName.equals(bf.getSystemName())) {
					adaptersToList.add(bf);
				}
				
			}
		}
    	
    }
    
}
