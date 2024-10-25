package org.notima.businessobjects.adapter.tools.command;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import jakarta.xml.bind.JAXB;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.MappingServiceFactory;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceList;
import org.notima.generic.businessobjects.OrderInvoiceOperationResult;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.businessobjects.util.SetSpecificPriceInvoiceLineValidator;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.MappingService;
import org.notima.generic.ifacebusinessobjects.MappingServiceInstanceFactory;

@Command(scope = "notima", name = "read-invoices", description = "Reads invoices from an adapter and writes them to the destination adapter (or XML-file if no adapter is specified")
@Service
public class ReadInvoices extends AbstractAction {
	
	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference
	private MappingServiceFactory mappingFactory;

    @Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo", required = false, multiValued = false)
    private String countryCode;
	
    @Option(name="--from-date", description="From date", required = false, multiValued = false)
    private String	fromDateStr;

    @Option(name="--until-date", description="Until date", required = false, multiValued = false)
    private String	untilDateStr;

    @Option(name="--create-limit", description="Create limit.", required = false, multiValued = false)
    private Integer	createLimit;
    
    @Option(name="--unit-price", description="Price per unit, unless specified in source", required = false, multiValued = false)
    private Double  unitPrice;
    
	@Option(name = "--price-includes-tax", description = "If price per unit contains tax", required = false, multiValued = false)
	private boolean priceIncludesTax;

	@Option(name = "--taxPercent", description = "Used in conjunction with --price-includes-tax", required = false, multiValued = false)
	private double taxPercent;
    
    @Option(name="--apartment-mapping-service", description="Apartment to customer mapping service to use", required = false, multiValued = false)
    private String  apartmentMappingService;
    
	@Argument(index = 0, name = "adapterName", description ="The source adapter name", required = true, multiValued = false)
	private String adapterName = "";

    @Argument(index = 1, name = "orgNo", description = "The org number of the tenant to read from", required = true, multiValued = false)
    private String orgNo;
	
	@Argument(index = 2, name = "invoiceFile", description ="The canonical invoice to write to (xml-format)", required = true, multiValued = false)
	@Completion(FileCompleter.class)   
	private String invoiceFile = "";
	
	private BusinessObjectFactory<?,?,?,?,?,?> adapter;
	private OrderInvoiceOperationResult invoiceResult;
	
	private MappingService mappingService = null;
	
	private Date	fromDate;
	private Date	untilDate;
	
	@Override
	protected Object onExecute() throws Exception {
		
		initBusinessObjectFactory();
		parseOptions();
		readInvoices();
		updateUnitPrice();
		remapCustomerIds();
		writeInvoicesToXmlFile();
		
		return null;
	}
	
	private void initBusinessObjectFactory() throws Exception {
		adapter = cof.lookupAdapter(adapterName);
		
		adapter.setTenant(orgNo, countryCode);
		
	}

	private void writeInvoicesToXmlFile() throws IOException {
		
		FileOutputStream fis = new FileOutputStream(invoiceFile);
		JAXB.marshal(invoiceResult.getAffectedInvoices(), fis);
		fis.close();
		
	}
	
	private void parseOptions() throws ParseException, NoSuchTenantException, Exception {
		
		if (fromDateStr!=null) {
			fromDate = dfmt.parse(fromDateStr);
		}
		if (untilDateStr!=null) {
			untilDate = dfmt.parse(untilDateStr);
		}
		
		if (createLimit==null) createLimit = 0;
		
		initiateMapper();
		
	}
	
	private void initiateMapper() throws NoSuchTenantException, Exception {
		
		if (apartmentMappingService!=null) {
			MappingServiceInstanceFactory instanceFactory = mappingFactory.getMappingServiceFor(apartmentMappingService);
			if (instanceFactory!=null) {
				mappingService = instanceFactory.getMappingService(new TaxSubjectIdentifier(orgNo, countryCode));
			} else {
				throw new Exception("No mapping service for " + apartmentMappingService + " found.");
			}
		}
		
	}
	
	
	private void readInvoices() throws Exception {
		
		invoiceResult = adapter.readInvoices(fromDate, untilDate, createLimit);
		
	}
	
	private void remapCustomerIds() {
		if (mappingService!=null) {
			mapFromApartmentsToCustomer();
		}
	}
	
	private void mapFromApartmentsToCustomer() {
		
		InvoiceList invoiceList = invoiceResult.getAffectedInvoices();
		TaxSubjectIdentifier resultCustomer;
		for (Invoice<?> il : invoiceList.getInvoiceList()) {
			resultCustomer = mappingService.mapApartmentNoToTaxSubject(il.getBusinessPartner().getIdentityNo());
			if (resultCustomer!=null && !resultCustomer.isUndefined()) {
				il.getBusinessPartner().setTaxId(resultCustomer.getTaxId());
			}
		}
		
	}
	
	
	private void updateUnitPrice() {
		
		if (unitPrice!=null) {

			SetSpecificPriceInvoiceLineValidator validator = new SetSpecificPriceInvoiceLineValidator(unitPrice, priceIncludesTax, priceIncludesTax, taxPercent);
			
			InvoiceList invoiceList = invoiceResult.getAffectedInvoices();
			for (Invoice<?> il : invoiceList.getInvoiceList()) {
				il.setOrderInvoiceLineValidator(validator);
				il.getInvalidLines();
				il.calculateGrandTotal();

			}
			
		}
		
	}
	
	
	
}
