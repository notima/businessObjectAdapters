package org.notima.businessobjects.adapter.tools.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.notima.businessobjects.adapter.tools.table.BusinessPartnerExtendedTable;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "notima", name = "show-business-partner", description = "Show businesspartner for given adapter and tenant")
@Service
public class ShowBusinessPartner extends AdapterCommand {
	
    @Option(name = _NotimaCmdOptions.ENRICH, description="Enrich data as much as possible. Function depends on adapter", required = false, multiValued = false)
    private boolean	enrich;
    
    @Option(name = "--show-inactive", description="Show inactive business partners. Function depends on adapter", required = false, multiValued = false)
    private boolean	showInactive;
	
    @Argument(index = 2, name = "bpNo", description = "The identification number no of businesspartner", required = true, multiValued = false)
    protected String bpNo;
    
	@Override
	public Object onExecute() throws Exception {

		populateAdapters();
		showBusinessPartner();
		
		return null;
	}

	@SuppressWarnings({ "rawtypes" })
	private void showBusinessPartner() throws Exception {

		if (adaptersToList.size()>0) {
			BusinessPartnerExtendedTable tt = null;
			
			for (BusinessObjectFactory bf : adaptersToList) {

				bf.setTenant(orgNo, countryCode);
				
				bf.setEnrichDataByDefault(enrich);
				
				BusinessPartner<?> bp = bf.lookupBusinessPartner(bpNo);
				
				List<BusinessPartner<?>> bpl = new ArrayList<BusinessPartner<?>>();
				
				if (bp!=null) {
					bpl.add(bp);
					tt = new BusinessPartnerExtendedTable(bpl);
				} else {
					tt = new BusinessPartnerExtendedTable(null);
				}

				tt.getShellTable().print(sess.getConsole());
				
				printableReport = tt;

				initAndRunReportFormatter();
				
			}
				
		}
		
	}
	
}
