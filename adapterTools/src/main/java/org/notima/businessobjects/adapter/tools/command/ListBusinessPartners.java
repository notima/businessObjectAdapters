package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.notima.businessobjects.adapter.tools.table.BusinessPartnerTable;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "notima", name = "list-business-partners", description = "Lists business partners for given adapter and tenant")
@Service
public class ListBusinessPartners extends AdapterCommand {
	
	
	
	@Override
	public Object onExecute() throws Exception {

		populateAdapters();
		listBusinessPartners();
		
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void listBusinessPartners() throws Exception {

		if (adaptersToList.size()>0) {
			BusinessPartnerTable tt = null;
			
			for (BusinessObjectFactory bf : adaptersToList) {
				
				List<BusinessPartner<?>> bpl = 
						bf.lookupAllBusinessPartners();
				if (bpl!=null) {
					tt = new BusinessPartnerTable(bpl);
				} else {
					tt = new BusinessPartnerTable(null);
				}

				tt.getShellTable().print(sess.getConsole());
				
				printableReport = tt;

				initAndRunReportFormatter();
				
			}
				
		}
		
	}
	
}
