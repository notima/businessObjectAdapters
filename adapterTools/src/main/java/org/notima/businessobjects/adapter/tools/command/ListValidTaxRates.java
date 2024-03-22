package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.table.TaxRateTable;
import org.notima.generic.businessobjects.Tax;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.businessobjects.exception.TaxRatesNotAvailableException;
import org.notima.generic.ifacebusinessobjects.TaxRateProvider;

@Command(scope = "notima", name = "list-valid-tax-rates", description = "List valid tax rates for a specific tenant")
@Service
public class ListValidTaxRates implements Action {

	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference 
	Session sess;

	@Argument(index = 0, name = "adapterName", description ="The adapter name", required = true, multiValued = false)
	private String adapterName = "";
	
	@Argument(index = 1, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";
	
    @Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo", required = false, multiValued = false)
    private String countryCode;
	
    private TaxRateProvider trp;
    private List<Tax> validTaxRates;
    private TaxSubjectIdentifier tsi;
    
	@Override
	public Object execute() throws Exception {
		
		setTaxSubject();
		
		getValidRates();
		
		TaxRateTable table = new TaxRateTable(validTaxRates);
		table.getShellTable().print(sess.getConsole());
		
		return null;
	}
	
	private void setTaxSubject() {
		
		tsi = new TaxSubjectIdentifier(orgNo, countryCode);
		trp = cof.lookupTaxRateProvider(adapterName);
		
	}
	
	
	private void getValidRates() throws NoSuchTenantException, TaxRatesNotAvailableException {
		
		validTaxRates = trp.getValidTaxRates(tsi, null); 
		
	}
	
	
	
}
