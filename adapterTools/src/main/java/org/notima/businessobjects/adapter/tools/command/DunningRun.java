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
import org.notima.generic.businessobjects.OrderInvoiceReaderOptions;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.util.LocalDateUtils;

@Command(scope = "notima", name = "dunning-run", description = "Creates a dunning run for given adapter and writes to xml-file")
@Service
public class DunningRun extends AbstractAction {
	
	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference
	private MappingServiceFactory mappingFactory;

    @Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo", required = false, multiValued = false)
    private String countryCode;

    @Option(name="--until-date", description="Until date", required = false, multiValued = false)
    private String	untilDateStr;
    
	@Argument(index = 0, name = "adapterName", description ="The source adapter name", required = true, multiValued = false)
	private String adapterName = "";

    @Argument(index = 1, name = "orgNo", description = "The org number of the tenant to read from", required = true, multiValued = false)
    private String orgNo;
	
	@Argument(index = 2, name = "dunningFile", description ="The canonical invoice to write to (xml-format)", required = true, multiValued = false)
	@Completion(FileCompleter.class)   
	private String invoiceFile = "";
	
	private BusinessObjectFactory<?,?,?,?,?,?> adapter;
	private OrderInvoiceReaderOptions readerOptions;
	private org.notima.generic.businessobjects.DunningRun<?, ?> dunningRun;
	
	private Date	untilDate;
	
	@Override
	protected Object onExecute() throws Exception {
		
		initBusinessObjectFactory();
		parseOptions();
		createDunningRun();
		writeDunningRunToXmlFile();
		
		return null;
	}
	
	private void initBusinessObjectFactory() throws Exception {
		adapter = cof.lookupAdapter(adapterName);
		
		adapter.setTenant(orgNo, countryCode);
		
	}

	private void writeDunningRunToXmlFile() throws IOException {

		// Remove references to any native formats
		// dunningRun.canonize();
		
		FileOutputStream fis = new FileOutputStream(invoiceFile);
		JAXB.marshal(dunningRun, fis);
		fis.close();
		
	}
	
	private void parseOptions() throws ParseException, NoSuchTenantException, Exception {
		
		readerOptions = new OrderInvoiceReaderOptions();		
		
		if (untilDateStr!=null) {
			untilDate = dfmt.parse(untilDateStr);
			readerOptions.setUntilDate(LocalDateUtils.asLocalDate(untilDate));
		}
		
	}
	
	
	
	private void createDunningRun() throws Exception {
		
		dunningRun = adapter.lookupDunningRun(null, untilDate);
		
	}
	
	
	
	
}
