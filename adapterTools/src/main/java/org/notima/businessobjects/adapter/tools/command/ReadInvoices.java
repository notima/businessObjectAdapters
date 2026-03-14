package org.notima.businessobjects.adapter.tools.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.JAXB;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.MappingServiceFactory;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceList;
import org.notima.generic.businessobjects.OrderInvoiceOperationResult;
import org.notima.generic.businessobjects.OrderInvoiceReaderOptions;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.businessobjects.util.SetSpecificPriceInvoiceLineValidator;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.MappingService;
import org.notima.generic.ifacebusinessobjects.MappingServiceInstanceFactory;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;
import org.notima.util.LocalDateUtils;

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
	@Completion(OrgNoCompleter.class)
    private String orgNo;

	@Argument(index = 2, name = "invoiceFile", description ="The canonical invoice to write to (xml-format). If omitted, the file is auto-generated using the default output directory configured for the tenant.", required = false, multiValued = false)
	@Completion(FileCompleter.class)
	private String invoiceFile;

	private BusinessObjectFactory<?,?,?,?,?,?> adapter;
	private OrderInvoiceReaderOptions readerOptions;
	private OrderInvoiceOperationResult invoiceResult;

	private boolean	unpostedOnly = true;
	private boolean salesOnly = true;

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

		String destination = resolveInvoiceFile();

		// Remove references to any native formats
		invoiceResult.canonize();

		FileOutputStream fis = new FileOutputStream(destination);
		JAXB.marshal(invoiceResult.getAffectedInvoices(), fis);
		fis.close();

	}

	private String resolveInvoiceFile() throws IOException {
		if (invoiceFile != null && invoiceFile.trim().length() > 0) {
			return invoiceFile;
		}
		TaxSubjectIdentifier tenantId = new TaxSubjectIdentifier(orgNo, countryCode);
		TenantInformationFactory tif = cof.lookupFirstTenantInformationFactory();
		if (tif != null) {
			TenantInformation ti = tif.getTenantInformation(tenantId);
			if (ti != null && ti.getDefaultOutputDirectory() != null && ti.getDefaultOutputDirectory().trim().length() > 0) {
				String tenantName = (ti.getTenant() != null && ti.getTenant().hasName())
						? ti.getTenant().getLegalName().replaceAll("[^a-zA-Z0-9_\\-]", "_")
						: orgNo;
				String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
				File dir = new File(ti.getDefaultOutputDirectory());
				dir.mkdirs();
				return new File(dir, tenantName + "-" + dateStr + ".xml").getPath();
			}
		}
		throw new IOException("No invoiceFile specified and no default output directory configured for tenant " + orgNo);
	}

	private void parseOptions() throws ParseException, NoSuchTenantException, Exception {

		readerOptions = new OrderInvoiceReaderOptions();

		if (fromDateStr!=null) {
			fromDate = dfmt.parse(fromDateStr);
			readerOptions.setFromDate(LocalDateUtils.asLocalDate(fromDate));
		}
		if (untilDateStr!=null) {
			untilDate = dfmt.parse(untilDateStr);
			readerOptions.setUntilDate(LocalDateUtils.asLocalDate(untilDate));
		}

		if (createLimit==null) createLimit = 0;
		readerOptions.setReadLimit(createLimit);

		readerOptions.setSalesOnly(salesOnly);
		readerOptions.setUnpostedOnly(unpostedOnly);

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

		invoiceResult = adapter.readInvoices(readerOptions);

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
