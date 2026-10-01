package org.notima.businessobjects.adapter.tools.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.AdapterToolsSettings;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.command.completer.AdapterCompleter;
import org.notima.businessobjects.adapter.tools.table.TenantTable;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;

@Command(scope = "notima", name = "list-tenants", description = "Lists tenants for given adapter")
@Service
public class ListTenants implements Action {

	@Reference
	private Session sess;
	
	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;

	@Reference
	private CanonicalObjectFactory cof;

	@Reference
	private AdapterToolsSettings settings;

	@Option(name = "-i", aliases = { "--with-info" }, description = "Show stored tenant information (output and report directory)", required = false, multiValued = false)
	private boolean withInfo;
	
    @Argument(index = 0, name = "adapter", description = "The adapter to use", required = false, multiValued = false)
    @Completion(AdapterCompleter.class)
    private String systemName;
	
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
			
			
			TenantTable tt = null;
			TenantInformationFactory tif = withInfo ? cof.lookupTenantInformationFactory() : null;
			if (withInfo && tif==null) {
				sess.getConsole().println("No TenantInformationFactory available. Tenant information not shown.");
			}
			
			for (BusinessObjectFactory bf : adaptersToList) {
				
				@SuppressWarnings("unchecked")
				BusinessPartnerList<Object> bpl = 
						bf.listTenants();
				if (bpl!=null) {
					List<BusinessPartner<Object>> tenants = bpl.getBusinessPartner();
					tt = new TenantTable(tenants, withInfo, tif, settings.getDefaultCountryCode());
				} else {
					tt = new TenantTable(null, withInfo, tif, settings.getDefaultCountryCode());
				}
				tt.setAdapterName(bf.getSystemName());

				tt.getShellTable().print(sess.getConsole());
				
			}
				
		}
		
		return null;
	}

}
