package org.notima.businessobjects.adapter.infometric.command;

import java.util.List;
import java.util.Properties;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.infometric.InfometricAdapter;
import org.notima.businessobjects.adapter.infometric.InfometricTenant;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * 
 * Adds an Infometric client.
 * 
 * @author Daniel Tamm
 *
 */
@Command(scope = _InfometricCommandNames.SCOPE, name = "add-infometric-client", description = "Add a client to Infometric integration")
@Service
public class AddClient implements Action {

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Reference
	private Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client to configure", required = false, multiValued = false)
	String orgNo;

    @Argument(index = 1, name = "tenantDirectory", description = "The directory where IMD-files are stored for this client", required = false, multiValued = false)
    String tenantDirectory;
   
    @Option(name = "--orgName", description = "The name of the organisation", required = false, multiValued = false)
    private String orgName;
    
    private InfometricAdapter fa;
    
	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {

		BusinessObjectFactory b = null;
		for (BusinessObjectFactory bf : bofs) {
			if (InfometricAdapter.SYSTEM.equals(bf.getSystemName())) {
				b = bf;
				break;
			}
		}

		if (b==null) {
			sess.getConsole().println("No Infometric adapter available");
			return null;
		}
		
		fa = (InfometricAdapter)b;
		
		if (orgNo==null) {
			sess.getConsole().println("OrgNo must be supplied when there are no working credentials");
			return null;
		}
		
		Properties props = new Properties();
		if (tenantDirectory!=null)
			props.setProperty(InfometricAdapter.PROP_TENANTDIRECTORY, tenantDirectory);
		
		BusinessPartner<InfometricTenant> bp = fa.addTenant(orgNo, "SE", orgName, props);

		if (bp!=null) {
			sess.getConsole().println("Tenant [" + bp.getTaxId() + "] " + bp.getName() + " added.");
		}
		return null;
	}
	
}
