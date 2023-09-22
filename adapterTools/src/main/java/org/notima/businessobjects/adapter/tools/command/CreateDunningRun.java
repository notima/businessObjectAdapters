package org.notima.businessobjects.adapter.tools.command;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.JAXB;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "notima", name = "create-dunning-run", description = "Create a dunning run file")
@Service
public class CreateDunningRun implements Action {

	public static DateFormat	s_dfmt = new SimpleDateFormat("yyyy-MM-dd");	
	
	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference
	Session sess;
	
	@Argument(index = 0, name = "adapterName", description ="The adapter name", required = true, multiValued = false)
	private String adapterName = "";
	
	@Argument(index = 1, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";
	
	@Option(name = "--duedateuntil", description = "Select invoices with max this due date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String dueDateUntilStr;
	
    @Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo", required = false, multiValued = false)
    private String countryCode = "SE";
	
    @Option(name = "-of", aliases = { "--outfile" }, description = "Write the dunning run to file", required = false, multiValued = false)
    private String outFile;
    
    private Date dueDateUntil;
    
	private BusinessObjectFactory<?,?,?,?,?,?> bof;

	private DunningRun<?,?> dunningRun;
	
	@Override
	public Object execute() throws Exception {
		
		initBusinessObjectFactory();
		
		parseDates();
		
		createDunningRun();
		
		writeToFileIfApplicable();
		
		return dunningRun;
		
	}
	
	
	private void parseDates() throws ParseException {
		
		if (dueDateUntilStr!=null) {
			dueDateUntil = s_dfmt.parse(dueDateUntilStr);
		}
		
	}
	
	private void initBusinessObjectFactory() throws NoSuchTenantException {
	
		bof = cof.lookupAdapter(adapterName);
		bof.setTenant(orgNo, countryCode);
		
	}
	
	private void createDunningRun() throws Exception {
		dunningRun = bof.lookupDunningRun(null, dueDateUntil);
	}
	
	private void writeToFileIfApplicable() {
		
		if (outFile!=null) {
				
			if (!outFile.endsWith(".xml")) {
				outFile += ".xml";
			}
				JAXB.marshal(dunningRun, new File(outFile));
			sess.getConsole().println("Dunning run written to " + outFile);
			
		}		
		
	}
	
}
