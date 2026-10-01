package org.notima.businessobjects.adapter.tools.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXB;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.businessobjects.adapter.tools.AdapterToolsSettings;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.DocumentNoSelection;
import org.notima.businessobjects.adapter.tools.MappingServiceFactory;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.generic.businessobjects.BusinessPartner;
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

	@Reference
	private AdapterToolsSettings settings;

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

	@Option(name = "--vendor", description = "Read vendor invoices instead of sales invoices", required = false, multiValued = false)
	private boolean vendor;

	@Option(name = "--invoices", description = "Read only these invoices, ie 105723 or 1,4-5,9,15-20. "
			+ "Single quote invoice numbers containing commas, hyphens or spaces, ie \"'F-100'-'F-120'\". "
			+ "Date and unposted filters don't apply.", required = false, multiValued = false)
	private String invoiceSelection;

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

	/** Max number of invoices that can be read using --invoices */
	private static final int MAX_SELECTED_INVOICES = 10000;

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
		completeCreditorPaymentInformation();
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
		sess.getConsole().println(invoiceResult.getAffectedInvoices().getInvoiceList().size() + " invoice(s) written to " + destination);

	}

	private String resolveInvoiceFile() throws IOException {
		if (invoiceFile != null && invoiceFile.trim().length() > 0) {
			return invoiceFile;
		}
		TenantInformation ti = lookupTenantInformation();
		if (ti != null && ti.getDefaultOutputDirectory() != null && ti.getDefaultOutputDirectory().trim().length() > 0) {
			String tenantName = (ti.getTenant() != null && ti.getTenant().hasName())
					? ti.getTenant().getLegalName().replaceAll("[^a-zA-Z0-9_\\-]", "_")
					: orgNo;
			String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
			File dir = new File(ti.getDefaultOutputDirectory());
			dir.mkdirs();
			return new File(dir, tenantName + "-" + dateStr + ".xml").getPath();
		}
		throw new IOException("No invoiceFile specified and no default output directory configured for tenant " + getTenantId());
	}

	private TaxSubjectIdentifier getTenantId() {
		String effectiveCountryCode = (countryCode != null && !countryCode.trim().isEmpty())
				? countryCode.trim()
				: settings.getDefaultCountryCode();
		return new TaxSubjectIdentifier(orgNo.trim(), effectiveCountryCode);
	}

	/**
	 * @return	The stored tenant information, or null if there is none.
	 */
	private TenantInformation lookupTenantInformation() {
		TenantInformationFactory tif = cof.lookupTenantInformationFactory();
		return tif != null ? tif.getTenantInformation(getTenantId()) : null;
	}

	/**
	 * Sets payment information on the creditor of sales invoices, so that complete invoices
	 * can be created from the file. Payment information in the tenant information overrides
	 * any payment information supplied by the adapter.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void completeCreditorPaymentInformation() {

		if (vendor) return;

		InvoiceList invoiceList = invoiceResult.getAffectedInvoices();
		BusinessPartner creditor = invoiceList.getCreditor();

		TenantInformation ti = lookupTenantInformation();
		if (ti == null || !ti.hasPaymentInformation()) {
			if (creditor == null || !hasPaymentInformation(creditor)) {
				sess.getConsole().println("Warning: No payment information found for the creditor. Set remitToAccount or remitToIBAN using set-tenant-info.");
			}
			return;
		}

		if (creditor == null) {
			TaxSubjectIdentifier tenantId = getTenantId();
			creditor = new BusinessPartner();
			creditor.setCompany(true);
			creditor.setTaxId(tenantId.getTaxId());
			creditor.setCountryCode(tenantId.getCountryCode());
			creditor.setName(ti.getTenant() != null ? ti.getTenant().getLegalName() : null);
			invoiceList.setCreditor(creditor);
		}
		ti.copyPaymentInformationTo(creditor);
		sess.getConsole().println("Payment information for the creditor taken from tenant information.");
	}

	private boolean hasPaymentInformation(BusinessPartner<?> bp) {
		return (bp.getRemitToAccount() != null && bp.getRemitToAccount().trim().length() > 0)
				|| (bp.getRemitToIBAN() != null && bp.getRemitToIBAN().trim().length() > 0);
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

		readerOptions.setSalesOnly(salesOnly && !vendor);
		readerOptions.setVendorOnly(vendor);
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

		if (invoiceSelection!=null) {
			invoiceResult = readSelectedInvoices();
		} else if (vendor) {
			invoiceResult = adapter.readVendorInvoices(readerOptions);
		} else {
			invoiceResult = adapter.readInvoices(readerOptions);
		}
		if (invoiceResult==null) {
			throw new Exception("Adapter " + adapterName + " doesn't support reading invoices");
		}

	}

	/**
	 * Looks up each invoice in the selection.
	 */
	private OrderInvoiceOperationResult readSelectedInvoices() throws Exception {

		List<String> invoiceNos = DocumentNoSelection.parse(invoiceSelection).expand(MAX_SELECTED_INVOICES);

		OrderInvoiceOperationResult result = new OrderInvoiceOperationResult();
		List<String> notFound = new ArrayList<String>();
		Invoice<?> invoice;
		for (String invoiceNo : invoiceNos) {
			invoice = vendor ? adapter.lookupVendorInvoice(invoiceNo) : adapter.lookupInvoice(invoiceNo);
			if (invoice!=null) {
				result.addAffectedInvoice(invoice);
			} else {
				notFound.add(invoiceNo);
			}
		}
		if (!notFound.isEmpty()) {
			sess.getConsole().println("Not found (" + notFound.size() + "): " + String.join(", ", notFound));
		}

		BusinessPartner<?> creditor = adapter.lookupThisCompanyInformation();
		if (creditor!=null) {
			result.getAffectedInvoices().setCreditor(creditor);
		}
		result.setSuccessful(true);
		return result;
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
