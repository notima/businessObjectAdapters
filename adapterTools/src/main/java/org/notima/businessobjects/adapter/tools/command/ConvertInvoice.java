package org.notima.businessobjects.adapter.tools.command;

import java.io.File;
import java.io.StringWriter;

import javax.xml.bind.JAXB;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.businessobjects.Invoice;

@Command(scope = "notima", name = "convert-invoice", description = "Converts an invoice to an adapter format")
@Service
public class ConvertInvoice implements Action {

	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference 
	Session sess;

	@Argument(index = 0, name = "adapterName", description ="The destination adapter name", required = true, multiValued = false)
	private String adapterName = "";
	
	@Argument(index = 1, name = "invoiceFile", description ="The canonical invoice file (xml-format)", required = true, multiValued = false)
	private String invoiceFile = "";
    
    @Option(name = "-of", aliases = { "--outfile" }, description = "Write the converted invoice to file", required = false, multiValued = false)
    private String outFile;
	
	@Override
	public Object execute() throws Exception {

		
		
		Object invoice = cof.convertToNativeInvoice(adapterName, null, null);
		
		if (invoice==null) {
			sess.getConsole().println("Invoice not found");
			return null;
		}
		
		if (outFile!=null) {
			if (!outFile.endsWith(".xml")) {
				outFile += ".xml";
			}
			JAXB.marshal(invoice, new File(outFile));
			sess.getConsole().println("Invoice written to " + outFile);
		} else {
			StringWriter sw = new StringWriter();
			JAXB.marshal(invoice,  sw);
			sess.getConsole().println(sw.toString());
		}
		
		return null;
	}
	
	
}
