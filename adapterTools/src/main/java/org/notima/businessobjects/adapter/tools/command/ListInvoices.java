package org.notima.businessobjects.adapter.tools.command;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
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
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceList;
import org.notima.generic.businessobjects.OrderInvoiceOperationResult;
import org.notima.generic.businessobjects.OrderInvoiceReaderOptions;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.businessobjects.util.SetSpecificPriceInvoiceLineValidator;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.MappingService;
import org.notima.generic.ifacebusinessobjects.MappingServiceInstanceFactory;
import org.notima.util.LocalDateUtils;

@Command(scope = "notima", name = "list-invoices", description = "Lists invoices from an adapter. See also read-invoices.")
@Service
public class ListInvoices extends AbstractAction {
	
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
    
	@Argument(index = 0, name = "adapterName", description ="The source adapter name", required = true, multiValued = false)
	private String adapterName = "";

    @Argument(index = 1, name = "orgNo", description = "The org number of the tenant to read from", required = true, multiValued = false)
    private String orgNo;
	
	private BusinessObjectFactory<?,?,?,?,?,?> adapter;
	private OrderInvoiceReaderOptions readerOptions;
	private OrderInvoiceOperationResult invoiceResult;
	
	
	private Date	fromDate;
	private Date	untilDate;
	
	@Override
	protected Object onExecute() throws Exception {
		
		initBusinessObjectFactory();
		parseOptions();
		readInvoices();
		
		return null;
	}
	
	private void initBusinessObjectFactory() throws Exception {
		adapter = cof.lookupAdapter(adapterName);
		
		adapter.setTenant(orgNo, countryCode);
		
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
		
		
	}
	
	private void readInvoices() throws Exception {
		
		invoiceResult = adapter.readInvoices(readerOptions);
		
	}
	
	
}
